package com.simpleledger.app;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
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
        holder.tvName.setText(stat.getCategoryName());
        holder.tvIcon.setText(stat.getCategoryIcon() != null ? stat.getCategoryIcon() : "📦");
        
        GradientDrawable bg = (GradientDrawable) holder.tvIcon.getBackground();
        if (bg != null) {
            bg.setColor(stat.getCategoryColor());
        }

        holder.tvAmount.setText(String.format("¥%.2f", stat.getTotal()));
        
        double percent = totalExpense > 0 ? (stat.getTotal() / totalExpense * 100) : 0;
        holder.tvPercent.setText(String.format("%.1f%%", percent));
        holder.progressBar.setProgress((int) percent);
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
        TextView tvIcon, tvName, tvAmount, tvPercent;
        ProgressBar progressBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
