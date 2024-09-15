package com.example.restaurantlogging.ui.order;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderFragment extends Fragment implements OrderAdapter.OnOrderActionListener {

    private static final String CHANNEL_ID = "order_notification_channel";
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;
    private DatabaseReference ordersRef;
    private OrderAdapter orderAdapter;
    private List<Map<String, Object>> orderList;
    private Map<String, Map<String, Object>> previousOrders = new HashMap<>();
    private FragmentOrderBinding binding;
    private String filterType = "pending";
    private RecyclerView recyclerView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable fetchOrdersRunnable;
    private String currentRestaurantName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrderBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 權限檢查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }

        // 創建通知通道
        createNotificationChannel();
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        // 從 arguments 獲取當前餐廳名稱
        Bundle arguments = getArguments();
        if (arguments != null) { // 修改：檢查 arguments 是否為 null
            currentRestaurantName = arguments.getString("restaurantName", "");  // 從 Bundle 中獲取傳遞過來的餐廳名稱
        } else {
            currentRestaurantName = "";
            Log.e("OrderFragment", "No restaurant name provided in arguments.");
        }

        setupRecyclerView(root);

        // 設定按鈕的點擊事件
        binding.buttonShowPendingOrders.setOnClickListener(v -> {
            filterType = "pending";
            orderAdapter.updateFilter(filterType);
            updateButtonColors();
            recyclerView.scrollToPosition(0);
        });

        binding.buttonShowAcceptedOrders.setOnClickListener(v -> {
            filterType = "accepted";
            orderAdapter.updateFilter(filterType);
            updateButtonColors();
            recyclerView.scrollToPosition(0);
        });

        binding.buttonShowDelayedOrders.setOnClickListener(v -> {
            filterType = "delayed";
            orderAdapter.updateFilter(filterType);
            updateButtonColors();
            recyclerView.scrollToPosition(0);
        });

        fetchUserOrders();  // 呼叫方法來獲取訂單
        return root;
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
        if (binding == null) return; // 確保 binding 不為 null

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isAdded()) return; // 檢查 Fragment 是否仍然附加到活動

                orderList.clear();
                long currentTime = System.currentTimeMillis();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> order = (Map<String, Object>) snapshot.getValue();
                    if (order != null) {
                        // 根據餐廳名稱篩選訂單
                        String restaurantName = (String) order.get("restaurantName");
                        if (!currentRestaurantName.equals(restaurantName)) {
                            continue;  // 跳過不屬於當前餐廳的訂單
                        }

                        String status = (String) order.get("接單狀況");
                        if ("完成訂單".equals(status) || "拒絕訂單".equals(status)) {
                            continue;  // 跳過已完成或已拒絕的訂單
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

                        // 檢查是否為新訂單，並發送通知
                        if (!previousOrders.containsKey(snapshot.getKey())) {
                            sendOrderNotification(order); // 修改：新增訂單時發送通知
                        }

                        previousOrders.put(snapshot.getKey(), order); // 更新已處理的訂單

                        orderList.add(order);
                    }
                }
                orderAdapter.updateFilter(filterType);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("RealtimeDatabase", "loadPost:onCancelled", databaseError.toException());
            }
        });

        fetchOrdersRunnable = this::fetchUserOrders;
        handler.postDelayed(fetchOrdersRunnable, 60000);
    }

    private String convertTimestampToReadableDate(Long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void updateButtonColors() {
        // 設定按鈕的顏色
        binding.buttonShowPendingOrders.setBackgroundColor(getResources().getColor(R.color.崎紅));
        binding.buttonShowPendingOrders.setTextColor(getResources().getColor(R.color.暖白));
        binding.buttonShowAcceptedOrders.setBackgroundColor(getResources().getColor(R.color.崎紅));
        binding.buttonShowAcceptedOrders.setTextColor(getResources().getColor(R.color.暖白));
        binding.buttonShowDelayedOrders.setBackgroundColor(getResources().getColor(R.color.崎紅));
        binding.buttonShowDelayedOrders.setTextColor(getResources().getColor(R.color.暖白));

        switch (filterType) {
            case "pending":
                binding.buttonShowPendingOrders.setBackgroundColor(getResources().getColor(R.color.暖白));
                binding.buttonShowPendingOrders.setTextColor(getResources().getColor(R.color.崎紅));
                break;
            case "accepted":
                binding.buttonShowAcceptedOrders.setBackgroundColor(getResources().getColor(R.color.暖白));
                binding.buttonShowAcceptedOrders.setTextColor(getResources().getColor(R.color.崎紅));
                break;
            case "delayed":
                binding.buttonShowDelayedOrders.setBackgroundColor(getResources().getColor(R.color.暖白));
                binding.buttonShowDelayedOrders.setTextColor(getResources().getColor(R.color.崎紅));
                break;
        }
    }

    private void createNotificationChannel() {
        if (!isAdded()) return; // 檢查 Fragment 是否仍然附加到活動

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Order Notification";
            String description = "Channel for order notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendOrderNotification(Map<String, Object> order) {
        if (!isAdded()) return; // 檢查 Fragment 是否仍然附加到活動

        String orderNumber = "訂單編號: " + order.get("orderNumber");
        String name = "訂購人姓名: " + order.get("名字");
        String timeDifference = "預計取餐時間: " + order.get("differenceInMinutes") + " 分鐘";
        String message = orderNumber + "\n" + name + "\n" + timeDifference;

        // 修改：使用唯一的 notificationId 來為每個訂單創建唯一的通知
        int notificationId = order.get("orderId").hashCode();  // 基於訂單ID生成唯一的通知ID

        sendNotification("有新訂單", message, notificationId);
    }

    private void sendNotification(String title, String message, int notificationId) { // 修改：添加 notificationId 參數
        if (!isAdded()) { // 檢查 Fragment 是否仍然附加到活動
            Log.w("OrderFragment", "Fragment not attached to context; cannot send notification.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_notifications_active_24)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
            notificationManager.notify(notificationId, builder.build()); // 修改：使用唯一的 notificationId 發送通知
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(fetchOrdersRunnable);
        binding = null;
    }
}
