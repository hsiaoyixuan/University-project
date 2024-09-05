package com.example.restaurantlogging;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;  // 導入 FirebaseAuth 庫，用於管理用戶身份驗證

import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.restaurantlogging.databinding.ActivityHomeBinding;

public class home extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityHomeBinding binding;
    private TextView restaurantname, opentime;
    private FirebaseAuth mAuth;  // FirebaseAuth 變數，用於身份驗證

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarHome.toolbar);
        binding.appBarHome.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_historicalorders, R.id.nav_excel, R.id.nav_menu, R.id.nav_service, R.id.nav_order, R.id.nav_logout)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_home);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // 初始化 FirebaseAuth
        mAuth = FirebaseAuth.getInstance();  // 新增：初始化 FirebaseAuth，用於後續獲取用戶 UID
        String uid = getIntent().getStringExtra("uid");  // 新增：獲取從 MainActivity 傳遞過來的 UID

        // 初始化導航頭部的 TextView，顯示餐廳名稱和營業時間
        View headerView = navigationView.getHeaderView(0);  // 新增：獲取導航頭部視圖
        restaurantname = headerView.findViewById(R.id.restaurant_name);  // 新增：初始化餐廳名稱的 TextView
        opentime = headerView.findViewById(R.id.open_time);  // 新增：初始化營業時間的 TextView

        // 根據 UID 設置不同的餐廳名稱和營業時間
        setRestaurantInfo(uid);  // 新增：調用方法來根據 UID 設置餐廳信息


    }

    // 新增此方法：根據 UID 設置不同的餐廳名稱和營業時間
    private void setRestaurantInfo(String uid) {
        switch (uid) {
            case "hhDjGejvu3bGzaoBAe7ymIGJjqP2":  // 修改：為每個 UID 設定不同的餐廳名稱和營業時間
                restaurantname.setText("美琪晨餐廳");
                opentime.setText("營業時間: 08:00 - 22:00");
                break;
            case "XlIoYWkELHR8gytiJYx7EF6rNHr2":
                restaurantname.setText("戀茶屋");
                opentime.setText("營業時間: 09:00 - 21:00");
                break;
            case "UID3":
                restaurantname.setText("餐廳名稱 C");
                opentime.setText("營業時間: 10:00 - 20:00");
                break;
            // 可以根據需要添加更多用戶的設定
            default:  // 修改：處理未知 UID 的情況，顯示默認的“未知”信息
                restaurantname.setText("未知餐廳");
                opentime.setText("未知營業時間");
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_home);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // 新增：用於處理用戶登出並返回登錄頁面的邏輯
    public void surelogout(View V) {
        Intent intent = new Intent(home.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
