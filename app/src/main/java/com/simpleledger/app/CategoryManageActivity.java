package com.simpleledger.app;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
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

    // 无限级分类：当前查看的父分类 id；用栈记录导航路径
    private long currentParentId = 0;
    private final List<Category> breadcrumb = new ArrayList<>();

    private static final String[] DEFAULT_ICONS = {
        "🍔", "🚗", "🛒", "🎮", "💊", "🏠", "📚", "📦",
        "💰", "🎁", "📈", "💵", "🍜", "☕", "✈️", "🚌",
        "🏥", "💡", "📱", "🎬", "🎵", "👕", "💄", "🎂"
    };

    private static final int[] COLORS = {
        0xFFE8B59A, 0xFFDBA9A0, 0xFFB5A8B8, 0xFFA9BBC4,
        0xFFA8BBA0, 0xFFE0A8A0, 0xFFD4C9A8, 0xFFA8C4C4,
        0xFFB5A8C4, 0xFFA8B5C4, 0xFFD4A8B5, 0xFFE0A8B8,
        0xFFB8A690, 0xFFC4B5A0, 0xFFA8C0D4, 0xFFB0B8C0
    };

    private LinearLayout breadcrumbContainer;
    private HorizontalScrollView breadcrumbScroll;

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

        breadcrumbContainer = findViewById(R.id.breadcrumbContainer);
        breadcrumbScroll = findViewById(R.id.breadcrumbScroll);

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
                currentParentId = 0;
                breadcrumb.clear();
                loadCategories();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (currentParentId != 0) {
                // 返回上一级
                goBackToParent();
            } else {
                finish();
            }
        });
        fabAdd.setOnClickListener(v -> showAddCategoryDialog());
    }

    /** 返回到当前父分类的父分类 */
    private void goBackToParent() {
        if (breadcrumb.isEmpty()) {
            currentParentId = 0;
        } else {
            breadcrumb.remove(breadcrumb.size() - 1);
            currentParentId = breadcrumb.isEmpty() ? 0 : breadcrumb.get(breadcrumb.size() - 1).getId();
        }
        loadCategories();
    }

    private void loadCategories() {
        categories.clear();
        categories.addAll(dbHelper.getCategoriesByParent(currentParentId, currentType));
        adapter.updateData(categories);
        updateBreadcrumb();
    }

    /** 更新面包屑导航条 */
    private void updateBreadcrumb() {
        breadcrumbContainer.removeAllViews();

        // 根节点
        TextView root = new TextView(this);
        root.setText(currentType == Record.TYPE_EXPENSE ? R.string.expense : R.string.income);
        root.setTextSize(13);
        root.setTextColor(getColor(R.color.colorPrimary));
        root.setPadding(8, 8, 8, 8);
        root.setOnClickListener(v -> {
            currentParentId = 0;
            breadcrumb.clear();
            loadCategories();
        });
        breadcrumbContainer.addView(root);

        // 各级父分类
        for (int i = 0; i < breadcrumb.size(); i++) {
            Category c = breadcrumb.get(i);

            TextView sep = new TextView(this);
            sep.setText(" > ");
            sep.setTextSize(13);
            sep.setTextColor(getColor(R.color.text_secondary));
            sep.setPadding(0, 8, 0, 8);
            breadcrumbContainer.addView(sep);

            TextView tv = new TextView(this);
            tv.setText(c.getName());
            tv.setTextSize(13);
            int idx = i;
            if (idx == breadcrumb.size() - 1) {
                tv.setTextColor(getColor(R.color.colorPrimary));
                tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
            } else {
                tv.setTextColor(getColor(R.color.text_secondary));
                tv.setOnClickListener(v -> {
                    // 跳转到对应层级
                    while (breadcrumb.size() > idx + 1) {
                        breadcrumb.remove(breadcrumb.size() - 1);
                    }
                    currentParentId = c.getId();
                    loadCategories();
                });
            }
            tv.setPadding(8, 8, 8, 8);
            breadcrumbContainer.addView(tv);
        }

        // 自动滚动到最右
        breadcrumbScroll.post(() -> breadcrumbScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT));
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_category);

        // 自定义布局：分类名称 + 父分类显示
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (getResources().getDisplayMetrics().density * 16);
        layout.setPadding(pad, pad, pad, pad);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.category_name);
        layout.addView(input);

        // 显示当前父分类
        TextView tvParent = new TextView(this);
        tvParent.setTextSize(13);
        tvParent.setPadding(0, (int) (getResources().getDisplayMetrics().density * 12), 0, 0);
        if (currentParentId == 0) {
            tvParent.setText(getString(R.string.parent_category) + ": " + getString(R.string.no_parent));
        } else {
            // 找到当前父分类名称
            String parentName = breadcrumb.isEmpty() ? "" : breadcrumb.get(breadcrumb.size() - 1).getName();
            tvParent.setText(getString(R.string.parent_category) + ": " + parentName);
        }
        tvParent.setTextColor(getColor(R.color.text_secondary));
        layout.addView(tvParent);

        builder.setView(layout);

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                Random random = new Random();
                String icon = DEFAULT_ICONS[random.nextInt(DEFAULT_ICONS.length)];
                int color = COLORS[random.nextInt(COLORS.length)];
                Category category = new Category(name, icon, color, currentType);
                category.setParentId(currentParentId);
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
        // 点击分类：进入下一级（查看子分类）
        long newParentId = category.getId();
        List<Category> children = dbHelper.getCategoriesByParent(newParentId, currentType);
        if (children.isEmpty()) {
            // 末级分类：弹出操作菜单（编辑/添加子分类/删除）
            showLeafCategoryMenu(category);
        } else {
            breadcrumb.add(category);
            currentParentId = newParentId;
            loadCategories();
        }
    }

    /** 末级分类操作菜单 */
    private void showLeafCategoryMenu(Category category) {
        new AlertDialog.Builder(this)
                .setTitle(category.getName())
                .setItems(new CharSequence[]{
                        "添加子分类",
                        "编辑分类",
                        "删除分类"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showAddSubCategoryDialog(category);
                            break;
                        case 1:
                            showEditCategoryDialog(category);
                            break;
                        case 2:
                            confirmDeleteCategory(category);
                            break;
                    }
                })
                .show();
    }

    private void showAddSubCategoryDialog(Category parent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加子分类 - " + parent.getName());

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.category_name);
        int pad = (int) (getResources().getDisplayMetrics().density * 16);
        input.setPadding(pad, pad, pad, pad);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                Random random = new Random();
                String icon = DEFAULT_ICONS[random.nextInt(DEFAULT_ICONS.length)];
                int color = COLORS[random.nextInt(COLORS.length)];
                Category category = new Category(name, icon, color, currentType);
                category.setParentId(parent.getId());
                dbHelper.addCategory(category);
                // 进入父分类层级
                breadcrumb.add(parent);
                currentParentId = parent.getId();
                loadCategories();
                Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showEditCategoryDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑分类");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(category.getName());
        int pad = (int) (getResources().getDisplayMetrics().density * 16);
        input.setPadding(pad, pad, pad, pad);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                category.setName(name);
                dbHelper.updateCategory(category);
                loadCategories();
                Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void confirmDeleteCategory(Category category) {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("确定删除分类\"" + category.getName() + "\"？\n注意：该分类及其子分类都会被删除，相关记录将无法正常显示分类。")
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deleteCategory(category.getId());
                    loadCategories();
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onCategoryLongClick(Category category) {
        confirmDeleteCategory(category);
    }

    @Override
    public void onBackPressed() {
        if (currentParentId != 0) {
            goBackToParent();
        } else {
            super.onBackPressed();
        }
    }
}
