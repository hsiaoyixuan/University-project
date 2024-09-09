package com.example.restaurantlogging.ui.excel;

import android.app.DatePickerDialog;
import android.graphics.Color;
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

import com.example.restaurantlogging.R;
import com.example.restaurantlogging.databinding.FragmentExcelBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
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

public class ExcelFragment extends Fragment {

    private FragmentExcelBinding binding;
    private String selectedDate;  // 保存選定日期的變量
    private HashMap<String, Integer> dailySalesMap = new HashMap<>(); // 保存每日銷售額的變量

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        ExcelViewModel excelViewModel =
                new ViewModelProvider(this).get(ExcelViewModel.class);

        binding = FragmentExcelBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.rsName;
        excelViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        setDateToToday();  // 初始化當天日期

        fetchOrdersData(binding.tableLayout, selectedDate);  // 加載當天的訂單數據

        // 設置選擇日期按鈕的點擊事件
        binding.btnSelectDate.setOnClickListener(v -> onSelectDateClicked());

        // 設置顯示模式切換按鈕的點擊事件
        binding.btnShowTable.setOnClickListener(v -> showTable());
        binding.btnShowPieChart.setOnClickListener(v -> showPieChart());
        binding.btnShowBarChart.setOnClickListener(v -> showBarChart());

