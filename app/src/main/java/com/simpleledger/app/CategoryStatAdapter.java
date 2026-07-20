package com.simpleledger.app;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryStatAdapter extends RecyclerView.Adapter<CategoryStatAdapter.ViewHolder> {
    private List<DatabaseHelper.CategoryStat> stats;
    private double totalExpense;

    public CategoryStatAdapter(List<DatabaseHelper.CategoryStat> stats) {
        this.stats = stats;
        calculateTotal();
    }

    private void calculateTotal() {
        totalExpense = 0;
        for (DatabaseHelper.CategoryStat stat : stats) {
            totalExpense += stat.getTotal();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_stat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.CategoryStat stat = stats.get(position);
        holder.tvCategoryName.setText(stat.getCategoryName());
        holder.tvCategoryIcon.setText(stat.getCategoryIcon() != null ? stat.getCategoryIcon() : "📦");
        
        GradientDrawable bg = (GradientDrawable) holder.tvCategoryIcon.getBackground();
        if (bg != null) {
            bg.setColor(stat.getCategoryColor());
        }

        holder.tvTotal.setText(String.format("¥%.2f", stat.getTotal()));
        
        double percent = totalExpense > 0 ? (stat.getTotal() / totalExpense * 100) : 0;
        holder.tvPercentage.setText(String.format("%.1f%%", percent));
    }

    @Override
    public int getItemCount() {
        return stats.size();
    }

    public void updateData(List<DatabaseHelper.CategoryStat> newStats) {
        this.stats = newStats;
        calculateTotal();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryIcon, tvCategoryName, tvTotal, tvPercentage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvPercentage = itemView.findViewById(R.id.tvPercentage);
        }
    }
}
