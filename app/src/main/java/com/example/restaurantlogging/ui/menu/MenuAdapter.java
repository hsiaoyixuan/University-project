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

            // 添加 ListView 的點擊事件處理
            itemDescriptionListView.setOnItemClickListener((parent, view, position, id) -> {
                // 獲取當前菜單項的描述行
                int adapterPosition = getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    MenuItem currentItem = menuItems.get(adapterPosition);
                    String selectedDescription = currentItem.getDescriptions().get(position);
                    String selectedDetail = currentItem.getItemDetails().get(position);

                    // 獲取當前登入的用戶 UID
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    String uid = currentUser != null ? currentUser.getUid() : null;

                    // 根據 UID 獲取對應的菜單路徑
                    String menuPath = uidToMenuPathMap.get(uid);

                    // 啟動 EditMenuItemActivity 並傳遞詳細信息、UID 和菜單路徑
                    Intent intent = new Intent(context, EditMenuItemActivity.class);
                    intent.putExtra("itemName", currentItem.getName());
                    intent.putExtra("itemDescription", selectedDescription);
                    intent.putExtra("itemDetail", selectedDetail);
                    intent.putExtra("uid", uid);  // 傳遞 UID
                    intent.putExtra("menuPath", menuPath);  // 傳遞對應的菜單路徑
                    context.startActivity(intent);
                }
            });
        }

        public void bind(MenuItem item) {
            // 根據描述是否可見設置描述的可見性
            if (item.isDescriptionVisible()) {
                itemDescriptionListView.setVisibility(View.VISIBLE); // 如果描述可見，設置描述可見
            } else {
                itemDescriptionListView.setVisibility(View.GONE); // 如果描述不可見，則隱藏描述
            }
            // 綁定數據到 TextView
            itemNameTextView.setText(item.getName());

            // 使用描述列表填充 ListView
            List<String> descriptions = item.getDescriptions();

            // 使用 ArrayAdapter 將描述列表添加到 ListView
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.description_item, descriptions);
            itemDescriptionListView.setAdapter(adapter);
            // 動態設置 ListView 高度，以適應內容
            setListViewHeightBasedOnItems(itemDescriptionListView);
        }

        // 動態設置 ListView 高度的方法
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
    }
}
