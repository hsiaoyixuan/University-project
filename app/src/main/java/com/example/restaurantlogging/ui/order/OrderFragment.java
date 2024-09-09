package com.example.restaurantlogging.ui.order;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
import android.os.Handler;
import android.os.Looper;

public class OrderFragment extends Fragment implements OrderAdapter.OnOrderActionListener {

    private static final String CHANNEL_ID = "order_notification_channel"; // 通知頻道ID
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1; // 通知權限請求代碼
    private DatabaseReference ordersRef; // Firebase資料庫參考
    private OrderAdapter orderAdapter; // RecyclerView的適配器
    private List<Map<String, Object>> orderList; // 訂單列表
    private Map<String, Map<String, Object>> previousOrders = new HashMap<>(); // 保存之前的訂單狀態
    private FragmentOrderBinding binding;  // 用於綁定UI組件的變量
    private String filterType = "pending"; // 默認顯示未接受訂單
    private RecyclerView recyclerView; // RecyclerView實例
    private Handler handler = new Handler(Looper.getMainLooper()); // 用於處理定時任務
    private Runnable fetchOrdersRunnable; // 定義一個Runnable

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 初始化綁定變量，並設置根視圖
        binding = FragmentOrderBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 檢查並請求通知權限（API 33及以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }

        createNotificationChannel();  // 創建通知頻道
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders"); // 初始化 Firebase 資料庫引用
        setupRecyclerView(root); // 設置RecyclerView

        // 設置按鈕點擊事件來切換顯示不同類型的訂單
        binding.buttonShowPendingOrders.setOnClickListener(v -> {
            filterType = "pending"; // 設置篩選類型為“未接受”
            orderAdapter.updateFilter(filterType);
            updateButtonColors(); // 更新按鈕顏色
            recyclerView.scrollToPosition(0); // 滾動到RecyclerView的頂部
        });

        binding.buttonShowAcceptedOrders.setOnClickListener(v -> {
            filterType = "accepted"; // 設置篩選類型為“已接受”
            orderAdapter.updateFilter(filterType);
            updateButtonColors(); // 更新按鈕顏色
            recyclerView.scrollToPosition(0); // 滾動到RecyclerView的頂部
        });

        binding.buttonShowDelayedOrders.setOnClickListener(v -> {
            filterType = "delayed"; // 設置篩選類型為“延遲”
            orderAdapter.updateFilter(filterType);
            updateButtonColors(); // 更新按鈕顏色
            recyclerView.scrollToPosition(0); // 滾動到RecyclerView的頂部
        });

        fetchUserOrders(); // 從 Firebase 獲取訂單數據
        return root;
    }

    // 初始化並設置RecyclerView
    private void setupRecyclerView(View root) {
        orderList = new ArrayList<>(); // 初始化訂單列表
        orderAdapter = new OrderAdapter(orderList, this, filterType); // 初始化適配器

        recyclerView = root.findViewById(R.id.recycler_view_orders); // 綁定RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity())); // 設置布局管理器
        recyclerView.setAdapter(orderAdapter); // 設置適配器
    }

    // 接受訂單的操作
    @Override
    public void onAcceptOrder(Map<String, Object> order) {
        order.put("接單狀況", "接受訂單"); // 更新本地狀態
        ordersRef.child((String) order.get("orderId")).child("接單狀況").setValue("接受訂單"); // 更新Firebase狀態
        fetchUserOrders(); // 重新加載數據，更新列表
    }

    // 拒絕訂單的操作
    @Override
    public void onRejectOrder(Map<String, Object> order, String reason) {
        ordersRef.child((String) order.get("orderId")).child("接單狀況").setValue("拒絕訂單"); // 更新Firebase狀態
        ordersRef.child((String) order.get("orderId")).child("拒絕原因").setValue(reason); // 保存拒絕原因
        fetchUserOrders(); // 重新加載數據，更新列表
        Toast.makeText(getActivity(), "訂單已拒絕: " + reason, Toast.LENGTH_SHORT).show(); // 顯示拒絕通知
    }

    // 從 Firebase 獲取訂單數據
    private void fetchUserOrders() {
        if (binding == null) return; // 檢查 binding 是否為 null

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!isAdded()) return; // 確保 Fragment 已附加

                orderList.clear(); // 清空舊的訂單數據
                long currentTime = System.currentTimeMillis(); // 獲取當前時間戳
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Map<String, Object> order = (Map<String, Object>) snapshot.getValue();
                    if (order != null) {
                        String status = (String) order.get("接單狀況");
                        if ("完成訂單".equals(status) || "拒絕訂單".equals(status)) {
                            continue; // 跳過已完成或已拒絕的訂單
                        }

                        Long timestamp = (Long) order.get("uploadTimestamp");
                        if (timestamp != null) {
                            String readableDate = convertTimestampToReadableDate(timestamp); // 將時間戳轉換為可讀的日期時間格式
                            order.put("readableDate", readableDate);
                        }
                        Long timestamp1 = (Long) order.get("timestamp");
                        if (timestamp1 != null) {
                            String readableDate = convertTimestampToReadableDate(timestamp1); // 將時間戳轉換為可讀的日期時間格式
                            order.put("readableDate1", readableDate);
                        }
                        Long orderTime = (Long) order.get("取餐時間"); // 獲取取餐時間
                        if (orderTime != null) {
                            long differenceInMillis = orderTime - currentTime;
                            long differenceInMinutes = differenceInMillis / (60 * 1000); // 計算取餐時間的分鐘數
                            order.put("differenceInMinutes", differenceInMinutes);
                        }
                        String orderId = snapshot.getKey();
                        if (orderId != null && orderId.length() > 6) {
                            String orderNumber = orderId.substring(orderId.length() - 6); // 提取訂單號碼
                            order.put("orderNumber", orderNumber); // 將訂單號碼存入 Map
                        }

                        order.put("orderId", snapshot.getKey()); // 保存訂單ID
                        checkForOrderUpdates(snapshot.getKey(), order); // 檢查訂單狀態是否有變更
                        orderList.add(order); // 添加訂單到列表
                    }
                }
                orderAdapter.notifyDataSetChanged(); // 通知適配器數據已更改
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("RealtimeDatabase", "loadPost:onCancelled", databaseError.toException()); // 錯誤處理
            }
        });

        // 使用Handler進行定時刷新操作
        fetchOrdersRunnable = this::fetchUserOrders; // 定義Runnable
        handler.postDelayed(fetchOrdersRunnable, 60000); // 每分鐘刷新一次數據
    }

    // 將時間戳轉換為可讀格式
    private String convertTimestampToReadableDate(Long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // 更新按鈕背景顏色和RecyclerView背景顏色的方法
    private void updateButtonColors() {
        binding.buttonShowPendingOrders.setBackgroundColor(getResources().getColor(R.color.崎紅));
        binding.buttonShowPendingOrders.setTextColor(getResources().getColor(R.color.暖白));
        binding.buttonShowAcceptedOrders.setBackgroundColor(getResources().getColor(R.color.崎紅));
        binding.buttonShowAcceptedOrders.setTextColor(getResources().getColor(R.color.暖白));
        binding.buttonShowDelayedOrders.setBackgroundColor(getResources().getColor(R.color.崎紅));
        binding.buttonShowDelayedOrders.setTextColor(getResources().getColor(R.color.暖白));

        // 設置當前選中的按鈕背景顏色
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

    // 檢查訂單狀態是否有變更
    private void checkForOrderUpdates(String orderId, Map<String, Object> currentOrder) {
        if (!isAdded()) return; // 確保 Fragment 已附加

        Map<String, Object> previousOrder = previousOrders.get(orderId); // 獲取之前的訂單狀態

        if (previousOrder == null) {
            sendOrderNotification(currentOrder); // 新訂單，直接發送通知
        } else {
            String currentStatus = (String) currentOrder.get("接單狀況");
            String previousStatus = (String) previousOrder.get("接單狀況");

            // 如果狀態不為空並且已更改，發送狀態更改通知
            if (currentStatus != null && !currentStatus.equals(previousStatus)) {
                sendStatusChangeNotification(currentOrder);
            }

            Long currentPickupTime = (Long) currentOrder.get("取餐時間");
            Long previousPickupTime = (Long) previousOrder.get("取餐時間");

            // 如果取餐時間不為空並且已更改，發送取餐時間更改通知
            if (currentPickupTime != null && !currentPickupTime.equals(previousPickupTime)) {
                sendPickupTimeChangeNotification(currentOrder);
            }
        }

        // 更新保存的訂單狀態
        previousOrders.put(orderId, currentOrder);
    }

    // 創建通知頻道
    private void createNotificationChannel() {
        if (!isAdded()) return; // 確保 Fragment 已附加

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Order Notification";
            String description = "Channel for order notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT; // 設置通知重要性
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel); // 創建通知頻道
        }
    }

    // 發送新訂單通知
    private void sendOrderNotification(Map<String, Object> order) {
        if (!isAdded()) return; // 確保 Fragment 已附加

        String orderNumber = "訂單編號: " + order.get("orderNumber");
        String name = "訂購人姓名: " + order.get("名字");
        String timeDifference = "預計取餐時間: " + order.get("differenceInMinutes") + " 分鐘";
        String message = orderNumber + "\n" + name + "\n" + timeDifference;

        sendNotification("有新訂單", message); // 發送通知
    }

    // 發送狀態變更通知
    private void sendStatusChangeNotification(Map<String, Object> order) {
        if (!isAdded()) return; // 確保 Fragment 已附加

        String orderNumber = "訂單編號: " + order.get("orderNumber");
        String name = "訂購人姓名: " + order.get("名字");
        String status = "接單狀況已更新: " + order.get("接單狀況");
        String message = orderNumber + "\n" + name + "\n" + status;

        sendNotification("接單狀況通知", message); // 發送通知
    }

    // 發送取餐時間變更通知
    private void sendPickupTimeChangeNotification(Map<String, Object> order) {
        if (!isAdded()) return; // 確保 Fragment 已附加

        String orderNumber = "訂單編號: " + order.get("orderNumber");
        String name = "訂購人姓名: " + order.get("名字");
        String pickupTimeMessage = "您的訂單取餐時間更改為 " + order.get("取餐時間") + " 分鐘後可以取餐。";
        String message = orderNumber + "\n" + name + "\n" + pickupTimeMessage;

        sendNotification("取餐通知", message); // 發送通知
    }

    // 發送通知
    private void sendNotification(String title, String message) {
        if (!isAdded()) {
            Log.w("OrderFragment", "Fragment not attached to context; cannot send notification.");
            return; // Fragment 未附加，不發送通知
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return; // 如果沒有通知權限，則不發送通知
            }
        }

        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_notifications_active_24)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
            notificationManager.notify(1, builder.build()); // 發送通知
        } catch (SecurityException e) {
            e.printStackTrace(); // 捕獲並處理安全性異常
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(fetchOrdersRunnable); // 清除所有Handler的回調
        binding = null; // 防止內存泄漏，將綁定設為空
    }
}