        return root;
    }

    private void setDateToToday() {
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd EEEE", Locale.getDefault());
        String formattedDate = dateFormat.format(today);
        binding.date.setText(formattedDate);

        selectedDate = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(today);  // 格式更改為 "yyyy/MM/dd"
    }

    private void onSelectDateClicked() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate = String.format("%04d/%02d/%02d", year, month + 1, dayOfMonth);  // 格式為 "yyyy/MM/dd"

                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    Date selectedDateObj = selectedCalendar.getTime();

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd EEEE", Locale.getDefault());
                    String formattedDate = dateFormat.format(selectedDateObj);

                    binding.date.setText(formattedDate);

                    fetchOrdersData(binding.tableLayout, selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void fetchOrdersData(TableLayout tableLayout, String date) {
        HashMap<String, int[]> itemsMap = new HashMap<>();
        int totalAmount = 0;

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ordersRef = database.getReference("Orders");

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                itemsMap.clear();
                tableLayout.removeAllViews();
                dailySalesMap.clear(); // 清除之前的每日銷售數據

                TableRow headerRow = new TableRow(getContext());
                String[] headers = {"品項", "單價", "數量", "小計"};
                for (String header : headers) {
                    TextView textView = new TextView(getContext());
                    textView.setText(header);
                    textView.setPadding(8, 8, 8, 8);
                    headerRow.addView(textView);
                }
                tableLayout.addView(headerRow);

                int totalAmount = 0;

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                    String status = orderSnapshot.child("接單狀況").getValue(String.class);

                    if (timestamp != null && "完成訂單".equals(status)) {
                        String orderDate = sdf.format(new Date(timestamp));  // 轉換時間戳為日期

                        // 累計每天的總銷售額
                        int dailyTotal = dailySalesMap.getOrDefault(orderDate, 0);

                        if (date.equals(orderDate)) {  // 與選擇的日期比較
                            for (DataSnapshot itemSnapshot : orderSnapshot.child("items").getChildren()) {
                                String title = itemSnapshot.child("title").getValue(String.class);
                                String price = itemSnapshot.child("description").getValue(String.class);
                                int quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                                int subtotal = calculateSubtotal(price, quantity);

                                totalAmount += subtotal;
                                dailyTotal += subtotal; // 累加當日的總銷售額

                                if (itemsMap.containsKey(title)) {
                                    int[] currentData = itemsMap.get(title);
                                    currentData[0] += quantity;
                                    currentData[1] += subtotal;
                                } else {
                                    itemsMap.put(title, new int[]{quantity, subtotal});
                                }
                            }
                        }

                        dailySalesMap.put(orderDate, dailyTotal); // 更新每天的總銷售額
                    }
                }

                for (Map.Entry<String, int[]> entry : itemsMap.entrySet()) {
                    String item = entry.getKey();
                    String quantity = String.valueOf(entry.getValue()[0]);
                    String subtotal = String.valueOf(entry.getValue()[1]) + "$";
                    String price = String.valueOf(entry.getValue()[1] / entry.getValue()[0]) + "$";

                    addRowToTable(tableLayout, item, price, quantity, subtotal);
                }

                binding.totalAmount.setText("當日總金額:$" + totalAmount);

                // 更新圓餅圖數據
                updatePieChart(itemsMap);
                // 更新長條圖數據
                updateBarChart();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // 處理 Firebase 查詢取消的情況
            }
        });
    }

    private int calculateSubtotal(String price, int quantity) {
        return Integer.parseInt(price.replace("$", "")) * quantity;
    }

    private void addRowToTable(TableLayout tableLayout, String item, String price, String quantity, String subtotal) {
        TableRow tableRow = new TableRow(getContext());

        String[] rowData = {item, price, quantity, subtotal};
        for (String data : rowData) {
            TextView textView = new TextView(getContext());
            textView.setText(data);
            textView.setPadding(8, 8, 8, 8);
            tableRow.addView(textView);
        }

        tableLayout.addView(tableRow);
    }

    // 顯示表格
    private void showTable() {
        binding.tableScrollView.setVisibility(View.VISIBLE);
        binding.pieChart.setVisibility(View.GONE);
        binding.barChart.setVisibility(View.GONE);
    }

    // 顯示圓餅圖
    private void showPieChart() {
        binding.tableScrollView.setVisibility(View.GONE);
        binding.pieChart.setVisibility(View.VISIBLE);
        binding.barChart.setVisibility(View.GONE);
    }

    // 顯示長條圖
    private void showBarChart() {
        binding.tableScrollView.setVisibility(View.GONE);
        binding.pieChart.setVisibility(View.GONE);
        binding.barChart.setVisibility(View.VISIBLE);
    }

    // 更新圓餅圖數據
    private void updatePieChart(HashMap<String, int[]> itemsMap) {
        PieChart pieChart = binding.pieChart;

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : itemsMap.entrySet()) {
            String item = entry.getKey();
            int quantity = entry.getValue()[0];
            entries.add(new PieEntry(quantity, item));
        }

        PieDataSet dataSet = new PieDataSet(entries, "銷售品項");

        // 使用定義的顏色資源
        int[] chartColors = new int[] {
                getResources().getColor(R.color.chart1),
                getResources().getColor(R.color.chart2),
                getResources().getColor(R.color.chart3),
                getResources().getColor(R.color.chart4)
        };
        dataSet.setColors(chartColors);  // 設置圓餅圖顏色
        dataSet.setValueTextColor(Color.BLACK);  // 設置數據文本顏色

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();  // 刷新圖表

        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);  // 顯示圖例
    }

    // 更新長條圖數據
    private void updateBarChart() {
        BarChart barChart = binding.barChart;

        List<BarEntry> entries = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Integer> entry : dailySalesMap.entrySet()) {
            String date = entry.getKey();
            int totalSales = entry.getValue();
            entries.add(new BarEntry(index++, totalSales)); // 使用索引添加條目
        }

        BarDataSet dataSet = new BarDataSet(entries, "每日總營業額");

        // 使用定義的顏色資源
        int[] barColors = new int[] {
                getResources().getColor(R.color.bar1),
                getResources().getColor(R.color.bar2),
                getResources().getColor(R.color.bar3),
                getResources().getColor(R.color.bar4)
        };
        dataSet.setColors(barColors);  // 設置顏色集合
        dataSet.setValueTextColor(Color.BLACK);  // 設置數據文本顏色

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.invalidate();  // 刷新圖表

        Legend legend = barChart.getLegend();
        legend.setEnabled(true);  // 顯示圖例
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
