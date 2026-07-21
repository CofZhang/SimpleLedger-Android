package com.simpleledger.app;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
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
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 6.0 OCR 记账：拍照/相册选图 → ML Kit 中文识别 → 提取金额 → 跳转记账
 *
 * 6.4 重大升级：支持多笔账单识别
 *  - 识别截图中的多笔交易（日期+金额+分类+备注）
 *  - 自动匹配本地分类（按关键词命中）
 *  - 列表展示，用户可编辑每条记录
 *  - 一键填充所有选中记录到数据库
 *  - 适合从其他记账 APP 截图导入账单
 */
public class OcrActivity extends AppCompatActivity {

    private LinearLayout topNav;
    private Button btnCamera, btnGallery, btnImport;
    private TextView tvHint, tvResult;
    private RecyclerView rvTransactions;
    private View layoutEmpty;
    private List<OcrTransaction> transactions = new ArrayList<>();
    private OcrTransactionAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<Category> allExpenseCategories = new ArrayList<>();
    private List<Category> allIncomeCategories = new ArrayList<>();
    private Map<String, Long> categoryKeywordMap = new HashMap<>(); // 关键词 -> categoryId

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else Toast.makeText(this, "需要摄像头权限才能拍照识别", Toast.LENGTH_LONG).show();
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                                Bundle extras = result.getData().getExtras();
                                if (extras != null && extras.get("data") instanceof Bitmap) {
                                    Bitmap bmp = (Bitmap) extras.get("data");
                                    recognize(bmp);
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
                                        Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                                        recognize(bmp);
                                    } catch (Exception e) {
                                        Toast.makeText(OcrActivity.this, "加载图片失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        dbHelper = new DatabaseHelper(this);
        loadCategories();

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
        btnImport = findViewById(R.id.btnImport);
        tvHint = findViewById(R.id.tvHint);
        tvResult = findViewById(R.id.tvResult);
        rvTransactions = findViewById(R.id.rvTransactions);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OcrTransactionAdapter(transactions, pos -> {
            // 切换收入/支出
            OcrTransaction t = transactions.get(pos);
            t.setType(t.getType() == Record.TYPE_EXPENSE ? Record.TYPE_INCOME : Record.TYPE_EXPENSE);
            // 切换后重新匹配分类
            matchCategory(t);
            adapter.notifyItemChanged(pos);
        });
        rvTransactions.setAdapter(adapter);

        btnCamera.setOnClickListener(v -> {
            HapticHelper.light(this);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnGallery.setOnClickListener(v -> {
            HapticHelper.light(this);
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        btnImport.setOnClickListener(v -> {
            HapticHelper.medium(this);
            importSelected();
        });
    }

    /** 加载本地分类，构建关键词映射表 */
    private void loadCategories() {
        allExpenseCategories.clear();
        allIncomeCategories.clear();
        categoryKeywordMap.clear();
        allExpenseCategories.addAll(dbHelper.getCategories(Record.TYPE_EXPENSE));
        allIncomeCategories.addAll(dbHelper.getCategories(Record.TYPE_INCOME));
        // 关键词映射（支出+收入合并，匹配时按类型区分）
        for (Category c : allExpenseCategories) categoryKeywordMap.put(c.getName(), c.getId());
        for (Category c : allIncomeCategories) categoryKeywordMap.put(c.getName(), c.getId());
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(intent);
        } else {
            Toast.makeText(this, "未找到相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    /** ML Kit 中文识别 + 多笔交易解析 */
    private void recognize(Bitmap bitmap) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("正在识别...");
        pd.setCancelable(false);
        pd.show();

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build())
                .process(image)
                .addOnSuccessListener(text -> {
                    pd.dismiss();
                    tvResult.setText(text.getText());
                    parseTransactions(text);
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    tvHint.setText("识别失败：" + e.getMessage());
                });
    }

    /**
     * 6.4 解析多笔交易
     * 策略：按行扫描，每行尝试匹配日期+金额+分类+备注
     */
    private void parseTransactions(Text text) {
        transactions.clear();
        List<Text.TextBlock> blocks = text.getTextBlocks();
        String lastDate = today();
        // 收集所有非空文本行
        List<String> lines = new ArrayList<>();
        for (Text.TextBlock b : blocks) {
            for (Text.Line line : b.getLines()) {
                String s = line.getText().trim();
                if (!s.isEmpty()) lines.add(s);
            }
        }

        // 尝试匹配多笔交易
        for (String line : lines) {
            OcrTransaction t = parseLine(line, lastDate);
            if (t != null) {
                if (t.getDate() != null && !t.getDate().isEmpty()) {
                    lastDate = t.getDate();
                } else {
                    t.setDate(lastDate);
                }
                // 自动匹配分类
                matchCategory(t);
                transactions.add(t);
            }
        }

        // 如果没匹配到多笔，尝试整段文本提取金额（兼容旧版单笔模式）
        if (transactions.isEmpty()) {
            String full = text.getText();
            List<Double> amounts = extractAllAmounts(full);
            for (Double amt : amounts) {
                OcrTransaction t = new OcrTransaction();
                t.setAmount(amt);
                t.setDate(today());
                transactions.add(t);
            }
        }

        if (transactions.isEmpty()) {
            tvHint.setText("未识别到账单信息，请尝试更清晰的截图");
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
            btnImport.setVisibility(View.GONE);
        } else {
            tvHint.setText("已识别 " + transactions.size() + " 笔账单，可编辑后一键填充");
            layoutEmpty.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
            btnImport.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    /** 解析单行文本，提取日期+金额+分类+备注 */
    private OcrTransaction parseLine(String line, String fallbackDate) {
        // 必须有金额才算有效行
        Double amount = extractFirstAmount(line);
        if (amount == null || amount <= 0) return null;

        OcrTransaction t = new OcrTransaction();
        t.setAmount(amount);

        // 提取日期
        String date = extractDate(line);
        if (date != null) {
            t.setDate(date);
        }

        // 提取分类（关键词匹配）
        String category = matchCategoryKeyword(line);
        if (category != null) {
            t.setCategoryName(category);
        }

        // 备注 = 去除金额、日期、分类后的剩余
        String remark = line;
        remark = remark.replaceAll("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}", "");
        remark = remark.replaceAll("\\d{1,2}[-/]\\d{1,2}", "");
        remark = remark.replaceAll("\\d+(?:\\.\\d{1,2})?\\s*(?:元|块钱?|圆|RMB|￥|¥)", "");
        remark = remark.replaceAll("(?:支出|收入|餐饮|交通|购物|娱乐|住房|医疗|教育|工资|奖金|红包|理财|其他)", "");
        remark = remark.replaceAll("[\\s\\-/:：¥￥]", "").trim();
        if (remark.isEmpty()) remark = null;
        t.setRemark(remark);

        // 判断收入/支出（默认支出）
        if (line.contains("收入") || line.contains("工资") || line.contains("奖金")
                || line.contains("红包") || line.contains("理财") || line.contains("报销")) {
            t.setType(Record.TYPE_INCOME);
        }

        return t;
    }

    /** 提取首个金额 */
    private Double extractFirstAmount(String text) {
        Pattern p = Pattern.compile("(?:¥|￥|RMB)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块|圆|RMB|￥|¥)?");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); }
            catch (Exception e) { return null; }
        }
        return null;
    }

    /** 提取所有金额 */
    private List<Double> extractAllAmounts(String text) {
        List<Double> list = new ArrayList<>();
        Pattern p = Pattern.compile("(?:¥|￥|RMB)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块|圆|RMB|￥|¥)?");
        Matcher m = p.matcher(text);
        while (m.find()) {
            try {
                double v = Double.parseDouble(m.group(1));
                if (v > 0) list.add(v);
            } catch (Exception ignored) {}
        }
        return list;
    }

    /** 提取日期：支持 yyyy-MM-dd, yyyy/MM/dd, MM-dd, MM/dd */
    private String extractDate(String text) {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        // yyyy-MM-dd
        Pattern p1 = Pattern.compile("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})");
        Matcher m1 = p1.matcher(text);
        if (m1.find()) {
            return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    Integer.parseInt(m1.group(1)),
                    Integer.parseInt(m1.group(2)),
                    Integer.parseInt(m1.group(3)));
        }
        // MM-dd
        Pattern p2 = Pattern.compile("(?<!\\d)(\\d{1,2})[-/](\\d{1,2})(?!\\d)");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) {
            try {
                int mm = Integer.parseInt(m2.group(1));
                int dd = Integer.parseInt(m2.group(2));
                if (mm >= 1 && mm <= 12 && dd >= 1 && dd <= 31) {
                    return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, mm, dd);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** 文本中匹配分类关键词 */
    private String matchCategoryKeyword(String text) {
        if (text == null) return null;
        // 常见分类关键词（含别名）
        String[][] keywords = {
                {"餐饮", "餐饮", "吃饭", "早餐", "午餐", "晚餐", "夜宵", "外卖", "饭店", "餐厅", "食堂", "美食", "小吃"},
                {"交通", "交通", "打车", "地铁", "公交", "出租车", "高铁", "火车", "飞机", "停车", "油费", "过路费"},
                {"购物", "购物", "超市", "商场", "便利店", "淘宝", "京东", "拼多多", "网购"},
                {"服饰", "服饰", "衣服", "鞋", "包", "服装"},
                {"娱乐", "娱乐", "电影", "游戏", "KTV", "演出", "演唱会"},
                {"住房", "住房", "房租", "水电", "物业", "燃气", "宽带"},
                {"医疗", "医疗", "医院", "药", "看病", "门诊"},
                {"教育", "教育", "书", "课程", "培训", "学费", "考试"},
                {"通讯", "通讯", "话费", "流量", "手机"},
                {"美容", "美容", "理发", "护肤", "化妆品"},
                {"零食", "零食", "奶茶", "咖啡", "饮料", "水果"},
                {"数码", "数码", "手机", "电脑", "耳机", "电子"},
                {"工资", "工资"},
                {"奖金", "奖金"},
                {"红包", "红包"},
                {"理财", "理财", "利息", "收益"},
                {"报销", "报销"}
        };
        for (String[] group : keywords) {
            for (int i = 1; i < group.length; i++) {
                if (text.contains(group[i])) {
                    return group[0];
                }
            }
        }
        return null;
    }

    /** 用本地分类库匹配分类 */
    private void matchCategory(OcrTransaction t) {
        if (t.getCategoryName() == null || t.getCategoryName().isEmpty()) {
            t.setCategoryId(0);
            return;
        }
        List<Category> cats = t.getType() == Record.TYPE_INCOME ? allIncomeCategories : allExpenseCategories;
        // 精确匹配
        for (Category c : cats) {
            if (c.getName().equals(t.getCategoryName())) {
                t.setCategoryId(c.getId());
                return;
            }
        }
        // 包含匹配
        for (Category c : cats) {
            if (c.getName().contains(t.getCategoryName()) || t.getCategoryName().contains(c.getName())) {
                t.setCategoryId(c.getId());
                return;
            }
        }
        // 关键词匹配
        String kw = matchCategoryKeyword(t.getCategoryName());
        if (kw != null) {
            for (Category c : cats) {
                if (c.getName().equals(kw)) {
                    t.setCategoryId(c.getId());
                    t.setCategoryName(c.getName());
                    return;
                }
            }
        }
        t.setCategoryId(0);
    }

    /** 一键填充所有选中的交易到数据库 */
    private void importSelected() {
        List<OcrTransaction> selected = new ArrayList<>();
        for (OcrTransaction t : transactions) {
            if (t.isSelected() && t.getAmount() > 0) selected.add(t);
        }
        if (selected.isEmpty()) {
            Toast.makeText(this, "请至少选中一条有效记录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取默认账户
        List<Account> accounts = dbHelper.getAllAccounts();
        long defaultAccountId = accounts.isEmpty() ? 0 : accounts.get(0).getId();

        int success = 0, skip = 0;
        for (OcrTransaction t : selected) {
            if (t.getAmount() <= 0) { skip++; continue; }
            Record r = new Record();
            r.setType(t.getType());
            r.setAmount(t.getAmount());
            r.setCategoryId(t.getCategoryId() > 0 ? t.getCategoryId() : getDefaultCategoryId(t.getType()));
            r.setAccountId(defaultAccountId);
            r.setDate(t.getDate() != null ? t.getDate() : today());
            r.setTimestamp(System.currentTimeMillis());
            r.setRemark(t.getRemark());
            try {
                dbHelper.addRecord(r);
                success++;
            } catch (Exception e) {
                skip++;
            }
        }

        Toast.makeText(this, "成功导入 " + success + " 笔" + (skip > 0 ? "，跳过 " + skip + " 笔" : ""),
                Toast.LENGTH_LONG).show();
        if (success > 0) {
            // 跳转到明细页
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TARGET, MainActivity.TARGET_RECORDS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }
    }

    private long getDefaultCategoryId(int type) {
        List<Category> cats = type == Record.TYPE_INCOME ? allIncomeCategories : allExpenseCategories;
        return cats.isEmpty() ? 0 : cats.get(0).getId();
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
    }
}
