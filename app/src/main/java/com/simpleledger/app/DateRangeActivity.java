package com.simpleledger.app;

import android.app.DatePickerDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 5.6 新增：自定义日期范围查看
 *
 * 功能：
 * 1. 选择开始日期和结束日期
 * 2. 显示该范围内的账单明细（按日期分组，紧凑布局）
 * 3. 显示该范围内的总支出、总收入、结余
 * 4. Tab 切换：明细 / 分类统计 / 标签统计
 *    - 分类统计：每个分类的总支出和总收入
 *    - 标签统计：每个标签的总支出和总收入
 * 5. 快捷选择：本月 / 本年
 */
public class DateRangeActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private Calendar startDate = Calendar.getInstance();
    private Calendar endDate = Calendar.getInstance();

    private TextView tvStartDate, tvEndDate;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_range);

        dbHelper = new DatabaseHelper(this);

        // 默认本月范围
        Calendar now = Calendar.getInstance();
        startDate.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1);
        endDate.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.getActualMaximum(Calendar.DAY_OF_MONTH));

        initViews();
        loadData();
    }

    private void initViews() {
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
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

        // 日期选择
        findViewById(R.id.btnStartDate).setOnClickListener(v -> showDatePicker(true));
        findViewById(R.id.btnEndDate).setOnClickListener(v -> showDatePicker(false));

        // 快捷选择：本月
        findViewById(R.id.btnQuickThisMonth).setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            startDate.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1);
            endDate.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                    now.getActualMaximum(Calendar.DAY_OF_MONTH));
            updateDateLabels();
            loadData();
        });

        // 快捷选择：本年
        findViewById(R.id.btnQuickThisYear).setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            startDate.set(now.get(Calendar.YEAR), 0, 1);
            endDate.set(now.get(Calendar.YEAR), 11, 31);
            updateDateLabels();
            loadData();
        });

        // Tab 切换
        tabRecords.setOnClickListener(v -> switchTab(0));
        tabCategory.setOnClickListener(v -> switchTab(1));
        tabTag.setOnClickListener(v -> switchTab(2));

        updateDateLabels();
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = isStart ? startDate : endDate;
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            if (isStart) {
                startDate.set(year, month, dayOfMonth);
            } else {
                endDate.set(year, month, dayOfMonth);
            }
            // 校验：开始日期不能晚于结束日期
            if (startDate.after(endDate)) {
                if (isStart) {
                    endDate = (Calendar) startDate.clone();
                } else {
                    startDate = (Calendar) endDate.clone();
                }
            }
            updateDateLabels();
            loadData();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabels() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvStartDate.setText(sdf.format(startDate.getTime()));
        tvEndDate.setText(sdf.format(endDate.getTime()));
    }

    private void loadData() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String start = sdf.format(startDate.getTime());
        String end = sdf.format(endDate.getTime());

        // 加载明细
        records.clear();
        records.addAll(dbHelper.getRecordsByDateRange(start, end));
        recordsAdapter.updateData(records);

        // 加载分类统计
        categoryStats.clear();
        categoryStats.addAll(dbHelper.getCategoryStatsBothForDateRange(start, end));
        categoryAdapter.updateData(categoryStats);

        // 加载标签统计
        tagStats.clear();
        tagStats.addAll(dbHelper.getTagStatsForDateRange(start, end));
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

    private void switchTab(int index) {
        // 切换页面显示
        pageRecords.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        pageCategory.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        pageTag.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        // 切换 Tab 样式
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
