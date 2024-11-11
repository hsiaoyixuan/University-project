package com.example.restaurantlogging.ui.excel;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.restaurantlogging.R;
import com.example.restaurantlogging.databinding.FragmentExcelBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.github.mikephil.charting.formatter.ValueFormatter;
public class ExcelFragment extends Fragment {

    private FragmentExcelBinding binding;
    private String selectedDate;
    private Calendar startOfWeek;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentExcelBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 取得 ExcelViewModel 中的顯示名稱與實際名稱
        ExcelViewModel excelViewModel = new ViewModelProvider(this).get(ExcelViewModel.class);
        excelViewModel.getDisplayName().observe(getViewLifecycleOwner(), restaurantName -> {
            // 顯示對應的餐廳名稱（餐廳一號或餐廳二號）
            binding.rsName.setText(restaurantName);

            // 使用實際餐廳名稱進行資料查詢和顯示
            String actualRestaurantName = excelViewModel.getRestaurantName();
            fetchOrdersData(binding.tableLayout, selectedDate, actualRestaurantName);  // 表格和圓餅圖依照選擇日期
            fetchWeeklySalesData(actualRestaurantName);  // 長條圖顯示一週的銷售額
        });

        // 初始化 UI 元件
        Button btnShowTable = binding.btnShowTable;
        Button btnShowPieChart = binding.btnShowPieChart;
        Button btnShowBarChart = binding.btnShowBarChart;
        ScrollView tableScrollView = binding.tableScrollView;
        PieChart pieChart = binding.pieChart;
        BarChart barChart = binding.barChart;

        // 預設顯示表格模式
        showTable(tableScrollView, pieChart, barChart);

        // 表格模式按鈕點擊事件
        btnShowTable.setOnClickListener(v -> {
            showTable(tableScrollView, pieChart, barChart);
        });

        // 圓餅圖模式按鈕點擊事件
        btnShowPieChart.setOnClickListener(v -> {
            showPieChart(tableScrollView, pieChart, barChart);
            // 確保圓餅圖刷新
            fetchOrdersData(binding.tableLayout, selectedDate, excelViewModel.getRestaurantName());
        });

        // 長條圖模式按鈕點擊事件
        btnShowBarChart.setOnClickListener(v -> {
            showBarChart(tableScrollView, pieChart, barChart);
            // 確保長條圖刷新
            fetchWeeklySalesData(excelViewModel.getRestaurantName());
        });

        // 設置當前日期並加載數據
        setDateToToday();
        binding.btnSelectDate.setOnClickListener(v -> onSelectDateClicked());

