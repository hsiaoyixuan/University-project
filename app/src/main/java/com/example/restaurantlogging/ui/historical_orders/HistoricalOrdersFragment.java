package com.example.restaurantlogging.ui.historical_orders;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.restaurantlogging.databinding.FragmentHistoricalOrdersBinding;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Map;

public class HistoricalOrdersFragment extends Fragment {

    private FragmentHistoricalOrdersBinding binding;
    private HistoricalOrdersViewModel historicalOrdersViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // 初始化 ViewModel
        historicalOrdersViewModel = new ViewModelProvider(this).get(HistoricalOrdersViewModel.class);

        // 綁定佈局
        binding = FragmentHistoricalOrdersBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 查找顯示完成和拒絕訂單的 ListView
        final ListView completedOrdersListView = binding.finOrderList;
        final ListView rejectedOrdersListView = binding.rejOrderList;

        // 建立 ArrayAdapter 以顯示完成和拒絕訂單
        final ArrayAdapter<String> completedOrdersAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
        final ArrayAdapter<String> rejectedOrdersAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);

        // 設置適配器
        completedOrdersListView.setAdapter(completedOrdersAdapter);
        rejectedOrdersListView.setAdapter(rejectedOrdersAdapter);

        // 根據 UID 靜態設置餐廳名稱
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String restaurantName = getRestaurantNameByUid(uid);
        historicalOrdersViewModel.setRestaurantName(restaurantName);

        // 觀察完成訂單列表的數據變化
        historicalOrdersViewModel.getCompletedOrdersList().observe(getViewLifecycleOwner(), completedOrders -> {
            completedOrdersAdapter.clear();
            for (Map.Entry<String, String> entry : completedOrders.entrySet()) {
                String orderKey = entry.getKey();
                String displayText = orderKey.substring(orderKey.length() - 6) + " - " + entry.getValue();
                completedOrdersAdapter.add(displayText);
            }
            completedOrdersAdapter.notifyDataSetChanged();
        });

        // 觀察拒絕訂單列表的數據變化
        historicalOrdersViewModel.getRejectedOrdersList().observe(getViewLifecycleOwner(), rejectedOrders -> {
            rejectedOrdersAdapter.clear();
            for (Map.Entry<String, String> entry : rejectedOrders.entrySet()) {
                String orderKey = entry.getKey();
                String displayText = orderKey.substring(orderKey.length() - 6) + " - " + entry.getValue();
                rejectedOrdersAdapter.add(displayText);
            }
            rejectedOrdersAdapter.notifyDataSetChanged();
        });

        // 設置完成訂單 ListView 項目點擊事件
        completedOrdersListView.setOnItemClickListener((parent, view, position, id) -> {
            String orderId = completedOrdersAdapter.getItem(position).split(" - ")[0];
            showOrderDetailsDialog(getContext(), historicalOrdersViewModel.getCompletedOrdersDetails(orderId));
        });

        // 設置拒絕訂單 ListView 項目點擊事件
        rejectedOrdersListView.setOnItemClickListener((parent, view, position, id) -> {
            String orderId = rejectedOrdersAdapter.getItem(position).split(" - ")[0];
            showOrderDetailsDialog(getContext(), historicalOrdersViewModel.getRejectedOrdersDetails(orderId));
        });

        return root;
    }

    // 根據 UID 返回餐廳名稱
    private String getRestaurantNameByUid(String uid) {
        if ("hhDjGejvu3bGzaoBAe7ymIGJjqP2".equals(uid)) {
            return "美琪晨餐館";
        } else if ("XlIoYWkELHR8gytiJYx7EF6rNHr2".equals(uid)) {
            return "戀茶屋";
        } else if ("sPoPsuMvvafICGhTtFzfkwlYHkQ2".equals(uid)) {
            return "MINI小晨堡";
        } else {
            return "未知餐廳";
        }
    }

    // 顯示訂單詳細資訊的 AlertDialog
    private void showOrderDetailsDialog(Context context, String orderDetails) {
        new AlertDialog.Builder(context)
                .setTitle("訂單詳細資訊")
                .setMessage(orderDetails)
                .setPositiveButton("確定", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
