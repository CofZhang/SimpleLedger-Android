package com.simpleledger.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 6.0 智能分析页：
 * 1. 近 6 个月支出趋势折线图
 * 2. 本月支出预测（基于近 6 个月线性回归）
 * 3. 同比（去年同期对比）/ 环比（上月对比）
 * 4. 异常消费提醒（远超均值时高亮显示）
 * 5. Top 分类涨幅榜
 */
public class AnalysisActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private LineChart chart;
    private TextView tvPrediction, tvMom, tvYoy, tvAnomaly, tvMomLabel, tvYoyLabel;
    private LinearLayout layoutAnomaly, layoutTopCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_analysis);

        dbHelper = new DatabaseHelper(this);

        LinearLayout topNav = findViewById(R.id.topNav);
        ViewCompat.setOnApplyWindowInsetsListener(topNav, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            HapticHelper.light(this);
            finish();
        });

        chart = findViewById(R.id.chart);
        tvPrediction = findViewById(R.id.tvPrediction);
        tvMom = findViewById(R.id.tvMom);
        tvMomLabel = findViewById(R.id.tvMomLabel);
        tvYoy = findViewById(R.id.tvYoy);
        tvYoyLabel = findViewById(R.id.tvYoyLabel);
        tvAnomaly = findViewById(R.id.tvAnomaly);
        layoutAnomaly = findViewById(R.id.layoutAnomaly);
        layoutTopCategories = findViewById(R.id.layoutTopCategories);

        loadAnalysis();
    }

    private void loadAnalysis() {
        // 1. 近 6 个月支出数据
        List<DatabaseHelper.MonthlySum> recent = dbHelper.getRecentMonthlyExpenses(6);
        List<String> labels = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < recent.size(); i++) {
            DatabaseHelper.MonthlySum ms = recent.get(i);
            labels.add(ms.month + "月");
            entries.add(new Entry(i, (float) ms.expense));
        }
        renderChart(entries, labels);

        // 2. 本月预测（简单线性回归）
        double prediction = predictNext(recent);
        String thisMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        double actualThisMonth = dbHelper.getMonthlyExpense(thisMonth);
        if (prediction > 0) {
            tvPrediction.setText(String.format(Locale.getDefault(),
                    "预测本月总支出：¥%.2f\n（当前已支出 ¥%.2f）", prediction, actualThisMonth));
        } else {
            tvPrediction.setText("数据不足，至少需要 2 个月数据才能预测");
        }

        // 3. 环比（本月 vs 上月）
        Calendar cal = Calendar.getInstance();
        String ymThis = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());
        cal.add(Calendar.MONTH, -1);
        String ymLast = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.getTime());
        double expenseThis = dbHelper.getMonthlyExpense(ymThis);
        double expenseLast = dbHelper.getMonthlyExpense(ymLast);
        renderComparison(tvMom, tvMomLabel, expenseThis, expenseLast, "上月");

        // 4. 同比（本月 vs 去年同月）
        Calendar cal2 = Calendar.getInstance();
        cal2.add(Calendar.YEAR, -1);
        String ymLastYear = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal2.getTime());
        double expenseLastYear = dbHelper.getMonthlyExpense(ymLastYear);
        renderComparison(tvYoy, tvYoyLabel, expenseThis, expenseLastYear, "去年同期");

        // 5. 异常消费检测
        renderAnomaly(recent, expenseThis);

        // 6. Top 分类涨幅榜
        renderTopCategories(ymThis, ymLast);
    }

    /** 简单线性回归预测下个月支出 */
    private double predictNext(List<DatabaseHelper.MonthlySum> recent) {
        if (recent.size() < 2) return 0;
        int n = recent.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = recent.get(i).expense;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) return recent.get(n - 1).expense;
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        double next = slope * n + intercept;
        return Math.max(0, next);
    }

    private void renderChart(List<Entry> entries, List<String> labels) {
        LineDataSet dataSet = new LineDataSet(entries, "月度支出");
        dataSet.setColor(Color.parseColor("#D4756A"));
        dataSet.setCircleColor(Color.parseColor("#D4756A"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#3D352C"));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#33D4756A"));

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                return (idx >= 0 && idx < labels.size()) ? labels.get(idx) : "";
            }
        });
        chart.animateX(600);
        chart.invalidate();
    }

    private void renderComparison(TextView tvValue, TextView tvLabel,
                                  double current, double compare, String compareLabel) {
        if (compare <= 0) {
            tvValue.setText("¥" + String.format("%.2f", current));
            tvValue.setTextColor(Color.parseColor("#8B7D6B"));
            tvLabel.setText("暂无" + compareLabel + "数据可对比");
            return;
        }
        double diff = current - compare;
        double percent = diff / compare * 100;
        String arrow = diff > 0 ? "↑" : (diff < 0 ? "↓" : "→");
        tvValue.setText(String.format(Locale.getDefault(),
                "%s ¥%.2f（%.1f%%）", arrow, Math.abs(diff), Math.abs(percent)));
        if (diff > 0) {
            tvValue.setTextColor(Color.parseColor("#D4756A"));
        } else if (diff < 0) {
            tvValue.setTextColor(Color.parseColor("#7BA678"));
        } else {
            tvValue.setTextColor(Color.parseColor("#8B7D6B"));
        }
        tvLabel.setText(String.format("较%s ¥%.2f %s", compareLabel, compare,
                diff > 0 ? "增加" : (diff < 0 ? "减少" : "持平")));
    }

    /** 异常消费检测：当本月支出超过近 6 个月均值 1.5 倍时预警 */
    private void renderAnomaly(List<DatabaseHelper.MonthlySum> recent, double thisMonth) {
        if (recent.isEmpty()) {
            layoutAnomaly.setVisibility(View.GONE);
            return;
        }
        double avg = 0;
        for (DatabaseHelper.MonthlySum ms : recent) avg += ms.expense;
        avg /= recent.size();
        if (avg <= 0) {
            layoutAnomaly.setVisibility(View.GONE);
            return;
        }
        double ratio = thisMonth / avg;
        if (ratio > 1.5) {
            layoutAnomaly.setVisibility(View.VISIBLE);
            tvAnomaly.setText(String.format(Locale.getDefault(),
                    "⚠️ 本月支出 ¥%.2f 已达月均 ¥%.2f 的 %.0f%%，请留意控制消费",
                    thisMonth, avg, ratio * 100));
            tvAnomaly.setTextColor(Color.parseColor("#D4756A"));
        } else if (ratio > 1.2) {
            layoutAnomaly.setVisibility(View.VISIBLE);
            tvAnomaly.setText(String.format(Locale.getDefault(),
                    "💡 本月支出 ¥%.2f 略高于月均 ¥%.2f（%.0f%%）",
                    thisMonth, avg, ratio * 100));
            tvAnomaly.setTextColor(Color.parseColor("#D9A85E"));
        } else {
            layoutAnomaly.setVisibility(View.VISIBLE);
            tvAnomaly.setText(String.format(Locale.getDefault(),
                    "✓ 本月支出 ¥%.2f 在月均 ¥%.2f 正常范围内",
                    thisMonth, avg, ratio * 100));
            tvAnomaly.setTextColor(Color.parseColor("#7BA678"));
        }
    }

    /** Top 分类涨幅榜：对比本月与上月各分类支出 */
    private void renderTopCategories(String ymThis, String ymLast) {
        layoutTopCategories.removeAllViews();
        List<DatabaseHelper.CategoryStat> thisStats = dbHelper.getCategoryStats(ymThis + "%", Record.TYPE_EXPENSE);
        List<DatabaseHelper.CategoryStat> lastStats = dbHelper.getCategoryStats(ymLast + "%", Record.TYPE_EXPENSE);

        // 计算每个分类的涨幅
        List<double[]> growthList = new ArrayList<>(); // [catId, growthAbs, growthPercent]
        List<String> names = new ArrayList<>();
        List<String> icons = new ArrayList<>();
        for (DatabaseHelper.CategoryStat ts : thisStats) {
            double lastAmount = 0;
            for (DatabaseHelper.CategoryStat ls : lastStats) {
                if (ls.getCategoryId() == ts.getCategoryId()) {
                    lastAmount = ls.getTotal();
                    break;
                }
            }
            // 只显示涨幅为正且金额 > 0 的分类
            if (ts.getTotal() > 0 && ts.getTotal() > lastAmount) {
                growthList.add(new double[]{ts.getCategoryId(), ts.getTotal() - lastAmount,
                        lastAmount > 0 ? (ts.getTotal() - lastAmount) / lastAmount * 100 : 100});
                names.add(ts.getCategoryName());
                icons.add(ts.getCategoryIcon());
            }
        }

        if (growthList.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("暂无涨幅明显的分类");
            tv.setTextColor(Color.parseColor("#B8AC9A"));
            tv.setTextSize(13);
            tv.setPadding(0, 16, 0, 16);
            layoutTopCategories.addView(tv);
            return;
        }

        // 按涨幅金额降序，取 Top 5
        for (int i = 0; i < growthList.size() - 1; i++) {
            for (int j = i + 1; j < growthList.size(); j++) {
                if (growthList.get(j)[1] > growthList.get(i)[1]) {
                    double[] tmp = growthList.get(i);
                    growthList.set(i, growthList.get(j));
                    growthList.set(j, tmp);
                    String tmpName = names.get(i);
                    names.set(i, names.get(j));
                    names.set(j, tmpName);
                    String tmpIcon = icons.get(i);
                    icons.set(i, icons.get(j));
                    icons.set(j, tmpIcon);
                }
            }
        }

        int count = Math.min(5, growthList.size());
        for (int i = 0; i < count; i++) {
            double[] g = growthList.get(i);
            View item = getLayoutInflater().inflate(R.layout.item_analysis_category, layoutTopCategories, false);
            TextView tvRank = item.findViewById(R.id.tvRank);
            TextView tvName = item.findViewById(R.id.tvName);
            TextView tvGrowth = item.findViewById(R.id.tvGrowth);
            tvRank.setText(String.valueOf(i + 1));
            tvName.setText((icons.get(i) != null ? icons.get(i) + " " : "") + names.get(i));
            tvGrowth.setText(String.format(Locale.getDefault(), "+¥%.2f（%.0f%%）", g[1], g[2]));
            tvGrowth.setTextColor(Color.parseColor("#D4756A"));
            layoutTopCategories.addView(item);
        }
    }
}
