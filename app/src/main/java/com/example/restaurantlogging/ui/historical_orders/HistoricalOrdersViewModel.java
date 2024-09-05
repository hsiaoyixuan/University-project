package com.example.restaurantlogging.ui.historical_orders;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoricalOrdersViewModel extends ViewModel {

    // 用於存儲完成訂單的 LiveData 列表
    private final MutableLiveData<Map<String, String>> mCompletedOrdersList;
    // 用於存儲拒絕訂單的 LiveData 列表
    private final MutableLiveData<Map<String, String>> mRejectedOrdersList;

    public HistoricalOrdersViewModel() {
        mCompletedOrdersList = new MutableLiveData<>();
        mRejectedOrdersList = new MutableLiveData<>();
        fetchOrdersFromFirebase();  // 從 Firebase 獲取訂單數據
    }

    private void fetchOrdersFromFirebase() {
        // 監聽 Firebase 資料庫中 orders 節點的變化
        FirebaseDatabase.getInstance().getReference("orders").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, String> completedOrders = new HashMap<>();
                Map<String, String> rejectedOrders = new HashMap<>();
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String name = orderSnapshot.child("name").getValue(String.class);
                    Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                    String status = orderSnapshot.child("接單狀況").getValue(String.class);
                    String rejectedReason = orderSnapshot.child("拒絕原因").getValue(String.class);
                    List<Map<String, Object>> itemList = (List<Map<String, Object>>) orderSnapshot.child("itemList").getValue(); // 獲取 itemList

                    // 構建 itemList 字符串
                    StringBuilder itemsStringBuilder = new StringBuilder();
                    if (itemList != null) {
                        for (Map<String, Object> item : itemList) {
                            itemsStringBuilder.append("\n項目: ")
                                    .append(item.get("item")).append(", 價格: ")
                                    .append(item.get("price")).append(", 數量: ")
                                    .append(item.get("quantity")).append(", 小計: ")
                                    .append(item.get("subtotal"));
                        }
                    }

                    // 如果訂單狀態為完成訂單
                    if ("完成訂單".equals(status)) {
                        completedOrders.put(name, "訂購人: " + name + ", 訂購時間: " + convertTimestampToReadableDate(timestamp) + itemsStringBuilder.toString()); // 存儲 name 和 itemList
                        // 如果訂單狀態為拒絕訂單
                    } else if ("拒絕訂單".equals(status)) {
                        rejectedOrders.put(name, "訂購人: " + name + ", 訂購時間: " + convertTimestampToReadableDate(timestamp) + ", 拒絕原因: " + rejectedReason + itemsStringBuilder.toString()); // 存儲 name 和 itemList
                    }
                }
                // 更新 LiveData
                mCompletedOrdersList.setValue(completedOrders);
                mRejectedOrdersList.setValue(rejectedOrders);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // 處理可能的錯誤
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

    private String convertTimestampToReadableDate(Long timestamp) {
        // 将时间戳转换为可读日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
