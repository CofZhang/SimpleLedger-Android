package com.simpleledger.app;

import android.app.DatePickerDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 5.7 新增：账户详情页
 *
 * 功能：
 * 1. 显示指定账户下的所有账单明细和总收支
 * 2. 可选筛选条件：日期范围 / 项目 / 标签（用户可自由组合或都不选）
 * 3. Tab 切换：明细 / 分类统计 / 标签统计
 */
public class AccountDetailActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private long accountId;
    private String accountName;

    // 筛选状态（null/0/空 表示不限制）
    private Calendar filterStartDate = null;
    private Calendar filterEndDate = null;
    private long filterProjectId = 0;
    private String filterProjectName = null;
    private String filterTag = null;

    // 视图引用
    private TextView tvTitle, tvFilterDate, tvFilterProject, tvFilterTag;
    private TextView tvTotalExpense, tvTotalIncome, tvBalance;
    private View pageRecords, pageCategory, pageTag;
    private TextView tabRecords, tabCategory, tabTag;
    private View tvEmptyRecords, tvEmptyCategory, tvEmptyTag;

    private GroupedRecordAdapter recordsAdapter;
    private CategoryStatAdapter categoryAdapter;
    private TagStatAdapter tagAdapter;

    private List<Record> records = new ArrayList<>();
    private List<DatabaseHelper.CategoryStatBoth> categoryStats = new ArrayList<>();
    private List<DatabaseHelper.TagStat> tagStats = new ArrayList<>();

    private List<Project> allProjects = new ArrayList<>();
    private List<String> allTags = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_detail);

        dbHelper = new DatabaseHelper(this);
        accountId = getIntent().getLongExtra("account_id", -1);
        accountName = getIntent().getStringExtra("account_name");
        if (accountName == null) accountName = "账户详情";

        initViews();
        loadData();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvFilterDate = findViewById(R.id.btnFilterDate);
        tvFilterProject = findViewById(R.id.btnFilterProject);
        tvFilterTag = findViewById(R.id.btnFilterTag);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvBalance = findViewById(R.id.tvBalance);

        pageRecords = findViewById(R.id.pageRecords);
        pageCategory = findViewById(R.id.pageCategory);
        pageTag = findViewById(R.id.pageTag);
        tabRecords = findViewById(R.id.tabRecords);
        tabCategory = findViewById(R.id.tabCategory);
        tabTag = findViewById(R.id.tabTag);
        tvEmptyRecords = findViewById(R.id.tvEmptyRecords);
        tvEmptyCategory = findViewById(R.id.tvEmptyCategory);
        tvEmptyTag = findViewById(R.id.tvEmptyTag);

        tvTitle.setText(accountName);

        RecyclerView rvRecords = findViewById(R.id.rvRecords);
        RecyclerView rvCategoryStats = findViewById(R.id.rvCategoryStats);
        RecyclerView rvTagStats = findViewById(R.id.rvTagStats);

        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        recordsAdapter = new GroupedRecordAdapter();
        rvRecords.setAdapter(recordsAdapter);

        rvCategoryStats.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryStatAdapter();
        rvCategoryStats.setAdapter(categoryAdapter);

        rvTagStats.setLayoutManager(new LinearLayoutManager(this));
        tagAdapter = new TagStatAdapter();
        rvTagStats.setAdapter(tagAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnReset).setOnClickListener(v -> {
            HapticHelper.light(this);
            resetFilters();
        });

        tvFilterDate.setOnClickListener(v -> {
            HapticHelper.light(this);
            showDateFilterDialog();
        });
        tvFilterProject.setOnClickListener(v -> {
            HapticHelper.light(this);
            showProjectFilterDialog();
        });
        tvFilterTag.setOnClickListener(v -> {
            HapticHelper.light(this);
            showTagFilterDialog();
        });

        tabRecords.setOnClickListener(v -> switchTab(0));
        tabCategory.setOnClickListener(v -> switchTab(1));
        tabTag.setOnClickListener(v -> switchTab(2));
    }

    /** 重置所有筛选条件 */
    private void resetFilters() {
        filterStartDate = null;
        filterEndDate = null;
        filterProjectId = 0;
        filterProjectName = null;
        filterTag = null;
        updateFilterLabels();
        loadData();
    }

    /** 更新筛选按钮文字 */
    private void updateFilterLabels() {
        if (filterStartDate != null && filterEndDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            tvFilterDate.setText("📅 " + sdf.format(filterStartDate.getTime()) + "~" + sdf.format(filterEndDate.getTime()));
        } else {
            tvFilterDate.setText("📅 全部");
        }

        if (filterProjectId > 0 && filterProjectName != null) {
            tvFilterProject.setText("📁 " + filterProjectName);
        } else {
            tvFilterProject.setText("📁 全部");
        }

        if (filterTag != null && !filterTag.isEmpty()) {
            tvFilterTag.setText("🏷️ " + filterTag);
        } else {
            tvFilterTag.setText("🏷️ 全部");
        }
    }

    /** 日期筛选对话框：不限 / 本月 / 本年 / 自定义 */
    private void showDateFilterDialog() {
        String[] options = {"不限", "本月", "本年", "自定义范围"};
        new AlertDialog.Builder(this)
                .setTitle("选择日期范围")
                .setItems(options, (dialog, which) -> {
                    Calendar now = Calendar.getInstance();
                    switch (which) {
                        case 0:
                            filterStartDate = null;
                            filterEndDate = null;
                            updateFilterLabels();
                            loadData();
                            break;
                        case 1:
                            filterStartDate = Calendar.getInstance();
                            filterStartDate.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1, 0, 0, 0);
                            filterEndDate = Calendar.getInstance();
                            filterEndDate.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                                    now.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
                            updateFilterLabels();
                            loadData();
                            break;
                        case 2:
                            filterStartDate = Calendar.getInstance();
                            filterStartDate.set(now.get(Calendar.YEAR), 0, 1, 0, 0, 0);
                            filterEndDate = Calendar.getInstance();
                            filterEndDate.set(now.get(Calendar.YEAR), 11, 31, 23, 59, 59);
                            updateFilterLabels();
                            loadData();
                            break;
                        case 3:
                            showCustomDatePicker();
                            break;
                    }
                })
                .show();
    }

    /** 自定义日期范围选择：先选开始日期，再选结束日期 */
    private void showCustomDatePicker() {
        final Calendar start = filterStartDate != null ? (Calendar) filterStartDate.clone() : Calendar.getInstance();
        final Calendar end = filterEndDate != null ? (Calendar) filterEndDate.clone() : Calendar.getInstance();

        new DatePickerDialog(this, (v, year, month, dayOfMonth) -> {
            start.set(year, month, dayOfMonth, 0, 0, 0);
            new DatePickerDialog(this, (v2, year2, month2, dayOfMonth2) -> {
                end.set(year2, month2, dayOfMonth2, 23, 59, 59);
                if (start.after(end)) {
                    Calendar tmp = start;
                    start.setTime(end.getTime());
                    end.setTime(tmp.getTime());
                }
                filterStartDate = start;
                filterEndDate = end;
                updateFilterLabels();
                loadData();
            }, end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH)).show();
        }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)).show();
    }

    /** 项目筛选对话框 */
    private void showProjectFilterDialog() {
        allProjects.clear();
        allProjects.addAll(dbHelper.getAllProjects());

        List<String> names = new ArrayList<>();
        names.add("不限");
        for (Project p : allProjects) {
            names.add(p.getName());
        }

        new AlertDialog.Builder(this)
                .setTitle("选择项目")
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        filterProjectId = 0;
                        filterProjectName = null;
                    } else {
                        Project p = allProjects.get(which - 1);
                        filterProjectId = p.getId();
                        filterProjectName = p.getName();
                    }
                    updateFilterLabels();
                    loadData();
                })
                .show();
    }

    /** 标签筛选对话框 */
    private void showTagFilterDialog() {
        allTags = dbHelper.getAllTags();

        List<String> display = new ArrayList<>();
        display.add("不限");
        display.addAll(allTags);

        new AlertDialog.Builder(this)
                .setTitle("选择标签")
                .setItems(display.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        filterTag = null;
                    } else {
                        filterTag = display.get(which);
                    }
                    updateFilterLabels();
                    loadData();
                })
                .show();
    }

    /** 根据筛选条件加载明细和统计 */
    private void loadData() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDate = filterStartDate != null ? sdf.format(filterStartDate.getTime()) : null;
        String endDate = filterEndDate != null ? sdf.format(filterEndDate.getTime()) : null;

        records.clear();
        records.addAll(dbHelper.getRecordsByFilter(accountId, startDate, endDate,
                filterProjectId > 0 ? filterProjectId : null, filterTag));
        recordsAdapter.updateData(records);

        // 内存中聚合分类统计
        categoryStats.clear();
        categoryStats.addAll(aggregateCategoryStats(records));
        categoryAdapter.updateData(categoryStats);

        // 内存中聚合并筛选标签统计
        tagStats.clear();
        tagStats.addAll(aggregateTagStats(records, filterTag));
        tagAdapter.updateData(tagStats);

        // 计算总收支
        double totalExpense = 0, totalIncome = 0;
        for (Record r : records) {
            if (r.getType() == Record.TYPE_EXPENSE) totalExpense += r.getAmount();
            else totalIncome += r.getAmount();
        }
        double balance = totalIncome - totalExpense;
        tvTotalExpense.setText(String.format("¥%.2f", totalExpense));
        tvTotalIncome.setText(String.format("¥%.2f", totalIncome));
        tvBalance.setText(String.format("¥%.2f", balance));

        // 空状态
        tvEmptyRecords.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyCategory.setVisibility(categoryStats.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyTag.setVisibility(tagStats.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** 从记录列表内存聚合分类统计（同时包含支出和收入） */
    private List<DatabaseHelper.CategoryStatBoth> aggregateCategoryStats(List<Record> records) {
        Map<Long, DatabaseHelper.CategoryStatBoth> map = new LinkedHashMap<>();
        for (Record r : records) {
            long catId = r.getCategoryId();
            DatabaseHelper.CategoryStatBoth stat = map.get(catId);
            if (stat == null) {
                stat = new DatabaseHelper.CategoryStatBoth();
                stat.categoryId = catId;
                stat.categoryName = r.getCategoryName() != null ? r.getCategoryName() : "未分类";
                stat.categoryIcon = r.getCategoryIcon();
                stat.categoryColor = r.getCategoryColor();
                stat.expense = 0;
                stat.income = 0;
                map.put(catId, stat);
            }
            if (r.getType() == Record.TYPE_EXPENSE) stat.expense += r.getAmount();
            else stat.income += r.getAmount();
        }
        List<DatabaseHelper.CategoryStatBoth> list = new ArrayList<>(map.values());
        java.util.Collections.sort(list, (a, b) ->
                Double.compare(b.expense + b.income, a.expense + a.income));
        return list;
    }

    /** 从记录列表内存聚合标签统计。selectedTag != null 时仅保留该标签 */
    private List<DatabaseHelper.TagStat> aggregateTagStats(List<Record> records, String selectedTag) {
        Map<String, DatabaseHelper.TagStat> map = new LinkedHashMap<>();
        for (Record r : records) {
            String tagsStr = r.getTags();
            if (tagsStr == null || tagsStr.isEmpty()) continue;
            String[] tags = tagsStr.trim().split("\\s+");
            for (String t : tags) {
                if (t.isEmpty()) continue;
                String key = t.startsWith("#") ? t : "#" + t;
                if (selectedTag != null && !selectedTag.isEmpty() && !t.equals(selectedTag)) continue;
                DatabaseHelper.TagStat stat = map.get(key);
                if (stat == null) {
                    stat = new DatabaseHelper.TagStat();
                    stat.tag = key;
                    stat.expense = 0;
                    stat.income = 0;
                    map.put(key, stat);
                }
                if (r.getType() == Record.TYPE_EXPENSE) stat.expense += r.getAmount();
                else stat.income += r.getAmount();
            }
        }
        List<DatabaseHelper.TagStat> list = new ArrayList<>(map.values());
        java.util.Collections.sort(list, (a, b) ->
                Double.compare(b.expense + b.income, a.expense + a.income));
        return list;
    }

    private void switchTab(int index) {
        pageRecords.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        pageCategory.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        pageTag.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        updateTabStyle(tabRecords, index == 0);
        updateTabStyle(tabCategory, index == 1);
        updateTabStyle(tabTag, index == 2);
    }

    private void updateTabStyle(TextView tab, boolean selected) {
        if (selected) {
            tab.setBackgroundResource(R.drawable.bg_segmented_selected);
            tab.setTextColor(getResources().getColor(R.color.white));
            tab.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            tab.setBackgroundResource(android.R.color.transparent);
            tab.setTextColor(getResources().getColor(R.color.text_secondary));
            tab.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    /** 分类统计适配器 */
    private static class CategoryStatAdapter extends RecyclerView.Adapter<CategoryStatAdapter.VH> {
        private List<DatabaseHelper.CategoryStatBoth> data = new ArrayList<>();

        void updateData(List<DatabaseHelper.CategoryStatBoth> newData) {
            this.data = newData;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_range_category, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DatabaseHelper.CategoryStatBoth stat = data.get(position);
            holder.tvName.setText(stat.categoryName);
            holder.tvIcon.setText(stat.categoryIcon != null ? stat.categoryIcon : "📦");
            GradientDrawable bg = (GradientDrawable) holder.tvIcon.getBackground();
            if (bg != null) bg.setColor(stat.categoryColor);
            holder.tvExpense.setText(String.format(Locale.getDefault(), "-¥%.2f", stat.expense));
            holder.tvIncome.setText(String.format(Locale.getDefault(), "+¥%.2f", stat.income));
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvIcon, tvName, tvExpense, tvIncome;
            VH(@NonNull View itemView) {
                super(itemView);
                tvIcon = itemView.findViewById(R.id.tvCategoryIcon);
                tvName = itemView.findViewById(R.id.tvCategoryName);
                tvExpense = itemView.findViewById(R.id.tvExpense);
                tvIncome = itemView.findViewById(R.id.tvIncome);
            }
        }
    }

    /** 标签统计适配器 */
    private static class TagStatAdapter extends RecyclerView.Adapter<TagStatAdapter.VH> {
        private List<DatabaseHelper.TagStat> data = new ArrayList<>();

        void updateData(List<DatabaseHelper.TagStat> newData) {
            this.data = newData;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_range_tag, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DatabaseHelper.TagStat stat = data.get(position);
            holder.tvName.setText(stat.tag);
            holder.tvExpense.setText(String.format(Locale.getDefault(), "-¥%.2f", stat.expense));
            holder.tvIncome.setText(String.format(Locale.getDefault(), "+¥%.2f", stat.income));
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvExpense, tvIncome;
            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvTagName);
                tvExpense = itemView.findViewById(R.id.tvExpense);
                tvIncome = itemView.findViewById(R.id.tvIncome);
            }
        }
    }
}
