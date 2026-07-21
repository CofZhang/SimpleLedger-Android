package com.simpleledger.app;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 4.5 设置页面：集合记账提醒、触觉反馈、年度热力图、多维度透视表、导出CSV、关于。
 *
 * 5.3 更新：
 * - 修复 CSV 导出失败问题（使用 SAF 无需权限，用户选择保存位置）
 * - 新增账单导入功能（从 CSV 文件导入，支持更换设备迁移数据）
 */
public class SettingsActivity extends AppCompatActivity {
    private SwitchCompat swReminder, swHaptic;
    private TextView tvReminderTime;
    private DatabaseHelper dbHelper;

    // 5.3 SAF 请求码
    private static final int REQUEST_EXPORT_CSV = 1001;
    private static final int REQUEST_IMPORT_CSV = 1002;

    // 5.8 通知权限请求 launcher（Android 13+）
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;
    // 5.8 触觉反馈等待权限的回调标记
    private boolean pendingHapticEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);

        // 5.8 注册通知权限请求 launcher（Android 13+）
        requestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        ReminderReceiver.setReminder(this, true, ReminderReceiver.getHour(this), ReminderReceiver.getMinute(this));
                        tvReminderTime.setEnabled(true);
                        findViewById(R.id.itemReminderTime).setAlpha(1.0f);
                        Toast.makeText(this, "已开启每日提醒", Toast.LENGTH_SHORT).show();
                    } else {
                        swReminder.setChecked(false);
                        Toast.makeText(this, "未授予通知权限，无法显示提醒", Toast.LENGTH_LONG).show();
                        // 引导用户去系统设置开启通知权限
                        new AlertDialog.Builder(this)
                                .setTitle("需要通知权限")
                                .setMessage("记账提醒需要通知权限才能在通知栏显示，是否前往系统设置开启？")
                                .setPositiveButton(R.string.confirm, (d, w) -> {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                                    startActivity(intent);
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    }
                });

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
            if (checked) {
                // 5.8 Android 13+ 需要运行时请求 POST_NOTIFICATIONS 权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        // 先暂时保持开关关闭，等待权限回调
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        return;
                    }
                }
                // 已有权限，直接开启提醒
                ReminderReceiver.setReminder(this, true, hour, minute);
                tvReminderTime.setEnabled(true);
                findViewById(R.id.itemReminderTime).setAlpha(1.0f);
                Toast.makeText(this, "已开启每日提醒", Toast.LENGTH_SHORT).show();
            } else {
                ReminderReceiver.setReminder(this, false, hour, minute);
                tvReminderTime.setEnabled(false);
                findViewById(R.id.itemReminderTime).setAlpha(0.5f);
                Toast.makeText(this, "已关闭提醒", Toast.LENGTH_SHORT).show();
            }
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

        // 5.8 修复：动态读取并显示当前应用版本号（替代布局中硬编码的 "5.3"）
        TextView tvVersion = findViewById(R.id.tvVersion);
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText(versionName != null ? versionName : "未知");
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setText("未知");
        }

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

        // 5.3 导出账单：使用 SAF 让用户选择保存位置（无需存储权限）
        findViewById(R.id.itemExport).setOnClickListener(v -> {
            HapticHelper.light(this);
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "记账导出_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis()) + ".csv");
            startActivityForResult(intent, REQUEST_EXPORT_CSV);
        });

        // 5.3 导入账单：使用 SAF 让用户选择 CSV 文件
        findViewById(R.id.itemImport).setOnClickListener(v -> {
            HapticHelper.light(this);
            new AlertDialog.Builder(this)
                    .setTitle("导入账单")
                    .setMessage("导入将向数据库追加新记录（不会删除现有数据）。\n请选择之前导出的 CSV 文件。")
                    .setPositiveButton(R.string.confirm, (d, w) -> {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("text/*");
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "text/plain"});
                        startActivityForResult(intent, REQUEST_IMPORT_CSV);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    // 5.3 SAF 回调：处理导出/导入
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT_CSV) {
            exportCsvToUri(uri);
        } else if (requestCode == REQUEST_IMPORT_CSV) {
            importCsvFromUri(uri);
        }
    }

    /**
     * 5.3 修复：使用 SAF 将 CSV 写入用户选择的 Uri
     * 解决原方案在 Android 10+ scoped storage 下 ENOENT 错误
     */
    private void exportCsvToUri(Uri uri) {
        try {
            OutputStream os = getContentResolver().openOutputStream(uri);
            if (os == null) {
                showError("无法打开文件写入流");
                return;
            }
            OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
            // BOM 头，让 Excel 正确识别 UTF-8 中文
            writer.write("\uFEFF");
            writer.write("日期,类型,分类,金额,账户,项目,备注,标签,重要\n");
            for (Record r : dbHelper.getAllRecords()) {
                String type = r.getType() == Record.TYPE_EXPENSE ? "支出" : "收入";
                String line = String.format(Locale.getDefault(), "%s,%s,%s,%.2f,%s,%s,%s,%s,%s\n",
                        safe(r.getDate()),
                        type,
                        safe(r.getCategoryName()),
                        r.getAmount(),
                        safe(r.getAccountName()),
                        safe(r.getProjectName()),
                        safe(r.getRemark()).replace(",", " "),
                        safe(r.getTags()).replace(",", " "),
                        r.isImportant() ? "是" : "否");
                writer.write(line);
            }
            writer.close();
            new AlertDialog.Builder(this)
                    .setTitle("导出成功")
                    .setMessage("账单已导出到所选位置。\n可将该 CSV 文件保存到云盘或发送到新设备，通过「导入账单」恢复。")
                    .setPositiveButton(R.string.confirm, null)
                    .show();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    /**
     * 5.3 新增：从用户选择的 CSV 文件导入账单
     * 按分类名/账户名/项目名查找对应 ID，找不到则使用默认分类
     */
    private void importCsvFromUri(Uri uri) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getContentResolver().openInputStream(uri), "UTF-8"));
            // 跳过 BOM
            reader.mark(1);
            int first = reader.read();
            if (first != 0xFEFF) reader.reset();

            String header = reader.readLine();
            if (header == null) {
                showError("文件为空");
                return;
            }
            // 校验表头
            if (!header.replace("\uFEFF", "").contains("日期")) {
                showError("文件格式不正确，缺少「日期」列");
                return;
            }

            // 预加载分类/账户/项目，按名称建立索引
            Map<String, Category> expenseCats = new HashMap<>();
            Map<String, Category> incomeCats = new HashMap<>();
            for (Category c : dbHelper.getCategories(Record.TYPE_EXPENSE)) {
                expenseCats.put(c.getName(), c);
            }
            for (Category c : dbHelper.getCategories(Record.TYPE_INCOME)) {
                incomeCats.put(c.getName(), c);
            }
            Map<String, Account> accounts = new HashMap<>();
            for (Account a : dbHelper.getAllAccounts()) {
                accounts.put(a.getName(), a);
            }
            Map<String, Project> projects = new HashMap<>();
            for (Project p : dbHelper.getAllProjects()) {
                projects.put(p.getName(), p);
            }

            int successCount = 0;
            int skipCount = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length < 9) {
                    skipCount++;
                    continue;
                }
                try {
                    Record r = new Record();
                    r.setDate(parts[0].trim());
                    r.setType("收入".equals(parts[1].trim()) ? Record.TYPE_INCOME : Record.TYPE_EXPENSE);

                    // 分类查找
                    String catName = parts[2].trim();
                    Map<String, Category> catMap = r.getType() == Record.TYPE_EXPENSE ? expenseCats : incomeCats;
                    Category cat = catMap.get(catName);
                    if (cat != null) {
                        r.setCategoryId(cat.getId());
                        r.setCategoryName(cat.getName());
                    } else {
                        // 找不到分类，使用第一个分类作为兜底
                        if (!catMap.isEmpty()) {
                            Category firstCat = catMap.values().iterator().next();
                            r.setCategoryId(firstCat.getId());
                            r.setCategoryName(firstCat.getName());
                        }
                    }

                    r.setAmount(Double.parseDouble(parts[3].trim().replace(",", "").replace("¥", "")));

                    // 账户查找
                    String accName = parts[4].trim();
                    if (!accName.isEmpty() && accounts.containsKey(accName)) {
                        Account a = accounts.get(accName);
                        r.setAccountId(a.getId());
                        r.setAccountName(a.getName());
                    }

                    // 项目查找
                    String projName = parts[5].trim();
                    if (!projName.isEmpty() && projects.containsKey(projName)) {
                        Project p = projects.get(projName);
                        r.setProjectId(p.getId());
                        r.setProjectName(p.getName());
                    }

                    r.setRemark(parts[6].trim());
                    r.setTags(parts[7].trim());
                    r.setImportant("是".equals(parts[8].trim()));
                    r.setTimestamp(System.currentTimeMillis());

                    dbHelper.addRecord(r);
                    successCount++;
                } catch (Exception e) {
                    skipCount++;
                }
            }
            reader.close();

            new AlertDialog.Builder(this)
                    .setTitle("导入完成")
                    .setMessage("成功导入 " + successCount + " 条记录。\n跳过 " + skipCount + " 条无效记录。")
                    .setPositiveButton(R.string.confirm, null)
                    .show();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    /** 简单 CSV 行解析（支持字段内逗号转义为空格，与导出逻辑一致） */
    private String[] parseCsvLine(String line) {
        return line.split(",", -1);
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private void showError(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("操作失败")
                .setMessage(msg)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }
}
