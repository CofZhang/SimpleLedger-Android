package com.simpleledger.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
 * 6.0 语音记账：使用系统 SpeechRecognizer 识别语音，
 * 解析"金额 + 分类关键词"，预填到记账页。
 * 全部依赖 Android 系统自带的语音识别（多数手机支持中文），
 * 无需任何服务器或第三方服务。
 *
 * 支持的语音示例：
 *  - "花了 25 元吃午餐"
 *  - "午餐 25 块"
 *  - "买水果 30 元"
 *  - "工资 8000 元"（自动识别为收入，需点击"记收入"切换）
 */
public class VoiceRecordActivity extends AppCompatActivity {
    private LinearLayout topNav;
    private Button btnSpeak, btnUse, btnExpense, btnIncome;
    private TextView tvResult, tvParsedAmount, tvParsedRemark, tvHint;
    private SpeechRecognizer speechRecognizer;
    private double parsedAmount = 0;
    private String parsedRemark = "";
    private int selectedType = Record.TYPE_EXPENSE;

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

        // 6.2 修复：不再使用 isRecognitionAvailable 判断（国产手机无 Google 服务会误报不支持）
        // 直接创建 SpeechRecognizer，由 onError 回调处理实际错误
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
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
                    String msg;
                    switch (error) {
                        case SpeechRecognizer.ERROR_NO_MATCH: msg = "未识别到内容，请重试"; break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: msg = "超时未说话，请重试"; break;
                        case SpeechRecognizer.ERROR_AUDIO: msg = "音频错误"; break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: msg = "识别器忙，请稍后"; break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: msg = "需要麦克风权限"; break;
                        case SpeechRecognizer.ERROR_NETWORK: msg = "网络错误，语音识别需要联网"; break;
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: msg = "网络超时，请重试"; break;
                        case SpeechRecognizer.ERROR_SERVER: msg = "服务器错误，请稍后重试"; break;
                        case SpeechRecognizer.ERROR_CLIENT: msg = "客户端错误，请重试"; break;
                        case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED: msg = "语言不支持"; break;
                        default: msg = "识别错误（" + error + "），请重试"; break;
                    }
                    tvHint.setText(msg);
                    btnSpeak.setEnabled(true);
                    btnSpeak.setText("🎤 点击说话");
                }
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        tvResult.setText(text);
                        parseText(text);
                    } else {
                        tvHint.setText("未识别到内容");
                    }
                    btnSpeak.setEnabled(true);
                    btnSpeak.setText("🎤 点击说话");
                }
                @Override
                public void onPartialResults(Bundle partialResults) {}
                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } catch (Exception e) {
            tvHint.setText("无法初始化语音识别：" + e.getMessage());
            btnSpeak.setEnabled(false);
        }

        btnSpeak.setOnClickListener(v -> {
            HapticHelper.medium(this);
            if (speechRecognizer == null) {
                // 6.2 兜底：尝试重新创建
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                } catch (Exception e) {
                    Toast.makeText(this, "无法启动语音识别", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            // 6.1 先检查 RECORD_AUDIO 权限，未授权则请求
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
        // 默认选中支出
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

    /** 6.1 启动语音识别（已取得 RECORD_AUDIO 权限后调用） */
    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        try {
            btnSpeak.setEnabled(false);
            btnSpeak.setText("🎤 正在录音...");
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法启动语音识别：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnSpeak.setEnabled(true);
            btnSpeak.setText("🎤 点击说话");
        }
    }

    /** 解析语音识别文本，提取金额和备注 */
    private void parseText(String text) {
        // 金额：支持 "25.5元" "25块" "25.5" "二十五元" 等
        double amount = 0;
        Pattern p = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块钱?|圆|RMB|￥|¥)?");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                amount = Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        // 中文数字转阿拉伯（简单实现：支持 0-9999）
        if (amount == 0) {
            amount = parseChineseNumber(text);
        }

        // 备注：去除金额后的剩余文本
        String remark = text;
        if (m.find(0)) {
            remark = text.replace(m.group(0), "").trim();
        }
        // 去除"花了""花了""收入""支出"等关键词
        remark = remark.replaceAll("(?:我|今天|昨天)?(?:花了|花|买了|买|付了|付|收入|收到|工资|报销)", "").trim();
        if (remark.isEmpty()) {
            remark = text;
        }
        // 截断过长备注
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
        // 匹配 "二十五元" "三十五块" 等
        Pattern p = Pattern.compile("([零一二三四五六七八九十百千万两]+)\\s*(?:元|块|圆)?");
        Matcher m = p.matcher(text);
        if (!m.find()) return 0;
        String s = m.group(1);
        double result = 0;
        double temp = 0;
        double unit = 1;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}
