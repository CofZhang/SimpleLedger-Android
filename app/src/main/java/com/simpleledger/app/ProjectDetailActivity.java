package com.simpleledger.app;

import android.os.Bundle;
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
    private RecordAdapter adapter;
    private List<Record> records;
    private TextView tvProjectTotal;
    private TextView tvRecordCount;

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
        TextView tvProjectDesc = findViewById(R.id.tvProjectDesc);
        tvProjectTotal = findViewById(R.id.tvProjectTotal);
        tvRecordCount = findViewById(R.id.tvRecordCount);
        RecyclerView rvRecords = findViewById(R.id.rvRecords);

        tvProjectName.setText(projectName);
        if (tvProjectDesc != null) {
            tvProjectDesc.setVisibility(android.view.View.GONE);
        }

        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordAdapter(records);
        rvRecords.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadData() {
        double totalExpense = dbHelper.getProjectExpense(projectId);
        List<Record> projectRecords = dbHelper.getRecordsByProject(projectId);
        records.clear();
        records.addAll(projectRecords);
        adapter.updateData(records);

        tvProjectTotal.setText(String.format("¥%.2f", totalExpense));
        tvRecordCount.setText(String.valueOf(projectRecords.size()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
}
