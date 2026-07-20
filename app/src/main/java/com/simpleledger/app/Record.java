package com.simpleledger.app;

public class Record {
    private long id;
    private int type;
    private double amount;
    private long categoryId;
    private String categoryName;
    private String categoryIcon;
    private int categoryColor;
    private long projectId;
    private String projectName;
    private long accountId;
    private String accountName;
    private String accountIcon;
    private String date;
    private long timestamp;
    private String remark;
    private String tags;

    public static final int TYPE_EXPENSE = 0;
    public static final int TYPE_INCOME = 1;

    public Record() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }

    public int getCategoryColor() { return categoryColor; }
    public void setCategoryColor(int categoryColor) { this.categoryColor = categoryColor; }

    public long getProjectId() { return projectId; }
    public void setProjectId(long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public long getAccountId() { return accountId; }
    public void setAccountId(long accountId) { this.accountId = accountId; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountIcon() { return accountIcon; }
    public void setAccountIcon(String accountIcon) { this.accountIcon = accountIcon; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}

