package com.example.restaurantlogging.ui.excel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ExcelViewModel extends ViewModel {

    // 用來保存表格標題或餐廳名稱的 MutableLiveData
    private final MutableLiveData<String> mText;

    public ExcelViewModel() {
        // 初始化 MutableLiveData
        mText = new MutableLiveData<>();
        // 取得當前登入的 Firebase 使用者
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // 檢查是否有使用者登入
        if (currentUser != null) {
            // 取得使用者的 UID
            String uid = currentUser.getUid();
            // 根據 UID 設定對應的餐廳名稱
            switch (uid) {
                case "hhDjGejvu3bGzaoBAe7ymIGJjqP2":
                    // 如果 UID 為指定的，顯示"美琪晨餐館"
                    mText.setValue("美琪晨餐館");
                    break;
                case "XlIoYWkELHR8gytiJYx7EF6rNHr2":
                    // 如果 UID 為指定的，顯示"戀茶屋"
                    mText.setValue("戀茶屋");
                    break;
                default:
                    // 如果 UID 不在已知列表中，則顯示"Unknown Restaurant"
                    Log.w("ExcelViewModel", "Unknown UID: " + uid);
                    mText.setValue("Unknown Restaurant");
                    break;
            }
        } else {
            // 如果未登入，顯示"Unknown Restaurant"
            Log.w("ExcelViewModel", "No current user logged in");
            mText.setValue("Unknown Restaurant");
        }
    }

    // 提供 LiveData 給 UI 層使用
    public LiveData<String> getText() {
        return mText;
    }
}
