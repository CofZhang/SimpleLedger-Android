package com.simpleledger.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 4.5 内置计算器键盘：禁用系统输入法，自定义数字+运算符键盘，
 * 支持表达式计算（如 120*3+50 = 410）+ 千分位实时格式化。
 *
 * 使用方式：
 *   CalculatorKeyboard keyboard = new CalculatorKeyboard(context);
 *   keyboard.bindEditText(editText);  // 绑定目标输入框
 *   // 将 keyboard 添加到布局中
 */
public class CalculatorKeyboard extends LinearLayout {
    private EditText targetEditText;
    private TextView tvPreview;

    private final String[] keys = {
            "7", "8", "9", "÷",
            "4", "5", "6", "×",
            "1", "2", "3", "−",
            ".", "0", "⌫", "+",
            "(", ")", "C", "="
    };

    public CalculatorKeyboard(Context context) {
        super(context);
        init();
    }

    public CalculatorKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CalculatorKeyboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        int padding = (int) (getResources().getDisplayMetrics().density * 8);
        setPadding(padding, padding, padding, padding);

        // 预览区域：显示当前表达式的实时计算结果
        tvPreview = new TextView(getContext());
        tvPreview.setGravity(Gravity.END);
        tvPreview.setPadding(padding, padding / 2, padding, padding / 2);
        tvPreview.setTextColor(0xFF8B7D6B);
        tvPreview.setTextSize(13);
        tvPreview.setMinHeight((int) (getResources().getDisplayMetrics().density * 24));
        addView(tvPreview, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        // 键盘网格 5行 x 4列
        android.widget.GridLayout grid = new android.widget.GridLayout(getContext());
        grid.setColumnCount(4);
        grid.setRowCount(5);
        int spacing = (int) (getResources().getDisplayMetrics().density * 4);
        grid.setUseDefaultMargins(false);

        int keySize = (int) (getResources().getDisplayMetrics().density * 48);
        for (String key : keys) {
            TextView btn = new TextView(getContext());
            btn.setText(key);
            btn.setGravity(Gravity.CENTER);
            btn.setTextSize(18);
            btn.setTextColor(0xFF3D352C);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            bg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
            bg.setColor(0xFFFFFFFF);
            btn.setBackground(bg);

            android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = keySize;
            lp.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1, 1f);
            lp.rowSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1, 1f);
            lp.setMargins(spacing, spacing, spacing, spacing);
            btn.setLayoutParams(lp);

            btn.setOnClickListener(v -> onKeyPressed(key));
            grid.addView(btn);
        }
        addView(grid, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void bindEditText(EditText et) {
        this.targetEditText = et;
        // 禁用系统输入法
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            et.setShowSoftInputOnFocus(false);
        }
        et.setOnClickListener(v -> hideIme(v));
        et.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) hideIme(v);
        });
        et.setOnTouchListener((v, event) -> {
            hideIme(v);
            return false;
        });
        updatePreview();
    }

    private void hideIme(View v) {
        android.content.Context ctx = v.getContext();
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void onKeyPressed(String key) {
        if (targetEditText == null) return;
        HapticHelper.light(getContext());

        int start = targetEditText.getSelectionStart();
        int end = targetEditText.getSelectionEnd();
        String text = targetEditText.getText().toString();
        if (start < 0) start = text.length();
        if (end < start) end = start;

        switch (key) {
            case "⌫":
                if (start > 0 && start <= text.length()) {
                    String newText = text.substring(0, start - 1) + text.substring(end);
                    setFormattedText(newText, start - 1);
                }
                break;
            case "C":
                setFormattedText("", 0);
                break;
            case "=":
                double result = ExpressionEvaluator.evaluate(text);
                if (!Double.isNaN(result)) {
                    String formatted = ExpressionEvaluator.formatWithThousands(result);
                    setFormattedText(formatted, formatted.length());
                } else {
                    tvPreview.setText("表达式无效");
                    tvPreview.setTextColor(0xFFD4756A);
                }
                break;
            case "+":
                insertOperator("+", text, start, end);
                break;
            case "−":
                insertOperator("-", text, start, end);
                break;
            case "×":
                insertOperator("*", text, start, end);
                break;
            case "÷":
                insertOperator("/", text, start, end);
                break;
            default:
                String newText = text.substring(0, start) + key + text.substring(end);
                setFormattedText(newText, start + key.length());
                break;
        }
        updatePreview();
    }

    private void insertOperator(String op, String text, int start, int end) {
        String newText = text.substring(0, start) + op + text.substring(end);
        setFormattedText(newText, start + 1);
    }

    /** 设置文本并自动应用千分位格式化 */
    private void setFormattedText(String rawText, int cursorPos) {
        // 移除已有的逗号再重新格式化（防止重复）
        String cleaned = rawText.replace(",", "");
        String formatted = ExpressionEvaluator.formatLive(cleaned);
        targetEditText.setText(formatted);
        // 修正光标位置（因为格式化后字符数可能变化）
        int newPos = Math.min(cursorPos + countCommaDiff(cleaned, formatted, cursorPos), formatted.length());
        if (newPos < 0) newPos = 0;
        targetEditText.setSelection(newPos);
    }

    private int countCommaDiff(String cleaned, String formatted, int upToCleanedPos) {
        // 计算 cleaned 中 [0, upToCleanedPos) 范围对应的 formatted 增加的字符数（逗号）
        int cleanedIdx = 0;
        int formattedIdx = 0;
        while (cleanedIdx < upToCleanedPos && formattedIdx < formatted.length()) {
            char c = formatted.charAt(formattedIdx);
            if (c == ',') {
                formattedIdx++;
                continue;
            }
            if (cleaned.charAt(cleanedIdx) == c) {
                cleanedIdx++;
            }
            formattedIdx++;
        }
        return formattedIdx - cleanedIdx;
    }

    /** 实时更新预览：显示表达式的计算结果 */
    private void updatePreview() {
        if (targetEditText == null) return;
        String text = targetEditText.getText().toString();
        if (text.isEmpty()) {
            tvPreview.setText("");
            tvPreview.setTextColor(0xFF8B7D6B);
            return;
        }
        if (ExpressionEvaluator.isExpression(text)) {
            double result = ExpressionEvaluator.evaluate(text);
            if (!Double.isNaN(result)) {
                tvPreview.setText("= " + ExpressionEvaluator.formatWithThousands(result));
                tvPreview.setTextColor(0xFF7BA678);
            } else {
                tvPreview.setText("...");
                tvPreview.setTextColor(0xFFB8AC9A);
            }
        } else {
            tvPreview.setText("");
            tvPreview.setTextColor(0xFF8B7D6B);
        }
    }

    /** 返回最终金额值（如果是表达式则计算，否则直接解析） */
    public double getAmount() {
        if (targetEditText == null) return 0;
        String text = targetEditText.getText().toString().trim();
        if (text.isEmpty()) return 0;
        double v = ExpressionEvaluator.evaluate(text);
        if (Double.isNaN(v)) {
            try {
                v = Double.parseDouble(text.replace(",", ""));
            } catch (Exception e) {
                return 0;
            }
        }
        return v;
    }
}

