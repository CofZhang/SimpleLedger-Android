package com.simpleledger.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
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
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {
    private DatabaseHelper dbHelper;
    private TabLayout tabPeriod, tabChartType;
    private FrameLayout chartContainer;
    private TextView tvPeriodLabel;
    private RecyclerView rvCategoryStats;
    private CategoryStatAdapter categoryStatAdapter;
    private List<DatabaseHelper.CategoryStat> categoryStats;

    private int currentPeriod = 1;
    private int currentChartType = 0;
    private Calendar currentDate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        dbHelper = new DatabaseHelper(getContext());
        currentDate = Calendar.getInstance();
        categoryStats = new ArrayList<>();

        tabPeriod = view.findViewById(R.id.tabPeriod);
        tabChartType = view.findViewById(R.id.tabChartType);
        chartContainer = view.findViewById(R.id.chartContainer);
        tvPeriodLabel = view.findViewById(R.id.tvPeriodLabel);
        rvCategoryStats = view.findViewById(R.id.rvCategoryStats);

        rvCategoryStats.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryStatAdapter = new CategoryStatAdapter(categoryStats);
        rvCategoryStats.setAdapter(categoryStatAdapter);

        tabPeriod.addTab(tabPeriod.newTab().setText(R.string.week));
        tabPeriod.addTab(tabPeriod.newTab().setText(R.string.month));
        tabPeriod.addTab(tabPeriod.newTab().setText(R.string.year));
        tabPeriod.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentPeriod = tab.getPosition();
                updateData();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        tabChartType.addTab(tabChartType.newTab().setText(R.string.bar_chart));
        tabChartType.addTab(tabChartType.newTab().setText(R.string.pie_chart));
        tabChartType.addTab(tabChartType.newTab().setText(R.string.line_chart));
        tabChartType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentChartType = tab.getPosition();
                updateChart();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        view.findViewById(R.id.btnPrevPeriod).setOnClickListener(v -> {
            if (currentPeriod == 0) currentDate.add(Calendar.WEEK_OF_YEAR, -1);
            else if (currentPeriod == 1) currentDate.add(Calendar.MONTH, -1);
            else currentDate.add(Calendar.YEAR, -1);
            updateData();
        });
        view.findViewById(R.id.btnNextPeriod).setOnClickListener(v -> {
            if (currentPeriod == 0) currentDate.add(Calendar.WEEK_OF_YEAR, 1);
            else if (currentPeriod == 1) currentDate.add(Calendar.MONTH, 1);
            else currentDate.add(Calendar.YEAR, 1);
            updateData();
        });

        updateData();
        return view;
    }

    private void updateData() {
        updatePeriodLabel();
        updateChart();
        updateCategoryStats();
    }

    private void updatePeriodLabel() {
        SimpleDateFormat sdf;
        if (currentPeriod == 0) {
            sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar start = (Calendar) currentDate.clone();
            start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            Calendar end = (Calendar) start.clone();
            end.add(Calendar.DAY_OF_MONTH, 6);
            tvPeriodLabel.setText(sdf.format(start.getTime()) + " ~ " + sdf.format(end.getTime()));
        } else if (currentPeriod == 1) {
            sdf = new SimpleDateFormat("yyyy年M月", Locale.getDefault());
            tvPeriodLabel.setText(sdf.format(currentDate.getTime()));
        } else {
            sdf = new SimpleDateFormat("yyyy年", Locale.getDefault());
            tvPeriodLabel.setText(sdf.format(currentDate.getTime()));
        }
    }

    private String getDatePattern() {
        if (currentPeriod == 1) {
            return new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentDate.getTime()) + "%";
        } else if (currentPeriod == 2) {
            return new SimpleDateFormat("yyyy", Locale.getDefault()).format(currentDate.getTime()) + "%";
        } else {
            Calendar start = (Calendar) currentDate.clone();
            start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.format(start.getTime()) + "%";
        }
    }

    private void updateChart() {
        chartContainer.removeAllViews();

        if (currentChartType == 0) {
            BarChart barChart = new BarChart(getContext());
            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            if (currentPeriod == 1) {
                List<DatabaseHelper.MonthlySum> data = dbHelper.getMonthlySumForYear(currentDate.get(Calendar.YEAR));
                for (DatabaseHelper.MonthlySum ms : data) {
                    entries.add(new BarEntry(ms.month - 1, (float) ms.expense));
                    labels.add(ms.month + "月");
                }
            } else if (currentPeriod == 2) {
                int curYear = currentDate.get(Calendar.YEAR);
                List<DatabaseHelper.YearlySum> data = dbHelper.getYearlySum(curYear - 2, curYear);
                for (int i = 0; i < data.size(); i++) {
                    entries.add(new BarEntry(i, (float) data.get(i).expense));
                    labels.add(String.valueOf(data.get(i).year));
                }
            } else {
                Calendar start = (Calendar) currentDate.clone();
                start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                List<DatabaseHelper.DailySum> data = dbHelper.getDailySumForWeek(start);
                SimpleDateFormat sdf = new SimpleDateFormat("EE", Locale.getDefault());
                for (int i = 0; i < data.size(); i++) {
                    entries.add(new BarEntry(i, (float) data.get(i).expense));
                    labels.add(sdf.format(start.getTime()));
                    start.add(Calendar.DAY_OF_MONTH, 1);
                }
            }

            BarDataSet dataSet = new BarDataSet(entries, "支出");
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            BarData barData = new BarData(dataSet);
            barChart.setData(barData);
            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            barChart.getDescription().setEnabled(false);
            barChart.animateY(1000);
            barChart.invalidate();
            chartContainer.addView(barChart);

        } else if (currentChartType == 1) {
            PieChart pieChart = new PieChart(getContext());
            List<PieEntry> entries = new ArrayList<>();
            List<DatabaseHelper.CategoryStat> stats = dbHelper.getCategoryStats(getDatePattern(), Record.TYPE_EXPENSE);
            for (DatabaseHelper.CategoryStat stat : stats) {
                if (stat.getTotal() > 0) {
                    entries.add(new PieEntry((float) stat.getTotal(), stat.getCategoryName()));
                }
            }
            PieDataSet dataSet = new PieDataSet(entries, "分类");
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            PieData pieData = new PieData(dataSet);
            pieChart.setData(pieData);
            pieChart.getDescription().setEnabled(false);
            pieChart.animateXY(1000, 1000);
            pieChart.invalidate();
            chartContainer.addView(pieChart);

        } else {
            LineChart lineChart = new LineChart(getContext());
            List<Entry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            if (currentPeriod == 1) {
                List<DatabaseHelper.MonthlySum> data = dbHelper.getMonthlySumForYear(currentDate.get(Calendar.YEAR));
                for (DatabaseHelper.MonthlySum ms : data) {
                    entries.add(new Entry(ms.month - 1, (float) ms.expense));
                    labels.add(ms.month + "月");
                }
            } else if (currentPeriod == 2) {
                int curYear = currentDate.get(Calendar.YEAR);
                List<DatabaseHelper.YearlySum> data = dbHelper.getYearlySum(curYear - 2, curYear);
                for (int i = 0; i < data.size(); i++) {
                    entries.add(new Entry(i, (float) data.get(i).expense));
                    labels.add(String.valueOf(data.get(i).year));
                }
            } else {
                Calendar start = (Calendar) currentDate.clone();
                start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                List<DatabaseHelper.DailySum> data = dbHelper.getDailySumForWeek(start);
                SimpleDateFormat sdf = new SimpleDateFormat("EE", Locale.getDefault());
                for (int i = 0; i < data.size(); i++) {
                    entries.add(new Entry(i, (float) data.get(i).expense));
                    labels.add(sdf.format(start.getTime()));
                    start.add(Calendar.DAY_OF_MONTH, 1);
                }
            }

            LineDataSet dataSet = new LineDataSet(entries, "支出");
            dataSet.setColor(Color.parseColor("#C4756B"));
            dataSet.setCircleColor(Color.parseColor("#C4756B"));
            dataSet.setLineWidth(2f);
            LineData lineData = new LineData(dataSet);
            lineChart.setData(lineData);
            lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            lineChart.getDescription().setEnabled(false);
            lineChart.animateY(1000);
            lineChart.invalidate();
            chartContainer.addView(lineChart);
        }
    }

    private void updateCategoryStats() {
        categoryStats.clear();
        categoryStats.addAll(dbHelper.getCategoryStats(getDatePattern(), Record.TYPE_EXPENSE));
        categoryStatAdapter.updateData(categoryStats);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dbHelper != null) updateData();
    }
}
