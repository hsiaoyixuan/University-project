package com.example.restaurantlogging.ui.excel;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.restaurantlogging.databinding.FragmentExcelBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class ExcelFragment extends Fragment {

    // 綁定物件，用於訪問 XML 中的視圖
    private FragmentExcelBinding binding;
    private String selectedDate;  // 保存選定日期的變量

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // 初始化 ViewModel
        ExcelViewModel excelViewModel =
                new ViewModelProvider(this).get(ExcelViewModel.class);

        // 使用綁定類來擴展佈局
        binding = FragmentExcelBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 設定餐廳名稱的 TextView 的內容
        final TextView textView = binding.rsName;
        excelViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // 在初始化時設置當天日期和星期到 date 的 TextView 中
        setDateToToday();  // 調用方法設置當天日期

        // 顯示當天的訂單資料
        fetchOrdersData(binding.tableLayout, selectedDate);  // 使用當天日期加載對應的訂單數據

        // 設置日期選擇按鈕的點擊事件處理
        binding.btnSelectDate.setOnClickListener(v -> onSelectDateClicked());

        return root;
    }

    // 設置當天日期和星期到 date 的 TextView
    private void setDateToToday() {
        Calendar calendar = Calendar.getInstance();  // 獲取當前時間的 Calendar 實例
        Date today = calendar.getTime();  // 獲取當前日期

        // 使用 SimpleDateFormat 格式化日期和星期，格式為 "yyyy/MM/dd EEEE"
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd EEEE", Locale.getDefault());
        String formattedDate = dateFormat.format(today);  // 將日期格式化

        // 將格式化後的日期顯示在 TextView 中
        binding.date.setText(formattedDate);

        // 初始化 selectedDate 為當天日期，用於後續數據過濾
        selectedDate = new SimpleDateFormat("MM/dd", Locale.getDefault()).format(today);
    }

    // 日期選擇處理方法，當用戶選擇日期時調用
    private void onSelectDateClicked() {
        Calendar calendar = Calendar.getInstance();  // 獲取當前時間的 Calendar 實例

        // 創建日期選擇對話框
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    // 設定選定的日期格式，格式為 "MM/dd"
                    selectedDate = String.format("%02d/%02d", month + 1, dayOfMonth);

                    // 將用戶選擇的日期設置到 Calendar 對象中
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    Date selectedDateObj = selectedCalendar.getTime();  // 獲取選擇的日期對象

                    // 格式化用戶選擇的日期和星期，格式為 "yyyy/MM/dd EEEE"
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd EEEE", Locale.getDefault());
                    String formattedDate = dateFormat.format(selectedDateObj);

                    // 將選擇的日期和星期顯示在 date 的 TextView 中
                    binding.date.setText(formattedDate);

                    // 根據選定的日期過濾並顯示對應的訂單數據
                    fetchOrdersData(binding.tableLayout, selectedDate);
                },
                calendar.get(Calendar.YEAR),  // 初始化年份
                calendar.get(Calendar.MONTH),  // 初始化月份
                calendar.get(Calendar.DAY_OF_MONTH)  // 初始化天
        );

        // 顯示日期選擇對話框
        datePickerDialog.show();
    }

    
    // 根據選定的日期從 Firebase 獲取訂單數據並生成表格
    private void fetchOrdersData(TableLayout tableLayout, String date) {
        HashMap<String, int[]> itemsMap = new HashMap<>();
        int totalAmount = 0;  // 用于累积总金额的变量

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ordersRef = database.getReference("Orders");

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                itemsMap.clear();
                tableLayout.removeAllViews();

                TableRow headerRow = new TableRow(getContext());
                String[] headers = {"品項", "單價", "數量", "小計"};
                for (String header : headers) {
                    TextView textView = new TextView(getContext());
                    textView.setText(header);
                    textView.setPadding(8, 8, 8, 8);
                    headerRow.addView(textView);
                }
                tableLayout.addView(headerRow);

                int totalAmount = 0;  // 用于计算总金额

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String orderDate = orderSnapshot.child("selectedDate").getValue(String.class);
                    String status = orderSnapshot.child("接單狀況").getValue(String.class);

                    if (date.equals(orderDate) && "完成訂單".equals(status)) {
                        for (DataSnapshot itemSnapshot : orderSnapshot.child("items").getChildren()) {
                            String title = itemSnapshot.child("title").getValue(String.class);
                            String price = itemSnapshot.child("description").getValue(String.class);
                            int quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                            int subtotal = calculateSubtotal(price, quantity);

                            totalAmount += subtotal;  // 累加小计到总金额

                            if (itemsMap.containsKey(title)) {
                                int[] currentData = itemsMap.get(title);
                                currentData[0] += quantity;
                                currentData[1] += subtotal;
                            } else {
                                itemsMap.put(title, new int[]{quantity, subtotal});
                            }
                        }
                    }
                }

                // 遍历 HashMap 并将累积结果添加到表格中
                for (Map.Entry<String, int[]> entry : itemsMap.entrySet()) {
                    String item = entry.getKey();
                    String quantity = String.valueOf(entry.getValue()[0]);
                    String subtotal = String.valueOf(entry.getValue()[1]) + "$";
                    String price = String.valueOf(entry.getValue()[1] / entry.getValue()[0]) + "$";

                    addRowToTable(tableLayout, item, price, quantity, subtotal);
                }

                // 在表格加载完成后显示总金额
                binding.totalAmount.setText("當日總金額:$" + totalAmount );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // 处理 Firebase 查询取消的情况
            }
        });
    }



    // 計算小計
    private int calculateSubtotal(String price, int quantity) {
        // 去掉價格中的 $ 符號，並將其轉換為整數後乘以數量
        return Integer.parseInt(price.replace("$", "")) * quantity;
    }

    // 將數據添加到表格中
    private void addRowToTable(TableLayout tableLayout, String item, String price, String quantity, String subtotal) {
        TableRow tableRow = new TableRow(getContext());  // 創建一個新的 TableRow

        // 將每個欄位的數據添加到 TableRow 中
        String[] rowData = {item, price, quantity, subtotal};
        for (String data : rowData) {
            TextView textView = new TextView(getContext());
            textView.setText(data);  // 設置 TextView 的文本為對應的數據
            textView.setPadding(8, 8, 8, 8);  // 設置內邊距
            tableRow.addView(textView);  // 將 TextView 添加到 TableRow 中
        }

        // 將這個新的 TableRow 添加到 TableLayout 中，形成表格中的一行
        tableLayout.addView(tableRow);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;  // 清理綁定對象以防止內存洩漏
    }
}