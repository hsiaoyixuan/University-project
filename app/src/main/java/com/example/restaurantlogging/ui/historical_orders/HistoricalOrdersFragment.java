package com.example.restaurantlogging.ui.historical_orders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AlertDialog;
import com.example.restaurantlogging.databinding.FragmentHistoricalOrdersBinding;
import java.util.HashMap;
import java.util.Map;

public class HistoricalOrdersFragment extends Fragment {

    private FragmentHistoricalOrdersBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // 獲取 ViewModel
        HistoricalOrdersViewModel historicalOrdersViewModel;
        historicalOrdersViewModel = new ViewModelProvider(this).get(HistoricalOrdersViewModel.class);

        // 綁定視圖
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

        // 觀察 LiveData，當數據變化時更新 ListView
        historicalOrdersViewModel.getCompletedOrdersList().observe(getViewLifecycleOwner(), completedOrders -> {
            completedOrdersAdapter.clear();
            completedOrdersAdapter.addAll(completedOrders.keySet()); // 使用訂單的 key (name) 作為列表項目
            completedOrdersAdapter.notifyDataSetChanged();
        });

        historicalOrdersViewModel.getRejectedOrdersList().observe(getViewLifecycleOwner(), rejectedOrders -> {
            rejectedOrdersAdapter.clear();
            rejectedOrdersAdapter.addAll(rejectedOrders.keySet()); // 使用訂單的 key (name) 作為列表項目
            rejectedOrdersAdapter.notifyDataSetChanged();
        });

        // 為 ListView 設置點擊事件監聽器，顯示訂單詳細資訊
        completedOrdersListView.setOnItemClickListener((parent, view, position, id) -> {
            String orderName = completedOrdersAdapter.getItem(position);
            String orderDetails = historicalOrdersViewModel.getCompletedOrdersList().getValue().get(orderName); // 獲取詳細資訊
            showOrderDetailsDialog(orderDetails);
        });

        rejectedOrdersListView.setOnItemClickListener((parent, view, position, id) -> {
            String orderName = rejectedOrdersAdapter.getItem(position);
            String orderDetails = historicalOrdersViewModel.getRejectedOrdersList().getValue().get(orderName); // 獲取詳細資訊
            showOrderDetailsDialog(orderDetails);
        });

        return root;
    }

    private void showOrderDetailsDialog(String orderDetails) {
        new AlertDialog.Builder(getContext())
                .setTitle("訂單詳情")
                .setMessage(orderDetails)
                .setPositiveButton("確定", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;  // 當視圖銷毀時清除 binding
    }
}
