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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuFragment extends Fragment {
    private FragmentMenuBinding binding;
    private DatabaseReference menuRef;
    private ValueEventListener menuListener; // 添加事件监听器引用
    private RecyclerView recyclerView;
    private MenuAdapter menuAdapter;
    private List<MenuItem> menuItemList;

    // 使用Map来存储UID与路径的映射
    private Map<String, String> uidToMenuPathMap;

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
        menuAdapter = new MenuAdapter(getContext(), menuItemList);// 传递上下文
        recyclerView.setAdapter(menuAdapter);

        // 初始化UID与菜单路径的映射
        initializeUidToMenuPathMap();

        // 获取菜单项
        getMenuItems();

        return root;
    }

    // 初始化 UID 对应路径的 Map
    private void initializeUidToMenuPathMap() {
        uidToMenuPathMap = new HashMap<>();
        uidToMenuPathMap.put("hhDjGejvu3bGzaoBAe7ymIGJjqP2", "校區/屏商校區/美琪晨餐館/食物");
        uidToMenuPathMap.put("XlIoYWkELHR8gytiJYx7EF6rNHr2", "校區/屏師校區/戀茶屋/食物");
        uidToMenuPathMap.put("sPoPsuMvvafICGhTtFzfkwlYHkQ2", "校區/民生校區/MINI小晨堡/食物");
        // 可以在此处继续添加更多UID与路径的映射
    }

    public void getMenuItems() {
        // 獲取當前用戶
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : null;  // 如果用户已登录，获取其 UID

        // 根據用戶的 UID 獲取對應的菜單路徑
        if (uid != null && uidToMenuPathMap.containsKey(uid)) {
            String menuPath = uidToMenuPathMap.get(uid);
            menuRef = FirebaseDatabase.getInstance().getReference(menuPath);
        } else {
            Log.w(TAG, "Unknown UID or no mapping found for UID: " + uid);
            return;
        }

        // 創建事件監聽器
        menuListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
                    // 如果活動已經停止或銷毀，不再更新UI
                    return;
                }

                Log.d(TAG, "DataSnapshot received: " + dataSnapshot.toString()); // 添加日誌以調試數據

                // 清空舊的數據
                menuItemList.clear();

                // 遍歷數據表中的每個子節點
                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                    String itemName = itemSnapshot.getKey();
                    List<String> itemDescriptions = new ArrayList<>(); // 用于存储多个描述
                    List<String> itemDetails = new ArrayList<>(); // 用于存储多个详细信息
                    boolean isClosed = false; // 默认为未关闭
                    for (DataSnapshot detailSnapshot : itemSnapshot.getChildren()) {
                        String key = detailSnapshot.getKey();
                        String value = detailSnapshot.getValue().toString();
                        itemDetails.add(key + ": " + value); // 存储完整的详细信息

                        if (key.equals("closed")) {
                            isClosed = Boolean.parseBoolean(value);// 获取closed状态
                        } else if (value.contains("價格=")) {
                            try {
                                String number = value.split("價格=")[1].split(",")[0].replaceAll("[^0-9.]", "");
                                itemDescriptions.add(key + ": " + number);
                            } catch (ArrayIndexOutOfBoundsException e) {
                                Log.e(TAG, "Error parsing price from value: " + value, e);
                            }
                        }
                    }

                    Log.d(TAG, "Item: " + itemName + ", Descriptions: " + itemDescriptions.toString() + ", Details: " + itemDetails.toString());

                    menuItemList.add(new MenuItem(itemName, itemDescriptions, itemDetails, isClosed));
                }

                menuAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // 處理取消事件
                Log.w(TAG, "Failed to read menu items.", databaseError.toException());
            }
        };
        menuRef.addValueEventListener(menuListener); // 添加監聽器到引用
    }
}