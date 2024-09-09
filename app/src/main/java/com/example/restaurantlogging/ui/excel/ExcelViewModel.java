package com.example.restaurantlogging.ui.excel;

import static android.content.ContentValues.TAG;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ExcelViewModel extends ViewModel {

    // 持有表格標題的 LiveData
    private final MutableLiveData<String> mText;

    // 持有表格數據的 LiveData
    private final MutableLiveData<String[][]> mTableData;

    public ExcelViewModel() {
        mText = new MutableLiveData<>();
        // 獲取當前用戶
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();

            // 根據 UID 設置表格標題
            switch (uid) {
                case "hhDjGejvu3bGzaoBAe7ymIGJjqP2":
                    mText.setValue("美琪晨餐館");
                    break;
                case "XlIoYWkELHR8gytiJYx7EF6rNHr2":
                    mText.setValue("戀茶屋");
                    break;
                default:
                    Log.w(TAG, "Unknown UID: " + uid);
                    mText.setValue("Unknown Restaurant");
                    break;
            }
        } else {
            Log.w(TAG, "No current user logged in");
            mText.setValue("Unknown Restaurant");
        }

        // 初始化表格數據
        mTableData = new MutableLiveData<>();
        mTableData.setValue(new String[][]{
                {"Header 1", "Header 2", "Header 3"},
                {"Row 1 Column 1", "Row 1 Column 2", "Row 1 Column 3"},
                {"Row 2 Column 1", "Row 2 Column 2", "Row 2 Column 3"}
        });
    }

    // 提供表格標題的 LiveData
    public LiveData<String> getText() {
        return mText;
    }

    // 提供表格數據的 LiveData
    public LiveData<String[][]> getTableData() {
        return mTableData;
    }
}
