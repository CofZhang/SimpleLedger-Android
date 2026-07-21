package com.simpleledger.app;

/**
 * 6.0 周期账单模型：自动记录房租/订阅/工资等固定收支
 */
public class RecurringBill {
    private long id;
    private String title;
    private int type; // Record.TYPE_EXPENSE / Record.TYPE_INCOME
    private double amount;
    private long categoryId;
    private long accountId = 1;
    private long projectId = 0;
    private int period; // 1=每日 2=每周 3=每月 4=每年
    private int dayOfPeriod = 1;
    private String nextDate; // yyyy-MM-dd
    private String remark;
    private boolean enabled = true;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
    public long getAccountId() { return accountId; }
    public void setAccountId(long accountId) { this.accountId = accountId; }
    public long getProjectId() { return projectId; }
    public void setProjectId(long projectId) { this.projectId = projectId; }
    public int getPeriod() { return period; }
    public void setPeriod(int period) { this.period = period; }
    public int getDayOfPeriod() { return dayOfPeriod; }
    public void setDayOfPeriod(int dayOfPeriod) { this.dayOfPeriod = dayOfPeriod; }
    public String getNextDate() { return nextDate; }
    public void setNextDate(String nextDate) { this.nextDate = nextDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getPeriodText() {
        switch (period) {
            case 1: return "每日";
            case 2: return "每周";
            case 3: return "每月";
            case 4: return "每年";
            default: return "未知";
        }
    }
}
