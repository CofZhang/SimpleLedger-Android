package com.simpleledger.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private RecordAdapter adapter;
    private List<Record> records;
    private RecyclerView rvRecords;
    private TextView tvEmpty, tvMonthExpense, tvMonthIncome, tvBalance;
    private TextView tvBudgetWarning, tvBudgetInfo;
    private ProgressBar progressBudget;
    private ImageButton btnBudget, btnMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        records = new ArrayList<>();

        initViews();
        loadData();
    }

    private void initViews() {
        rvRecords = findViewById(R.id.rvRecords);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvMonthExpense = findViewById(R.id.tvMonthExpense);
        tvMonthIncome = findViewById(R.id.tvMonthIncome);
        tvBalance = findViewById(R.id.tvBalance);
        tvBudgetWarning = findViewById(R.id.tvBudgetWarning);
        tvBudgetInfo = findViewById(R.id.tvBudgetInfo);
        progressBudget = findViewById(R.id.progressBudget);
        btnBudget = findViewById(R.id.btnBudget);
        btnMore = findViewById(R.id.btnMore);

        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordAdapter(records);
        adapter.setOnRecordLongClickListener((record, position) -> showDeleteDialog(record));
        rvRecords.setAdapter(adapter);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddRecordActivity.class)));

        btnBudget.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, BudgetActivity.class)));

        btnMore.setOnClickListener(v -> showMoreMenu());
    }

    private void showMoreMenu() {
        PopupMenu popup = new PopupMenu(this, btnMore);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_stats) {
                startActivity(new Intent(MainActivity.this, StatsActivity.class));
                return true;
            } else if (itemId == R.id.menu_projects) {
                startActivity(new Intent(MainActivity.this, ProjectsActivity.class));
                return true;
            } else if (itemId == R.id.menu_categories) {
                startActivity(new Intent(MainActivity.this, CategoryManageActivity.class));
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showDeleteDialog(Record record) {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deleteRecord(record.getId());
                    loadData();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadData() {
        String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        records.clear();
        records.addAll(dbHelper.getAllRecords());
        adapter.updateData(records);

        double expense = dbHelper.getMonthlyExpense(yearMonth);
        double income = dbHelper.getMonthlyIncome(yearMonth);
        double balance = income - expense;

        tvMonthExpense.setText(String.format("支出: ¥%.2f", expense));
        tvMonthIncome.setText(String.format("收入: ¥%.2f", income));
        String balanceText = String.format("结余: ¥%.2f", balance);
        tvBalance.setText(balanceText);

        double budget = dbHelper.getBudget(yearMonth);
        if (budget > 0) {
            progressBudget.setVisibility(View.VISIBLE);
            tvBudgetInfo.setVisibility(View.VISIBLE);
            
            int progress = (int) ((expense / budget) * 100);
            progressBudget.setProgress(Math.min(progress, 100));
            
            if (expense > budget) {
                tvBudgetWarning.setVisibility(View.VISIBLE);
                double overspend = expense - budget;
                tvBudgetInfo.setText(String.format("已超支 ¥%.2f (预算 ¥%.2f)", overspend, budget));
                progressBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.expense_color)));
            } else {
                tvBudgetWarning.setVisibility(View.GONE);
                double remaining = budget - expense;
                tvBudgetInfo.setText(String.format("已用 ¥%.2f / ¥%.2f，剩余 ¥%.2f", expense, budget, remaining));
                progressBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.colorPrimary)));
            }
        } else {
            progressBudget.setVisibility(View.GONE);
            tvBudgetInfo.setVisibility(View.GONE);
            tvBudgetWarning.setVisibility(View.GONE);
        }

        if (records.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvRecords.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvRecords.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
}
