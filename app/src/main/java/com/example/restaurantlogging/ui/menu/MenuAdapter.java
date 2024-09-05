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

import java.util.ArrayList;
import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {
    private List<MenuItem> menuItems;
    private Context context;

    // 构造函数，用于传递菜单项数据
    public MenuAdapter(Context context,List<MenuItem> menuItems) {
        this.context= context;// 确保正确传递context
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

    public class MenuViewHolder extends RecyclerView.ViewHolder {
        private TextView itemNameTextView;
        private ListView itemDescriptionListView;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            // 初始化itemView中的TextView
            itemNameTextView = itemView.findViewById(R.id.itemName);
            itemDescriptionListView = itemView.findViewById(R.id.itemDescriptionListView);
            // 新增的部分：設置itemNameTextView的點擊事件監聽器
            itemNameTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 取得當前菜單項
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        MenuItem item = menuItems.get(position);
                        // 切換descriptionVisible變量
                        item.setDescriptionVisible(!item.isDescriptionVisible());
                        // 通知適配器指定位置的數據已更改
                        notifyItemChanged(position);
                    }
                }
            });
            // 添加 ListView 的点击事件处理
            itemDescriptionListView.setOnItemClickListener((parent, view, position, id) -> {
                // 获取当前菜单项的描述行
                int adapterPosition = getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    MenuItem currentItem = menuItems.get(adapterPosition);
                    String selectedDescription = currentItem.getDescriptions().get(position);
                    String selectedDetail = currentItem.getItemDetails().get(position);
                    // 启动 EditMenuItemActivity 并传递详细信息
                    Intent intent = new Intent(context, EditMenuItemActivity.class);
                    intent.putExtra("itemName", currentItem.getName());
                    intent.putExtra("itemDescription", selectedDescription);
                    intent.putExtra("itemDetail", selectedDetail); // 传递itemDetail
                    context.startActivity(intent);
                }
            });
        }

        public void bind(MenuItem item) {
            // 根据描述是否可见设置描述的可见性
            if (item.isDescriptionVisible()) {
                itemDescriptionListView.setVisibility(View.VISIBLE); // 如果描述可见，则设置描述可见
            } else {
                itemDescriptionListView.setVisibility(View.GONE); // 如果描述不可见，则隐藏描述
            }
            // 绑定数据到TextView
            itemNameTextView.setText(item.getName());

            // 使用描述列表填充ListView
            List<String> descriptions = item.getDescriptions();

            // 使用ArrayAdapter将描述列表添加到ListView
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.description_item, descriptions);
            itemDescriptionListView.setAdapter(adapter);
            // 动态设置 ListView 高度，以适应内容
            setListViewHeightBasedOnItems(itemDescriptionListView);
        }

        // 动态设置 ListView 高度的方法
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