package com.simpleledger.app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddRecordActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener {
    private DatabaseHelper dbHelper;
    private int currentType = Record.TYPE_EXPENSE;
    private Category selectedCategory;
    private long selectedProjectId = 0;
    private String selectedProjectName = null;
    private Calendar selectedDate;
    private List<Category> categories;
    private List<Project> projects;
    private CategoryAdapter categoryAdapter;

    private EditText etAmount, etRemark;
    private TextView tvDate, tvProject;
    private TabLayout tabType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_record);

        dbHelper = new DatabaseHelper(this);
        categories = new ArrayList<>();
        projects = new ArrayList<>();
        selectedDate = Calendar.getInstance();

        initViews();
        loadCategories();
    }

    private void initViews() {
        etAmount = findViewById(R.id.etAmount);
        etRemark = findViewById(R.id.etRemark);
        tvDate = findViewById(R.id.tvDate);
        tvProject = findViewById(R.id.tvProject);
        tabType = findViewById(R.id.tabType);
        RecyclerView rvCategories = findViewById(R.id.rvCategories);
        ImageButton btnSelectProject = findViewById(R.id.btnSelectProject);

        rvCategories.setLayoutManager(new GridLayoutManager(this, 4));
        categoryAdapter = new CategoryAdapter(categories, this);
        rvCategories.setAdapter(categoryAdapter);

        tabType.addTab(tabType.newTab().setText(R.string.expense));
        tabType.addTab(tabType.newTab().setText(R.string.income));
        tabType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentType = tab.getPosition();
                loadCategories();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        updateDateDisplay();
        tvDate.setOnClickListener(v -> showDatePicker());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveRecord());
        
        tvProject.setOnClickListener(v -> showProjectPicker());
        btnSelectProject.setOnClickListener(v -> showProjectPicker());
    }

    private void showProjectPicker() {
        projects.clear();
        projects.addAll(dbHelper.getAllProjects());
        
        List<String> projectNames = new ArrayList<>();
        projectNames.add(getString(R.string.no_project));
        for (Project p : projects) {
            projectNames.add(p.getName());
        }
        
        String[] namesArray = projectNames.toArray(new String[0]);
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_project)
                .setItems(namesArray, (dialog, which) -> {
                    if (which == 0) {
                        selectedProjectId = 0;
                        selectedProjectName = null;
                        tvProject.setText(R.string.no_project);
                    } else {
                        Project p = projects.get(which - 1);
                        selectedProjectId = p.getId();
                        selectedProjectName = p.getName();
                        tvProject.setText(p.getName());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadCategories() {
        categories.clear();
        categories.addAll(dbHelper.getCategories(currentType));
        categoryAdapter.updateData(categories);
        if (!categories.isEmpty()) {
            selectedCategory = categories.get(0);
            categoryAdapter.setSelectedCategoryId(selectedCategory.getId());
        } else {
            selectedCategory = null;
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateDisplay();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void saveRecord() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, R.string.input_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory == null) {
            Toast.makeText(this, R.string.select_category, Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "金额格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "金额必须大于0", Toast.LENGTH_SHORT).show();
            return;
        }

        Record record = new Record();
        record.setType(currentType);
        record.setAmount(amount);
        record.setCategoryId(selectedCategory.getId());
        record.setProjectId(selectedProjectId);
        record.setRemark(etRemark.getText().toString().trim());
        record.setTimestamp(selectedDate.getTimeInMillis());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        record.setDate(sdf.format(selectedDate.getTime()));

        dbHelper.addRecord(record);
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onCategoryClick(Category category) {
        selectedCategory = category;
        categoryAdapter.setSelectedCategoryId(category.getId());
    }

    @Override
    public void onCategoryLongClick(Category category) {
    }
}
