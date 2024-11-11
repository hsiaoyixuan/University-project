package com.example.restaurantlogging.ui.menu;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantlogging.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {
    private List<MenuItem> menuItems;
    private Context context;

    // FirebaseAuth 用於獲取當前登入用戶
    private FirebaseAuth mAuth;

    // 使用 Map 保存 UID 和菜單路徑的映射
    private Map<String, String> uidToMenuPathMap;

    // 構造函數，傳遞菜單項目和上下文
    public MenuAdapter(Context context, List<MenuItem> menuItems) {
        this.context = context;
        this.menuItems = menuItems;
        mAuth = FirebaseAuth.getInstance();  // 初始化 FirebaseAuth
        initializeUidToMenuPathMap(); // 初始化 UID 和菜單路徑的對應
    }

    // 初始化 UID 對應的路徑
    private void initializeUidToMenuPathMap() {
        uidToMenuPathMap = new HashMap<>();
        uidToMenuPathMap.put("hhDjGejvu3bGzaoBAe7ymIGJjqP2", "校區/屏商校區/美琪晨餐館/食物");
        uidToMenuPathMap.put("XlIoYWkELHR8gytiJYx7EF6rNHr2", "校區/屏師校區/戀茶屋/食物");
        uidToMenuPathMap.put("sPoPsuMvvafICGhTtFzfkwlYHkQ2", "校區/民生校區/MINI小晨堡/食物");
        // 可以在此处继续添加更多UID与路径的映射
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 使用 LayoutInflater 加載 item 布局
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.menu_item, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        // 綁定數據到 ViewHolder
        MenuItem item = menuItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        // 返回菜單項目的數量
        return menuItems.size();
    }

    public class MenuViewHolder extends RecyclerView.ViewHolder {
        private TextView itemNameTextView;
        private ListView itemDescriptionListView;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            // 初始化 itemView 中的 TextView
            itemNameTextView = itemView.findViewById(R.id.itemName);
            itemDescriptionListView = itemView.findViewById(R.id.itemDescriptionListView);

            // 為 itemNameTextView 設置點擊事件監聽器
            itemNameTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 取得當前菜單項目
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        MenuItem item = menuItems.get(position);
                        // 切換 descriptionVisible 變量
                        item.setDescriptionVisible(!item.isDescriptionVisible());
                        // 通知適配器指定位置的數據已更改
                        notifyItemChanged(position);
                    }
                }
            });

            // 在点击“新增”时，添加额外的 Intent 标志，指示这是“新增”操作
            itemDescriptionListView.setOnItemClickListener((parent, view, position, id) -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    MenuItem currentItem = menuItems.get(adapterPosition);
                    if (position >= 0 && position < currentItem.getDescriptions().size()) {
                        // 點擊現有描述項
                        startEditMenuItemActivity(currentItem, false, position);
                    } else if (position == currentItem.getDescriptions().size()) {
                        // 點擊 "新增" 項
                        startEditMenuItemActivity(currentItem, true, position);
                    }
                }
            });
        }

        public void bind(MenuItem item) {
            // 控制描述列表顯示或隱藏
            if (item.isDescriptionVisible()) {
                itemDescriptionListView.setVisibility(View.VISIBLE);
            } else {
                itemDescriptionListView.setVisibility(View.GONE);
            }

            // 設置菜單項目名稱
            itemNameTextView.setText(item.getName());

            // 構建描述列表並添加 "新增" 項
            List<String> descriptions = new ArrayList<>(item.getDescriptions());
            descriptions.add("新增"); // 在末尾添加“新增”項


            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.description_item, descriptions);
            itemDescriptionListView.setAdapter(adapter);
            setListViewHeightBasedOnItems(itemDescriptionListView);
        }

        private void setListViewHeightBasedOnItems(ListView listView) {
            ArrayAdapter adapter = (ArrayAdapter) listView.getAdapter();
            if (adapter == null) {
                return;
            }

            int totalHeight = 0;
            for (int i = 0; i < adapter.getCount(); i++) {
                View listItem = adapter.getView(i, null, listView);
                listItem.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                totalHeight += listItem.getMeasuredHeight();
            }

            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
            listView.setLayoutParams(params);
            listView.requestLayout();
        }

        private void startEditMenuItemActivity(MenuItem item, boolean isNew, int position) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                // 若未登入，顯示提示或進行其他處理
                return;
            }

            Intent intent = new Intent(context, EditMenuItemActivity.class);
            intent.putExtra("itemName", item.getName());
            intent.putExtra("menuPath", uidToMenuPathMap.get(currentUser.getUid()));

            if (isNew) {
                intent.putExtra("isNew", true); // 添加標志以指示是“新增”
            } else {
                String selectedDescription = item.getDescriptions().get(position);
                String selectedDetail = item.getItemDetails().get(position);
                intent.putExtra("itemDescription", selectedDescription);
                intent.putExtra("itemDetail", selectedDetail);
                intent.putExtra("isNew", false); // 添加標志以指示不是“新增”
            }

            context.startActivity(intent);
        }
    }
}
