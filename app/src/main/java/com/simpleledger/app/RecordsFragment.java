package com.simpleledger.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
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
    private Calendar currentMonth;

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
        view.findViewById(R.id.btnCalendar).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CalendarActivity.class);
            intent.putExtra("year", currentMonth.get(Calendar.YEAR));
            intent.putExtra("month", currentMonth.get(Calendar.MONTH));
            startActivity(intent);
        });
        view.findViewById(R.id.btnBudget).setOnClickListener(v -> startActivity(new Intent(getActivity(), BudgetActivity.class)));
        view.findViewById(R.id.btnMore).setOnClickListener(v -> showMoreMenu());

        loadData();
        return view;
    }

    private void showMoreMenu() {
        PopupMenu popup = new PopupMenu(getContext(), getView().findViewById(R.id.btnMore));
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_projects) {
                startActivity(new Intent(getActivity(), ProjectsActivity.class));
                return true;
            } else if (itemId == R.id.menu_categories) {
                startActivity(new Intent(getActivity(), CategoryManageActivity.class));
                return true;
            }
            return false;
        });
        popup.show();
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
        records.addAll(dbHelper.getRecordsByMonth(yearMonth));
        adapter.updateData(records);

        double expense = dbHelper.getMonthlyExpense(yearMonth);
        double income = dbHelper.getMonthlyIncome(yearMonth);
        double balance = income - expense;

        tvMonthExpense.setText(String.format("¥%.2f", expense));
        tvMonthIncome.setText(String.format("¥%.2f", income));
        tvBalance.setText(String.format("¥%.2f", balance));

        double budget = dbHelper.getBudget(yearMonth);
        if (budget > 0) {
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
