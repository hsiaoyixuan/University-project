package com.example.restaurantlogging.ui.historical_orders;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoricalOrdersViewModel extends ViewModel {

    private final MutableLiveData<Map<String, String>> mCompletedOrdersList;
    private final MutableLiveData<Map<String, String>> mRejectedOrdersList;
    private final Map<String, String> completedOrdersDetails;
    private final Map<String, String> rejectedOrdersDetails;
    private String restaurantName;

    public HistoricalOrdersViewModel() {
        mCompletedOrdersList = new MutableLiveData<>();
        mRejectedOrdersList = new MutableLiveData<>();
        completedOrdersDetails = new HashMap<>();
        rejectedOrdersDetails = new HashMap<>();
    }

    // 1. 設置餐廳名稱並觸發 Firebase 訂單數據的過濾
    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
        fetchOrdersFromFirebase(); // 調用方法來獲取並過濾訂單
    }

    private void fetchOrdersFromFirebase() {
        FirebaseDatabase.getInstance().getReference("Orders").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, String> completedOrders = new HashMap<>();
                Map<String, String> rejectedOrders = new HashMap<>();

                // 2. 獲取今天的日期，將時間部分設為 0，獲得當天的開始時間戳
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long startOfDayTimestamp = calendar.getTimeInMillis(); // 當天開始的時間戳

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    // 3. 獲取每筆訂單的餐廳名稱與時間戳
                    String orderRestaurantName = orderSnapshot.child("restaurantName").getValue(String.class);
                    Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);

                    // 4. 檢查餐廳名稱是否匹配且訂單時間是否為當天
                    if (!orderRestaurantName.equals(restaurantName) || timestamp < startOfDayTimestamp) {
                        continue;  // 餐廳名稱不匹配或訂單時間不是當天，則跳過該訂單
                    }

                    // 符合條件的訂單：獲取訂購人信息與訂單狀態
                    String name = orderSnapshot.child("名字").getValue(String.class);
                    String status = orderSnapshot.child("接單狀況").getValue(String.class);
                    String rejectedReason = orderSnapshot.child("拒絕原因").getValue(String.class);
                    List<Map<String, Object>> itemList = (List<Map<String, Object>>) orderSnapshot.child("items").getValue();

                    // 5. 構建訂單項目的詳細字符串
                    StringBuilder itemsStringBuilder = new StringBuilder();
                    if (itemList != null) {
                        for (Map<String, Object> item : itemList) {
                            itemsStringBuilder.append("\n項目: ")
                                    .append(item.get("title")).append(", 價格: ")
                                    .append(item.get("description")).append(", 數量: ")
                                    .append(item.get("quantity"));
                        }
                    }

                    String orderId = orderSnapshot.getKey().substring(orderSnapshot.getKey().length() - 6); // 訂單ID的後六位

                    // 6. 根據訂單狀態（完成或拒絕）將訂單加入相應的列表
                    if ("完成訂單".equals(status)) {
                        String orderSummary = "訂購人: " + name;
                        String orderDetails = "訂購人: " + name + ", 訂購時間: " + convertTimestampToReadableDate(timestamp) + itemsStringBuilder.toString();
                        completedOrders.put(orderId, orderSummary);
                        completedOrdersDetails.put(orderId, orderDetails);
                    } else if ("拒絕訂單".equals(status)) {
                        String orderSummary = "訂購人: " + name;
                        String orderDetails = "訂購人: " + name + ", 訂購時間: " + convertTimestampToReadableDate(timestamp) + ", 拒絕原因: " + rejectedReason + itemsStringBuilder.toString();
                        rejectedOrders.put(orderId, orderSummary);
                        rejectedOrdersDetails.put(orderId, orderDetails);
                    }
                }

                // 7. 更新 LiveData，以便 UI 能即時顯示更新後的訂單
                mCompletedOrdersList.setValue(completedOrders);
                mRejectedOrdersList.setValue(rejectedOrders);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // 錯誤處理（可選）
            }
        });
    }

    // 獲取完成訂單的 LiveData 列表
    public LiveData<Map<String, String>> getCompletedOrdersList() {
        return mCompletedOrdersList;
    }

    // 獲取拒絕訂單的 LiveData 列表
    public LiveData<Map<String, String>> getRejectedOrdersList() {
        return mRejectedOrdersList;
    }

    // 獲取完成訂單的詳細信息
    public String getCompletedOrdersDetails(String orderId) {
        return completedOrdersDetails.get(orderId);
    }

    // 獲取拒絕訂單的詳細信息
    public String getRejectedOrdersDetails(String orderId) {
        return rejectedOrdersDetails.get(orderId);
    }

    // 8. 將時間戳轉換為可讀日期格式
    private String convertTimestampToReadableDate(Long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
