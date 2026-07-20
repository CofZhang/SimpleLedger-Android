package com.simpleledger.app;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BudgetActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private EditText etBudget;
    private TextView tvUsed, tvRemaining, tvCurrentBudget;
    private ProgressBar progressBudget;
    private String currentYearMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        dbHelper = new DatabaseHelper(this);
        currentYearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        etBudget = findViewById(R.id.etBudget);
        tvUsed = findViewById(R.id.tvUsed);
        tvRemaining = findViewById(R.id.tvRemaining);
        tvCurrentBudget = findViewById(R.id.tvCurrentBudget);
        progressBudget = findViewById(R.id.progressBudget);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveBudget());

        loadBudget();
    }

    private void loadBudget() {
        double budget = dbHelper.getBudget(currentYearMonth);
        double expense = dbHelper.getMonthlyExpense(currentYearMonth);

        if (budget > 0) {
            etBudget.setText(String.valueOf(budget));
        }

        updateBudgetStatus(budget, expense);
    }

    private void updateBudgetStatus(double budget, double expense) {
        tvUsed.setText(String.format("已支出: ¥%.2f", expense));

        if (budget <= 0) {
            tvCurrentBudget.setText("尚未设置本月预算");
            tvRemaining.setText("");
            progressBudget.setProgress(0);
            return;
        }

        int progress = (int) ((expense / budget) * 100);
        progressBudget.setProgress(Math.min(progress, 100));

        tvCurrentBudget.setText(String.format("当前预算: ¥%.2f", budget));
        if (expense > budget) {
            double overspend = expense - budget;
            tvRemaining.setText(String.format("⚠️ 已超支 ¥%.2f", overspend));
            tvRemaining.setTextColor(getColor(R.color.expense_color));
            progressBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.expense_color)));
        } else {
            double remaining = budget - expense;
            tvRemaining.setText(String.format("剩余 ¥%.2f (使用率 %d%%)", remaining, progress));
            tvRemaining.setTextColor(getColor(R.color.income_color));
            progressBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.colorAccent)));
        }
    }

    private void saveBudget() {
        String budgetStr = etBudget.getText().toString().trim();
        
        double budget;
        if (budgetStr.isEmpty()) {
            budget = 0;
        } else {
            try {
                budget = Double.parseDouble(budgetStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "请输入正确的预算金额", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (budget < 0) {
            Toast.makeText(this, "预算金额不能为负数", Toast.LENGTH_SHORT).show();
            return;
        }

        dbHelper.setBudget(currentYearMonth, budget);

        double expense = dbHelper.getMonthlyExpense(currentYearMonth);
        updateBudgetStatus(budget, expense);
        Toast.makeText(this, "预算设置成功", Toast.LENGTH_SHORT).show();
    }
}
