package com.simpleledger.app;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * 4.5 设置页面：集合记账提醒、触觉反馈、年度热力图、多维度透视表、导出CSV、关于。
 */
public class SettingsActivity extends AppCompatActivity {
    private SwitchCompat swReminder, swHaptic;
    private TextView tvReminderTime;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, statusBarHeight, 0, 0);
            return insets;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            HapticHelper.light(this);
            finish();
        });

        swReminder = findViewById(R.id.swReminder);
        swHaptic = findViewById(R.id.swHaptic);
        tvReminderTime = findViewById(R.id.tvReminderTime);

        // 初始化提醒开关
        boolean reminderEnabled = ReminderReceiver.isEnabled(this);
        int hour = ReminderReceiver.getHour(this);
        int minute = ReminderReceiver.getMinute(this);
        swReminder.setChecked(reminderEnabled);
        tvReminderTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        tvReminderTime.setEnabled(reminderEnabled);
        findViewById(R.id.itemReminderTime).setAlpha(reminderEnabled ? 1.0f : 0.5f);

        swReminder.setOnCheckedChangeListener((button, checked) -> {
            HapticHelper.light(this);
            ReminderReceiver.setReminder(this, checked, hour, minute);
            tvReminderTime.setEnabled(checked);
            findViewById(R.id.itemReminderTime).setAlpha(checked ? 1.0f : 0.5f);
            Toast.makeText(this, checked ? "已开启每日提醒" : "已关闭提醒", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.itemReminderTime).setOnClickListener(v -> {
            if (!swReminder.isChecked()) {
                Toast.makeText(this, "请先开启每日提醒", Toast.LENGTH_SHORT).show();
                return;
            }
            HapticHelper.light(this);
            new TimePickerDialog(this, (view, h, m) -> {
                ReminderReceiver.setReminder(this, true, h, m);
                tvReminderTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
            }, hour, minute, true).show();
        });

        // 触觉反馈开关
        swHaptic.setChecked(HapticHelper.isEnabled(this));
        swHaptic.setOnCheckedChangeListener((button, checked) -> {
            HapticHelper.setEnabled(this, checked);
            if (checked) HapticHelper.light(this);
            Toast.makeText(this, checked ? "已开启触觉反馈" : "已关闭触觉反馈", Toast.LENGTH_SHORT).show();
        });

        // 跳转年度热力图
        findViewById(R.id.itemHeatmap).setOnClickListener(v -> {
            HapticHelper.light(this);
            startActivity(new Intent(this, HeatmapActivity.class));
        });

        // 跳转透视表
        findViewById(R.id.itemPivot).setOnClickListener(v -> {
            HapticHelper.light(this);
            startActivity(new Intent(this, PivotActivity.class));
        });

        // 导出CSV
        findViewById(R.id.itemExport).setOnClickListener(v -> {
            HapticHelper.light(this);
            exportCsv();
        });
    }

    private void exportCsv() {
        try {
            File dir = new File(android.os.Environment.getExternalStorageDirectory(), "CoffeeLedger");
            if (!dir.exists()) dir.mkdirs();
            String fileName = "记账导出_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis()) + ".csv";
            File file = new File(dir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write("日期,类型,分类,金额,账户,项目,备注,标签,重要\n");
            for (Record r : dbHelper.getAllRecords()) {
                String type = r.getType() == Record.TYPE_EXPENSE ? "支出" : "收入";
                String line = String.format("%s,%s,%s,%.2f,%s,%s,%s,%s,%s\n",
                        r.getDate() != null ? r.getDate() : "",
                        type,
                        r.getCategoryName() != null ? r.getCategoryName() : "",
                        r.getAmount(),
                        r.getAccountName() != null ? r.getAccountName() : "",
                        r.getProjectName() != null ? r.getProjectName() : "",
                        r.getRemark() != null ? r.getRemark().replace(",", " ") : "",
                        r.getTags() != null ? r.getTags().replace(",", " ") : "",
                        r.isImportant() ? "是" : "否");
                writer.write(line);
            }
            writer.close();
            new AlertDialog.Builder(this)
                    .setTitle("导出成功")
                    .setMessage("文件已保存至：\n" + file.getAbsolutePath())
                    .setPositiveButton(R.string.confirm, null)
                    .show();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("导出失败")
                    .setMessage(e.getMessage())
                    .setPositiveButton(R.string.confirm, null)
                    .show();
        }
    }
}
