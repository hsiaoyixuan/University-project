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
    private static final int NOTIFICATION_ID = 1001; // 通知ID，保證唯一性
    private DatabaseReference ordersRef;
    private OrderAdapter orderAdapter;
    private List<Map<String, Object>> orderList;
    private FragmentOrderBinding binding;
    private String filterType = "pending";
    private RecyclerView recyclerView;
    private String currentRestaurantName;
    private long lastNotifiedOrderTime = 0; // 記錄最後通知的訂單時間

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrderBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        // 檢查通知權限（Android 13 及以上）
        checkNotificationPermission();

        // 創建通知通道
        createNotificationChannel();

        // 從 arguments 獲取當前餐廳名稱
        Bundle arguments = getArguments();
        if (arguments != null) {
            currentRestaurantName = arguments.getString("restaurantName", "");
        } else {
            currentRestaurantName = "";
            Log.e("OrderFragment", "No restaurant name provided in arguments.");
        }

        setupRecyclerView(root);

        // 設置按鈕點擊事件
        binding.buttonShowPendingOrders.setOnClickListener(v -> {
            filterType = "pending";
            orderAdapter.updateFilter(filterType);
            recyclerView.scrollToPosition(0);

            // 更新按鈕的樣式
            updateButtonStyles(binding.buttonShowPendingOrders);
        });

        binding.buttonShowAcceptedOrders.setOnClickListener(v -> {
            filterType = "accepted";
            orderAdapter.updateFilter(filterType);
            recyclerView.scrollToPosition(0);

            // 更新按鈕的樣式
            updateButtonStyles(binding.buttonShowAcceptedOrders);
        });

        binding.buttonShowDelayedOrders.setOnClickListener(v -> {
            filterType = "delayed";
            orderAdapter.updateFilter(filterType);
            recyclerView.scrollToPosition(0);

            // 更新按鈕的樣式
            updateButtonStyles(binding.buttonShowDelayedOrders);
        });

        fetchUserOrders();  // 調用方法獲取訂單
        return root;
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void updateButtonStyles(Button selectedButton) {
        // 重置所有按鈕的樣式為默認
        binding.buttonShowPendingOrders.setTextColor(Color.parseColor("#F2ebd9"));
        binding.buttonShowPendingOrders.setBackgroundColor(Color.parseColor("#66363C"));

        binding.buttonShowAcceptedOrders.setTextColor(Color.parseColor("#F2ebd9"));
        binding.buttonShowAcceptedOrders.setBackgroundColor(Color.parseColor("#66363C"));

        binding.buttonShowDelayedOrders.setTextColor(Color.parseColor("#F2ebd9"));
        binding.buttonShowDelayedOrders.setBackgroundColor(Color.parseColor("#66363C"));

        // 設置選中按鈕的樣式
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

    @Override
    public void onAcceptOrder(Map<String, Object> order) {
        order.put("接單狀況", "接受訂單");
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
                    if (order != null) {
                        // 篩選餐廳
                        String restaurantName = (String) order.get("restaurantName");
                        if (!currentRestaurantName.equals(restaurantName)) {
                            continue;
                        }

                        String status = (String) order.get("接單狀況");
                        if ("完成訂單".equals(status) || "拒絕訂單".equals(status)) {
                            continue;
                        }

                        Long timestamp = (Long) order.get("uploadTimestamp");
                        if (timestamp != null) {
                            String readableDate = convertTimestampToReadableDate(timestamp);
                            order.put("readableDate", readableDate);
                        }
                        Long timestamp1 = (Long) order.get("timestamp");
                        if (timestamp1 != null) {
                            String readableDate = convertTimestampToReadableDate(timestamp1);
                            order.put("readableDate1", readableDate);
                        }

                        Long orderTime = (Long) order.get("取餐時間");
                        if (orderTime != null) {
                            long differenceInMillis = orderTime - currentTime;
                            long differenceInMinutes = differenceInMillis / (60 * 1000);
                            order.put("differenceInMinutes", differenceInMinutes);
                        }

                        String orderId = snapshot.getKey();
                        if (orderId != null && orderId.length() > 6) {
                            String orderNumber = orderId.substring(orderId.length() - 6);
                            order.put("orderNumber", orderNumber);
                        }

                        order.put("orderId", snapshot.getKey());

                        // 若新訂單的上傳時間大於最後通知的時間，則發送通知
                        if (timestamp != null && timestamp > lastNotifiedOrderTime) {
                            sendOrderNotification(order);
                            lastNotifiedOrderTime = timestamp; // 更新最後通知的訂單時間
                        }

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

    private String convertTimestampToReadableDate(Long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Order Notifications";
            String description = "Channel for new order notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendOrderNotification(Map<String, Object> order) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 檢查是否授權了通知權限
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // 未授權，無法發送通知
                Log.w("OrderFragment", "通知權限未授權，無法發送通知。");
                return;
            }
        }

        String orderNumber = (String) order.get("orderNumber");
        String message = "有新訂單！訂單號碼: " + orderNumber;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notifications_active_24)
                .setContentTitle("新訂單")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    private void saveLastNotifiedOrderTime(long timestamp) {
        SharedPreferences preferences = requireContext().getSharedPreferences("OrderPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong("lastNotifiedOrderTime", timestamp);
        editor.apply();
    }

    private long getLastNotifiedOrderTime() {
        SharedPreferences preferences = requireContext().getSharedPreferences("OrderPreferences", Context.MODE_PRIVATE);
        return preferences.getLong("lastNotifiedOrderTime", 0);
    }
    private boolean isRelevantOrder(Map<String, Object> order) {
        if (order == null) return false;
        String restaurantName = (String) order.get("restaurantName");
        String status = (String) order.get("接單狀況");
        return currentRestaurantName.equals(restaurantName) && !"完成訂單".equals(status) && !"拒絕訂單".equals(status);
    }


}