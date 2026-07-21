package com.simpleledger.app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProjectDetailActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private long projectId;
    private String projectName;
    private GroupedRecordAdapter adapter;
    private List<Record> records;
    private TextView tvProjectTotal, tvProjectExpense, tvProjectIncome, tvRecordCount, tvEmpty;
    private RecyclerView rvRecords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        dbHelper = new DatabaseHelper(this);
        records = new ArrayList<>();

        projectId = getIntent().getLongExtra("project_id", -1);
        projectName = getIntent().getStringExtra("project_name");

        initViews();
        loadData();
    }

    private void initViews() {
        TextView tvProjectName = findViewById(R.id.tvProjectName);
        tvProjectTotal = findViewById(R.id.tvProjectTotal);
        tvProjectExpense = findViewById(R.id.tvProjectExpense);
        tvProjectIncome = findViewById(R.id.tvProjectIncome);
        tvRecordCount = findViewById(R.id.tvRecordCount);
        rvRecords = findViewById(R.id.rvRecords);
        tvEmpty = findViewById(R.id.tvEmpty);

        tvProjectName.setText(projectName);

        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GroupedRecordAdapter();
        rvRecords.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadData() {
        double totalExpense = dbHelper.getProjectExpense(projectId);
        double totalIncome = dbHelper.getProjectIncome(projectId);
        double balance = totalIncome - totalExpense;
        List<Record> projectRecords = dbHelper.getRecordsByProject(projectId);
        records.clear();
        records.addAll(projectRecords);
        adapter.updateData(records);

        tvProjectTotal.setText(String.format("¥%.2f", balance));
        tvProjectExpense.setText(String.format("¥%.2f", totalExpense));
        tvProjectIncome.setText(String.format("¥%.2f", totalIncome));
        tvRecordCount.setText(String.format("共 %d 笔记录", projectRecords.size()));

        if (projectRecords.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvRecords.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvRecords.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
}
