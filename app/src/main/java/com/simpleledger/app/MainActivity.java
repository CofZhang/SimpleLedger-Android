package com.simpleledger.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private Fragment recordsFragment, addRecordFragment, statsFragment;
    private Fragment activeFragment;
    private BottomNavigationView bottomNav;
    private View bottomNavContainer;
    private View fabAdd;

    // 4.6 修复：保存底部导航栏尺寸，用于正确放置凸起按钮
    private static final int TAB_BAR_HEIGHT_DP = 64;
    private static final int FAB_SIZE_DP = 56;

    // 5.2 桌面小组件：通过 Intent extra 指定启动后切换到哪个 Fragment
    // 取值："records"（账单）、"add_expense"（记支出）、"add_income"（记收入）、"stats"（统计）
    public static final String EXTRA_TARGET = "extra_target";
    public static final String TARGET_RECORDS = "records";
    public static final String TARGET_ADD_EXPENSE = "add_expense";
    public static final String TARGET_ADD_INCOME = "add_income";
    public static final String TARGET_STATS = "stats";

    // 6.0 OCR/语音记账：通过 Intent extra 预填金额和备注
    public static final String EXTRA_AMOUNT = "extra_amount";
    public static final String EXTRA_REMARK = "extra_remark";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 沉浸式全屏：内容延伸到状态栏，由 insets 处理安全区域
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        // 6.0 启动时检查并生成到期的周期账单
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            int generated = dbHelper.generateDueRecurringBills();
            if (generated > 0) {
                android.widget.Toast.makeText(this,
                        "已自动生成 " + generated + " 笔周期账单",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }

        // 6.3 启动时确保提醒闹钟被注册（防止应用更新/系统清理后闹钟丢失）
        try {
            if (ReminderReceiver.isEnabled(this)) {
                ReminderReceiver.scheduleNextDay(this);
            }
        } catch (Exception ignored) {
        }

        recordsFragment = new RecordsFragment();
        addRecordFragment = new AddRecordFragment();
        statsFragment = new StatsFragment();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, statsFragment, "stats").hide(statsFragment)
                .add(R.id.container, addRecordFragment, "add").hide(addRecordFragment)
                .add(R.id.container, recordsFragment, "records")
                .commit();
        activeFragment = recordsFragment;

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_records) {
                switchFragment(recordsFragment);
                return true;
            } else if (id == R.id.nav_add) {
                switchFragment(addRecordFragment);
                return true;
            } else if (id == R.id.nav_stats) {
                switchFragment(statsFragment);
                return true;
            }
            return false;
        });

        // 凸起中间按钮：直接切换到记账页
        fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            bottomNav.setSelectedItemId(R.id.nav_add);
        });

        // 4.6 修复：正确处理系统内边距
        // 1) Fragment 容器顶部不再设置 padding（由各 Fragment 自行处理状态栏 insets）
        // 2) 底部导航栏容器避开系统底部 Home Indicator 区域
        // 3) 凸起按钮圆心位于 Tab 栏上边缘线上
        View container = findViewById(R.id.container);
        bottomNavContainer = (View) bottomNav.getParent();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            // 容器顶部留出状态栏高度（保险措施，fragment 内部也会处理）
            container.setPadding(0, 0, 0, 0);

            // 底部导航栏容器底部留出系统导航栏高度
            bottomNavContainer.setPadding(0, 0, 0, navBarHeight);

            // 4.6 修复：凸起按钮 marginBottom = navBarHeight + tabHeight - fabRadius
            // 让按钮圆心位于 Tab 栏上边缘线上，上半圆完全突出在 Tab 栏上方
            float density = getResources().getDisplayMetrics().density;
            int tabHeightPx = (int) (TAB_BAR_HEIGHT_DP * density);
            int fabRadiusPx = (int) (FAB_SIZE_DP / 2f * density);
            int fabMarginBottom = navBarHeight + tabHeightPx - fabRadiusPx;
            FrameLayout.LayoutParams fabLp = (FrameLayout.LayoutParams) fabAdd.getLayoutParams();
            fabLp.bottomMargin = fabMarginBottom;
            fabAdd.setLayoutParams(fabLp);

            return insets;
        });

        // 5.2 桌面小组件：根据 Intent extra 切换到目标 Fragment
        handleWidgetIntent(getIntent());
    }

    // 5.2 桌面小组件：处理从桌面小组件点击传来的 Intent
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleWidgetIntent(intent);
    }

    private void handleWidgetIntent(Intent intent) {
        if (intent == null) return;
        String target = intent.getStringExtra(EXTRA_TARGET);
        if (target == null) return;

        switch (target) {
            case TARGET_RECORDS:
                bottomNav.setSelectedItemId(R.id.nav_records);
                break;
            case TARGET_ADD_EXPENSE:
                bottomNav.setSelectedItemId(R.id.nav_add);
                if (addRecordFragment instanceof AddRecordFragment) {
                    ((AddRecordFragment) addRecordFragment).setType(Record.TYPE_EXPENSE);
                }
                break;
            case TARGET_ADD_INCOME:
                bottomNav.setSelectedItemId(R.id.nav_add);
                if (addRecordFragment instanceof AddRecordFragment) {
                    ((AddRecordFragment) addRecordFragment).setType(Record.TYPE_INCOME);
                }
                break;
            case TARGET_STATS:
                bottomNav.setSelectedItemId(R.id.nav_stats);
                break;
        }

        // 6.0 OCR/语音记账：预填金额和备注
        if (addRecordFragment instanceof AddRecordFragment) {
            if (intent.hasExtra(EXTRA_AMOUNT)) {
                double amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0);
                ((AddRecordFragment) addRecordFragment).setAmount(amount);
            }
            if (intent.hasExtra(EXTRA_REMARK)) {
                String remark = intent.getStringExtra(EXTRA_REMARK);
                ((AddRecordFragment) addRecordFragment).setPrefillRemark(remark);
            }
        }

        // 清除 extra，避免旋转屏幕时重复触发
        intent.removeExtra(EXTRA_TARGET);
        intent.removeExtra(EXTRA_AMOUNT);
        intent.removeExtra(EXTRA_REMARK);
    }

    private void switchFragment(Fragment fragment) {
        if (fragment == activeFragment) return;
        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(fragment)
                .commit();
        activeFragment = fragment;
        if (fragment instanceof RecordsFragment) {
            ((RecordsFragment) fragment).refreshData();
        }
    }

    public void switchToStats() {
        bottomNav.setSelectedItemId(R.id.nav_stats);
    }

    /** 5.7 记账成功后自动跳转到明细界面 */
    public void switchToRecordsAfterSave() {
        bottomNav.setSelectedItemId(R.id.nav_records);
    }

    /**
     * 返回逻辑：非明细页先返回到明细页，再点返回才退出 APP
     */
    @Override
    public void onBackPressed() {
        if (activeFragment != recordsFragment) {
            bottomNav.setSelectedItemId(R.id.nav_records);
        } else {
            super.onBackPressed();
        }
    }
}
