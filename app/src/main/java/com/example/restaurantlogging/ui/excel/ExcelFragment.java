package com.example.restaurantlogging.ui.excel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.restaurantlogging.databinding.FragmentExcelBinding;
import com.example.restaurantlogging.databinding.FragmentHomeBinding;
import com.example.restaurantlogging.databinding.FragmentSearchBinding;
import com.example.restaurantlogging.ui.home.HomeViewModel;
import com.example.restaurantlogging.ui.search.SearchViewModel;

public class ExcelFragment extends Fragment {

    private FragmentExcelBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        ExcelViewModel excelViewModel;
        excelViewModel = new ViewModelProvider(this).get(ExcelViewModel.class);

        binding = FragmentExcelBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.excel;
        excelViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
