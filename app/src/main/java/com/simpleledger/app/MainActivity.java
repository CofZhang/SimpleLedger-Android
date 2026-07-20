package com.simpleledger.app;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 沉浸式全屏：内容延伸到状态栏
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

        // 应用系统内边距到顶部状态栏区域
        View container = findViewById(R.id.container);
        ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, statusBarHeight, 0, 0);
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
     * 返回逻辑：非明细页先返回到明细页，再次返回才退出 APP
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
