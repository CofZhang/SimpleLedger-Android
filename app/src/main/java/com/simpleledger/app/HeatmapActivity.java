package com.simpleledger.app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 4.5 年度账单热力图 Activity
 * - 顶部 56dp 玻璃条导航栏（沉浸式状态栏）
 * - HeatmapView 显示一整年的消费热力图
 * - 年份按钮弹出 NumberPicker（2000-2100）
 * - 点击某天的方块加载当日明细到 RecyclerView
 */
public class HeatmapActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private HeatmapView heatmapView;
    private TextView btnYear;
    private TextView tvSelectedDate;
    private TextView tvDayTotal;
    private TextView tvEmpty;
    private RecyclerView rvDayDetail;
    private RecordAdapter recordAdapter;
    private final List<Record> dayRecords = new ArrayList<>();

    private int currentYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heatmap);

        // 沉浸式状态栏
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, statusBarHeight, 0, 0);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        currentYear = Calendar.getInstance().get(Calendar.YEAR);

        btnYear = findViewById(R.id.btnYear);
        heatmapView = findViewById(R.id.heatmapView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvDayTotal = findViewById(R.id.tvDayTotal);
        tvEmpty = findViewById(R.id.tvEmpty);
        rvDayDetail = findViewById(R.id.rvDayDetail);

        rvDayDetail.setLayoutManager(new LinearLayoutManager(this));
        recordAdapter = new RecordAdapter(dayRecords);
        rvDayDetail.setAdapter(recordAdapter);

        // 返回按钮
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 年份按钮
        btnYear.setText(currentYear + "年");
        btnYear.setOnClickListener(v -> showYearPicker());

        // HeatmapView 点击事件
        heatmapView.setOnDayClickListener((date, amount) -> loadDayRecords(date, amount));

        // 初始化数据
        loadHeatmapData();
    }

    private void showYearPicker() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_year_picker, null);
        NumberPicker npYear = dialogView.findViewById(R.id.npYear);
        npYear.setMinValue(2000);
        npYear.setMaxValue(2100);
        npYear.setValue(currentYear);
        new AlertDialog.Builder(this)
                .setTitle("选择年份")
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    currentYear = npYear.getValue();
                    btnYear.setText(currentYear + "年");
                    heatmapView.setYear(currentYear);
                    loadHeatmapData();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadHeatmapData() {
        Map<String, Double> data = dbHelper.getDailyExpenseForYear(currentYear);
        heatmapView.setDailyExpense(data);
    }

    private void loadDayRecords(String date, double amount) {
        // 显示日期与金额
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(date));
            SimpleDateFormat displayFmt = new SimpleDateFormat("M月d日 EEEE", Locale.getDefault());
            tvSelectedDate.setText(displayFmt.format(cal.getTime()));
        } catch (Exception e) {
            tvSelectedDate.setText(date);
        }
        tvDayTotal.setText(String.format("支出 ¥%.2f", amount));

        // 加载当日记录
        List<Record> records = dbHelper.getRecordsByDate(date);
        recordAdapter.updateData(records);

        if (records.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("当日无记录");
            rvDayDetail.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvDayDetail.setVisibility(View.VISIBLE);
        }
    }
}
