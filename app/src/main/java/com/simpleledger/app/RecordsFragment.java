package com.simpleledger.app;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecordsFragment extends Fragment {
    private DatabaseHelper dbHelper;
    private RecordAdapter adapter;
    private List<Record> records;
    private RecyclerView rvRecords;
    private TextView tvEmpty, tvMonthExpense, tvMonthIncome, tvBalance, tvMonth, tvBudgetInfo;
    private ProgressBar progressBudget;
    private EditText etSearch;
    private Calendar currentMonth;
    private boolean searching = false;
    private String searchKeyword = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_records, container, false);

        dbHelper = new DatabaseHelper(getContext());
        records = new ArrayList<>();
        currentMonth = Calendar.getInstance();

        rvRecords = view.findViewById(R.id.rvRecords);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvMonthExpense = view.findViewById(R.id.tvMonthExpense);
        tvMonthIncome = view.findViewById(R.id.tvMonthIncome);
        tvBalance = view.findViewById(R.id.tvBalance);
        tvMonth = view.findViewById(R.id.tvMonth);
        tvBudgetInfo = view.findViewById(R.id.tvBudgetInfo);
        progressBudget = view.findViewById(R.id.progressBudget);
        etSearch = view.findViewById(R.id.etSearch);

        rvRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecordAdapter(records);
        adapter.setOnRecordLongClickListener((record, position) -> showDeleteDialog(record));
        rvRecords.setAdapter(adapter);

        view.findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            loadData();
        });
        view.findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            loadData();
        });
        // 点击月份标签也可快速跳转
        tvMonth.setOnClickListener(v -> showMonthPicker());
        view.findViewById(R.id.btnCalendar).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CalendarActivity.class);
            intent.putExtra("year", currentMonth.get(Calendar.YEAR));
            intent.putExtra("month", currentMonth.get(Calendar.MONTH));
            startActivity(intent);
        });
        view.findViewById(R.id.btnBudget).setOnClickListener(v -> startActivity(new Intent(getActivity(), BudgetActivity.class)));
        view.findViewById(R.id.btnMore).setOnClickListener(v -> showMoreDialog());

        // 搜索框
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchKeyword = s.toString().trim();
                searching = !searchKeyword.isEmpty();
                loadData();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadData();
        return view;
    }

    private void showMonthPicker() {
        new DatePickerDialog(getContext(), (v, year, month, dayOfMonth) -> {
            currentMonth.set(Calendar.YEAR, year);
            currentMonth.set(Calendar.MONTH, month);
            loadData();
        }, currentMonth.get(Calendar.YEAR), currentMonth.get(Calendar.MONTH), 1).show();
    }

    private void showMoreDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_more_menu, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.itemStats).setOnClickListener(v -> {
            dialog.dismiss();
            // 切换到统计Tab
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToStats();
            }
        });
        dialogView.findViewById(R.id.itemProjects).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(getActivity(), ProjectsActivity.class));
        });
        dialogView.findViewById(R.id.itemCategories).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(getActivity(), CategoryManageActivity.class));
        });
        dialogView.findViewById(R.id.itemAccounts).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(getActivity(), AccountsActivity.class));
        });
        dialogView.findViewById(R.id.itemExport).setOnClickListener(v -> {
            dialog.dismiss();
            exportCsv();
        });
        dialogView.findViewById(R.id.itemSearch).setOnClickListener(v -> {
            dialog.dismiss();
            etSearch.requestFocus();
        });
        dialog.show();
    }

    private void exportCsv() {
        try {
            java.io.File dir = new java.io.File(android.os.Environment.getExternalStorageDirectory(), "CoffeeLedger");
            if (!dir.exists()) dir.mkdirs();
            String fileName = "记账导出_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis()) + ".csv";
            java.io.File file = new java.io.File(dir, fileName);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write("日期,类型,分类,金额,账户,项目,备注,标签\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (Record r : dbHelper.getAllRecords()) {
                String type = r.getType() == Record.TYPE_EXPENSE ? "支出" : "收入";
                String line = String.format("%s,%s,%s,%.2f,%s,%s,%s,%s\n",
                        r.getDate() != null ? r.getDate() : "",
                        type,
                        r.getCategoryName() != null ? r.getCategoryName() : "",
                        r.getAmount(),
                        r.getAccountName() != null ? r.getAccountName() : "",
                        r.getProjectName() != null ? r.getProjectName() : "",
                        r.getRemark() != null ? r.getRemark().replace(",", " ") : "",
                        r.getTags() != null ? r.getTags().replace(",", " ") : "");
                writer.write(line);
            }
            writer.close();
            new AlertDialog.Builder(getContext())
                    .setTitle("导出成功")
                    .setMessage("文件已保存至：\n" + file.getAbsolutePath())
                    .setPositiveButton(R.string.confirm, null)
                    .show();
        } catch (Exception e) {
            new AlertDialog.Builder(getContext())
                    .setTitle("导出失败")
                    .setMessage(e.getMessage())
                    .setPositiveButton(R.string.confirm, null)
                    .show();
        }
    }

    private void showDeleteDialog(Record record) {
        new AlertDialog.Builder(getContext())
                .setTitle("提示")
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deleteRecord(record.getId());
                    loadData();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public void refreshData() {
        if (dbHelper != null) loadData();
    }

    private void loadData() {
        String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentMonth.getTime());
        String displayMonth = new SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(currentMonth.getTime());
        tvMonth.setText(displayMonth);

        records.clear();
        if (searching) {
            records.addAll(dbHelper.searchRecords(searchKeyword));
        } else {
            records.addAll(dbHelper.getRecordsByMonth(yearMonth));
        }
        adapter.updateData(records);

        double expense = dbHelper.getMonthlyExpense(yearMonth);
        double income = dbHelper.getMonthlyIncome(yearMonth);
        double balance = income - expense;

        tvMonthExpense.setText(String.format("¥%.2f", expense));
        tvMonthIncome.setText(String.format("¥%.2f", income));
        tvBalance.setText(String.format("¥%.2f", balance));

        double budget = dbHelper.getBudget(yearMonth);
        if (budget > 0 && !searching) {
            progressBudget.setVisibility(View.VISIBLE);
            tvBudgetInfo.setVisibility(View.VISIBLE);
            int progress = (int) ((expense / budget) * 100);
            progressBudget.setProgress(Math.min(progress, 100));
            if (expense > budget) {
                double overspend = expense - budget;
                tvBudgetInfo.setText(String.format("已超支 ¥%.2f (预算 ¥%.2f)", overspend, budget));
                progressBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.expense_color)));
            } else {
                double remaining = budget - expense;
                tvBudgetInfo.setText(String.format("已用 ¥%.2f / ¥%.2f，剩余 ¥%.2f", expense, budget, remaining));
                progressBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
            }
        } else {
            progressBudget.setVisibility(View.GONE);
            tvBudgetInfo.setVisibility(View.GONE);
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
    public void onResume() {
        super.onResume();
        refreshData();
    }
}
