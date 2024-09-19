package com.example.restaurantlogging.ui.order;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.restaurantlogging.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Map;

public class OrderDetailsActivity extends AppCompatActivity {

    private TextView tvOrderTime;  // 顯示距離取餐時間的 TextView
    private Handler handler;
    private long differenceInMinutes;
    private long updateInterval = 1000; // 每秒更新一次

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        TextView tvorderdetails = findViewById(R.id.tv_order_details);
        TextView tvprice = findViewById(R.id.tv_order_price);
        tvOrderTime = findViewById(R.id.tv_order_time);
        Button btOrderEdit = findViewById(R.id.bt_order_edit);
        Button btOrderFin = findViewById(R.id.bt_order_fin);  // 获取完成订单按钮

        // 從 Intent 獲取傳遞過來的訂單數據
        Map<String, Object> order = (Map<String, Object>) getIntent().getSerializableExtra("order");
        differenceInMinutes = getIntent().getIntExtra("differenceInMinutes", 0);

        // 構建訂單詳細信息的 StringBuilder
        StringBuilder details = new StringBuilder();
        StringBuilder price = new StringBuilder();

        // 獲取訂單中的 itemList
        List<Map<String, Object>> itemList = (List<Map<String, Object>>) order.get("items");
        if (itemList != null) {
            for (Map<String, Object> item : itemList) {
                details.append(item.get("title")).append(" ");
                details.append("$").append(item.get("description")).append(" ");
                details.append("X").append(item.get("quantity")).append(" ");
                details.append("\n");
            }
        }

        // 將構建的訂單詳細信息設置到 tvOrderDetails
        tvorderdetails.setText(details.toString());
        tvprice.setText("總計: " + order.get("totalPrice") + "元");

        // 開始更新取餐時間顯示
        handler = new Handler(Looper.getMainLooper());
        startUpdatingTime();

        // 按鈕點擊事件，顯示編輯時間的對話框
        btOrderEdit.setOnClickListener(v -> showEditTimeDialog(order, (int) differenceInMinutes));

        // 完成訂單按鈕點擊事件
        btOrderFin.setOnClickListener(v -> {
            String orderId = (String) order.get("orderId");
            DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId);
            orderRef.child("接單狀況").setValue("完成訂單").addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(OrderDetailsActivity.this, "訂單完成", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(OrderDetailsActivity.this, "訂單完成更新失敗", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // 開始定時更新取餐時間顯示
    private void startUpdatingTime() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updatePickupTime();
                handler.postDelayed(this, updateInterval);  // 每秒更新一次
            }
        }, updateInterval);
    }

    // 更新取餐時間顯示並檢查是否超時
    private void updatePickupTime() {
        if (differenceInMinutes > 0) {
            tvOrderTime.setText(String.format("距離取餐時間: %02d 分鐘", differenceInMinutes));
            differenceInMinutes--;  // 每秒減少 1 分鐘的計算
        } else {
            tvOrderTime.setText("已超過取餐時間");
            tvOrderTime.setBackgroundColor(Color.RED);  // 超時後背景設置為紅色
        }
    }

    // 顯示編輯時間的對話框
    private void showEditTimeDialog(Map<String, Object> order, int currentMinutes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_time, null);
        builder.setView(dialogView);

        NumberPicker numberPickerMinutesTens = dialogView.findViewById(R.id.numberPickerMinutesTens);
        NumberPicker numberPickerMinutesUnits = dialogView.findViewById(R.id.numberPickerMinutesUnits);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        // 設置分鐘十位數字的範圍和顯示值
        numberPickerMinutesTens.setMinValue(0);
        numberPickerMinutesTens.setMaxValue(9);

        // 設置分鐘個位數字的範圍和顯示值
        numberPickerMinutesUnits.setMinValue(0);
        numberPickerMinutesUnits.setMaxValue(9);

        // 將當前分鐘拆分為十位和個位，並設置到 NumberPicker
        int tens = currentMinutes / 10;
        int units = currentMinutes % 10;
        numberPickerMinutesTens.setValue(tens);
        numberPickerMinutesUnits.setValue(units);

        AlertDialog alertDialog = builder.create();

        btnConfirm.setOnClickListener(v -> {
            // 獲取選中的分鐘數
            int selectedTens = numberPickerMinutesTens.getValue();
            int selectedUnits = numberPickerMinutesUnits.getValue();
            int selectedMinutes = selectedTens * 10 + selectedUnits;

            // 更新 Firebase 的取餐時間和本地顯示
            differenceInMinutes = selectedMinutes;
            order.put("取餐時間", selectedMinutes);
            DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("Orders").child((String) order.get("orderId"));
            orderRef.child("differenceInMinutes").setValue(selectedMinutes).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(OrderDetailsActivity.this, "取餐時間更新成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OrderDetailsActivity.this, "取餐時間更新失敗", Toast.LENGTH_SHORT).show();
                }
            });

            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);  // 停止定時任務
    }
}
