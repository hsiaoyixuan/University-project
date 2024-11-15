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
        uidToMenuPathMap.put("pUIVdsTMPNOcoWPSFh1Z9WL1Mfu2", "校區/屏商校區/紅鈕扣/食物");
        uidToMenuPathMap.put("XlIoYWkELHR8gytiJYx7EF6rNHr2", "校區/屏師校區/戀茶屋/食物");
        uidToMenuPathMap.put("sPoPsuMvvafICGhTtFzfkwlYHkQ2", "校區/民生校區/MINI小晨堡/食物");
        uidToMenuPathMap.put("qT0C0R1VhhblSSOy5Wj2ZgxNeju2", "校區/民生校區/阿布早午餐/食物");
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
                // 確保 Activity 仍在運行，避免因 Activity 銷毀而導致錯誤
                if (getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
                    return; // 防止在Activity銷毀時更新UI
                }

                Log.d(TAG, "DataSnapshot received: " + dataSnapshot.toString()); // 日誌以調試數據

                // 清空舊的數據，準備載入新的菜單資料
                menuItemList.clear();

                // 遍歷資料庫中的每個子節點
                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                    // 取得品項名稱，直接使用，不做格式限制
                    String itemName = itemSnapshot.getKey();

                    // 確保 itemName 不為空
                    if (itemName != null) {
                        // 初始化描述和詳細信息的列表
                        List<String> itemDescriptions = new ArrayList<>();
                        List<String> itemDetails = new ArrayList<>();
                        boolean isClosed = false; // 默認為未關閉

                        // 遍歷每個品項的詳細信息
                        for (DataSnapshot detailSnapshot : itemSnapshot.getChildren()) {
                            String key = detailSnapshot.getKey();
                            String value = detailSnapshot.getValue().toString();

                            // 將詳細信息以 "鍵: 值" 格式加入列表
                            itemDetails.add(key + ": " + value);

                            // 檢查是否有 'closed' 鍵來判斷品項是否關閉
                            if (key.equals("closed")) {
                                isClosed = Boolean.parseBoolean(value); // 轉換為布爾值
                            }
                            // 檢查值中是否包含 '價格='，提取價格並添加到描述列表中
                            else if (value.contains("價格=")) {
                                try {
                                    // 使用正則表達式和分割提取價格值
                                    String number = value.split("價格=")[1].split(",")[0].replaceAll("[^0-9.]", "");
                                    itemDescriptions.add(key + ": " + number); // 將價格添加到描述列表中
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    // 當分割數組超出邊界時捕獲異常
                                    Log.e(TAG, "Error parsing price from value: " + value, e);
                                }
                            }
                        }

                        // 調試用的日誌，顯示每個品項的名稱、描述和詳細信息
                        Log.d(TAG, "Item: " + itemName + ", Descriptions: " + itemDescriptions.toString() + ", Details: " + itemDetails.toString());

                        // 將新建的 MenuItem 對象添加到列表中
                        menuItemList.add(new MenuItem(itemName, itemDescriptions, itemDetails, isClosed));
                    }
                }

                // 通知 RecyclerView 的適配器更新UI
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