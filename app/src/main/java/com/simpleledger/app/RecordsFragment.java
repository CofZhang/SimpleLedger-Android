package com.simpleledger.app;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecordsFragment extends Fragment {
    private DatabaseHelper dbHelper;
    private GroupedRecordAdapter adapter;
    private List<Record> records;
    private RecyclerView rvRecords;
    private TextView tvEmpty, tvMonthExpense, tvMonthIncome, tvBalance, tvMonth, tvBudgetInfo;
    private ProgressBar progressBudget;
    private EditText etSearch;
    private Calendar currentMonth;
    private boolean searching = false;
    private String searchKeyword = "";

    // 4.5 删除撤销支持
    private Record lastDeletedRecord;
    private final Handler undoHandler = new Handler(Looper.getMainLooper());
    private Runnable undoRunnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_records, container, false);

        dbHelper = new DatabaseHelper(getContext());
        records = new ArrayList<>();
        currentMonth = Calendar.getInstance();

        // 4.6 修复：直接在 topNav 上处理状态栏 insets，
        // 确保顶部三点菜单按钮完整显示在状态栏下方
        LinearLayout topNav = view.findViewById(R.id.topNav);
        ViewCompat.setOnApplyWindowInsetsListener(topNav, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

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
        adapter = new GroupedRecordAdapter();
        adapter.setOnRecordLongClickListener((record, position) -> showRecordActionMenu(record, position));
        adapter.setOnRecordClickListener((record, position) -> showRecordDetail(record));
        rvRecords.setAdapter(adapter);

        view.findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            HapticHelper.light(getContext());
            currentMonth.add(Calendar.MONTH, -1);
            loadData();
        });
        view.findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            HapticHelper.light(getContext());
            currentMonth.add(Calendar.MONTH, 1);
            loadData();
        });
        // 点击月份标签也可快速跳转
        tvMonth.setOnClickListener(v -> {
            HapticHelper.light(getContext());
            showMonthPicker();
        });
        view.findViewById(R.id.btnCalendar).setOnClickListener(v -> {
            HapticHelper.light(getContext());
            Intent intent = new Intent(getActivity(), CalendarActivity.class);
            intent.putExtra("year", currentMonth.get(Calendar.YEAR));
            intent.putExtra("month", currentMonth.get(Calendar.MONTH));
            startActivity(intent);
        });
        view.findViewById(R.id.btnBudget).setOnClickListener(v -> {
            HapticHelper.light(getContext());
            startActivity(new Intent(getActivity(), BudgetActivity.class));
        });
        view.findViewById(R.id.btnMore).setOnClickListener(v -> {
            HapticHelper.light(getContext());
            showMoreDialog();
        });

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
            // 5.0 增强背景虚化：提高 dimAmount 使背景更暗更模糊
            dialog.getWindow().setDimAmount(0.85f);
        }

        dialogView.findViewById(R.id.itemStats).setOnClickListener(v -> {
            HapticHelper.light(getContext());
            dialog.dismiss();
            // 切换到统计Tab
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToStats();
            }
        });
        dialogView.findViewById(R.id.itemProjects).setOnClickListener(v -> {
            HapticHelper.light(getContext());
            dialog.dismiss();
            startActivity(new Intent(getActivity(), ProjectsActivity.class));
        });
        dialogView.findViewById(R.id.itemCategories).setOnClickListener(v -> {
            HapticHelper.light(getContext());
            dialog.dismiss();
            startActivity(new Intent(getActivity(), CategoryManageActivity.class));
        });
        dialogView.findViewById(R.id.itemAccounts).setOnClickListener(v -> {
            HapticHelper.light(getContext());
            dialog.dismiss();
            startActivity(new Intent(getActivity(), AccountsActivity.class));
        });
        // 5.4 导出CSV已移至设置页，此处改为日历入口
        dialogView.findViewById(R.id.itemCalendar).setOnClickListener(v -> {
            HapticHelper.light(getContext());
            dialog.dismiss();
            Intent intent = new Intent(getActivity(), CalendarActivity.class);
            intent.putExtra("year", currentMonth.get(Calendar.YEAR));
            intent.putExtra("month", currentMonth.get(Calendar.MONTH));
            startActivity(intent);
        });
        dialogView.findViewById(R.id.itemSearch).setOnClickListener(v -> {
            HapticHelper.light(getContext());
            dialog.dismiss();
            etSearch.requestFocus();
        });
        // 4.5 设置入口
        dialogView.findViewById(R.id.itemSettings).setOnClickListener(v -> {
            HapticHelper.light(getContext());
            dialog.dismiss();
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });
        dialog.show();
    }

    /** 4.5 长按记录操作菜单：查看详情 / 分享为图片 / 标记为重要 / 删除 */
    private void showRecordActionMenu(Record record, int position) {
        HapticHelper.medium(getContext());
        String[] items = {
                "📋 查看详情",
                "📤 分享为图片",
                record.isImportant() ? "☆ 取消重要标记" : "⭐ 标记为重要",
                "🗑 删除"
        };
        new AlertDialog.Builder(getContext())
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showRecordDetail(record);
                            break;
                        case 1:
                            shareRecordAsImage(record);
                            break;
                        case 2:
                            boolean newImportant = !record.isImportant();
                            dbHelper.setRecordImportant(record.getId(), newImportant);
                            record.setImportant(newImportant);
                            adapter.updateData(records);
                            Toast.makeText(getContext(), newImportant ? "已标记为重要" : "已取消标记", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            deleteWithUndo(record, position);
                            break;
                    }
                })
                .show();
    }

    /** 4.5 查看详情对话框 */
    private void showRecordDetail(Record record) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_record_detail, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("关闭", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // 5.0 增强背景虚化：提高 dimAmount 使背景更暗更模糊
            dialog.getWindow().setDimAmount(0.85f);
        }

        TextView tvCategory = dialogView.findViewById(R.id.tvDetailCategory);
        TextView tvAmount = dialogView.findViewById(R.id.tvDetailAmount);
        TextView tvDate = dialogView.findViewById(R.id.tvDetailDate);
        TextView tvAccount = dialogView.findViewById(R.id.tvDetailAccount);
        TextView tvProject = dialogView.findViewById(R.id.tvDetailProject);
        TextView tvRemark = dialogView.findViewById(R.id.tvDetailRemark);
        TextView tvTags = dialogView.findViewById(R.id.tvDetailTags);
        TextView tvTitle = dialogView.findViewById(R.id.tvDetailTitle);

        tvTitle.setText((record.getCategoryIcon() != null ? record.getCategoryIcon() + " " : "") + record.getCategoryName());
        tvCategory.setText(record.getCategoryName());
        String amountStr = (record.getType() == Record.TYPE_EXPENSE ? "-¥" : "+¥") + String.format("%.2f", record.getAmount());
        tvAmount.setText(amountStr);
        tvAmount.setTextColor(record.getType() == Record.TYPE_EXPENSE ?
                getContext().getColor(R.color.expense_color) : getContext().getColor(R.color.income_color));
        tvDate.setText(record.getDate());
        tvAccount.setText((record.getAccountIcon() != null ? record.getAccountIcon() + " " : "")
                + (record.getAccountName() != null ? record.getAccountName() : ""));
        tvProject.setText(record.getProjectName() != null && !record.getProjectName().isEmpty() ? record.getProjectName() : "无");
        tvRemark.setText(record.getRemark() != null && !record.getRemark().isEmpty() ? record.getRemark() : "无");
        tvTags.setText(record.getTags() != null && !record.getTags().isEmpty() ? record.getTags() : "无");

        dialog.show();
    }

    /** 4.5 分享为图片：将记录卡片渲染为 Bitmap 并分享 */
    private void shareRecordAsImage(Record record) {
        try {
            View cardView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_record_detail, null);
            TextView tvCategory = cardView.findViewById(R.id.tvDetailCategory);
            TextView tvAmount = cardView.findViewById(R.id.tvDetailAmount);
            TextView tvDate = cardView.findViewById(R.id.tvDetailDate);
            TextView tvAccount = cardView.findViewById(R.id.tvDetailAccount);
            TextView tvProject = cardView.findViewById(R.id.tvDetailProject);
            TextView tvRemark = cardView.findViewById(R.id.tvDetailRemark);
            TextView tvTags = cardView.findViewById(R.id.tvDetailTags);
            TextView tvTitle = cardView.findViewById(R.id.tvDetailTitle);

            tvTitle.setText("☕ 咖啡记账");
            tvCategory.setText(record.getCategoryName());
            String amountStr = (record.getType() == Record.TYPE_EXPENSE ? "-¥" : "+¥") + String.format("%.2f", record.getAmount());
            tvAmount.setText(amountStr);
            tvAmount.setTextColor(record.getType() == Record.TYPE_EXPENSE ?
                    getContext().getColor(R.color.expense_color) : getContext().getColor(R.color.income_color));
            tvDate.setText(record.getDate());
            tvAccount.setText(record.getAccountName());
            tvProject.setText(record.getProjectName() != null ? record.getProjectName() : "无");
            tvRemark.setText(record.getRemark() != null ? record.getRemark() : "无");
            tvTags.setText(record.getTags() != null ? record.getTags() : "无");

            // 测量并布局
            int widthSpec = View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().widthPixels - 80, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            cardView.measure(widthSpec, heightSpec);
            cardView.layout(0, 0, cardView.getMeasuredWidth(), cardView.getMeasuredHeight());

            Bitmap bitmap = Bitmap.createBitmap(cardView.getMeasuredWidth(), cardView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.parseColor("#F8F3ED"));
            cardView.draw(canvas);

            // 保存到缓存
            File cacheDir = new File(getContext().getCacheDir(), "share_images");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File imageFile = new File(cacheDir, "record_" + record.getId() + ".png");
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            // 分享
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imageFile));
            shareIntent.putExtra(Intent.EXTRA_TEXT, "咖啡记账 - " + record.getCategoryName() + " " + amountStr);
            startActivity(Intent.createChooser(shareIntent, "分享账单"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** 4.5 删除并支持 10 秒内撤销 */
    private void deleteWithUndo(Record record, int position) {
        lastDeletedRecord = record;
        // 临时移除（不立即删数据库）
        records.remove(position);
        adapter.updateData(records);
        updateSummary();

        if (undoRunnable != null) undoHandler.removeCallbacks(undoRunnable);
        undoRunnable = () -> {
            // 10秒后真正删除
            if (lastDeletedRecord != null && lastDeletedRecord.getId() == record.getId()) {
                dbHelper.deleteRecord(record.getId());
                lastDeletedRecord = null;
                updateSummary();
            }
        };
        undoHandler.postDelayed(undoRunnable, 10000);

        Snackbar.make(rvRecords, "已删除「" + record.getCategoryName() + "」", Snackbar.LENGTH_LONG)
                .setAction("撤销", v -> {
                    HapticHelper.light(getContext());
                    undoHandler.removeCallbacks(undoRunnable);
                    records.add(position, record);
                    adapter.updateData(records);
                    updateSummary();
                    lastDeletedRecord = null;
                    Toast.makeText(getContext(), "已恢复", Toast.LENGTH_SHORT).show();
                })
                .show();
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
        updateSummary();
    }

    private void updateSummary() {
        double expense = 0, income = 0;
        for (Record r : records) {
            if (r.getType() == Record.TYPE_EXPENSE) expense += r.getAmount();
            else income += r.getAmount();
        }
        double balance = income - expense;
        tvMonthExpense.setText(String.format("¥%.2f", expense));
        tvMonthIncome.setText(String.format("¥%.2f", income));
        tvBalance.setText(String.format("¥%.2f", balance));

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

