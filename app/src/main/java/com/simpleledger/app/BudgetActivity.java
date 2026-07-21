package com.simpleledger.app;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 6.0 增强预算管理：总预算 + 分类预算 + 进度条可视化 + 超支预警
 */
public class BudgetActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private EditText etBudget;
    private TextView tvUsed, tvRemaining, tvMonth;
    private ProgressBar progressBudget;
    private String currentYearMonth;
    private Calendar currentDate;
    private CategoryBudgetAdapter adapter;
    private List<Category> expenseCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        dbHelper = new DatabaseHelper(this);
        currentDate = Calendar.getInstance();
        currentYearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        etBudget = findViewById(R.id.etBudget);
        tvUsed = findViewById(R.id.tvUsed);
        tvRemaining = findViewById(R.id.tvRemaining);
        tvMonth = findViewById(R.id.tvMonth);
        progressBudget = findViewById(R.id.progressBudget);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveBudget());

        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentDate.add(Calendar.MONTH, -1);
            currentYearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    .format(currentDate.getTime());
            loadData();
        });
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentDate.add(Calendar.MONTH, 1);
            currentYearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    .format(currentDate.getTime());
            loadData();
        });

        RecyclerView rv = findViewById(R.id.rvCategoryBudgets);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryBudgetAdapter();
        rv.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        tvMonth.setText(currentYearMonth);
        double budget = dbHelper.getBudget(currentYearMonth);
        double expense = dbHelper.getMonthlyExpense(currentYearMonth);

        if (budget > 0) {
            etBudget.setText(String.valueOf(budget));
        } else {
            etBudget.setText("");
        }
        updateTotalBudgetStatus(budget, expense);

        // 加载分类预算
        expenseCategories = dbHelper.getCategories(Record.TYPE_EXPENSE);
        adapter.notifyDataSetChanged();
    }

    private void updateTotalBudgetStatus(double budget, double expense) {
        tvUsed.setText(String.format("已支出 ¥%.2f", expense));

        if (budget <= 0) {
            tvRemaining.setText("未设置预算");
            tvRemaining.setTextColor(getColor(R.color.text_secondary));
            progressBudget.setProgress(0);
            return;
        }

        int progress = (int) ((expense / budget) * 100);
        progressBudget.setProgress(Math.min(progress, 100));

        if (expense > budget) {
            double overspend = expense - budget;
            tvRemaining.setText(String.format("⚠️ 超支 ¥%.2f", overspend));
            tvRemaining.setTextColor(getColor(R.color.expense_color));
            progressBudget.setProgressTintList(ColorStateList.valueOf(getColor(R.color.expense_color)));
        } else {
            double remaining = budget - expense;
            tvRemaining.setText(String.format("剩余 ¥%.2f (%d%%)", remaining, progress));
            tvRemaining.setTextColor(getColor(R.color.income_color));
            progressBudget.setProgressTintList(ColorStateList.valueOf(getColor(R.color.colorAccent)));
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
        updateTotalBudgetStatus(budget, expense);
        Toast.makeText(this, "预算设置成功", Toast.LENGTH_SHORT).show();
    }

    /** 分类预算适配器 */
    private class CategoryBudgetAdapter extends RecyclerView.Adapter<CategoryBudgetAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_budget, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Category cat = expenseCategories.get(position);
            holder.tvIcon.setText(cat.getIcon());
            holder.tvName.setText(cat.getName());

            double budget = dbHelper.getCategoryBudget(cat.getId(), currentYearMonth);
            double expense = dbHelper.getCategoryExpenseByMonth(cat.getId(), currentYearMonth);

            if (budget > 0) {
                holder.tvBudgetAmount.setText(String.format("¥%.2f", budget));
                int progress = (int) ((expense / budget) * 100);
                holder.progress.setProgress(Math.min(progress, 100));

                if (expense > budget) {
                    holder.tvStatus.setText(String.format("超支 ¥%.2f", expense - budget));
                    holder.tvStatus.setTextColor(getColor(R.color.expense_color));
                    holder.progress.setProgressTintList(
                            ColorStateList.valueOf(getColor(R.color.expense_color)));
                } else {
                    holder.tvStatus.setText(String.format("已用 ¥%.2f / 剩 ¥%.2f", expense, budget - expense));
                    holder.tvStatus.setTextColor(getColor(R.color.text_secondary));
                    holder.progress.setProgressTintList(
                            ColorStateList.valueOf(getColor(R.color.colorAccent)));
                }
            } else {
                holder.tvBudgetAmount.setText("未设置");
                holder.tvBudgetAmount.setTextColor(getColor(R.color.text_hint));
                holder.tvStatus.setText(expense > 0 ? String.format("已支出 ¥%.2f", expense) : "暂无支出");
                holder.tvStatus.setTextColor(getColor(R.color.text_hint));
                holder.progress.setProgress(0);
            }

            holder.itemView.setOnClickListener(v -> showSetBudgetDialog(cat));
        }

        @Override
        public int getItemCount() {
            return expenseCategories.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvIcon, tvName, tvStatus, tvBudgetAmount;
            ProgressBar progress;

            VH(@NonNull View itemView) {
                super(itemView);
                tvIcon = itemView.findViewById(R.id.tvCategoryIcon);
                tvName = itemView.findViewById(R.id.tvCategoryName);
                tvStatus = itemView.findViewById(R.id.tvBudgetStatus);
                tvBudgetAmount = itemView.findViewById(R.id.tvBudgetAmount);
                progress = itemView.findViewById(R.id.progressCategory);
            }
        }
    }

    private void showSetBudgetDialog(Category cat) {
        double currentBudget = dbHelper.getCategoryBudget(cat.getId(), currentYearMonth);
        EditText et = new EditText(this);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setText(currentBudget > 0 ? String.valueOf(currentBudget) : "");
        et.setHint("设置 " + cat.getName() + " 的月度预算");

        new AlertDialog.Builder(this)
                .setTitle(cat.getIcon() + " " + cat.getName() + " 预算")
                .setView(et)
                .setPositiveButton("保存", (d, w) -> {
                    String s = et.getText().toString().trim();
                    double amount = 0;
                    if (!s.isEmpty()) {
                        try { amount = Double.parseDouble(s); }
                        catch (Exception e) { amount = 0; }
                    }
                    dbHelper.setCategoryBudget(cat.getId(), currentYearMonth, amount);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "分类预算已保存", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("清除", (d, w) -> {
                    dbHelper.setCategoryBudget(cat.getId(), currentYearMonth, 0);
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
