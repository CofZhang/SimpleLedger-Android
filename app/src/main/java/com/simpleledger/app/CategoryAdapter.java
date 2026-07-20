package com.simpleledger.app;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private List<Category> categories;
    private long selectedCategoryId = -1;
    private OnCategoryClickListener listener;
    private boolean showDelete = false;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
        void onCategoryLongClick(Category category);
    }

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public void setShowDelete(boolean showDelete) {
        this.showDelete = showDelete;
    }

    public void setSelectedCategoryId(long id) {
        this.selectedCategoryId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.tvName.setText(category.getName());
        holder.tvIcon.setText(category.getIcon() != null ? category.getIcon() : "📦");

        boolean isSelected = selectedCategoryId == category.getId();

        // 4.2 版本：选中态用外圈棕色圆环表示，图标本身颜色和图案完全保持不变
        if (isSelected) {
            holder.viewSelected.setVisibility(View.VISIBLE);
            holder.tvName.setTextColor(holder.itemView.getContext().getColor(R.color.colorPrimary));
            holder.tvName.setTypeface(holder.tvName.getTypeface(), android.graphics.Typeface.BOLD);
        } else {
            holder.viewSelected.setVisibility(View.GONE);
            holder.tvName.setTextColor(holder.itemView.getContext().getColor(R.color.text_primary));
            holder.tvName.setTypeface(holder.tvName.getTypeface(), android.graphics.Typeface.NORMAL);
        }

        // 图标背景圆形保持分类本身的低饱和颜色（选中不改变）
        GradientDrawable bg = (GradientDrawable) holder.tvIcon.getBackground();
        if (bg != null) {
            bg.setColor(category.getColor());
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });

        if (showDelete) {
            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryLongClick(category);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateData(List<Category> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName;
        View viewSelected;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvName = itemView.findViewById(R.id.tvName);
            viewSelected = itemView.findViewById(R.id.viewSelected);
        }
    }
}
