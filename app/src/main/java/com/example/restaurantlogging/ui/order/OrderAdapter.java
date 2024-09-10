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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Map<String, Object>> allOrdersList; // 原始订单列表
    private List<Map<String, Object>> filteredOrderList; // 筛选后的订单列表
    private OnOrderActionListener onOrderActionListener;
    private String filterType; // 用于标识当前显示的订单类型

    public OrderAdapter(List<Map<String, Object>> orderList, OnOrderActionListener onOrderActionListener, String filterType) {
        this.allOrdersList = orderList;
        this.filteredOrderList = new ArrayList<>(); // 初始化筛选后的订单列表
        this.onOrderActionListener = onOrderActionListener;
        this.filterType = filterType; // 保存过滤类型
        filterOrders(); // 初始化筛选结果
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Map<String, Object> order = filteredOrderList.get(position); // 仅获取筛选后的订单

        holder.itemView.setVisibility(View.VISIBLE); // 仅显示符合条件的订单项
        holder.textViewOrderItem.setText((String) order.get("名字"));
        holder.textViewOrderDate.setText((String) order.get("readableDate"));

        String orderNumber = (String) order.get("orderNumber"); // 获取订单编号
        if (orderNumber != null) {
            holder.textViewOrderNum.setText("#" + orderNumber); // 显示订单编号
        }

        Long differenceInMinutes = (Long) order.get("differenceInMinutes");
        int minutes = differenceInMinutes != null ? Math.abs(differenceInMinutes.intValue()) : 0;

        holder.textViewOrderTime.setText("距離取餐時間: " + minutes + "分鐘\n" + (String) order.get("readableDate1"));

        // 设置接受和拒绝按钮的可见性
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
        filterOrders(); // 每次更新过滤类型时重新筛选订单
        notifyDataSetChanged(); // 通知适配器刷新数据
    }

    // 筛选订单方法
    private void filterOrders() {
        filteredOrderList.clear(); // 清空旧的筛选列表
        for (Map<String, Object> order : allOrdersList) {
            if (shouldDisplayOrder(order)) {
                filteredOrderList.add(order); // 仅添加符合条件的订单
            }
        }
    }

    @Override
    public int getItemCount() {
        return filteredOrderList.size(); // 返回筛选后列表的大小
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
