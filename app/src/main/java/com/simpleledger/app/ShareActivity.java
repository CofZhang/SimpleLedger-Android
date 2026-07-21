package com.simpleledger.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 6.0 账单分享页：将本月（或选定的月份）的收支情况生成精美图片，
 * 通过系统分享面板发送给好友/保存到相册。
 */
public class ShareActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private Calendar currentMonth;
    private TextView tvMonth, tvCardMonth, tvIncome, tvExpense, tvBalance, tvTopCategories;
    private LinearLayout shareCardView;
    private View btnShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_share);

        dbHelper = new DatabaseHelper(this);
        currentMonth = Calendar.getInstance();

        LinearLayout topNav = findViewById(R.id.topNav);
        ViewCompat.setOnApplyWindowInsetsListener(topNav, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            HapticHelper.light(this);
            finish();
        });

        tvMonth = findViewById(R.id.tvMonth);
        tvCardMonth = findViewById(R.id.tvCardMonth);
        shareCardView = findViewById(R.id.shareCardView);
        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        tvBalance = findViewById(R.id.tvBalance);
        tvTopCategories = findViewById(R.id.tvTopCategories);
        btnShare = findViewById(R.id.btnShare);

        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            HapticHelper.light(this);
            currentMonth.add(Calendar.MONTH, -1);
            loadData();
        });
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            HapticHelper.light(this);
            currentMonth.add(Calendar.MONTH, 1);
            loadData();
        });

        btnShare.setOnClickListener(v -> {
            HapticHelper.medium(this);
            shareAsImage();
        });

        loadData();
    }

    private void loadData() {
        String ym = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentMonth.getTime());
        String display = currentMonth.get(Calendar.YEAR) + "年" + (currentMonth.get(Calendar.MONTH) + 1) + "月";
        tvMonth.setText(display);
        tvCardMonth.setText(display);

        double income = dbHelper.getMonthlyIncome(ym);
        double expense = dbHelper.getMonthlyExpense(ym);
        double balance = income - expense;

        tvIncome.setText(String.format(Locale.getDefault(), "¥%.2f", income));
        tvExpense.setText(String.format(Locale.getDefault(), "¥%.2f", expense));
        tvBalance.setText(String.format(Locale.getDefault(), "¥%.2f", balance));

        // Top 5 支出分类
        List<DatabaseHelper.CategoryStat> stats = dbHelper.getCategoryStats(ym + "%", Record.TYPE_EXPENSE);
        StringBuilder sb = new StringBuilder();
        int count = Math.min(5, stats.size());
        double total = 0;
        for (DatabaseHelper.CategoryStat s : stats) total += s.getTotal();
        for (int i = 0; i < count; i++) {
            DatabaseHelper.CategoryStat s = stats.get(i);
            double percent = total > 0 ? s.getTotal() / total * 100 : 0;
            sb.append(String.format(Locale.getDefault(), "%d. %s%s  ¥%.2f  (%.1f%%)\n",
                    i + 1,
                    s.getCategoryIcon() != null ? s.getCategoryIcon() + " " : "",
                    s.getCategoryName(),
                    s.getTotal(),
                    percent));
        }
        if (sb.length() == 0) {
            tvTopCategories.setText("本月暂无支出记录");
        } else {
            tvTopCategories.setText(sb.toString().trim());
        }
    }

    private void shareAsImage() {
        try {
            Bitmap bitmap = Bitmap.createBitmap(shareCardView.getWidth(),
                    shareCardView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.parseColor("#F8F3ED"));
            shareCardView.draw(canvas);

            File cacheDir = new File(getCacheDir(), "shared");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            String fileName = "bill_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(System.currentTimeMillis()) + ".png";
            File imageFile = new File(cacheDir, fileName);
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "分享账单图片"));
        } catch (Exception e) {
            Toast.makeText(this, "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
