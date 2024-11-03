package com.example.restaurantlogging.ui.excel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ExcelViewModel extends ViewModel {

    // 用來保存表格顯示的名稱
    private final MutableLiveData<String> displayName;
    // 用來保存實際的餐廳名稱
    private String restaurantName;

    public ExcelViewModel() {
        displayName = new MutableLiveData<>();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            switch (uid) {
                case "hhDjGejvu3bGzaoBAe7ymIGJjqP2":
                    // 顯示為「餐廳一號」，實際餐廳名稱為「美琪晨餐館」
                    displayName.setValue("美琪晨餐館");
                    restaurantName = "美琪晨餐館";
                    break;
                case "XlIoYWkELHR8gytiJYx7EF6rNHr2":
                    // 顯示為「餐廳二號」，實際餐廳名稱為「戀茶屋」
                    displayName.setValue("戀茶屋");
                    restaurantName = "戀茶屋";
                    break;
                case "sPoPsuMvvafICGhTtFzfkwlYHkQ2":
                    displayName.setValue("MINI小晨堡");
                    restaurantName = "MINI小晨堡";
                    break;
                default:
                    Log.w("ExcelViewModel", "Unknown UID: " + uid);
                    displayName.setValue("Unknown Restaurant");
                    restaurantName = "Unknown Restaurant";
                    break;
            }
        } else {
            Log.w("ExcelViewModel", "No current user logged in");
            displayName.setValue("Unknown Restaurant");
            restaurantName = "Unknown Restaurant";
        }
    }

    // 提供顯示名稱給 UI 層使用
    public LiveData<String> getDisplayName() {
        return displayName;
    }

    // 提供實際餐廳名稱用於資料查詢等操作
    public String getRestaurantName() {
        return restaurantName;
    }
}

