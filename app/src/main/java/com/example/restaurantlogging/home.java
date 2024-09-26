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

import java.util.Calendar;

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
                R.id.nav_historicalorders, R.id.nav_excel, R.id.nav_menu, R.id.nav_order, R.id.nav_logout)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_home);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // 初始化 FirebaseAuth
        mAuth = FirebaseAuth.getInstance();  // 初始化 FirebaseAuth，用於後續獲取用戶 UID
        String uid = getIntent().getStringExtra("uid");  // 獲取從 MainActivity 傳遞過來的 UID

        // 初始化導航頭部的 TextView，顯示餐廳名稱和營業時間
        View headerView = navigationView.getHeaderView(0);
        restaurantname = headerView.findViewById(R.id.restaurant_name);
        opentime = headerView.findViewById(R.id.open_time);

        // 根據 UID 設置不同的餐廳名稱和營業時間
        String restaurantName = setRestaurantInfo(uid);  // 設置餐廳信息並返回餐廳名稱

        // 修改：傳遞餐廳名稱給 OrderFragment
        Bundle bundle = new Bundle();
        bundle.putString("restaurantName", restaurantName);  // 將餐廳名稱存入 Bundle
        navController.navigate(R.id.nav_order, bundle);  // 導航到 OrderFragment，並傳遞 Bundle
    }

    private String setRestaurantInfo(String uid) {
        String restaurantName;
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        boolean isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);

        switch (uid) {
            case "hhDjGejvu3bGzaoBAe7ymIGJjqP2":
                //restaurantname.setText("美琪晨餐館");
                restaurantname.setText("餐廳一號");
                if (isWeekend) {
                    opentime.setText("假日營業時間 10:00~14:00");
                } else {
                    opentime.setText("平日營業時間 6:30~10:50 & 16:00~19:00");
                }
                restaurantName = "美琪晨餐館";  // 返回餐廳名稱
                break;
            case "XlIoYWkELHR8gytiJYx7EF6rNHr2":
                //restaurantname.setText("戀茶屋");
                restaurantname.setText("餐廳二號");
                if (isWeekend) {
                    opentime.setText("假日營業時間 10:00~14:00");
                } else {
                    opentime.setText("平日營業時間 6:30~10:50 & 16:00~19:00");
                }
                restaurantName = "戀茶屋";  // 返回餐廳名稱
                break;
            case "UID3":
                restaurantname.setText("餐廳名稱 C");
                if (isWeekend) {
                    opentime.setText("假日營業時間 10:00~14:00");
                } else {
                    opentime.setText("平日營業時間 10:00 - 20:00");
                }
                restaurantName = "餐廳名稱 C";  // 返回餐廳名稱
                break;
            default:
                restaurantname.setText("未知餐廳");
                opentime.setText("未知營業時間");
                restaurantName = "未知餐廳";  // 返回默認餐廳名稱
                break;
        }
        return restaurantName;  // 返回對應的餐廳名稱
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

    public void surelogout(View V) {
        Intent intent = new Intent(home.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
