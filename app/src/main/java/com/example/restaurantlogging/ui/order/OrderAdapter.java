package com.example.restaurantlogging.ui.order;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.restaurantlogging.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Map<String, Object>> orderList;
    private OnOrderActionListener onOrderActionListener;
    private String filterType; // 用于标识当前显示的订单类型

    public OrderAdapter(List<Map<String, Object>> orderList, OnOrderActionListener onOrderActionListener, String filterType) {
        this.orderList = orderList;
        this.onOrderActionListener = onOrderActionListener;
        this.filterType = filterType; // 保存过滤类型
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Map<String, Object> order = orderList.get(position);

        // 根據 filterType 過濾訂單並決定是否顯示
        if (shouldDisplayOrder(order)) {
            holder.itemView.setVisibility(View.VISIBLE); // 顯示符合條件的訂單項
            holder.textViewOrderItem.setText((String) order.get("名字"));
            holder.textViewOrderDate.setText((String) order.get("readableDate"));

            String orderNumber = (String) order.get("orderNumber"); // 獲取訂單號碼
            if (orderNumber != null) {
                holder.textViewOrderNum.setText("#" + orderNumber); // 顯示訂單編號
            }

            Long differenceInMinutes = (Long) order.get("differenceInMinutes");
            int minutes = differenceInMinutes != null ? Math.abs(differenceInMinutes.intValue()) : 0;

            holder.textViewOrderTime.setText("距離取餐時間: " + minutes + "分鐘\n" + (String) order.get("readableDate1"));

            // 設置接受和拒絕按鈕的可見性
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
                intent.putExtra("differenceInMinutes", minutes);
                context.startActivity(intent);
            });

            holder.buttonAccept.setOnClickListener(v -> onOrderActionListener.onAcceptOrder(order));

            holder.buttonReject.setOnClickListener(v -> {
                Context context = v.getContext();
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                LayoutInflater inflater = LayoutInflater.from(context);
                View dialogView = inflater.inflate(R.layout.dialog_reject_order, null);
                builder.setView(dialogView);

                AlertDialog alertDialog = builder.create();

                RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group_reject_reason);
                EditText editTextOtherReason = dialogView.findViewById(R.id.edit_text_other_reason);
                Button buttonConfirm = dialogView.findViewById(R.id.button_confirm);
                Button buttonCancel = dialogView.findViewById(R.id.button_cancel);

                buttonConfirm.setOnClickListener(view -> {
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    String reason;
                    if (selectedId != -1) {
                        RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
                        reason = selectedRadioButton.getText().toString();
                    } else {
                        reason = editTextOtherReason.getText().toString();
                    }

                    if (reason.isEmpty()) {
                        Toast.makeText(context, "請輸入拒絕原因", Toast.LENGTH_SHORT).show();
                    } else {
                        onOrderActionListener.onRejectOrder(order, reason);
                        alertDialog.dismiss();
                    }
                });

                buttonCancel.setOnClickListener(view -> alertDialog.dismiss());
                alertDialog.show();
            });
        } else {
            holder.itemView.setVisibility(View.GONE); // 隱藏不符合條件的訂單項
        }
    }

    // 判断订单是否符合当前显示类型
    private boolean shouldDisplayOrder(Map<String, Object> order) {
        String status = (String) order.get("接單狀況");
        Long differenceInMinutes = (Long) order.get("differenceInMinutes");
        int minutes = differenceInMinutes != null ? Math.abs(differenceInMinutes.intValue()) : 0;

        // 根据时间和接单状态来分类
        if ("accepted".equals(filterType)) {
            return "接受訂單".equals(status);
        } else if ("delayed".equals(filterType)) {
            return minutes >= 30 && !"接受訂單".equals(status) && !"完成訂單".equals(status) && !"拒絕訂單".equals(status);
        } else if ("pending".equals(filterType)) {
            return minutes < 30 && !"接受訂單".equals(status) && !"完成訂單".equals(status) && !"拒絕訂單".equals(status);
        }
        return false;
    }

    // 提供方法动态更新显示的订单类型
    public void updateFilter(String newFilterType) {
        this.filterType = newFilterType;
        notifyDataSetChanged(); // 通知适配器刷新数据
    }

    @Override
    public int getItemCount() {
        return orderList.size();
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
