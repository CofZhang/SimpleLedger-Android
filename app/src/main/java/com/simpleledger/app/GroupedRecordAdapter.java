package com.simpleledger.app;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 5.4 按日期分组的账单列表适配器（鲨鱼记账风格）
 *
 * 将账单按日期分组，每个日期分组前显示一个日期标题行：
 * - 左侧：07月21日 Monday
 * - 右侧：当日支出小计
 *
 * 每条账单记录内部不再显示日期（日期已在分组标题中）。
 * 不同日期分组之间有留白间距，强化分隔感。
 */
public class GroupedRecordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<Object> items = new ArrayList<>();
    private OnRecordLongClickListener longListener;
    private OnRecordClickListener clickListener;

    public interface OnRecordLongClickListener {
        void onRecordLongClick(Record record, int position);
    }

    public interface OnRecordClickListener {
        void onRecordClick(Record record, int position);
    }

    public void setOnRecordLongClickListener(OnRecordLongClickListener listener) {
        this.longListener = listener;
    }

    public void setOnRecordClickListener(OnRecordClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * 日期分组标题数据
     */
    private static class DateHeader {
        final String date;       // 原始日期 yyyy-MM-dd
        final String label;      // 显示文本 "07月21日 Monday"
        final double dayExpense; // 当日支出小计

        DateHeader(String date, String label, double dayExpense) {
            this.date = date;
            this.label = label;
            this.dayExpense = dayExpense;
        }
    }

    /**
     * 用扁平的 Record 列表更新数据，内部自动按日期分组
     */
    public void updateData(List<Record> records) {
        items.clear();

        // 按日期分组（保持插入顺序，即按记录的时间戳倒序——因 DatabaseHelper 已按 timestamp DESC 排序）
        LinkedHashMap<String, List<Record>> grouped = new LinkedHashMap<>();
        for (Record r : records) {
            String date = r.getDate();
            if (date == null || date.isEmpty()) date = "未知日期";
            if (!grouped.containsKey(date)) {
                grouped.put(date, new ArrayList<>());
            }
            grouped.get(date).add(r);
        }

        // 构建带 header 的 items 列表
        SimpleDateFormat labelFormat = new SimpleDateFormat("MM月dd日 EEEE", Locale.ENGLISH);
        for (Map.Entry<String, List<Record>> entry : grouped.entrySet()) {
            String dateStr = entry.getKey();
            List<Record> dayRecords = entry.getValue();

            // 计算当日支出小计
            double dayExpense = 0;
            for (Record r : dayRecords) {
                if (r.getType() == Record.TYPE_EXPENSE) dayExpense += r.getAmount();
            }

            // 生成显示标签
            String label;
            try {
                SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.setTime(parseFormat.parse(dateStr));
                label = labelFormat.format(cal.getTime());
            } catch (Exception e) {
                label = dateStr;
            }

            items.add(new DateHeader(dateStr, label, dayExpense));
            items.addAll(dayRecords);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof DateHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_record, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            DateHeader header = (DateHeader) items.get(position);
            ((HeaderViewHolder) holder).tvDateLabel.setText(header.label);
            ((HeaderViewHolder) holder).tvDayTotal.setText(
                    String.format(Locale.getDefault(), "-¥%.2f", header.dayExpense));
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder vh = (ItemViewHolder) holder;
            Record record = (Record) items.get(position);

            vh.tvCategory.setText(record.getCategoryName());
            vh.tvImportant.setVisibility(record.isImportant() ? View.VISIBLE : View.GONE);

            // 5.7 合并显示备注 + 项目 + 标签为一行浅灰色小字，用分号分隔
            StringBuilder descSb = new StringBuilder();
            if (record.getRemark() != null && !record.getRemark().isEmpty()) {
                descSb.append(record.getRemark());
            }
            if (record.getProjectName() != null && !record.getProjectName().isEmpty()) {
                if (descSb.length() > 0) descSb.append("；");
                descSb.append("项目: ").append(record.getProjectName());
            }
            if (record.getTags() != null && !record.getTags().isEmpty()) {
                if (descSb.length() > 0) descSb.append("；");
                descSb.append("标签: ").append(record.getTags());
            }
            if (descSb.length() > 0) {
                vh.tvRemark.setText(descSb.toString());
                vh.tvRemark.setVisibility(View.VISIBLE);
            } else {
                vh.tvRemark.setVisibility(View.GONE);
            }

            // 5.7 项目已合并到备注行，单独的项目 tag 隐藏
            vh.tvProjectTag.setVisibility(View.GONE);

            // 5.4 分组模式下隐藏日期（日期已在分组标题中显示）
            vh.tvDate.setVisibility(View.GONE);

            vh.tvCategoryIcon.setText(record.getCategoryIcon() != null ? record.getCategoryIcon() : "📦");
            GradientDrawable bg = (GradientDrawable) vh.tvCategoryIcon.getBackground();
            if (bg != null) {
                bg.setColor(record.getCategoryColor());
            }

            String amountStr;
            if (record.getType() == Record.TYPE_EXPENSE) {
                amountStr = String.format(Locale.getDefault(), "-¥%.2f", record.getAmount());
                vh.tvAmount.setTextColor(vh.itemView.getContext().getColor(R.color.expense_color));
            } else {
                amountStr = String.format(Locale.getDefault(), "+¥%.2f", record.getAmount());
                vh.tvAmount.setTextColor(vh.itemView.getContext().getColor(R.color.income_color));
            }
            vh.tvAmount.setText(amountStr);

            // 6.6 修复：用 holder.getAdapterPosition() 获取实时 position，
            // 避免 ViewHolder 复用后 position 过期导致"选 A 删 B"
            vh.itemView.setOnLongClickListener(v -> {
                if (longListener != null) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        longListener.onRecordLongClick(record, pos);
                    }
                }
                return true;
            });

            vh.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        clickListener.onRecordClick(record, pos);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateLabel, tvDayTotal;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateLabel = itemView.findViewById(R.id.tvDateLabel);
            tvDayTotal = itemView.findViewById(R.id.tvDayTotal);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryIcon, tvCategory, tvProjectTag, tvDate, tvRemark, tvAmount, tvImportant;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvProjectTag = itemView.findViewById(R.id.tvProjectTag);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvRemark = itemView.findViewById(R.id.tvRemark);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvImportant = itemView.findViewById(R.id.tvImportant);
        }
    }
}
