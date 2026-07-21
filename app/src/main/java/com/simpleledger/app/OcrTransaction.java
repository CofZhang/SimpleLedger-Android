package com.simpleledger.app;

/**
 * 6.4 OCR 识别出的单笔交易数据模型
 * 用户可编辑每个字段，确认后一键填充到数据库
 */
public class OcrTransaction {
    private String date;          // yyyy-MM-dd
    private double amount;        // 金额
    private String categoryName;  // 分类名称（待匹配）
    private long categoryId;      // 匹配到的分类 ID，0=未匹配
    private String remark;        // 备注
    private int type;             // 0=支出, 1=收入
    private boolean selected;     // 是否选中导入

    public OcrTransaction() {
        this.selected = true;
        this.type = Record.TYPE_EXPENSE;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
