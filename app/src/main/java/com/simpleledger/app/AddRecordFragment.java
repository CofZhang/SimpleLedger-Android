package com.simpleledger.app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddRecordFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_record, container, false);

        dbHelper = new DatabaseHelper(getContext());
        categories = new ArrayList<>();
        projects = new ArrayList<>();
        selectedDate = Calendar.getInstance();

        etAmount = view.findViewById(R.id.etAmount);
        etRemark = view.findViewById(R.id.etRemark);
        tvDate = view.findViewById(R.id.tvDate);
        tvProject = view.findViewById(R.id.tvProject);
        tabType = view.findViewById(R.id.tabType);
        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        ImageButton btnSelectProject = view.findViewById(R.id.btnSelectProject);

        rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 4));
        categoryAdapter = new CategoryAdapter(categories, this);
        rvCategories.setAdapter(categoryAdapter);

        tabType.addTab(tabType.newTab().setText(R.string.expense));
        tabType.addTab(tabType.newTab().setText(R.string.income));
        tabType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentType = tab.getPosition();
                selectedProjectId = 0;
                selectedProjectName = null;
                tvProject.setText(R.string.no_project);
                loadCategories();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        updateDateDisplay();
        tvDate.setOnClickListener(v -> showDatePicker());

        view.findViewById(R.id.btnSave).setOnClickListener(v -> saveRecord());

        tvProject.setOnClickListener(v -> showProjectPicker());
        btnSelectProject.setOnClickListener(v -> showProjectPicker());

        loadCategories();
        return view;
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

        new AlertDialog.Builder(getContext())
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
        new DatePickerDialog(getContext(), (v, year, month, dayOfMonth) -> {
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
            Toast.makeText(getContext(), R.string.input_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory == null) {
            Toast.makeText(getContext(), R.string.select_category, Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "金额格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(getContext(), "金额必须大于0", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getContext(), "保存成功", Toast.LENGTH_SHORT).show();

        etAmount.setText("");
        etRemark.setText("");
        selectedProjectId = 0;
        selectedProjectName = null;
        tvProject.setText(R.string.no_project);
        loadCategories();
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
