package com.simpleledger.app;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 6.0 语音记账：使用 SpeechRecognizer API 直接调用手机自带麦克风识别语音。
 *
 * 6.4 重大修复：用户反馈 vivo OriginOS 上 Intent 方式弹窗选择应用体验差，
 * 改回 SpeechRecognizer API 直接调用（不弹窗），并：
 *  - 显式尝试厂商自带 RecognitionService（vivo/华为/小米/OPPO/三星等）
 *  - ERROR_CLIENT 时自动重建实例并重试一次
 *  - APP 内显示录音状态动画（ProgressBar 旋转）
 *  - 失败时给出清晰提示和解决建议
 */
public class VoiceRecordActivity extends AppCompatActivity {
    private LinearLayout topNav;
    private Button btnSpeak, btnUse, btnExpense, btnIncome;
    private TextView tvResult, tvParsedAmount, tvParsedRemark, tvHint;
    private ProgressBar progressRecord;
    private double parsedAmount = 0;
    private String parsedRemark = "";
    private int selectedType = Record.TYPE_EXPENSE;

    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private int retryCount = 0;
    private static final int MAX_RETRY = 1;

    // 6.1 运行时请求 RECORD_AUDIO 权限
    private final ActivityResultLauncher<String> audioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startListening();
                } else {
                    Toast.makeText(this, "需要麦克风权限才能进行语音识别", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_voice);

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

        btnSpeak = findViewById(R.id.btnSpeak);
        btnUse = findViewById(R.id.btnUse);
        btnExpense = findViewById(R.id.btnExpense);
        btnIncome = findViewById(R.id.btnIncome);
        tvResult = findViewById(R.id.tvResult);
        tvParsedAmount = findViewById(R.id.tvParsedAmount);
        tvParsedRemark = findViewById(R.id.tvParsedRemark);
        tvHint = findViewById(R.id.tvHint);
        progressRecord = findViewById(R.id.progressRecord);
        progressRecord.setVisibility(View.GONE);

        initSpeechRecognizer();

        btnSpeak.setOnClickListener(v -> {
            HapticHelper.medium(this);
            if (speechRecognizer == null) {
                initSpeechRecognizer();
            }
            if (speechRecognizer == null) {
                Toast.makeText(this, "无法初始化语音识别", Toast.LENGTH_SHORT).show();
                return;
            }
            // 6.1 先检查 RECORD_AUDIO 权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        });

        // 类型切换
        btnExpense.setOnClickListener(v -> {
            HapticHelper.light(this);
            selectedType = Record.TYPE_EXPENSE;
            btnExpense.setBackgroundColor(0xFFD6C7B2);
            btnExpense.setTextColor(0xFFFFFFFF);
            btnIncome.setBackgroundColor(0xFFF8F3ED);
            btnIncome.setTextColor(0xFF3D352C);
        });
        btnIncome.setOnClickListener(v -> {
            HapticHelper.light(this);
            selectedType = Record.TYPE_INCOME;
            btnIncome.setBackgroundColor(0xFFD6C7B2);
            btnIncome.setTextColor(0xFFFFFFFF);
            btnExpense.setBackgroundColor(0xFFF8F3ED);
            btnExpense.setTextColor(0xFF3D352C);
        });
        btnExpense.performClick();

        btnUse.setOnClickListener(v -> {
            HapticHelper.medium(this);
            if (parsedAmount <= 0) {
                Toast.makeText(this, "请先识别到金额", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TARGET,
                    selectedType == Record.TYPE_INCOME ? MainActivity.TARGET_ADD_INCOME : MainActivity.TARGET_ADD_EXPENSE);
            intent.putExtra(MainActivity.EXTRA_AMOUNT, parsedAmount);
            intent.putExtra(MainActivity.EXTRA_REMARK, parsedRemark);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    /**
     * 6.4 初始化 SpeechRecognizer，显式尝试厂商自带 RecognitionService
     */
    private void initSpeechRecognizer() {
        try {
            // 尝试厂商自带的 RecognitionService
            ComponentName service = findBestRecognitionService();
            if (service != null) {
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this, service);
                } catch (Exception e) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                }
            } else {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            }
            speechRecognizer.setRecognitionListener(new SpeechListener());
        } catch (Exception e) {
            speechRecognizer = null;
        }
    }

    /**
     * 6.4 查找系统中最佳的 RecognitionService
     * 优先级：厂商自带 > Google > 默认
     */
    private ComponentName findBestRecognitionService() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            PackageManager pm = getPackageManager();
            List<ResolveInfo> services = pm.queryIntentServices(intent, 0);
            if (services == null || services.isEmpty()) return null;

            // 厂商优先级列表
            String[] vendorPrefixes = {
                    "com.vivo", "com.huawei", "com.miui", "com.xiaomi",
                    "com.oppo", "com.coloros", "com.meizu", "com.samsung",
                    "com.google.android.googlequicksearchbox"
            };

            // 先找厂商服务
            for (String prefix : vendorPrefixes) {
                for (ResolveInfo ri : services) {
                    if (ri.serviceInfo != null && ri.serviceInfo.packageName.startsWith(prefix)) {
                        return new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name);
                    }
                }
            }
            // 找到任何可用的服务
            for (ResolveInfo ri : services) {
                if (ri.serviceInfo != null) {
                    return new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 6.4 启动语音识别（已取得 RECORD_AUDIO 权限后调用） */
    private void startListening() {
        if (speechRecognizer == null) {
            initSpeechRecognizer();
            if (speechRecognizer == null) {
                tvHint.setText("无法启动语音识别，请检查系统语音服务");
                return;
            }
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        try {
            isListening = true;
            btnSpeak.setEnabled(false);
            btnSpeak.setText("🎤 正在录音...");
            progressRecord.setVisibility(View.VISIBLE);
            tvHint.setText("请说话，比如：花了 25 元吃午餐");
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            isListening = false;
            btnSpeak.setEnabled(true);
            btnSpeak.setText("🎤 点击说话");
            progressRecord.setVisibility(View.GONE);
            tvHint.setText("启动失败：" + e.getMessage());
        }
    }

    /** 6.4 ERROR_CLIENT 时重建实例并重试 */
    private void retryOnClientError() {
        if (retryCount >= MAX_RETRY) {
            tvHint.setText("语音识别不可用，请尝试：\n1. 检查系统语音服务是否开启\n2. 说出更清晰的语句\n3. 或手动输入金额");
            return;
        }
        retryCount++;
        try {
            if (speechRecognizer != null) {
                try { speechRecognizer.stopListening(); } catch (Exception ignored) {}
                try { speechRecognizer.cancel(); } catch (Exception ignored) {}
                try { speechRecognizer.destroy(); } catch (Exception ignored) {}
                speechRecognizer = null;
            }
            initSpeechRecognizer();
            tvHint.setText("正在重试...");
            startListening();
        } catch (Exception e) {
            tvHint.setText("重试失败：" + e.getMessage());
        }
    }

    private class SpeechListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            tvHint.setText("请说话...");
        }
        @Override
        public void onBeginningOfSpeech() {
            tvHint.setText("正在听...");
        }
        @Override
        public void onRmsChanged(float rmsdB) {}
        @Override
        public void onBufferReceived(byte[] buffer) {}
        @Override
        public void onEndOfSpeech() {
            tvHint.setText("识别中...");
        }
        @Override
        public void onError(int error) {
            isListening = false;
            btnSpeak.setEnabled(true);
            btnSpeak.setText("🎤 点击说话");
            progressRecord.setVisibility(View.GONE);
            String msg;
            switch (error) {
                case SpeechRecognizer.ERROR_NO_MATCH: msg = "未识别到内容，请重试"; break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: msg = "超时未说话，请重试"; break;
                case SpeechRecognizer.ERROR_AUDIO: msg = "音频错误，请检查麦克风"; break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: msg = "识别器忙，请稍后"; break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: msg = "需要麦克风权限"; break;
                case SpeechRecognizer.ERROR_NETWORK: msg = "网络错误，语音识别需要联网"; break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: msg = "网络超时，请重试"; break;
                case SpeechRecognizer.ERROR_SERVER: msg = "服务器错误，请稍后重试"; break;
                case SpeechRecognizer.ERROR_CLIENT:
                    // 6.4 ERROR_CLIENT 自动重试
                    retryOnClientError();
                    return;
                case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED: msg = "语言不支持"; break;
                case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE: msg = "语言数据不可用"; break;
                default: msg = "识别错误（" + error + "），请重试"; break;
            }
            tvHint.setText(msg);
        }
        @Override
        public void onResults(Bundle results) {
            isListening = false;
            btnSpeak.setEnabled(true);
            btnSpeak.setText("🎤 点击说话");
            progressRecord.setVisibility(View.GONE);
            retryCount = 0;
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String text = matches.get(0);
                tvResult.setText(text);
                parseText(text);
            } else {
                tvHint.setText("未识别到内容");
            }
        }
        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (partial != null && !partial.isEmpty()) {
                tvResult.setText(partial.get(0));
            }
        }
        @Override
        public void onEvent(int eventType, Bundle params) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            try {
                if (isListening) speechRecognizer.stopListening();
            } catch (Exception ignored) {}
            try { speechRecognizer.cancel(); } catch (Exception ignored) {}
            try { speechRecognizer.destroy(); } catch (Exception ignored) {}
            speechRecognizer = null;
        }
    }

    /** 解析语音识别文本，提取金额和备注 */
    private void parseText(String text) {
        double amount = 0;
        Pattern p = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块钱?|圆|RMB|￥|¥)?");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                amount = Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        if (amount == 0) {
            amount = parseChineseNumber(text);
        }
        String remark = text;
        if (m.find(0)) {
            remark = text.replace(m.group(0), "").trim();
        }
        remark = remark.replaceAll("(?:我|今天|昨天)?(?:花了|花|买了|买|付了|付|收入|收到|工资|报销)", "").trim();
        if (remark.isEmpty()) {
            remark = text;
        }
        if (remark.length() > 30) {
            remark = remark.substring(0, 30);
        }
        parsedAmount = amount;
        parsedRemark = remark;
        tvParsedAmount.setText(String.format(Locale.getDefault(), "¥%.2f", amount));
        tvParsedRemark.setText(remark);
        if (amount > 0) {
            tvHint.setText("已识别金额 " + String.format("%.2f", amount) + "，可点击\"去记账\"");
        } else {
            tvHint.setText("未识别到金额，请尝试说\"花了 25 元吃午餐\"");
        }
    }

    /** 简单中文数字解析（0-9999） */
    private double parseChineseNumber(String text) {
        Pattern p = Pattern.compile("([零一二三四五六七八九十百千万两]+)\\s*(?:元|块|圆)?");
        Matcher m = p.matcher(text);
        if (!m.find()) return 0;
        String s = m.group(1);
        double result = 0;
        double temp = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            double v = 0;
            if (c == '零') v = 0;
            else if (c == '一') v = 1;
            else if (c == '二' || c == '两') v = 2;
            else if (c == '三') v = 3;
            else if (c == '四') v = 4;
            else if (c == '五') v = 5;
            else if (c == '六') v = 6;
            else if (c == '七') v = 7;
            else if (c == '八') v = 8;
            else if (c == '九') v = 9;
            else if (c == '十') { temp = (temp == 0 ? 1 : temp) * 10; result += temp; temp = 0; continue; }
            else if (c == '百') { temp = (temp == 0 ? 1 : temp) * 100; result += temp; temp = 0; continue; }
            else if (c == '千') { temp = (temp == 0 ? 1 : temp) * 1000; result += temp; temp = 0; continue; }
            else if (c == '万') { result = (result + temp) * 10000; temp = 0; continue; }
            temp = temp * 10 + v;
        }
        result += temp;
        return result > 0 ? result : 0;
    }
}
