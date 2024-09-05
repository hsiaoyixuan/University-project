package com.example.restaurantlogging.ui.menu;

import java.util.List;

public class MenuItem {
    private String name;
    private List<String> descriptions; // 将描述修改为列表
    private List<String> itemDetails; // 新增字段用于存储详细信息列表
    private boolean descriptionVisible; // 新增描述是否可見的變量
    private boolean isClosed; // 新增字段表示是否关闭

    public MenuItem() {
        // 空的构造函数，用于Firebase反序列化
    }

    public MenuItem(String name, List<String> descriptions, List<String> itemDetails,boolean isClosed) {
        this.name = name;
        this.descriptions = descriptions;
        this.itemDetails = itemDetails; // 初始化新的字段
        this.descriptionVisible = false; // 預設為不可見
        this.isClosed = isClosed;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public List<String> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<String> descriptions) {
        this.descriptions = descriptions;
    }

    public List<String> getItemDetails() {
        return itemDetails; // 新增getter方法
    }

    public void setItemDetails(List<String> itemDetails) {
        this.itemDetails = itemDetails; // 新增setter方法
    }

    public boolean isDescriptionVisible() {

        return descriptionVisible;
    }

    public void setDescriptionVisible(boolean descriptionVisible) {
        this.descriptionVisible = descriptionVisible;
    }
    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }


}