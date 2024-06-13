package com.example.restaurantlogging.ui.menu;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantlogging.R;
import com.example.restaurantlogging.databinding.FragmentExcelBinding;
import com.example.restaurantlogging.databinding.FragmentMenuBinding;
import com.example.restaurantlogging.ui.excel.ExcelViewModel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MenuFragment extends Fragment {
    TextView menuTextView;
    private FragmentMenuBinding binding;
    private DatabaseReference menuRef;
    private ValueEventListener menuListener; // 添加事件监听器引用
    private RecyclerView recyclerView;
    private MenuAdapter menuAdapter;
    private List<MenuItem> menuItemList;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        MenuViewModel menuViewModel;
        menuViewModel = new ViewModelProvider(this).get(MenuViewModel.class);
        binding = FragmentMenuBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化RecyclerView
        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 初始化菜单项列表和适配器
        menuItemList = new ArrayList<>();
        menuAdapter = new MenuAdapter(menuItemList);
        recyclerView.setAdapter(menuAdapter);



        getMenuItems();
        return root;


    }
    public void getMenuItems() {
        menuRef = FirebaseDatabase.getInstance().getReference("屏商/美琪晨餐館");
        // 创建事件监听器
        menuListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
                    // 如果活動已經停止或銷毀，不再更新UI
                    return;
                }

                Log.d(TAG, "DataSnapshot received: " + dataSnapshot.toString()); // 添加日志以调试数据

                // 清空旧的数据
                menuItemList.clear();


                // 遍历数据表中的每个子节点
                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                    // 获取每个子节点的键和值
                    String itemName = itemSnapshot.getKey();
                    String itemDescription = itemSnapshot.getValue().toString();
                    Log.d(TAG, "Item: " + itemName + ", Description: " + itemDescription); // 添加日志以调试每个子节点的数据
                    // 创建新的菜单项对象并添加到列表中
                    menuItemList.add(new MenuItem(itemName, itemDescription));

                }
                // 通知适配器数据已更改
                menuAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // 处理取消事件
                Log.w(TAG, "Failed to read menu items.", databaseError.toException());
            }
        };
        menuRef.addValueEventListener(menuListener); // 添加监听器到引用

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        // 釋放 Firebase 資源
        if (menuListener != null && menuRef != null) {
            menuRef.removeEventListener(menuListener);
    }
}
}
