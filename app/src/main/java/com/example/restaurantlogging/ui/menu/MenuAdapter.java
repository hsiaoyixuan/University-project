package com.example.restaurantlogging.ui.menu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.restaurantlogging.R;

import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {
    private List<MenuItem> menuItems;

    // 构造函数，用于传递菜单项数据
    public MenuAdapter(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 使用LayoutInflater加载item布局
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.menu_item, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        // 绑定数据到ViewHolder
        MenuItem item = menuItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        // 返回菜单项的数量
        return menuItems.size();
    }

    public static class MenuViewHolder extends RecyclerView.ViewHolder {
        private TextView itemNameTextView;
        private TextView itemDescriptionTextView;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            // 初始化itemView中的TextView
            itemNameTextView = itemView.findViewById(R.id.itemName);
            itemDescriptionTextView = itemView.findViewById(R.id.itemDescription);
        }

        public void bind(MenuItem item) {
            // 绑定数据到TextView
            itemNameTextView.setText(item.getName());
            itemDescriptionTextView.setText(item.getDescription());
        }
    }
}
