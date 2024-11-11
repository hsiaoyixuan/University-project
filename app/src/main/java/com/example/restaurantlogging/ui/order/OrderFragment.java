package com.example.restaurantlogging.ui.order;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantlogging.R;
import com.example.restaurantlogging.databinding.FragmentOrderBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderFragment extends Fragment implements OrderAdapter.OnOrderActionListener {

    private static final String CHANNEL_ID = "order_notification_channel";
    private DatabaseReference ordersRef;
    private OrderAdapter orderAdapter;
    private List<Map<String, Object>> orderList;
    private FragmentOrderBinding binding;
    private String filterType = "pending";
    private RecyclerView recyclerView;
    private String currentRestaurantName;
    private long lastNotifiedOrderTime; // 儲存最後通知時間

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrderBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        checkNotificationPermission();
        createNotificationChannel();

        Bundle arguments = getArguments();
        if (arguments != null) {
            currentRestaurantName = arguments.getString("restaurantName", "");
        } else {
            currentRestaurantName = "";
            Log.e("OrderFragment", "No restaurant name provided in arguments.");
        }

        setupRecyclerView(root);

        // 初始化最後通知時間
        lastNotifiedOrderTime = getLastNotifiedOrderTime();

        // 設置按鈕點擊事件
        binding.buttonShowPendingOrders.setOnClickListener(v -> updateFilter("pending", binding.buttonShowPendingOrders));
        binding.buttonShowAcceptedOrders.setOnClickListener(v -> updateFilter("accepted", binding.buttonShowAcceptedOrders));
        binding.buttonShowDelayedOrders.setOnClickListener(v -> updateFilter("delayed", binding.buttonShowDelayedOrders));

        fetchUserOrders();
        return root;
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
    }

    private void updateButtonStyles(Button selectedButton) {
        binding.buttonShowPendingOrders.setTextColor(Color.parseColor("#F2ebd9"));
        binding.buttonShowPendingOrders.setBackgroundColor(Color.parseColor("#66363C"));

        binding.buttonShowAcceptedOrders.setTextColor(Color.parseColor("#F2ebd9"));
        binding.buttonShowAcceptedOrders.setBackgroundColor(Color.parseColor("#66363C"));

        binding.buttonShowDelayedOrders.setTextColor(Color.parseColor("#F2ebd9"));
        binding.buttonShowDelayedOrders.setBackgroundColor(Color.parseColor("#66363C"));

        selectedButton.setTextColor(Color.parseColor("#66363C")); // 崎紅
        selectedButton.setBackgroundColor(Color.parseColor("#F2ebd9")); // 暖白
    }

    private void setupRecyclerView(View root) {
        orderList = new ArrayList<>();
        orderAdapter = new OrderAdapter(orderList, this, filterType);
        recyclerView = root.findViewById(R.id.recycler_view_orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(orderAdapter);
    }

    private void updateFilter(String type, Button selectedButton) {
        filterType = type;
        orderAdapter.updateFilter(filterType);
        recyclerView.scrollToPosition(0);
        updateButtonStyles(selectedButton);
    }

    @Override
    public void onAcceptOrder(Map<String, Object> order) {
        ordersRef.child((String) order.get("orderId")).child("接單狀況").setValue("接受訂單");
        fetchUserOrders();
    }

    @Override
    public void onRejectOrder(Map<String, Object> order, String reason) {
        ordersRef.child((String) order.get("orderId")).child("接單狀況").setValue("拒絕訂單");
        ordersRef.child((String) order.get("orderId")).child("拒絕原因").setValue(reason);
        fetchUserOrders();
        Toast.makeText(getActivity(), "訂單已拒絕: " + reason, Toast.LENGTH_SHORT).show();
    }

    private void fetchUserOrders() {
        if (binding == null) return;

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isAdded()) return;

                List<Map<String, Object>> newOrderList = new ArrayList<>();
                long currentTime = System.currentTimeMillis();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> order = (Map<String, Object>) snapshot.getValue();
                    if (isRelevantOrder(order)) {
                        processOrderData(order, currentTime);
                        newOrderList.add(order);
                    }
                }
                orderAdapter.updateOrderList(newOrderList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("RealtimeDatabase", "loadPost:onCancelled", databaseError.toException());
            }
        });
    }

    private boolean isRelevantOrder(Map<String, Object> order) {
        if (order == null) return false;
        String restaurantName = (String) order.get("restaurantName");
        String status = (String) order.get("接單狀況");
        return currentRestaurantName.equals(restaurantName) && !"完成訂單".equals(status) && !"拒絕訂單".equals(status);
    }

    private void processOrderData(Map<String, Object> order, long currentTime) {
        Long timestamp = (Long) order.get("uploadTimestamp");
        if (timestamp != null && timestamp > lastNotifiedOrderTime) {
            sendOrderNotification(order);
            saveLastNotifiedOrderTime(timestamp);
        }

        order.put("readableDate", convertTimestampToReadableDate(timestamp));
        Long orderTime = (Long) order.get("取餐時間");
        if (orderTime != null) {
            long differenceInMinutes = (orderTime - currentTime) / (60 * 1000);
            order.put("differenceInMinutes", differenceInMinutes);
        }

        String orderId = (String) order.get("orderId");
        if (orderId != null && orderId.length() > 6) {
            order.put("orderNumber", orderId.substring(orderId.length() - 6));
        }
    }

    private String convertTimestampToReadableDate(Long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Order Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Channel for new order notifications");
            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendOrderNotification(Map<String, Object> order) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("OrderFragment", "通知權限未授權，無法發送通知。");
            return;
        }

        String orderNumber = (String) order.get("orderNumber");
        String message = "有新訂單！訂單號碼: " + orderNumber;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notifications_active_24)
                .setContentTitle("新訂單")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        notificationManager.notify(orderNumber.hashCode(), builder.build()); // 使用 orderNumber 的 hash 作為通知ID
    }

    private void saveLastNotifiedOrderTime(long timestamp) {
        SharedPreferences preferences = requireContext().getSharedPreferences("OrderPreferences", Context.MODE_PRIVATE);
        preferences.edit().putLong("lastNotifiedOrderTime", timestamp).apply();
        lastNotifiedOrderTime = timestamp;
    }

    private long getLastNotifiedOrderTime() {
        SharedPreferences preferences = requireContext().getSharedPreferences("OrderPreferences", Context.MODE_PRIVATE);
        return preferences.getLong("lastNotifiedOrderTime", 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
