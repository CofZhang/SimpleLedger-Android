package com.simpleledger.app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 4.5 多维度透视表 Activity
 * - 行=月份（1-12），列=分类，值=支出/收入金额合计
 * - 类型切换：支出 / 收入
 * - 年份选择 NumberPicker
 * - 用 TableLayout 动态构建表格，含合计行与合计列
 */
public class PivotActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TableLayout tablePivot;
    private TextView btnPivotYear;
    private View btnTypeExpense;
    private View btnTypeIncome;

    private int currentYear;
    private int currentType = Record.TYPE_EXPENSE;

    private final int colorHeaderBg = Color.parseColor("#E8DDD0");
    private final int colorMonthColBg = Color.parseColor("#EDE4D6");
    private final int colorEvenRowBg = Color.parseColor("#FFFFFF");
    private final int colorOddRowBg = Color.parseColor("#FAF5EE");
    private final int colorTotalRowBg = Color.parseColor("#E8DDD0");
    private final int colorTextPrimary = Color.parseColor("#3D352C");
    private final int colorTextSecondary = Color.parseColor("#8B7D6B");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pivot);

        // 沉浸式状态栏
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, statusBarHeight, 0, 0);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        currentYear = Calendar.getInstance().get(Calendar.YEAR);

        tablePivot = findViewById(R.id.tablePivot);
        btnPivotYear = findViewById(R.id.btnPivotYear);
        btnTypeExpense = findViewById(R.id.btnTypeExpense);
        btnTypeIncome = findViewById(R.id.btnTypeIncome);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnPivotYear.setText(currentYear + "年");
        btnPivotYear.setOnClickListener(v -> showYearPicker());

        btnTypeExpense.setOnClickListener(v -> {
            if (currentType != Record.TYPE_EXPENSE) {
                currentType = Record.TYPE_EXPENSE;
                updateTypeToggle();
                refreshTable();
            }
        });
        btnTypeIncome.setOnClickListener(v -> {
            if (currentType != Record.TYPE_INCOME) {
                currentType = Record.TYPE_INCOME;
                updateTypeToggle();
                refreshTable();
            }
        });

        updateTypeToggle();
        refreshTable();
    }

    private void updateTypeToggle() {
        if (currentType == Record.TYPE_EXPENSE) {
            btnTypeExpense.setBackgroundResource(R.drawable.bg_segmented_selected);
            ((TextView) btnTypeExpense).setTextColor(Color.WHITE);
            btnTypeIncome.setBackground(null);
            ((TextView) btnTypeIncome).setTextColor(colorTextSecondary);
        } else {
            btnTypeIncome.setBackgroundResource(R.drawable.bg_segmented_selected);
            ((TextView) btnTypeIncome).setTextColor(Color.WHITE);
            btnTypeExpense.setBackground(null);
            ((TextView) btnTypeExpense).setTextColor(colorTextSecondary);
        }
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
                    btnPivotYear.setText(currentYear + "年");
                    refreshTable();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void refreshTable() {
        Map<String, Map<String, Double>> pivot = dbHelper.getPivotData(currentYear, currentType);
        // 收集所有出现过的分类名（保持顺序）
        Set<String> categorySet = new LinkedHashSet<>();
        for (Map<String, Double> row : pivot.values()) {
            categorySet.addAll(row.keySet());
        }
        List<String> categories = new ArrayList<>(categorySet);

        tablePivot.removeAllViews();

        // 1. 表头行：月份 + 各分类
        TableRow header = new TableRow(this);
        header.setBackgroundColor(colorHeaderBg);
        header.addView(makeCell("月份", true, true, false, colorHeaderBg));
        for (String cat : categories) {
            header.addView(makeCell(cat, true, false, false, colorHeaderBg));
        }
        header.addView(makeCell("合计", true, false, false, colorHeaderBg));
        tablePivot.addView(header);

        // 2. 12 个月份行
        double[] colTotals = new double[categories.size()];
        double grandTotal = 0.0;
        for (int m = 1; m <= 12; m++) {
            String monthKey = String.format("%04d-%02d", currentYear, m);
            Map<String, Double> row = pivot.get(monthKey);
            TableRow tr = new TableRow(this);
            boolean odd = (m % 2 == 1);
            int rowBg = odd ? colorOddRowBg : colorEvenRowBg;
            tr.addView(makeCell(m + "月", false, true, false, colorMonthColBg));

            double rowTotal = 0.0;
            for (int i = 0; i < categories.size(); i++) {
                double v = 0.0;
                if (row != null) {
                    Double d = row.get(categories.get(i));
                    if (d != null) v = d;
                }
                rowTotal += v;
                colTotals[i] += v;
                tr.addView(makeCell(String.format("%.2f", v), false, false, false, rowBg));
            }
            grandTotal += rowTotal;
            tr.addView(makeCell(String.format("%.2f", rowTotal), false, false, true, rowBg));
            tablePivot.addView(tr);
        }

        // 3. 合计行
        TableRow totalRow = new TableRow(this);
        totalRow.setBackgroundColor(colorTotalRowBg);
        totalRow.addView(makeCell("合计", true, true, true, colorTotalRowBg));
        for (int i = 0; i < categories.size(); i++) {
            totalRow.addView(makeCell(String.format("%.2f", colTotals[i]), true, false, true, colorTotalRowBg));
        }
        totalRow.addView(makeCell(String.format("%.2f", grandTotal), true, false, true, colorTotalRowBg));
        tablePivot.addView(totalRow);
    }

    /**
     * 构造单元格 TextView
     *
     * @param text       文本
     * @param bold       是否加粗
     * @param isMonthCol 是否是月份列
     * @param isTotal    是否是合计列/行
     * @param bgColor    背景色
     */
    private TextView makeCell(String text, boolean bold, boolean isMonthCol, boolean isTotal, int bgColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(8), dp(8), dp(8), dp(8));
        tv.setMinimumWidth(dp(80));
        tv.setMinWidth(dp(80));
        tv.setTextColor(colorTextPrimary);
        if (bold) {
            tv.setTypeface(android.graphics.Typeface.create(
                    tv.getTypeface(), android.graphics.Typeface.BOLD));
        }
        tv.setBackgroundColor(bgColor);
        // 用 LayoutParams 让单元格均匀
        TableRow.LayoutParams lp = new TableRow.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(1, 1, 1, 1);
        tv.setLayoutParams(lp);
        return tv;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
