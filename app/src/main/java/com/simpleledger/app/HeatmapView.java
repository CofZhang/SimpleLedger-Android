package com.simpleledger.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * 4.5 年度账单热力图（GitHub 贡献图风格）
 * - 53 周 x 7 天彩色方块网格
 * - 5 个颜色级别：无消费 + 4 个消费等级
 * - 行左侧显示星期标签（一/三/五）
 * - 顶部显示月份标签
 * - 支持横向滚动（外层 HorizontalScrollView）
 * - 点击某天回调 OnDayClickListener
 */
public class HeatmapView extends View {

    /** 颜色级别（无消费 + L1-L4） */
    private static final int[] LEVEL_COLORS = {
            Color.parseColor("#EDE4D6"), // 无消费
            Color.parseColor("#E8C9B5"), // L1
            Color.parseColor("#D6A88C"), // L2
            Color.parseColor("#C4856A"), // L3
            Color.parseColor("#A86050")  // L4
    };

    private static final int TOTAL_WEEKS = 53;
    private static final int TOTAL_DAYS_OF_WEEK = 7;

    // 星期标签：周一、周三、周五
    private static final String[] WEEK_LABELS = {"", "一", "", "三", "", "五", ""};

    private Paint cellPaint;
    private Paint labelPaint;
    private Paint monthLabelPaint;
    private Paint selectedPaint;

    private final float density;
    private final float cellSize;     // 方块边长 px
    private final float cellGap;      // 方块间距 px
    private final float cornerRadius; // 圆角 px
    private final float leftLabelWidth;  // 左侧星期标签宽
    private final float topLabelHeight; // 顶部月份标签高
    private final float rightPadding;
    private final float bottomPadding;

    private int year;
    private Map<String, Double> dailyExpense = new HashMap<>();
    private double maxAmount = 0.0;

    private String selectedDate = null;
    private int selectedCol = -1;
    private int selectedRow = -1;

    private OnDayClickListener onDayClickListener;

    /** 缓存每日在网格中的位置：日期字符串 -> [col, row] */
    private final Map<String, int[]> dateToGrid = new HashMap<>();

    public interface OnDayClickListener {
        void onDayClick(String date, double amount);
    }

    public HeatmapView(Context context) {
        this(context, null);
    }

    public HeatmapView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeatmapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        cellSize = 14f * density;
        cellGap = 2f * density;
        cornerRadius = 2f * density;
        leftLabelWidth = 22f * density;
        topLabelHeight = 18f * density;
        rightPadding = 12f * density;
        bottomPadding = 6f * density;

