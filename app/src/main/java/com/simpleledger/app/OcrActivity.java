package com.simpleledger.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 6.0 OCR 记账：拍照或从相册选取小票图片，使用 ML Kit 中文文字识别
 * 提取所有金额数字（支持 ¥、元、￥ 符号 + 数字 + 小数），点击金额跳转记账页。
 * ML Kit bundled 模型离线运行，无需 Google Play 服务。
 */
public class OcrActivity extends AppCompatActivity {
    private LinearLayout topNav;
    private Button btnCamera, btnGallery;
    private TextView tvResult, tvHint;
    private LinearLayout layoutAmounts;
    private Bitmap currentBitmap;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                                Bundle extras = result.getData().getExtras();
                                if (extras != null && extras.get("data") != null) {
                                    currentBitmap = (Bitmap) extras.get("data");
                                    recognize(currentBitmap);
                                }
                            }
                        }
                    });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                                Uri uri = result.getData().getData();
                                if (uri != null) {
                                    try {
                                        currentBitmap = MediaStore.Images.Media.getBitmap(
                                                getContentResolver(), uri);
                                        recognize(currentBitmap);
                                    } catch (Exception e) {
                                        Toast.makeText(OcrActivity.this,
                                                "读取图片失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_ocr);

        topNav = findViewById(R.id.topNav);
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

        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);
        tvResult = findViewById(R.id.tvResult);
        tvHint = findViewById(R.id.tvHint);
        layoutAmounts = findViewById(R.id.layoutAmounts);

        btnCamera.setOnClickListener(v -> {
            HapticHelper.light(this);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });

        btnGallery.setOnClickListener(v -> {
            HapticHelper.light(this);
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });
    }

    private void recognize(Bitmap bitmap) {
        tvHint.setText("正在识别...");
        layoutAmounts.removeAllViews();
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build())
                    .process(image)
                    .addOnSuccessListener(this::processText)
                    .addOnFailureListener(e -> {
                        tvHint.setText("识别失败：" + e.getMessage());
                    });
        } catch (Exception e) {
            tvHint.setText("识别出错：" + e.getMessage());
        }
    }

    private void processText(Text text) {
        String fullText = text.getText();
        tvResult.setText(fullText);

        // 提取金额：匹配 "¥12.34" "12.34元" "￥12" "合计 123.45" 等
        List<String> amounts = new ArrayList<>();
        Pattern p = Pattern.compile("(?:¥|￥|RMB)?\\s*(\\d+\\.\\d{1,2}|\\d+)\\s*(?:元|RMB|yuan)?");
        Matcher m = p.matcher(fullText);
        java.util.Set<String> seen = new java.util.HashSet<>();
        while (m.find()) {
            String amt = m.group(1);
            try {
                double d = Double.parseDouble(amt);
                if (d > 0 && !seen.contains(amt)) {
                    seen.add(amt);
                    amounts.add(amt);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (amounts.isEmpty()) {
            tvHint.setText("未识别到金额，请尝试更清晰的图片");
            return;
        }

        tvHint.setText("识别到 " + amounts.size() + " 个金额，点击任意一个去记账：");
        DecimalFormat df = new DecimalFormat("0.00");
        for (String amt : amounts) {
            Button b = new Button(this);
            double d = Double.parseDouble(amt);
            b.setText("¥" + df.format(d));
            b.setBackgroundColor(0xFFD6C7B2);
            b.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(8, 8, 8, 8);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> {
                HapticHelper.medium(this);
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_TARGET, MainActivity.TARGET_ADD_EXPENSE);
                intent.putExtra(MainActivity.EXTRA_AMOUNT, d);
                intent.putExtra(MainActivity.EXTRA_REMARK, "OCR识别");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
            layoutAmounts.addView(b);
        }
    }
}
