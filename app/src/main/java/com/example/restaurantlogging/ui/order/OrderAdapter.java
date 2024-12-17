package com.example.restaurantlogging.ui.order;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantlogging.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Map<String, Object>> allOrdersList; // 原始訂單列表
    private List<Map<String, Object>> filteredOrderList; // 篩選後的訂單列表
    private OnOrderActionListener onOrderActionListener;
    private String filterType;
    private Handler handler = new Handler(); // Handler 用於定時更新倒數計時
    private DatabaseReference ordersRef; // Firebase Database 參考

    // 構造函數，初始化訂單列表、篩選條件和 Firebase 參考
    public OrderAdapter(List<Map<String, Object>> orderList, OnOrderActionListener onOrderActionListener, String filterType) {
        this.allOrdersList = orderList;
        this.filteredOrderList = new ArrayList<>();
        this.onOrderActionListener = onOrderActionListener;
        this.filterType = filterType;

        // 初始化 Firebase 參考，指向 Orders 節點
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders");

        filterOrders(); // 初始化篩選結果
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item, parent, false);
        return new OrderViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Map<String, Object> order = filteredOrderList.get(position);

        holder.textViewOrderItem.setText((String) order.get("名字"));
        holder.textViewOrderDate.setText((String) order.get("readableDate"));

        String orderNumber = (String) order.get("orderNumber");
        if (orderNumber != null) {
            holder.textViewOrderNum.setText("#" + orderNumber);
        }

        // 設置唯一的 Runnable Tag，避免混亂
        String orderId = (String) order.get("orderId");
        holder.textViewOrderTime.setTag(orderId); // 綁定訂單 ID 作為唯一標識

        String readableDate1 = (String) order.get("readableDate1");
        updateCountdown(holder, readableDate1, orderId);

        // 設定按鈕可見性
        if ("pending".equals(filterType) || "delayed".equals(filterType)) {
            holder.buttonAccept.setVisibility(View.VISIBLE);
            holder.buttonReject.setVisibility(View.VISIBLE);
        } else {
            holder.buttonAccept.setVisibility(View.GONE);
            holder.buttonReject.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            intent.putExtra("order", (java.io.Serializable) order);
            context.startActivity(intent);
        });

        holder.buttonAccept.setOnClickListener(v -> onOrderActionListener.onAcceptOrder(order));

        holder.buttonReject.setOnClickListener(v -> showRejectReasonDialog(holder.itemView.getContext(), order));
    }

    // 為每個訂單設置倒數計時
    private void updateCountdown(OrderViewHolder holder, String readableDate1, String orderId) {
        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                // 確保只有當前訂單的回調生效
                if (orderId.equals(holder.textViewOrderTime.getTag())) {
                    long updatedDifferenceInMinutes = calculateTimeDifferenceFromNow(readableDate1);

                    if (updatedDifferenceInMinutes > 0) {
                        holder.textViewOrderTime.setText(getFormattedTimeDisplay(updatedDifferenceInMinutes, readableDate1));
                        holder.textViewOrderTime.setTextColor(Color.BLACK);
                    } else {
                        holder.textViewOrderTime.setText("已超過取餐時間");
                        holder.textViewOrderTime.setTextColor(Color.RED);
                    }

                    // 設置下一次回調
                    handler.postDelayed(this, 60000);
                }
            }
        };

        // 先移除該 View 的舊回調，再添加新回調
        handler.removeCallbacks(countdownRunnable);
        handler.post(countdownRunnable);
    }

    @Override
    public void onViewRecycled(@NonNull OrderViewHolder holder) {
        super.onViewRecycled(holder);

        // 根據 Tag 清除特定回調，避免影響其他 ViewHolder
        String orderId = (String) holder.textViewOrderTime.getTag();
        if (orderId != null) {
            handler.removeCallbacksAndMessages(orderId);
        }
    }



    // 計算 readableDate1 與當前時間的差值（分鐘），向上取整
    private long calculateTimeDifferenceFromNow(String readableDate1) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        try {
            Date orderDate = sdf.parse(readableDate1); // 將 readableDate1 轉換為 Date
            long differenceInMillis = orderDate.getTime() - System.currentTimeMillis(); // 計算毫秒差值

            // 改為浮點計算，向上取整，避免少 1 分鐘的問題
            return (long) Math.ceil(differenceInMillis / (60.0 * 1000));
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // 若解析失敗，返回0
        }
    }




    // 根據分鐘數決定顯示格式
    private String getFormattedTimeDisplay(long minutes, String readableDate) {
        if (minutes >= 1440) {  // 超過1天，顯示天數
            int days = (int) (minutes / 1440);
            return String.format("距離取餐時間: %d 天\n取餐日期: %s", days, readableDate);
        } else if (minutes >= 60) {  // 超過1小時，顯示小時
            int hours = (int) (minutes / 60);
            int remainingMinutes = (int) (minutes % 60);
            return String.format("距離取餐時間: %d 小時 %02d 分鐘\n取餐日期: %s", hours, remainingMinutes, readableDate);
        } else {  // 60分鐘以下，顯示分鐘
            return String.format("距離取餐時間: %02d 分鐘\n取餐日期: %s", minutes, readableDate);
        }
    }

    // 更新篩選條件
    public void updateFilter(String newFilterType) {
        this.filterType = newFilterType;
        filterOrders(); // 篩選訂單
        notifyDataSetChanged(); // 通知適配器刷新數據
    }

    // 更新訂單列表
    public void updateOrderList(List<Map<String, Object>> newOrderList) {
        this.allOrdersList = newOrderList; // 更新原始訂單列表
        filterOrders(); // 根據篩選條件重新篩選
        notifyDataSetChanged(); // 通知適配器刷新數據
    }

    private void filterOrders() {
        filteredOrderList.clear(); // 清空舊的篩選列表
        for (Map<String, Object> order : allOrdersList) {
            if (shouldDisplayOrder(order)) {
                filteredOrderList.add(order); // 添加符合條件的訂單
            }
        }
    }

    private boolean shouldDisplayOrder(Map<String, Object> order) {
        String status = (String) order.get("接單狀況");
        Long differenceInMinutes = (Long) order.get("differenceInMinutes");
        int minutes = differenceInMinutes != null ? Math.abs(differenceInMinutes.intValue()) : 0;

        if ("accepted".equals(filterType)) {
            return "接受訂單".equals(status);
        } else if ("delayed".equals(filterType)) {
            return minutes >= 30 && !"接受訂單".equals(status) && !"完成訂單".equals(status) && !"拒絕訂單".equals(status);
        } else if ("pending".equals(filterType)) {
            return minutes < 30 && !"接受訂單".equals(status) && !"完成訂單".equals(status) && !"拒絕訂單".equals(status);
        }
        return false;
    }

    private void showRejectReasonDialog(Context context, Map<String, Object> order) {
        // 創建一個 AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_reject_order, null);
        builder.setView(dialogView);

        // 取得對話框內的 UI 元素
        RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group_reject_reason);
        EditText editTextOtherReason = dialogView.findViewById(R.id.edit_text_other_reason);
        Button buttonConfirm = dialogView.findViewById(R.id.button_confirm);
        Button buttonCancel = dialogView.findViewById(R.id.button_cancel);

        AlertDialog dialog = builder.create();

        // 確定按鈕的點擊事件
        buttonConfirm.setOnClickListener(v -> {
            String reason = "";
            int selectedId = radioGroup.getCheckedRadioButtonId();

            if (selectedId == R.id.radio_closed) {
                reason = "歇業時間";
            } else if (selectedId == R.id.radio_insufficient_material) {
                reason = "材料不足";
            } else if (!editTextOtherReason.getText().toString().isEmpty()) {
                reason = editTextOtherReason.getText().toString();
            }

            if (!reason.isEmpty()) {
                // 更新 Firebase 中的訂單狀態和拒絕原因
                ordersRef.child((String) order.get("orderId"))
                        .child("接單狀況").setValue("拒絕訂單");
                ordersRef.child((String) order.get("orderId"))
                        .child("拒絕原因").setValue(reason);

                // 更新 UI
                onOrderActionListener.onRejectOrder(order, reason);

                dialog.dismiss();
            } else {
                Toast.makeText(context, "請選擇或輸入拒絕理由", Toast.LENGTH_SHORT).show();
            }
        });

        // 取消按鈕點擊事件
        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return filteredOrderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewOrderItem;
        TextView textViewOrderDate;
        TextView textViewOrderTime;
        TextView textViewOrderNum;
        Button buttonAccept;
        Button buttonReject;

        OrderViewHolder(View itemView) {
            super(itemView);
            textViewOrderItem = itemView.findViewById(R.id.text_view_order_item);
            textViewOrderDate = itemView.findViewById(R.id.text_view_order_date);
            textViewOrderTime = itemView.findViewById(R.id.text_view_order_time);
            textViewOrderNum = itemView.findViewById(R.id.order_num);
            buttonAccept = itemView.findViewById(R.id.button_accept);
            buttonReject = itemView.findViewById(R.id.button_reject);
        }
    }

    public interface OnOrderActionListener {
        void onAcceptOrder(Map<String, Object> order);
        void onRejectOrder(Map<String, Object> order, String reason);
    }
}
