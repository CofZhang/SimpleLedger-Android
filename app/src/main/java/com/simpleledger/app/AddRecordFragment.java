package com.simpleledger.app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
    private long selectedAccountId = 1;
    private String selectedAccountName = "现金";
    private Calendar selectedDate;
    private List<Category> categories;
    private List<Project> projects;
    private List<Account> accounts;
    private CategoryAdapter categoryAdapter;
    private CalculatorKeyboard calculatorKeyboard;
    private View rootView;  // 5.1 修复：保存 onCreateView 中的 view，避免 getView() 返回 null 导致闪退

    private EditText etAmount, etRemark, etTags;
    private TextView tvDate, tvProject, tvAccount;
    private TabLayout tabType;
    private long lastCategoryHeaderClickTime = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_record, container, false);
        rootView = view;  // 5.1 修复：保存 view 引用

        dbHelper = new DatabaseHelper(getContext());
        categories = new ArrayList<>();
        projects = new ArrayList<>();
        accounts = new ArrayList<>();
        selectedDate = Calendar.getInstance();

        // 4.6 修复：处理状态栏 insets，确保顶部导航栏完整显示
        LinearLayout topNav = view.findViewById(R.id.topNav);
        ViewCompat.setOnApplyWindowInsetsListener(topNav, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        etAmount = view.findViewById(R.id.etAmount);
        etRemark = view.findViewById(R.id.etRemark);
        etTags = view.findViewById(R.id.etTags);
        tvDate = view.findViewById(R.id.tvDate);
        tvProject = view.findViewById(R.id.tvProject);
        tvAccount = view.findViewById(R.id.tvAccount);
        tabType = view.findViewById(R.id.tabType);
        calculatorKeyboard = view.findViewById(R.id.calculatorKeyboard);
        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        ImageButton btnSelectProject = view.findViewById(R.id.btnSelectProject);
        ImageButton btnSelectAccount = view.findViewById(R.id.btnSelectAccount);

        // 4.5 绑定计算器键盘到金额输入框（禁用系统输入法）
        calculatorKeyboard.bindEditText(etAmount);

        rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 4));
        categoryAdapter = new CategoryAdapter(categories, this);
        rvCategories.setAdapter(categoryAdapter);

        tabType.addTab(tabType.newTab().setText(R.string.expense));
        tabType.addTab(tabType.newTab().setText(R.string.income));
        tabType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                HapticHelper.light(getContext());
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
        tvDate.setOnClickListener(v -> {
            HapticHelper.light(getContext());
            showDatePicker();
        });

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            HapticHelper.medium(getContext());
            saveRecord();
        });

        tvProject.setOnClickListener(v -> {
            HapticHelper.light(getContext());
            showProjectPicker();
        });
        btnSelectProject.setOnClickListener(v -> {
            HapticHelper.light(getContext());
            showProjectPicker();
        });
        tvAccount.setOnClickListener(v -> {
            HapticHelper.light(getContext());
            showAccountPicker();
        });
        btnSelectAccount.setOnClickListener(v -> {
            HapticHelper.light(getContext());
            showAccountPicker();
        });

        // 4.5 分类标题双击：快速打开最近使用分类
        View categoryHeader = null;
        // 标题在 R.id.tvCategoryHeader（如果有），否则跳过
        // 由于现有布局没有显式 ID 的分类标题，我们让整个分类网格区可双击
        rvCategories.setOnTouchListener(new View.OnTouchListener() {
            private long lastTapTime = 0;
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    long now = System.currentTimeMillis();
                    if (now - lastTapTime < 350) {
                        // 双击触发
                        showRecentCategoriesDialog();
                        lastTapTime = 0;
                    } else {
                        lastTapTime = now;
                    }
                }
                return false;
            }
        });

        // 4.5 分类联想：输入备注时自动推荐分类
        etRemark.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                suggestCategory(s.toString());
            }
        });

        // 初始化默认账户
        accounts = dbHelper.getAllAccounts();
        if (!accounts.isEmpty()) {
            selectedAccountId = accounts.get(0).getId();
            selectedAccountName = accounts.get(0).getName();
            tvAccount.setText(selectedAccountName);
        }

        loadCategories();
        return view;
    }

    /** 5.2 桌面小组件：外部调用切换支出/收入类型 */
    public void setType(int type) {
        if (tabType == null) return;
        int targetTab = (type == Record.TYPE_INCOME) ? 1 : 0;
        if (tabType.getSelectedTabPosition() != targetTab) {
            tabType.getTabAt(targetTab).select();
        }
    }

    /** 4.5 双击分类选择框：快速打开最近使用分类 */
    private void showRecentCategoriesDialog() {
        List<Category> recent = dbHelper.getRecentCategories(currentType, 12);
        if (recent.isEmpty()) {
            Toast.makeText(getContext(), "暂无最近使用的分类", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> names = new ArrayList<>();
        for (Category c : recent) {
            names.add(c.getIcon() + " " + c.getName());
        }
        new AlertDialog.Builder(getContext())
                .setTitle("最近使用分类")
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    Category c = recent.get(which);
                    selectedCategory = c;
                    categoryAdapter.setSelectedCategoryId(c.getId());
                    HapticHelper.light(getContext());
                    Toast.makeText(getContext(), "已选择 " + c.getName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** 4.5 分类联想：根据备注关键词推荐分类 */
    private void suggestCategory(String remark) {
        if (remark == null || remark.trim().isEmpty()) return;
        String kw = remark.trim();
        for (Category c : categories) {
            if (c.getName().contains(kw) || kw.contains(c.getName())) {
                if (selectedCategory == null || selectedCategory.getId() != c.getId()) {
                    selectedCategory = c;
                    categoryAdapter.setSelectedCategoryId(c.getId());
                }
                return;
            }
        }
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

    private void showAccountPicker() {
        accounts.clear();
        accounts.addAll(dbHelper.getAllAccounts());

        if (accounts.isEmpty()) {
            Toast.makeText(getContext(), "请先添加账户", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> accountDisplay = new ArrayList<>();
        for (Account a : accounts) {
            accountDisplay.add(a.getIcon() + " " + a.getName() + " (余额¥" + String.format("%.2f", a.getBalance()) + ")");
        }

        String[] namesArray = accountDisplay.toArray(new String[0]);

        new AlertDialog.Builder(getContext())
                .setTitle("选择账户")
                .setItems(namesArray, (dialog, which) -> {
                    Account a = accounts.get(which);
                    selectedAccountId = a.getId();
                    selectedAccountName = a.getName();
                    tvAccount.setText(a.getIcon() + " " + a.getName());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadCategories() {
        categories.clear();
        categories.addAll(dbHelper.getCategories(currentType));
        categoryAdapter.updateData(categories);

        // 5.1 修复：使用 rootView 代替 getView()，避免在 onCreateView 期间 getView() 返回 null 导致 NPE 闪退
        // 同时手动设置 RecyclerView 高度，避免在 NestedScrollView 中
        // GridLayoutManager 的 wrap_content 测量 bug 导致只显示部分分类
        RecyclerView rvCats = rootView.findViewById(R.id.rvCategories);
        if (rvCats != null && !categories.isEmpty()) {
            int spanCount = 4;
            int rowCount = (int) Math.ceil(categories.size() / (double) spanCount);
            // 每个 item 高度约 90dp（58dp 图标 + 4dp 间距 + 16dp 文字 + 12dp padding）
            float density = getResources().getDisplayMetrics().density;
            int itemHeightPx = (int) (90 * density);
            ViewGroup.LayoutParams lp = rvCats.getLayoutParams();
            lp.height = rowCount * itemHeightPx;
            rvCats.setLayoutParams(lp);
        }

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
        // 4.5 使用 CalculatorKeyboard 获取最终金额（支持表达式）
        double amount = calculatorKeyboard.getAmount();
        if (amount <= 0) {
            Toast.makeText(getContext(), R.string.input_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory == null) {
            Toast.makeText(getContext(), R.string.select_category, Toast.LENGTH_SHORT).show();
            return;
        }

        Record record = new Record();
        record.setType(currentType);
        record.setAmount(amount);
        record.setCategoryId(selectedCategory.getId());
        record.setProjectId(selectedProjectId);
        record.setAccountId(selectedAccountId);
        record.setRemark(etRemark.getText().toString().trim());
        record.setTags(etTags.getText().toString().trim());
        record.setTimestamp(selectedDate.getTimeInMillis());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        record.setDate(sdf.format(selectedDate.getTime()));

        dbHelper.addRecord(record);
        Toast.makeText(getContext(), "保存成功 ¥" + String.format("%.2f", amount), Toast.LENGTH_SHORT).show();

        etAmount.setText("");
        etRemark.setText("");
        etTags.setText("");
        selectedProjectId = 0;
        selectedProjectName = null;
        tvProject.setText(R.string.no_project);
        loadCategories();
    }

    @Override
    public void onCategoryClick(Category category) {
        HapticHelper.light(getContext());
        selectedCategory = category;
        categoryAdapter.setSelectedCategoryId(category.getId());
    }

    @Override
    public void onCategoryLongClick(Category category) {
    }
}

