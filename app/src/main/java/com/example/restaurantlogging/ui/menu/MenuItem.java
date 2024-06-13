package com.example.restaurantlogging.ui.menu;

public class MenuItem {
    private String name;
    private String description;

    public MenuItem() {
        // 空的构造函数，用于Firebase反序列化
    }

    public MenuItem(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
