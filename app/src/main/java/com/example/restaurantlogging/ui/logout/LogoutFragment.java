package com.example.restaurantlogging.ui.logout;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AlertDialog;

import com.example.restaurantlogging.MainActivity;
import com.example.restaurantlogging.R;
import com.example.restaurantlogging.databinding.FragmentLogoutBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LogoutFragment extends Fragment {
    Button logout;

    private FragmentLogoutBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        LogoutViewModel logoutViewModel;
        logoutViewModel = new ViewModelProvider(this).get(LogoutViewModel.class);

        binding = FragmentLogoutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Button buttonlogout = root.findViewById(R.id.logout_btn);
        buttonlogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmDialog();
            }
        });

        return root;
    }

    // 显示登出确认对话框
    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("確認登出")
                .setMessage("你確定要登出嗎?(❁´◡`❁)")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> logout())
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // 登出方法
    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
        getActivity().finish(); // 结束当前的 Activity
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
