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
     * 6.5 大幅优化解析多笔交易
     * 策略：
     *  1) 收集所有文本行
     *  2) 先扫描找出"日期行"（含明确日期格式的行）
     *  3) 扫描每行，用严格金额识别（带货币符号优先）找出"交易行"
     *  4) 对每个交易行，向上找最近的日期作为该交易日期
     *  5) 从交易行及相邻行提取分类关键词
     */
    private void parseTransactions(Text text) {
        transactions.clear();
        List<Text.TextBlock> blocks = text.getTextBlocks();
        String globalDate = today();

        // 收集所有非空文本行，保留原始顺序
        List<String> lines = new ArrayList<>();
        for (Text.TextBlock b : blocks) {
            for (Text.Line line : b.getLines()) {
                String s = line.getText().trim();
                if (!s.isEmpty()) lines.add(s);
            }
        }

        // 第一步：找全局日期（截图顶部通常有月份/日期）
        for (String line : lines) {
            String d = extractDate(line);
            if (d != null) {
                globalDate = d;
                break;
            }
        }

        // 第二步：扫描每行，识别交易
        String currentDate = globalDate;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // 更新当前日期（如果该行有日期）
            String lineDate = extractDate(line);
            if (lineDate != null) {
                currentDate = lineDate;
            }

            // 识别该行是否为交易行（必须能提取到严格金额）
            Double amount = extractStrictAmount(line);
            if (amount == null || amount <= 0) continue;

            OcrTransaction t = new OcrTransaction();
            t.setAmount(amount);
            t.setDate(currentDate);

            // 从当前行和相邻行（上下各 1 行）提取分类
            StringBuilder context = new StringBuilder(line);
            if (i > 0) context.insert(0, lines.get(i - 1) + " ");
            if (i < lines.size() - 1) context.append(" " + lines.get(i + 1));
            String category = matchCategoryKeyword(context.toString());
            if (category != null) {
                t.setCategoryName(category);
            }

            // 备注：从行中去除金额、日期、分类关键词后的剩余
            String remark = cleanRemark(line);
            if (remark != null && !remark.isEmpty()) {
                t.setRemark(remark);
            }

            // 判断收入/支出
            if (line.contains("收入") || line.contains("工资") || line.contains("奖金")
                    || line.contains("红包") || line.contains("理财") || line.contains("报销")
                    || line.contains("退款") || line.contains("转入")) {
                t.setType(Record.TYPE_INCOME);
            }

            matchCategory(t);
            transactions.add(t);
        }

        // 兜底：如果按行没识别到，尝试按 TextBlock 整块识别
        if (transactions.isEmpty()) {
            for (Text.TextBlock b : blocks) {
                String blockText = b.getText();
                Double amt = extractStrictAmount(blockText);
                if (amt != null && amt > 0) {
                    OcrTransaction t = new OcrTransaction();
                    t.setAmount(amt);
                    t.setDate(globalDate);
                    String cat = matchCategoryKeyword(blockText);
                    if (cat != null) t.setCategoryName(cat);
                    matchCategory(t);
                    transactions.add(t);
                }
            }
        }

        if (transactions.isEmpty()) {
            tvHint.setText("未识别到账单信息，请尝试更清晰的截图\n提示：金额需带 ¥/￥/元 符号或小数点");
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
            btnImport.setVisibility(View.GONE);
        } else {
            tvHint.setText("已识别 " + transactions.size() + " 笔账单，可编辑后一键填充\n如识别有误可手动修改每条记录");
            layoutEmpty.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
            btnImport.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * 6.5 严格金额识别（大幅优化）
     * 优先级：
     *   1) 带货币符号：¥123.45、￥123、123元、123块、RMB123
     *   2) 带小数点且小数位 1-2 位：123.45、123.5
     *   3) 排除：年份 19xx/20xx、时间 HH:mm、订单号（>8位）、日期数字
     *   4) 金额范围：0.01 ~ 1000000
     */
    private Double extractStrictAmount(String text) {
        if (text == null || text.isEmpty()) return null;

        // 优先级 1：带货币符号的金额
        // ¥123.45 / ￥123 / 123元 / 123.5元 / 123块钱 / 123.45块
        Pattern pSym = Pattern.compile("(?:¥|￥|RMB|￥)\\s*(\\d+(?:\\.\\d{1,2})?)");
        Matcher mSym = pSym.matcher(text);
        if (mSym.find()) {
            try {
                double v = Double.parseDouble(mSym.group(1));
                if (isValidAmount(v)) return v;
            } catch (Exception ignored) {}
        }
        Pattern pSym2 = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)\\s*(?:元|块钱?|圆|RMB)");
        Matcher mSym2 = pSym2.matcher(text);
        if (mSym2.find()) {
            try {
                double v = Double.parseDouble(mSym2.group(1));
                if (isValidAmount(v)) return v;
            } catch (Exception ignored) {}
        }

        // 优先级 2：带小数点且小数位 1-2 位的数字（排除年份、时间）
        // 匹配 123.45 或 123.4，不匹配 123.456（小数位>2）
        Pattern pDecimal = Pattern.compile("(?<!\\d)(\\d{1,7}\\.\\d{1,2})(?!\\d)");
        Matcher mDecimal = pDecimal.matcher(text);
        double bestDecimal = 0;
        while (mDecimal.find()) {
            try {
                double v = Double.parseDouble(mDecimal.group(1));
                if (isValidAmount(v) && v > bestDecimal) {
                    bestDecimal = v;
                }
            } catch (Exception ignored) {}
        }
        if (bestDecimal > 0) return bestDecimal;

        // 优先级 3：纯整数金额（仅在同行有"支出/收入/消费/花费"等关键词时才识别）
        if (text.contains("支出") || text.contains("收入") || text.contains("消费")
                || text.contains("花费") || text.contains("花") || text.contains("付")
                || text.contains("买") || text.contains("账单")) {
            // 匹配 1-5 位整数，排除年份 19xx/20xx 和时间 HH:mm
            Pattern pInt = Pattern.compile("(?<!\\d)([1-9]\\d{0,5})(?!\\d|[:-])");
            Matcher mInt = pInt.matcher(text);
            double bestInt = 0;
            while (mInt.find()) {
                try {
                    double v = Double.parseDouble(mInt.group(1));
                    // 排除年份 1900-2099
                    if (v >= 1900 && v <= 2099) continue;
                    if (isValidAmount(v) && v > bestInt) {
                        bestInt = v;
                    }
                } catch (Exception ignored) {}
            }
            if (bestInt > 0) return bestInt;
        }

        return null;
    }

    /** 6.5 判断是否为有效金额 */
    private boolean isValidAmount(double v) {
        return v >= 0.01 && v <= 1000000;
    }

    /** 6.5 清理备注：去除金额、日期、分类关键词、特殊符号 */
    private String cleanRemark(String line) {
        String remark = line;
        // 去除货币符号+金额
        remark = remark.replaceAll("(?:¥|￥|RMB)\\s*\\d+(?:\\.\\d{1,2})?", "");
        remark = remark.replaceAll("\\d+(?:\\.\\d{1,2})?\\s*(?:元|块钱?|圆|RMB)", "");
        // 去除纯金额
        remark = remark.replaceAll("\\d+\\.\\d{1,2}", "");
        // 去除日期
        remark = remark.replaceAll("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}", "");
        remark = remark.replaceAll("\\d{1,2}月\\d{1,2}日?", "");
        remark = remark.replaceAll("\\d{4}年\\d{1,2}月\\d{1,2}日?", "");
        // 去除时间
        remark = remark.replaceAll("\\d{1,2}:\\d{2}", "");
        // 去除分类关键词
        remark = remark.replaceAll("(?:支出|收入|餐饮|交通|购物|娱乐|住房|医疗|教育|通讯|美容|零食|数码|工资|奖金|红包|理财|报销|其他|今日|昨日|今天|昨天|明细|账单|合计|总计|余额|结余)", "");
        // 去除特殊符号
        remark = remark.replaceAll("[\\s\\-/:：¥￥·•|+]", "").trim();
        return remark;
    }

    /**
     * 6.5 优化日期识别
     * 支持：yyyy-MM-dd、yyyy/MM/dd、yyyy年MM月dd日、MM月dd日、今天、昨天、前天
     * 不再匹配 MM-dd（避免和时间 HH-mm 混淆）
     */
    private String extractDate(String text) {
        if (text == null || text.isEmpty()) return null;
        int year = Calendar.getInstance().get(Calendar.YEAR);
        Calendar cal = Calendar.getInstance();

        // 今天/昨天/前天
        if (text.contains("今天") || text.contains("今日")) {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        }
        if (text.contains("昨天") || text.contains("昨日")) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        }
        if (text.contains("前天")) {
            cal.add(Calendar.DAY_OF_MONTH, -2);
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        }

        // yyyy年MM月dd日
        Pattern p1 = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日?");
        Matcher m1 = p1.matcher(text);
        if (m1.find()) {
            try {
                int y = Integer.parseInt(m1.group(1));
                int mm = Integer.parseInt(m1.group(2));
                int dd = Integer.parseInt(m1.group(3));
                if (isValidDate(y, mm, dd)) {
                    return String.format(Locale.getDefault(), "%04d-%02d-%02d", y, mm, dd);
                }
            } catch (Exception ignored) {}
        }

        // MM月dd日（用当前年份补全）
        Pattern p2 = Pattern.compile("(?<!\\d)(\\d{1,2})月(\\d{1,2})日?");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) {
            try {
                int mm = Integer.parseInt(m2.group(1));
                int dd = Integer.parseInt(m2.group(2));
                if (isValidDate(year, mm, dd)) {
                    return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, mm, dd);
                }
            } catch (Exception ignored) {}
        }

        // yyyy-MM-dd 或 yyyy/MM/dd（严格：年份 1900-2099，月 1-12，日 1-31）
        Pattern p3 = Pattern.compile("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})");
        Matcher m3 = p3.matcher(text);
        if (m3.find()) {
            try {
                int y = Integer.parseInt(m3.group(1));
                int mm = Integer.parseInt(m3.group(2));
                int dd = Integer.parseInt(m3.group(3));
                if (isValidDate(y, mm, dd)) {
                    return String.format(Locale.getDefault(), "%04d-%02d-%02d", y, mm, dd);
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    /** 6.5 校验日期是否有效 */
    private boolean isValidDate(int y, int m, int d) {
        if (y < 1900 || y > 2099) return false;
        if (m < 1 || m > 12) return false;
        if (d < 1 || d > 31) return false;
        return true;
    }

    /** 文本中匹配分类关键词（6.5 扩充） */
    private String matchCategoryKeyword(String text) {
        if (text == null) return null;
        // 常见分类关键词（含别名），6.5 大幅扩充
        String[][] keywords = {
                {"餐饮", "餐饮", "吃饭", "早餐", "午餐", "晚餐", "夜宵", "外卖", "饭店", "餐厅",
                        "食堂", "美食", "小吃", "早饭", "午饭", "晚饭", "火锅", "烧烤", "快餐",
                        "面", "米粉", "饺子", "汉堡", "披萨", "寿司", "日料", "韩餐", "中餐",
                        "西餐", "饮料", "可乐", "雪碧", "果汁", "茶", "咖啡", "奶茶", "啤酒"},
                {"交通", "交通", "打车", "地铁", "公交", "出租车", "高铁", "火车", "飞机",
                        "停车", "油费", "过路费", "滴滴", "快的", " Uber", "机票", "车票",
                        "火车票", "bus", "taxi", "加油", "充电桩", "共享单车", "哈啰"},
                {"购物", "购物", "超市", "商场", "便利店", "淘宝", "京东", "拼多多", "网购",
                        "天猫", "苏宁", "唯品会", "亚马逊", "买东西", "日用品", "纸巾", "洗衣液",
                        "洗发水", "沐浴露", "牙膏", "毛巾"},
                {"服饰", "服饰", "衣服", "鞋", "包", "服装", "裤子", "裙子", "外套", "T恤",
                        "衬衫", "运动鞋", "皮鞋", "帽子", "围巾", "手套", "内衣", "袜子"},
                {"娱乐", "娱乐", "电影", "游戏", "KTV", "演出", "演唱会", "网吧", "ktv",
                        "音乐会", "话剧", "展览", "游乐园", "密室", "桌游", "Steam", "ps5"},
                {"住房", "住房", "房租", "水电", "物业", "燃气", "宽带", "电费", "水费",
                        "天然气", "暖气", "房贷", "租金"},
                {"医疗", "医疗", "医院", "药", "看病", "门诊", "挂号", "体检", "牙科",
                        "眼科", "感冒药", "维生素", "保健品", "诊所"},
                {"教育", "教育", "书", "课程", "培训", "学费", "考试", "教材", "网课",
                        "英语", "数学", "语文", "物理", "化学", "钢琴", "吉他", "绘画",
                        "培训班", "辅导"},
                {"通讯", "通讯", "话费", "流量", "手机", "宽带费", "月租", "充值",
                        "移动", "联通", "电信"},
                {"美容", "美容", "理发", "护肤", "化妆品", "美甲", "美发", "烫发", "染发",
                        "面膜", "口红", "粉底", "香水", "spa", "按摩"},
                {"零食", "零食", "奶茶", "咖啡", "饮料", "水果", "薯片", "巧克力", "饼干",
                        "糖果", "坚果", "瓜子", "可乐", "果汁", "蛋糕", "甜品", "冰淇淋"},
                {"数码", "数码", "手机", "电脑", "耳机", "电子", "相机", "平板", "笔记本",
                        "键盘", "鼠标", "显示器", "路由器", "U盘", "硬盘", "充电器", "数据线",
                        "手机壳", "贴膜"},
                {"宠物", "宠物", "猫粮", "狗粮", "猫砂", "兽医", "宠物医院", "牵引绳"},
                {"运动", "运动", "健身", "健身房", "瑜伽", "跑步", "游泳", "篮球", "足球",
                        "羽毛球", "网球", "乒乓球", "器械", "蛋白粉"},
                {"旅行", "旅行", "旅游", "酒店", "民宿", "景点", "门票", "签证", "跟团",
                        "自由行", "攻略"},
                {"人情", "人情", "份子钱", "红包", "礼金", "随礼", "结婚", "生日"},
                {"工资", "工资", "薪水", "底薪", "加班费"},
                {"奖金", "奖金", "年终奖", "绩效", "提成", "分红"},
                {"红包", "红包", "微信红包", "支付宝红包", "转账"},
                {"理财", "理财", "利息", "收益", "基金", "股票", "债券", "余额宝", "定期",
                        "理财收益"},
                {"报销", "报销", "退款", "退货", "退钱"},
                {"兼职", "兼职", "外快", "副业", "稿费", "咨询费"}
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
