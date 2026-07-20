package com.simpleledger.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {
    private List<Project> projects;
    private DatabaseHelper dbHelper;
    private OnProjectClickListener listener;

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
        void onProjectLongClick(Project project);
    }

    public ProjectAdapter(List<Project> projects, DatabaseHelper dbHelper) {
        this.projects = projects;
        this.dbHelper = dbHelper;
    }

    public void setOnProjectClickListener(OnProjectClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Project project = projects.get(position);
        holder.tvName.setText(project.getName());
        
        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
            holder.tvDesc.setText(project.getDescription());
            holder.tvDesc.setVisibility(View.VISIBLE);
        } else {
            holder.tvDesc.setVisibility(View.GONE);
        }

        double totalExpense = dbHelper.getProjectExpense(project.getId());
        List<Record> records = dbHelper.getRecordsByProject(project.getId());
        holder.tvTotal.setText(String.format("总支出: ¥%.2f", totalExpense));
        holder.tvCount.setText(String.format("%d笔记录", records.size()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProjectClick(project);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onProjectLongClick(project);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public void updateData(List<Project> newProjects) {
        this.projects = newProjects;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvTotal, tvCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProjectName);
            tvDesc = itemView.findViewById(R.id.tvProjectDesc);
            tvTotal = itemView.findViewById(R.id.tvProjectTotal);
            tvCount = itemView.findViewById(R.id.tvRecordCount);
        }
    }
}
