package com.simpleledger.app;

public class Account {
    public static final int TYPE_CASH = 0;
    public static final int TYPE_BANK = 1;
    public static final int TYPE_ALIPAY = 2;
    public static final int TYPE_WECHAT = 3;
    public static final int TYPE_CREDIT = 4;
    public static final int TYPE_OTHER = 5;

    private long id;
    private String name;
    private String icon;
    private int type;
    private double initBalance;
    private double balance;
    private String remark;

    public Account() {}

    public Account(String name, String icon, int type, double initBalance, String remark) {
        this.name = name;
        this.icon = icon;
        this.type = type;
        this.initBalance = initBalance;
        this.balance = initBalance;
        this.remark = remark;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public double getInitBalance() { return initBalance; }
    public void setInitBalance(double initBalance) { this.initBalance = initBalance; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
