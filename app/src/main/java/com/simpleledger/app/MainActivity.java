package com.simpleledger.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private Fragment recordsFragment, addRecordFragment, statsFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
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
}
