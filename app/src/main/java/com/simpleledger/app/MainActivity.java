package com.simpleledger.app;

import android.os.Bundle;
import android.view.View;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 沉浸式全屏：内容延伸到状态栏，由 insets 处理安全区域
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

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
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            bottomNav.setSelectedItemId(R.id.nav_add);
        });

        // 4.2 版本：正确处理系统内边距
        // 1) Fragment 容器顶部避开状态栏（解决三点菜单被裁切）
        // 2) 底部导航栏容器避开系统底部 Home Indicator 区域
        View container = findViewById(R.id.container);
        bottomNavContainer = (View) bottomNav.getParent();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            // 容器顶部留出状态栏高度
            container.setPadding(0, statusBarHeight, 0, 0);

            // 底部导航栏容器底部留出系统导航栏高度
            bottomNavContainer.setPadding(0, 0, 0, navBarHeight);
            return insets;
        });
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
