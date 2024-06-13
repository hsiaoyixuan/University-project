package com.example.restaurantlogging.ui.excel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
public class ExcelViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ExcelViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("報表");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