        initPaints();
        year = Calendar.getInstance().get(Calendar.YEAR);
        rebuildGridIndex();
    }

    private void initPaints() {
        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellPaint.setStyle(Paint.Style.FILL);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(10f * density);
        labelPaint.setColor(Color.parseColor("#8B7D6B"));
        labelPaint.setTextAlign(Paint.Align.LEFT);

        monthLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        monthLabelPaint.setTextSize(11f * density);
        monthLabelPaint.setColor(Color.parseColor("#3D352C"));
        monthLabelPaint.setFakeBoldText(true);
        monthLabelPaint.setTextAlign(Paint.Align.LEFT);

        selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(1.5f * density);
        selectedPaint.setColor(Color.parseColor("#3D352C"));
    }

    /** 设置年份并刷新数据 */
    public void setYear(int year) {
        this.year = year;
        rebuildGridIndex();
        invalidate();
    }

    public int getYear() {
        return year;
    }

    /** 设置日支出数据：key = "yyyy-MM-dd"，value = 支出金额 */
    public void setDailyExpense(Map<String, Double> data) {
        this.dailyExpense = data != null ? data : new HashMap<>();
        maxAmount = 0.0;
        for (Double v : this.dailyExpense.values()) {
            if (v != null && v > maxAmount) maxAmount = v;
        }
        invalidate();
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.onDayClickListener = listener;
    }

    /** 设置当前选中日期（用于外部同步），传 null 清除 */
    public void setSelectedDate(String date) {
        this.selectedDate = date;
        if (date != null) {
            int[] pos = dateToGrid.get(date);
            if (pos != null) {
                selectedCol = pos[0];
                selectedRow = pos[1];
            } else {
                selectedCol = -1;
                selectedRow = -1;
            }
        } else {
            selectedCol = -1;
            selectedRow = -1;
        }
        invalidate();
    }

    /** 重建日期到网格位置的索引 */
    private void rebuildGridIndex() {
        dateToGrid.clear();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        // 周日=1，周一=2 ... 周六=7
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int offset = firstDayOfWeek - Calendar.SUNDAY; // 0..6

        int daysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR);
        for (int dayOfYear = 1; dayOfYear <= daysInYear; dayOfYear++) {
            int pos = dayOfYear - 1 + offset;
            int col = pos / 7;
            int row = pos % 7;
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            String dateStr = String.format("%04d-%02d-%02d", year, month + 1, day);
            dateToGrid.put(dateStr, new int[]{col, row});
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int contentWidth = (int) (leftLabelWidth + (cellSize + cellGap) * TOTAL_WEEKS + rightPadding);
        int contentHeight = (int) (topLabelHeight + (cellSize + cellGap) * TOTAL_DAYS_OF_WEEK + bottomPadding);
        setMeasuredDimension(contentWidth, contentHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 背景透明，使用父布局背景
        drawWeekLabels(canvas);
        drawMonthLabels(canvas);
        drawCells(canvas);
        drawSelection(canvas);
    }

    private void drawWeekLabels(Canvas canvas) {
        float x = 0f;
        for (int row = 0; row < TOTAL_DAYS_OF_WEEK; row++) {
            String label = WEEK_LABELS[row];
            if (label != null && !label.isEmpty()) {
                float y = topLabelHeight + row * (cellSize + cellGap) + cellSize * 0.75f;
                canvas.drawText(label, x, y, labelPaint);
            }
        }
    }

    private void drawMonthLabels(Canvas canvas) {
        // 月份标签放在对应周列顶部
        int lastMonth = -1;
        for (int col = 0; col < TOTAL_WEEKS; col++) {
            int month = monthOfColumn(col);
            if (month >= 0 && month != lastMonth) {
                float x = leftLabelWidth + col * (cellSize + cellGap);
                canvas.drawText((month + 1) + "月", x, topLabelHeight * 0.85f, monthLabelPaint);
                lastMonth = month;
            }
        }
    }

    /** 根据列号返回该列第一个有效日期的月份，返回 -1 表示该列无有效日期 */
    private int monthOfColumn(int col) {
        for (Map.Entry<String, int[]> e : dateToGrid.entrySet()) {
            if (e.getValue()[0] == col) {
                String date = e.getKey();
                try {
                    return Integer.parseInt(date.substring(5, 7)) - 1;
                } catch (Exception ignore) {
                }
            }
        }
        return -1;
    }

    private void drawCells(Canvas canvas) {
        // 先画所有空格子（无消费色）
        RectF rect = new RectF();
        for (int col = 0; col < TOTAL_WEEKS; col++) {
            for (int row = 0; row < TOTAL_DAYS_OF_WEEK; row++) {
                float left = leftLabelWidth + col * (cellSize + cellGap);
                float top = topLabelHeight + row * (cellSize + cellGap);
                rect.set(left, top, left + cellSize, top + cellSize);
                cellPaint.setColor(LEVEL_COLORS[0]);
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, cellPaint);
            }
        }
        // 画有数据的格子
        for (Map.Entry<String, int[]> e : dateToGrid.entrySet()) {
            int col = e.getValue()[0];
            int row = e.getValue()[1];
            if (col < 0 || col >= TOTAL_WEEKS || row < 0 || row >= TOTAL_DAYS_OF_WEEK) continue;
            Double amount = dailyExpense.get(e.getKey());
            int level = amountToLevel(amount);
            float left = leftLabelWidth + col * (cellSize + cellGap);
            float top = topLabelHeight + row * (cellSize + cellGap);
            rect.set(left, top, left + cellSize, top + cellSize);
            cellPaint.setColor(LEVEL_COLORS[level]);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, cellPaint);
        }
    }

    private void drawSelection(Canvas canvas) {
        if (selectedCol < 0 || selectedRow < 0) return;
        float left = leftLabelWidth + selectedCol * (cellSize + cellGap);
        float top = topLabelHeight + selectedRow * (cellSize + cellGap);
        RectF rect = new RectF(left - 1f, top - 1f, left + cellSize + 1f, top + cellSize + 1f);
        canvas.drawRoundRect(rect, cornerRadius + 1f, cornerRadius + 1f, selectedPaint);
    }

    /** 金额映射到颜色级别：0=无消费，1-4 为消费等级 */
    private int amountToLevel(Double amount) {
        if (amount == null || amount <= 0) return 0;
        if (maxAmount <= 0) return 1;
        // 基于最大值的四分位
        double q1 = maxAmount * 0.25;
        double q2 = maxAmount * 0.50;
        double q3 = maxAmount * 0.75;
        if (amount <= q1) return 1;
        if (amount <= q2) return 2;
        if (amount <= q3) return 3;
        return 4;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            int col = colAtX(x);
            int row = rowAtY(y);
            if (col >= 0 && col < TOTAL_WEEKS && row >= 0 && row < TOTAL_DAYS_OF_WEEK) {
                String date = dateAt(col, row);
                if (date != null) {
                    double amount = 0.0;
                    Double v = dailyExpense.get(date);
                    if (v != null) amount = v;
                    selectedDate = date;
                    selectedCol = col;
                    selectedRow = row;
                    invalidate();
                    if (onDayClickListener != null) {
                        onDayClickListener.onDayClick(date, amount);
                    }
                    performClick();
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private int colAtX(float x) {
        float start = leftLabelWidth;
        if (x < start) return -1;
        float offset = x - start;
        return (int) (offset / (cellSize + cellGap));
    }

    private int rowAtY(float y) {
        if (y < topLabelHeight) return -1;
        float offset = y - topLabelHeight;
        return (int) (offset / (cellSize + cellGap));
    }

    private String dateAt(int col, int row) {
        for (Map.Entry<String, int[]> e : dateToGrid.entrySet()) {
            if (e.getValue()[0] == col && e.getValue()[1] == row) {
                return e.getKey();
            }
        }
        return null;
    }
}
