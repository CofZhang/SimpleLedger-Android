package com.simpleledger.app;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {
    private List<Record> records;
    private OnRecordLongClickListener longListener;
    private OnRecordClickListener clickListener;

    public interface OnRecordLongClickListener {
        void onRecordLongClick(Record record, int position);
    }

    public interface OnRecordClickListener {
        void onRecordClick(Record record, int position);
    }

    public RecordAdapter(List<Record> records) {
        this.records = records;
    }

    public void setOnRecordLongClickListener(OnRecordLongClickListener listener) {
        this.longListener = listener;
    }

    public void setOnRecordClickListener(OnRecordClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Record record = records.get(position);
        holder.tvCategory.setText(record.getCategoryName());

        // 4.5 重要标记
        holder.tvImportant.setVisibility(record.isImportant() ? View.VISIBLE : View.GONE);

        if (record.getRemark() != null && !record.getRemark().isEmpty()) {
            holder.tvRemark.setText(record.getRemark());
            holder.tvRemark.setVisibility(View.VISIBLE);
        } else {
            holder.tvRemark.setVisibility(View.GONE);
        }

        if (record.getProjectName() != null && !record.getProjectName().isEmpty()) {
            holder.tvProjectTag.setText(record.getProjectName());
            holder.tvProjectTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvProjectTag.setVisibility(View.GONE);
        }

        if (record.getDate() != null && record.getDate().length() >= 10) {
            holder.tvDate.setText(record.getDate().substring(5));
        } else {
            holder.tvDate.setText(record.getDate());
        }

        holder.tvCategoryIcon.setText(record.getCategoryIcon() != null ? record.getCategoryIcon() : "📦");
        GradientDrawable bg = (GradientDrawable) holder.tvCategoryIcon.getBackground();
        if (bg != null) {
            bg.setColor(record.getCategoryColor());
        }

        String amountStr;
        if (record.getType() == Record.TYPE_EXPENSE) {
            amountStr = String.format("-¥%.2f", record.getAmount());
            holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(R.color.expense_color));
        } else {
            amountStr = String.format("+¥%.2f", record.getAmount());
            holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(R.color.income_color));
        }
        holder.tvAmount.setText(amountStr);

        holder.itemView.setOnLongClickListener(v -> {
            if (longListener != null) {
                longListener.onRecordLongClick(record, position);
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRecordClick(record, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void updateData(List<Record> newRecords) {
        this.records = newRecords;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryIcon, tvCategory, tvProjectTag, tvDate, tvRemark, tvAmount, tvImportant;

        ViewHolder(@NonNull View itemView) {
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
