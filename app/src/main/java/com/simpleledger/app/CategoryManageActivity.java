package com.simpleledger.app;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CategoryManageActivity extends AppCompatActivity implements CategoryAdapter.OnCategoryClickListener {
    private DatabaseHelper dbHelper;
    private int currentType = Record.TYPE_EXPENSE;
    private List<Category> categories;
    private CategoryAdapter adapter;

    private static final String[] DEFAULT_ICONS = {
        "🍔", "🚗", "🛒", "🎮", "💊", "🏠", "📚", "📦",
        "💰", "🎁", "📈", "💵", "🍜", "☕", "✈️", "🚌",
        "🏥", "💡", "📱", "🎬", "🎵", "👕", "💄", "🎂"
    };

    private static final int[] COLORS = {
        0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
        0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
        0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39,
        0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);

        dbHelper = new DatabaseHelper(this);
        categories = new ArrayList<>();

        initViews();
        loadCategories();
    }

    private void initViews() {
        TabLayout tabType = findViewById(R.id.tabType);
        RecyclerView rvCategories = findViewById(R.id.rvCategories);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddCategory);

        rvCategories.setLayoutManager(new GridLayoutManager(this, 4));
        adapter = new CategoryAdapter(categories, this);
        adapter.setShowDelete(true);
        rvCategories.setAdapter(adapter);

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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        fabAdd.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void loadCategories() {
        categories.clear();
        categories.addAll(dbHelper.getCategories(currentType));
        adapter.updateData(categories);
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_category);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.category_name);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                Random random = new Random();
                String icon = DEFAULT_ICONS[random.nextInt(DEFAULT_ICONS.length)];
                int color = COLORS[random.nextInt(COLORS.length)];
                Category category = new Category(name, icon, color, currentType);
                dbHelper.addCategory(category);
                loadCategories();
                Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onCategoryClick(Category category) {
    }

    @Override
    public void onCategoryLongClick(Category category) {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("确定删除分类\"" + category.getName() + "\"？\n注意：该分类下的记录将无法正常显示。")
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deleteCategory(category.getId());
                    loadCategories();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
