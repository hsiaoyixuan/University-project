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

        // 1. 根據 UID 靜態設置餐廳名稱
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String restaurantName;

        // 2. 使用 UID 判斷對應的餐廳名稱
        if ("hhDjGejvu3bGzaoBAe7ymIGJjqP2".equals(uid)) {
            restaurantName = "美琪晨餐館";  // UID 對應美琪晨餐館
        } else if ("XlIoYWkELHR8gytiJYx7EF6rNHr2".equals(uid)) {
            restaurantName = "戀茶屋";  // UID 對應戀茶屋
        } else if("sPoPsuMvvafICGhTtFzfkwlYHkQ2".equals(uid)) {
            restaurantName = "MINI小晨堡";  // UID 對應戀茶屋
        }
        else {
            restaurantName = "未知餐廳";  // 如果 UID 不匹配，設置為未知
        }

        // 3. 直接設置餐廳名稱到 ViewModel 中
        historicalOrdersViewModel.setRestaurantName(restaurantName);

        // 觀察 ViewModel 中的完成訂單列表，並在數據變化時更新 ListView
        historicalOrdersViewModel.getCompletedOrdersList().observe(getViewLifecycleOwner(), completedOrders -> {
            completedOrdersAdapter.clear();
            for (Map.Entry<String, String> entry : completedOrders.entrySet()) {
                String orderKey = entry.getKey();
                String displayText = orderKey.substring(orderKey.length() - 6) + " - " + entry.getValue();
                completedOrdersAdapter.add(displayText);
            }
            completedOrdersAdapter.notifyDataSetChanged();
        });

        // 觀察 ViewModel 中的拒絕訂單列表，並在數據變化時更新 ListView
        historicalOrdersViewModel.getRejectedOrdersList().observe(getViewLifecycleOwner(), rejectedOrders -> {
            rejectedOrdersAdapter.clear();
            for (Map.Entry<String, String> entry : rejectedOrders.entrySet()) {
                String orderKey = entry.getKey();
                String displayText = orderKey.substring(orderKey.length() - 6) + " - " + entry.getValue();
                rejectedOrdersAdapter.add(displayText);
            }
            rejectedOrdersAdapter.notifyDataSetChanged();
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
