package com.example.restaurantlogging.ui.logout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.restaurantlogging.databinding.FragmentHomeBinding;
import com.example.restaurantlogging.databinding.FragmentLogoutBinding;
import com.example.restaurantlogging.databinding.FragmentMenuBinding;
import com.example.restaurantlogging.ui.home.HomeViewModel;
import com.example.restaurantlogging.ui.menu.MenuViewModel;

public class LogoutFragment extends Fragment {

    private FragmentLogoutBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        LogoutViewModel logoutViewModel;
        logoutViewModel = new ViewModelProvider(this).get(LogoutViewModel.class);

        binding = FragmentLogoutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
