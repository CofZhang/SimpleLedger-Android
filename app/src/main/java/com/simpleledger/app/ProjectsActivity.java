package com.simpleledger.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ProjectsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ProjectAdapter adapter;
    private List<Project> projects;
    private RecyclerView rvProjects;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);

        dbHelper = new DatabaseHelper(this);
        projects = new ArrayList<>();

        initViews();
        loadProjects();
    }

    private void initViews() {
        rvProjects = findViewById(R.id.rvProjects);
        tvEmpty = findViewById(R.id.tvEmpty);

        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProjectAdapter(projects, dbHelper);
        adapter.setOnProjectClickListener(new ProjectAdapter.OnProjectClickListener() {
            @Override
            public void onProjectClick(Project project) {
                Intent intent = new Intent(ProjectsActivity.this, ProjectDetailActivity.class);
                intent.putExtra("project_id", project.getId());
                intent.putExtra("project_name", project.getName());
                startActivity(intent);
            }

            @Override
            public void onProjectLongClick(Project project) {
                showProjectOptions(project);
            }
        });
        rvProjects.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        FloatingActionButton fabAdd = findViewById(R.id.fabAddProject);
        fabAdd.setOnClickListener(v -> showAddProjectDialog());
    }

    private void loadProjects() {
        projects.clear();
        projects.addAll(dbHelper.getAllProjects());
        adapter.updateData(projects);

        if (projects.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvProjects.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvProjects.setVisibility(View.VISIBLE);
        }
    }

    private void showAddProjectDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_project, null);
        EditText etName = dialogView.findViewById(R.id.etProjectName);
        EditText etDesc = dialogView.findViewById(R.id.etProjectDesc);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_project)
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    
                    if (name.isEmpty()) {
                        Toast.makeText(this, "请输入项目名称", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Project project = new Project();
                    project.setName(name);
                    project.setDescription(desc);
                    project.setCreateTime(System.currentTimeMillis());
                    dbHelper.addProject(project);
                    loadProjects();
                    Toast.makeText(this, "项目创建成功", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showProjectOptions(Project project) {
        String[] options = {"查看详情", "删除项目"};
        new AlertDialog.Builder(this)
                .setTitle(project.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(ProjectsActivity.this, ProjectDetailActivity.class);
                        intent.putExtra("project_id", project.getId());
                        intent.putExtra("project_name", project.getName());
                        startActivity(intent);
                    } else if (which == 1) {
                        confirmDeleteProject(project);
                    }
                })
                .show();
    }

    private void confirmDeleteProject(Project project) {
        new AlertDialog.Builder(this)
                .setTitle("删除项目")
                .setMessage(String.format("确定要删除项目「%s」吗？项目下的记录将保留，但不再关联该项目。", project.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deleteProject(project.getId());
                    loadProjects();
                    Toast.makeText(this, "项目已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }
}