        return root;
    }


    // 設置今天的日期並設定當週開始日
    private void setDateToToday() {
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd EEEE", Locale.getDefault());
        String formattedDate = dateFormat.format(today);
        binding.date.setText(formattedDate);

        selectedDate = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(today);
        setStartOfWeek(calendar);  // 設置當前周的開始日（周日）
    }

    // 設置一週的開始日（周日）
    private void setStartOfWeek(Calendar calendar) {
        startOfWeek = (Calendar) calendar.clone();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);  // 設定該週的開始日為周日
    }

    // 日期選擇器事件，選擇的日期用來顯示表格和圓餅圖，而長條圖仍顯示該周數據
    private void onSelectDateClicked() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate = String.format("%04d/%02d/%02d", year, month + 1, dayOfMonth);  // 格式化選擇的日期
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    Date selectedDateObj = selectedCalendar.getTime();

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd EEEE", Locale.getDefault());
                    String formattedDate = dateFormat.format(selectedDateObj);

                    binding.date.setText(formattedDate);

                    setStartOfWeek(selectedCalendar);  // 重新設定選擇日期的一周開始日
                    fetchOrdersData(binding.tableLayout, selectedDate, binding.rsName.getText().toString());  // 表格和圓餅圖依照選擇日期
                    fetchWeeklySalesData(binding.rsName.getText().toString());  // 長條圖依然顯示一週的數據
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    // 從 Firebase 讀取選擇日期的訂單資料（表格和圓餅圖依照選擇的日期顯示）
    private void fetchOrdersData(TableLayout tableLayout, String selectedDate, String restaurantName) {
        HashMap<String, int[]> itemsMap = new HashMap<>();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ordersRef = database.getReference("Orders");

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                itemsMap.clear();
                tableLayout.removeAllViews();

                Context context = getContext();
                if (context == null) return;

                // 添加表頭
                TableRow headerRow = new TableRow(context);
                String[] headers = {"品項", "單價", "數量", "小計"};
                for (String header : headers) {
                    TextView textView = new TextView(context);
                    textView.setText(header);
                    textView.setPadding(8, 8, 8, 8);
                    headerRow.addView(textView);
                }
                tableLayout.addView(headerRow);

                int totalAmount = 0;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

                // 迭代 Firebase 中的每一筆訂單數據
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                    String status = orderSnapshot.child("接單狀況").getValue(String.class);
                    String orderRestaurantName = orderSnapshot.child("restaurantName").getValue(String.class);

                    if (timestamp != null && "完成訂單".equals(status) && restaurantName.equals(orderRestaurantName)) {
                        String orderDate = sdf.format(new Date(timestamp));  // 將訂單日期轉換為可讀格式

                        // 如果訂單日期符合選擇的日期
                        if (selectedDate.equals(orderDate)) {
                            for (DataSnapshot itemSnapshot : orderSnapshot.child("items").getChildren()) {
                                String title = itemSnapshot.child("title").getValue(String.class);
                                String price = itemSnapshot.child("description").getValue(String.class);
                                int quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                                int subtotal = calculateSubtotal(price, quantity);

                                totalAmount += subtotal;

                                // 累積同一個品項的數量與小計
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
                }

                // 在表格中顯示每個訂單的數據
                for (Map.Entry<String, int[]> entry : itemsMap.entrySet()) {
                    String item = entry.getKey();
                    String quantity = String.valueOf(entry.getValue()[0]);
                    String subtotal = String.valueOf(entry.getValue()[1]) + "$";
                    String price = String.valueOf(entry.getValue()[1] / entry.getValue()[0]) + "$";

                    addRowToTable(tableLayout, item, price, quantity, subtotal);
                }

                // 顯示當日的總金額
                binding.totalAmount.setText("當日總金額:$" + totalAmount);

                // 更新圓餅圖
                updatePieChart(itemsMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    // 從 Firebase 讀取該周的訂單資料（長條圖顯示一週的每日總營業額）
    private void fetchWeeklySalesData(String restaurantName) {
        HashMap<String, Integer> dailySalesMap = new HashMap<>();  // 儲存每天的總銷售金額
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ordersRef = database.getReference("Orders");

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                dailySalesMap.clear();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

                // 迭代 Firebase 中的每一筆訂單數據
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                    String status = orderSnapshot.child("接單狀況").getValue(String.class);
                    String orderRestaurantName = orderSnapshot.child("restaurantName").getValue(String.class);

                    if (timestamp != null && "完成訂單".equals(status) && restaurantName.equals(orderRestaurantName)) {
                        String orderDate = sdf.format(new Date(timestamp));  // 將訂單日期轉換為可讀格式

                        Calendar orderCalendar = Calendar.getInstance();
                        orderCalendar.setTimeInMillis(timestamp);

                        // 如果訂單日期屬於當前周
                        if (orderCalendar.after(startOfWeek) && orderCalendar.before(getEndOfWeek())) {
                            int totalAmount = 0;
                            for (DataSnapshot itemSnapshot : orderSnapshot.child("items").getChildren()) {
                                String price = itemSnapshot.child("description").getValue(String.class);
                                int quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                                totalAmount += calculateSubtotal(price, quantity);
                            }

                            // 更新 dailySalesMap，累積該日期的銷售額
                            dailySalesMap.put(orderDate, dailySalesMap.getOrDefault(orderDate, 0) + totalAmount);
                        }
                    }
                }

                // 更新長條圖
                updateBarChart(dailySalesMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    // 計算小計
    private int calculateSubtotal(String price, int quantity) {
        return Integer.parseInt(price.replace("$", "")) * quantity;
    }

    // 將資料添加到表格中
    private void addRowToTable(TableLayout tableLayout, String item, String price, String quantity, String subtotal) {
        TableRow tableRow = new TableRow(requireContext());

        String[] rowData = {item, price, quantity, subtotal};
        for (String data : rowData) {
            TextView textView = new TextView(requireContext());
            textView.setText(data);
            textView.setPadding(8, 8, 8, 8);
            tableRow.addView(textView);
        }

        tableLayout.addView(tableRow);
    }

    // 更新圓餅圖
    private void updatePieChart(HashMap<String, int[]> itemsMap) {
        PieChart pieChart = binding.pieChart;

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : itemsMap.entrySet()) {
            String item = entry.getKey();
            int quantity = entry.getValue()[0];
            entries.add(new PieEntry(quantity, item));
        }

        PieDataSet dataSet = new PieDataSet(entries, "銷售品項");
        dataSet.setColors(getResources().getColor(R.color.chart1), getResources().getColor(R.color.chart2),
                getResources().getColor(R.color.chart3), getResources().getColor(R.color.chart4));
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate(); // 刷新圖表
    }

    // 更新長條圖，顯示一週的每日總營業額
    private void updateBarChart(HashMap<String, Integer> dailySalesMap) {
        BarChart barChart = binding.barChart;

        List<BarEntry> entries = new ArrayList<>();
        int index = 0;

        Calendar calendar = (Calendar) startOfWeek.clone();  // 從一週的開始日（周日）開始

        // 定義一週的每一天的標籤
        final String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};

        // 迭代該周的每一天（從周日到周六）
        for (int i = 0; i < 7; i++) {
            String dateStr = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(calendar.getTime());
            int totalSales = dailySalesMap.getOrDefault(dateStr, 0);  // 獲取該日的銷售額
            entries.add(new BarEntry(index++, totalSales));  // 添加到長條圖的數據中
            calendar.add(Calendar.DAY_OF_MONTH, 1);  // 移動到下一天
        }

        BarDataSet dataSet = new BarDataSet(entries, "每日總營業額");
        dataSet.setColors(getResources().getColor(R.color.bar1), getResources().getColor(R.color.bar2),
                getResources().getColor(R.color.bar3), getResources().getColor(R.color.bar4));
        BarData data = new BarData(dataSet);

        // 設置 X 軸標籤格式
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int indexVal = (int) value; // 明確轉換 float 為 int
                if (indexVal >= 0 && indexVal < weekDays.length) {
                    return weekDays[indexVal];
                }
                return "";
            }
        });

        barChart.getXAxis().setGranularity(1f); // 確保標籤間隔為 1
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM); // 將 X 軸顯示在底部

        barChart.setData(data);
        barChart.invalidate(); // 刷新圖表
    }


    // 計算一週結束日期
    private Calendar getEndOfWeek() {
        Calendar endOfWeek = (Calendar) startOfWeek.clone();
        endOfWeek.add(Calendar.DAY_OF_WEEK, 6);  // 周日到周六
        return endOfWeek;
    }

    // 顯示表格模式
    private void showTable(ScrollView tableScrollView, PieChart pieChart, BarChart barChart) {
        tableScrollView.setVisibility(View.VISIBLE);
        pieChart.setVisibility(View.GONE);
        barChart.setVisibility(View.GONE);
    }

    // 顯示圓餅圖模式
    private void showPieChart(ScrollView tableScrollView, PieChart pieChart, BarChart barChart) {
        tableScrollView.setVisibility(View.GONE);
        pieChart.setVisibility(View.VISIBLE);
        barChart.setVisibility(View.GONE);
    }

    // 顯示長條圖模式
    private void showBarChart(ScrollView tableScrollView, PieChart pieChart, BarChart barChart) {
        tableScrollView.setVisibility(View.GONE);
        pieChart.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
