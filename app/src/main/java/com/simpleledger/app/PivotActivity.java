package com.simpleledger.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 5.1 重新设计：年维度透视表 → 年账单汇总页面（暖米色简约风格）
 *
 * 布局结构：
 * - 顶部 56dp 玻璃条导航栏（返回 + 标题"年账单"）
 * - 顶部切换栏：左侧年份选择 + 右侧月账单/年账单切换标签
 * - 汇总卡片（暖棕色渐变背景）：年结余 / 年收入 / 年支出
 * - 月份列表（4列 + 右侧箭头）：月份、月收入、月支出、月结余（倒序）
 *
 * 数据来源：DatabaseHelper.getMonthlySumForYear(year) 返回 12 个月份的收支汇总
 */
public class PivotActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView btnPivotYear;
    private View btnTabMonth;
    private View btnTabYear;
    private TextView tvYearBalance;
    private TextView tvYearIncome;
    private TextView tvYearExpense;
    private LinearLayout listMonths;

    private int currentYear;

    // 颜色常量（暖米色简约风格）
    private final int colorTextPrimary = Color.parseColor("#3D352C");   // 深灰
    private final int colorTextSecondary = Color.parseColor("#8B7D6B"); // 中灰
    private final int colorIncome = Color.parseColor("#7BA678");        // 柔和绿
    private final int colorExpense = Color.parseColor("#D4756A");       // 柔和红
    private final int colorBalancePositive = Color.parseColor("#7BA678");
    private final int colorBalanceNegative = Color.parseColor("#D4756A");
    private final int colorArrow = Color.parseColor("#B8AC9A");         // 浅灰箭头

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pivot);

        // 沉浸式状态栏
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            View topNav = findViewById(R.id.topNav);
            if (topNav != null) {
                topNav.setPadding(0, statusBarHeight, 0, 0);
            }
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        currentYear = Calendar.getInstance().get(Calendar.YEAR);

        btnPivotYear = findViewById(R.id.btnPivotYear);
        btnTabMonth = findViewById(R.id.btnTabMonth);
        btnTabYear = findViewById(R.id.btnTabYear);
        tvYearBalance = findViewById(R.id.tvYearBalance);
        tvYearIncome = findViewById(R.id.tvYearIncome);
        tvYearExpense = findViewById(R.id.tvYearExpense);
        listMonths = findViewById(R.id.listMonths);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            HapticHelper.light(this);
            finish();
        });

        btnPivotYear.setText(currentYear + "年");
        btnPivotYear.setOnClickListener(v -> {
            HapticHelper.light(this);
            showYearPicker();
        });

        // 月账单切换：跳转到 CalendarActivity（月维度日历视图）
        btnTabMonth.setOnClickListener(v -> {
            HapticHelper.light(this);
            Intent intent = new Intent(this, CalendarActivity.class);
            startActivity(intent);
            finish();
        });

        // 年账单切换：当前页面，无操作（已是选中态）
        btnTabYear.setOnClickListener(v -> {
            HapticHelper.light(this);
        });

        refreshData();
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
                    refreshData();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void refreshData() {
        // 获取当前年份 12 个月的收支汇总
        List<DatabaseHelper.MonthlySum> monthlySums = dbHelper.getMonthlySumForYear(currentYear);

        // 计算年度合计
        double yearIncome = 0;
        double yearExpense = 0;
        for (DatabaseHelper.MonthlySum ms : monthlySums) {
            yearIncome += ms.income;
            yearExpense += ms.expense;
        }
        double yearBalance = yearIncome - yearExpense;

        // 更新汇总卡片
        tvYearBalance.setText(formatMoney(yearBalance));
        tvYearIncome.setText(formatMoney(yearIncome));
        tvYearExpense.setText(formatMoney(yearExpense));
        // 结余为负时显示红色
        tvYearBalance.setTextColor(yearBalance < 0 ? colorBalanceNegative : Color.WHITE);

        // 渲染月份列表（倒序：最新月份在上）
        listMonths.removeAllViews();
        for (int i = monthlySums.size() - 1; i >= 0; i--) {
            DatabaseHelper.MonthlySum ms = monthlySums.get(i);
            // 未来月份（无数据）也显示，但金额为 0
            View row = buildMonthRow(ms);
            listMonths.addView(row);
        }
    }

    /** 构建单个月份行：月份 | 收入 | 支出 | 结余 | > */
    private View buildMonthRow(DatabaseHelper.MonthlySum ms) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(getResources().getDrawable(R.drawable.bg_month_row));
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(8);
        row.setLayoutParams(rowLp);

        // 第1列：月份
        TextView tvMonth = new TextView(this);
        tvMonth.setText(ms.month + "月");
        tvMonth.setTextColor(colorTextPrimary);
        tvMonth.setTextSize(15f);
        tvMonth.setTypeface(tvMonth.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lpMonth = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvMonth.setLayoutParams(lpMonth);
        row.addView(tvMonth);

        // 第2列：月收入
        TextView tvIncome = new TextView(this);
        tvIncome.setText(formatMoney(ms.income));
        tvIncome.setTextColor(ms.income > 0 ? colorIncome : colorTextSecondary);
        tvIncome.setTextSize(14f);
        LinearLayout.LayoutParams lpIncome = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f);
        lpIncome.gravity = Gravity.END;
        tvIncome.setLayoutParams(lpIncome);
        row.addView(tvIncome);

        // 第3列：月支出
        TextView tvExpense = new TextView(this);
        tvExpense.setText(formatMoney(ms.expense));
        tvExpense.setTextColor(ms.expense > 0 ? colorExpense : colorTextSecondary);
        tvExpense.setTextSize(14f);
        LinearLayout.LayoutParams lpExpense = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f);
        lpExpense.gravity = Gravity.END;
        tvExpense.setLayoutParams(lpExpense);
        row.addView(tvExpense);

        // 第4列：月结余
        double balance = ms.income - ms.expense;
        TextView tvBalance = new TextView(this);
        tvBalance.setText(formatMoney(balance));
        tvBalance.setTextColor(balance > 0 ? colorBalancePositive
                : (balance < 0 ? colorBalanceNegative : colorTextSecondary));
        tvBalance.setTextSize(14f);
        tvBalance.setTypeface(tvBalance.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lpBalance = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f);
        lpBalance.gravity = Gravity.END;
        tvBalance.setLayoutParams(lpBalance);
        row.addView(tvBalance);

        // 第5列：右侧箭头 >
        TextView tvArrow = new TextView(this);
        tvArrow.setText(">");
        tvArrow.setTextColor(colorArrow);
        tvArrow.setTextSize(18f);
        LinearLayout.LayoutParams lpArrow = new LinearLayout.LayoutParams(
                dp(20), ViewGroup.LayoutParams.WRAP_CONTENT);
        lpArrow.gravity = Gravity.CENTER;
        lpArrow.setMarginStart(dp(6));
        tvArrow.setLayoutParams(lpArrow);
        row.addView(tvArrow);

        // 点击行：跳转到该月详情（目前用 Toast 提示，未来可扩展为月详情页）
        row.setOnClickListener(v -> {
            HapticHelper.light(this);
            Toast.makeText(this, currentYear + "年" + ms.month + "月详情", Toast.LENGTH_SHORT).show();
        });

        return row;
    }

    /** 格式化金额：负数带 - 号，正数带 + 号（仅在月结余场景），其他用 ¥ 前缀 */
    private String formatMoney(double value) {
        if (value == 0) {
            return "¥0.00";
        }
        return "¥" + String.format(Locale.getDefault(), "%.2f", value);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
