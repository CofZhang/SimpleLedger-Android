package com.simpleledger.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private int currentPeriod = 1;
    private int currentChartType = 0;
    
    private TabLayout tabPeriod, tabChartType;
    private TextView tvStatsTitle, tvStatsExpense, tvStatsIncome;
    private BarChart barChart;
    private PieChart pieChart;
    private LineChart lineChart;
    private RecyclerView rvCategoryStats;
    private CategoryStatAdapter categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        dbHelper = new DatabaseHelper(this);
        initViews();
        loadStats();
    }

    private void initViews() {
        tabPeriod = findViewById(R.id.tabPeriod);
        tabChartType = findViewById(R.id.tabChartType);
        tvStatsTitle = findViewById(R.id.tvStatsTitle);
        tvStatsExpense = findViewById(R.id.tvStatsExpense);
        tvStatsIncome = findViewById(R.id.tvStatsIncome);
        barChart = findViewById(R.id.barChart);
        pieChart = findViewById(R.id.pieChart);
        lineChart = findViewById(R.id.lineChart);
        rvCategoryStats = findViewById(R.id.rvCategoryStats);

        rvCategoryStats.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryStatAdapter(new ArrayList<>());
        rvCategoryStats.setAdapter(categoryAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tabPeriod.addTab(tabPeriod.newTab().setText(R.string.week));
        tabPeriod.addTab(tabPeriod.newTab().setText(R.string.month));
        tabPeriod.addTab(tabPeriod.newTab().setText(R.string.year));
        tabPeriod.getTabAt(1).select();

        tabChartType.addTab(tabChartType.newTab().setText(R.string.bar_chart));
        tabChartType.addTab(tabChartType.newTab().setText(R.string.pie_chart));
        tabChartType.addTab(tabChartType.newTab().setText(R.string.line_chart));

        tabPeriod.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentPeriod = tab.getPosition();
                loadStats();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        tabChartType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentChartType = tab.getPosition();
                switch (currentChartType) {
                    case 0:
                        barChart.setVisibility(View.VISIBLE);
                        pieChart.setVisibility(View.GONE);
                        lineChart.setVisibility(View.GONE);
                        break;
                    case 1:
                        barChart.setVisibility(View.GONE);
                        pieChart.setVisibility(View.VISIBLE);
                        lineChart.setVisibility(View.GONE);
                        break;
                    case 2:
                        barChart.setVisibility(View.GONE);
                        pieChart.setVisibility(View.GONE);
                        lineChart.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        setupCharts();
    }

    private void setupCharts() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setFitBars(true);
        barChart.animateY(500);

        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setHoleRadius(30f);
        pieChart.setTransparentCircleRadius(40f);
        pieChart.animateY(500);

        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.animateX(500);
    }

    private void loadStats() {
        Calendar cal = Calendar.getInstance();
        double totalExpense = 0;
        double totalIncome = 0;
        String datePattern;
        String title;

        if (currentPeriod == 0) {
            Calendar weekStartCal = Calendar.getInstance();
            weekStartCal.set(Calendar.DAY_OF_WEEK, weekStartCal.getFirstDayOfWeek());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date weekStart = weekStartCal.getTime();
            Calendar weekEndCal = (Calendar) weekStartCal.clone();
            weekEndCal.add(Calendar.DAY_OF_MONTH, 6);
            Date weekEnd = weekEndCal.getTime();
            title = String.format("%s - %s", sdf.format(weekStart), sdf.format(weekEnd));
            datePattern = sdf.format(weekStart).substring(0, 7) + "%";
            
            List<DatabaseHelper.DailySum> dailySums = dbHelper.getDailySumForWeek((Calendar) weekStartCal.clone());
            totalExpense = 0;
            totalIncome = 0;
            for (DatabaseHelper.DailySum ds : dailySums) {
                totalExpense += ds.expense;
                totalIncome += ds.income;
            }
            
            showWeekData(dailySums);
        } else if (currentPeriod == 1) {
            String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
            title = yearMonth + " 月";
            datePattern = yearMonth + "%";
            
            Calendar monthStart = Calendar.getInstance();
            monthStart.set(Calendar.DAY_OF_MONTH, 1);
            List<DatabaseHelper.DailySum> dailySums = getDailySumForMonth(monthStart);
            
            totalExpense = dbHelper.getMonthlyExpense(yearMonth);
            totalIncome = dbHelper.getMonthlyIncome(yearMonth);
            
            showMonthData(dailySums);
        } else {
            int year = cal.get(Calendar.YEAR);
            title = year + " 年";
            datePattern = year + "%";
            
            List<DatabaseHelper.MonthlySum> monthlySums = dbHelper.getMonthlySumForYear(year);
            
            totalExpense = 0;
            totalIncome = 0;
            for (DatabaseHelper.MonthlySum ms : monthlySums) {
                totalExpense += ms.expense;
                totalIncome += ms.income;
            }
            
            showYearData(monthlySums);
        }

        tvStatsTitle.setText(title);
        tvStatsExpense.setText(String.format("总支出: ¥%.2f", totalExpense));
        tvStatsIncome.setText(String.format("总收入: ¥%.2f", totalIncome));

        List<DatabaseHelper.CategoryStat> categoryStats = dbHelper.getCategoryStats(datePattern, Record.TYPE_EXPENSE);
        categoryAdapter.updateData(categoryStats);
        showPieChart(categoryStats);
    }

    private List<DatabaseHelper.DailySum> getDailySumForMonth(Calendar monthStart) {
        List<DatabaseHelper.DailySum> result = new ArrayList<>();
        Calendar cal = (Calendar) monthStart.clone();
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 0; i < maxDay; i++) {
            String date = String.format("%04d-%02d-%02d",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            DatabaseHelper.DailySum ds = new DatabaseHelper.DailySum();
            ds.date = date;
            ds.expense = getDaySum(date, Record.TYPE_EXPENSE);
            ds.income = getDaySum(date, Record.TYPE_INCOME);
            result.add(ds);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return result;
    }

    private double getDaySum(String date, int type) {
        String query = "SELECT SUM(amount) FROM records WHERE date = ? AND type = ?";
        android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery(query, new String[]{date, String.valueOf(type)});
        double sum = 0;
        if (cursor.moveToFirst()) {
            sum = cursor.getDouble(0);
        }
        cursor.close();
        return sum;
    }

    private void showWeekData(List<DatabaseHelper.DailySum> dailySums) {
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<Entry> lineEntries = new ArrayList<>();
        String[] labels = new String[7];
        String[] weekDays = {"日", "一", "二", "三", "四", "五", "六"};
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        
        for (int i = 0; i < dailySums.size() && i < 7; i++) {
            DatabaseHelper.DailySum ds = dailySums.get(i);
            barEntries.add(new BarEntry(i, (float) ds.expense));
            lineEntries.add(new Entry(i, (float) ds.expense));
            labels[i] = weekDays[(cal.get(Calendar.DAY_OF_WEEK) - 1) % 7];
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        showBarChart(barEntries, labels);
        showLineChart(lineEntries, labels);
    }

    private void showMonthData(List<DatabaseHelper.DailySum> dailySums) {
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<Entry> lineEntries = new ArrayList<>();
        List<String> labelList = new ArrayList<>();
        
        for (int i = 0; i < dailySums.size(); i++) {
            DatabaseHelper.DailySum ds = dailySums.get(i);
            if (i % 3 == 0 || i == dailySums.size() - 1) {
                barEntries.add(new BarEntry(labelList.size(), (float) ds.expense));
                lineEntries.add(new Entry(labelList.size(), (float) ds.expense));
                labelList.add(String.valueOf(i + 1));
            }
        }

        String[] labels = labelList.toArray(new String[0]);
        showBarChart(barEntries, labels);
        showLineChart(lineEntries, labels);
    }

    private void showYearData(List<DatabaseHelper.MonthlySum> monthlySums) {
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<Entry> lineEntries = new ArrayList<>();
        String[] labels = new String[12];
        
        for (int i = 0; i < 12; i++) {
            double expense = 0;
            if (i < monthlySums.size()) {
                expense = monthlySums.get(i).expense;
            }
            barEntries.add(new BarEntry(i, (float) expense));
            lineEntries.add(new Entry(i, (float) expense));
            labels[i] = (i + 1) + "月";
        }

        showBarChart(barEntries, labels);
        showLineChart(lineEntries, labels);
    }

    private void showBarChart(ArrayList<BarEntry> entries, String[] labels) {
        BarDataSet dataSet = new BarDataSet(entries, "支出");
        dataSet.setColor(getColor(R.color.colorPrimary));
        dataSet.setValueTextSize(10f);
        
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        
        barChart.invalidate();
    }

    private void showLineChart(ArrayList<Entry> entries, String[] labels) {
        LineDataSet dataSet = new LineDataSet(entries, "支出");
        dataSet.setColor(getColor(R.color.colorPrimary));
        dataSet.setCircleColor(getColor(R.color.colorPrimary));
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(10f);
        
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        
        lineChart.invalidate();
    }

    private void showPieChart(List<DatabaseHelper.CategoryStat> categoryStats) {
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        
        for (int i = 0; i < Math.min(categoryStats.size(), 8); i++) {
            DatabaseHelper.CategoryStat stat = categoryStats.get(i);
            pieEntries.add(new PieEntry((float) stat.getTotal(), stat.getCategoryName()));
            colors.add(stat.getCategoryColor());
        }
        
        if (categoryStats.size() > 8) {
            float otherSum = 0;
            for (int i = 8; i < categoryStats.size(); i++) {
                otherSum += categoryStats.get(i).getTotal();
            }
            if (otherSum > 0) {
                pieEntries.add(new PieEntry(otherSum, "其他"));
                colors.add(Color.GRAY);
            }
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);
        
        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter());
        pieChart.setData(pieData);
        pieChart.invalidate();
    }
}
