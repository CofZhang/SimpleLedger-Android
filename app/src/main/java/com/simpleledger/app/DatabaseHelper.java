package com.simpleledger.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "simple_ledger.db";
    private static final int DATABASE_VERSION = 5;

    private static final String TABLE_RECORDS = "records";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_PROJECTS = "projects";
    private static final String TABLE_BUDGETS = "budgets";
    private static final String TABLE_ACCOUNTS = "accounts";

    private static final String KEY_ID = "id";
    private static final String KEY_TYPE = "type";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_CATEGORY_ID = "category_id";
    private static final String KEY_PROJECT_ID = "project_id";
    private static final String KEY_ACCOUNT_ID = "account_id";
    private static final String KEY_DATE = "date";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_REMARK = "remark";
    private static final String KEY_TAGS = "tags";
    private static final String KEY_IS_IMPORTANT = "is_important";

    private static final String KEY_ACC_NAME = "name";
    private static final String KEY_ACC_ICON = "icon";
    private static final String KEY_ACC_BALANCE = "balance";
    private static final String KEY_ACC_INIT_BALANCE = "init_balance";
    private static final String KEY_ACC_REMARK = "remark";
    private static final String KEY_ACC_TYPE = "type";

    private static final String KEY_CAT_NAME = "name";
    private static final String KEY_CAT_ICON = "icon";
    private static final String KEY_CAT_COLOR = "color";
    private static final String KEY_CAT_TYPE = "type";
    private static final String KEY_CAT_PARENT_ID = "parent_id";

    private static final String KEY_PROJ_NAME = "name";
    private static final String KEY_PROJ_DESC = "description";
    private static final String KEY_PROJ_TIME = "create_time";

    private static final String KEY_BUDGET_MONTH = "year_month";
    private static final String KEY_BUDGET_AMOUNT = "amount";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createCategoriesTable = "CREATE TABLE " + TABLE_CATEGORIES + " ("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_CAT_NAME + " TEXT NOT NULL, "
                + KEY_CAT_ICON + " TEXT NOT NULL, "
                + KEY_CAT_COLOR + " INTEGER NOT NULL, "
                + KEY_CAT_TYPE + " INTEGER NOT NULL, "
                + KEY_CAT_PARENT_ID + " INTEGER DEFAULT 0)";
        db.execSQL(createCategoriesTable);

        String createRecordsTable = "CREATE TABLE " + TABLE_RECORDS + " ("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_TYPE + " INTEGER NOT NULL, "
                + KEY_AMOUNT + " REAL NOT NULL, "
                + KEY_CATEGORY_ID + " INTEGER NOT NULL, "
                + KEY_PROJECT_ID + " INTEGER DEFAULT 0, "
                + KEY_ACCOUNT_ID + " INTEGER DEFAULT 1, "
                + KEY_DATE + " TEXT NOT NULL, "
                + KEY_TIMESTAMP + " INTEGER NOT NULL, "
                + KEY_REMARK + " TEXT, "
                + KEY_TAGS + " TEXT, "
                + KEY_IS_IMPORTANT + " INTEGER DEFAULT 0)";
        db.execSQL(createRecordsTable);

        String createProjectsTable = "CREATE TABLE " + TABLE_PROJECTS + " ("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_PROJ_NAME + " TEXT NOT NULL, "
                + KEY_PROJ_DESC + " TEXT, "
                + KEY_PROJ_TIME + " INTEGER NOT NULL)";
        db.execSQL(createProjectsTable);

        String createBudgetsTable = "CREATE TABLE " + TABLE_BUDGETS + " ("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_BUDGET_MONTH + " TEXT UNIQUE NOT NULL, "
                + KEY_BUDGET_AMOUNT + " REAL NOT NULL)";
        db.execSQL(createBudgetsTable);

        String createAccountsTable = "CREATE TABLE " + TABLE_ACCOUNTS + " ("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_ACC_NAME + " TEXT NOT NULL, "
                + KEY_ACC_ICON + " TEXT NOT NULL, "
                + KEY_ACC_TYPE + " INTEGER NOT NULL, "
                + KEY_ACC_INIT_BALANCE + " REAL NOT NULL DEFAULT 0, "
                + KEY_ACC_BALANCE + " REAL NOT NULL DEFAULT 0, "
                + KEY_ACC_REMARK + " TEXT)";
        db.execSQL(createAccountsTable);

        initDefaultCategories(db);
        initDefaultAccounts(db);
    }

    private void initDefaultCategories(SQLiteDatabase db) {
        // 4.2 版本：全部降饱和度，莫兰迪/马卡龙色系，与奶茶棕主色调协调
        addCategory(db, new Category("餐饮", "🍜", 0xFFE8B59A, Record.TYPE_EXPENSE));  // 低饱和暖橘
        addCategory(db, new Category("购物", "🛒", 0xFFDBA9A0, Record.TYPE_EXPENSE));  // 低饱和豆沙红
        addCategory(db, new Category("日用", "🧴", 0xFFB5A8B8, Record.TYPE_EXPENSE));  // 低饱和灰紫
        addCategory(db, new Category("交通", "🚗", 0xFFA9BBC4, Record.TYPE_EXPENSE));  // 低饱和灰蓝
        addCategory(db, new Category("蔬菜", "🥬", 0xFFA8BBA0, Record.TYPE_EXPENSE));  // 低饱和抹茶绿
        addCategory(db, new Category("水果", "🍎", 0xFFE0A8A0, Record.TYPE_EXPENSE));  // 低饱和苹果红
        addCategory(db, new Category("零食", "🍪", 0xFFD4C9A8, Record.TYPE_EXPENSE));  // 低饱和奶黄
        addCategory(db, new Category("运动", "⚽", 0xFFA8C4C4, Record.TYPE_EXPENSE));  // 低饱和灰青
        addCategory(db, new Category("娱乐", "🎮", 0xFFB5A8C4, Record.TYPE_EXPENSE));  // 低饱和灰紫
        addCategory(db, new Category("通讯", "📱", 0xFFA8B5C4, Record.TYPE_EXPENSE));  // 低饱和雾蓝
        addCategory(db, new Category("服饰", "👕", 0xFFD4A8B5, Record.TYPE_EXPENSE));  // 低饱和玫红
        addCategory(db, new Category("美容", "💄", 0xFFE0A8B8, Record.TYPE_EXPENSE));  // 低饱和粉
        addCategory(db, new Category("住房", "🏠", 0xFFB8A690, Record.TYPE_EXPENSE));  // 低饱和暖棕
        addCategory(db, new Category("居家", "🛋️", 0xFFC4B5A0, Record.TYPE_EXPENSE)); // 低饱和米棕
        addCategory(db, new Category("孩子", "👶", 0xFFA8C0D4, Record.TYPE_EXPENSE));  // 低饱和粉蓝
        addCategory(db, new Category("长辈", "👴", 0xFFB0B8C0, Record.TYPE_EXPENSE));  // 低饱和灰
        addCategory(db, new Category("社交", "👥", 0xFFBBA8C4, Record.TYPE_EXPENSE));  // 低饱和紫灰
        addCategory(db, new Category("旅行", "✈️", 0xFFA8C4D4, Record.TYPE_EXPENSE));  // 低饱和天蓝
        addCategory(db, new Category("烟酒", "🚬", 0xFFA89080, Record.TYPE_EXPENSE));  // 低饱和深棕
        addCategory(db, new Category("数码", "💻", 0xFFA8B8B0, Record.TYPE_EXPENSE));  // 低饱和灰绿
        addCategory(db, new Category("汽车", "🚙", 0xFF9AA8B0, Record.TYPE_EXPENSE));  // 低饱和蓝灰
        addCategory(db, new Category("医疗", "💊", 0xFFE0A8A8, Record.TYPE_EXPENSE));  // 低饱和柔红
        addCategory(db, new Category("书籍", "📚", 0xFFA8A8C4, Record.TYPE_EXPENSE));  // 低饱和蓝紫
        addCategory(db, new Category("学习", "✏️", 0xFFA8C0A8, Record.TYPE_EXPENSE));  // 低饱和草绿
        addCategory(db, new Category("宠物", "🐱", 0xFFD4BC8C, Record.TYPE_EXPENSE));  // 低饱和奶橙
        addCategory(db, new Category("礼金", "🧧", 0xFFD8A8A0, Record.TYPE_EXPENSE));  // 低饱和红
        addCategory(db, new Category("礼物", "🎁", 0xFFD4A8B8, Record.TYPE_EXPENSE));  // 低饱和粉红
        addCategory(db, new Category("办公", "📎", 0xFFB0B8C0, Record.TYPE_EXPENSE));  // 低饱和灰
        addCategory(db, new Category("维修", "🔧", 0xFFD4A890, Record.TYPE_EXPENSE));  // 低饱和橙
        addCategory(db, new Category("捐赠", "❤️", 0xFFE0B5B5, Record.TYPE_EXPENSE));  // 低饱和浅红
        addCategory(db, new Category("彩票", "🎰", 0xFFC4A8C4, Record.TYPE_EXPENSE));  // 低饱和紫
        addCategory(db, new Category("亲友", "👨‍👩‍👧", 0xFFA8C4C0, Record.TYPE_EXPENSE)); // 低饱和青
        addCategory(db, new Category("快递", "📦", 0xFFC4B098, Record.TYPE_EXPENSE));  // 低饱和卡其
        addCategory(db, new Category("其他", "📋", 0xFFB0A8A0, Record.TYPE_EXPENSE));  // 低饱和暖灰

        addCategory(db, new Category("工资", "💰", 0xFFA8C0A8, Record.TYPE_INCOME));   // 低饱和绿
        addCategory(db, new Category("奖金", "🏆", 0xFFC4D4A8, Record.TYPE_INCOME));   // 低饱和黄绿
        addCategory(db, new Category("投资", "📈", 0xFFA8C0B8, Record.TYPE_INCOME));   // 低饱和青绿
        addCategory(db, new Category("兼职", "💼", 0xFFA8B8C4, Record.TYPE_INCOME));   // 低饱和蓝
        addCategory(db, new Category("红包", "🧧", 0xFFE0A890, Record.TYPE_INCOME));   // 低饱和橙红
        addCategory(db, new Category("其他收入", "💵", 0xFFA8C4C4, Record.TYPE_INCOME));// 低饱和青
    }

    private long addCategory(SQLiteDatabase db, Category category) {
        ContentValues values = new ContentValues();
        values.put(KEY_CAT_NAME, category.getName());
        values.put(KEY_CAT_ICON, category.getIcon());
        values.put(KEY_CAT_COLOR, category.getColor());
        values.put(KEY_CAT_TYPE, category.getType());
        values.put(KEY_CAT_PARENT_ID, category.getParentId());
        return db.insert(TABLE_CATEGORIES, null, values);
    }

    public long addCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();
        return addCategory(db, category);
    }

    public void updateCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CAT_NAME, category.getName());
        values.put(KEY_CAT_ICON, category.getIcon());
        values.put(KEY_CAT_COLOR, category.getColor());
        values.put(KEY_CAT_TYPE, category.getType());
        values.put(KEY_CAT_PARENT_ID, category.getParentId());
        db.update(TABLE_CATEGORIES, values, KEY_ID + " = ?", new String[]{String.valueOf(category.getId())});
    }

    public List<Category> getCategoriesByParent(long parentId, int type) {
        List<Category> categories = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CATEGORIES
                + " WHERE " + KEY_CAT_TYPE + " = ? AND " + KEY_CAT_PARENT_ID + " = ?"
                + " ORDER BY " + KEY_ID;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(type), String.valueOf(parentId)});
        if (cursor.moveToFirst()) {
            do {
                Category category = new Category();
                category.setId(cursor.getLong(0));
                category.setName(cursor.getString(1));
                category.setIcon(cursor.getString(2));
                category.setColor(cursor.getInt(3));
                category.setType(cursor.getInt(4));
                category.setParentId(cursor.getInt(5));
                categories.add(category);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return categories;
    }

    private void initDefaultAccounts(SQLiteDatabase db) {
        addAccount(db, new Account("现金", "💵", Account.TYPE_CASH, 0, ""));
        addAccount(db, new Account("银行卡", "🏦", Account.TYPE_BANK, 0, ""));
        addAccount(db, new Account("支付宝", "📱", Account.TYPE_ALIPAY, 0, ""));
        addAccount(db, new Account("微信", "💬", Account.TYPE_WECHAT, 0, ""));
    }

    private long addAccount(SQLiteDatabase db, Account account) {
        ContentValues values = new ContentValues();
        values.put(KEY_ACC_NAME, account.getName());
        values.put(KEY_ACC_ICON, account.getIcon());
        values.put(KEY_ACC_TYPE, account.getType());
        values.put(KEY_ACC_INIT_BALANCE, account.getInitBalance());
        values.put(KEY_ACC_BALANCE, account.getInitBalance());
        values.put(KEY_ACC_REMARK, account.getRemark());
        return db.insert(TABLE_ACCOUNTS, null, values);
    }

    public long addAccount(Account account) {
        SQLiteDatabase db = this.getWritableDatabase();
        return addAccount(db, account);
    }

    public List<Account> getAllAccounts() {
        List<Account> accounts = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_ACCOUNTS + " ORDER BY " + KEY_ID;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                Account acc = new Account();
                acc.setId(cursor.getLong(0));
                acc.setName(cursor.getString(1));
                acc.setIcon(cursor.getString(2));
                acc.setType(cursor.getInt(3));
                acc.setInitBalance(cursor.getDouble(4));
                acc.setBalance(cursor.getDouble(5));
                acc.setRemark(cursor.getString(6));
                accounts.add(acc);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return accounts;
    }

    public Account getAccount(long id) {
        String query = "SELECT * FROM " + TABLE_ACCOUNTS + " WHERE " + KEY_ID + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(id)});
        Account acc = null;
        if (cursor.moveToFirst()) {
            acc = new Account();
            acc.setId(cursor.getLong(0));
            acc.setName(cursor.getString(1));
            acc.setIcon(cursor.getString(2));
            acc.setType(cursor.getInt(3));
            acc.setInitBalance(cursor.getDouble(4));
            acc.setBalance(cursor.getDouble(5));
            acc.setRemark(cursor.getString(6));
        }
        cursor.close();
        return acc;
    }

    public void updateAccountBalance(long accountId, double delta) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_ACCOUNTS + " SET " + KEY_ACC_BALANCE + " = " + KEY_ACC_BALANCE + " + ? WHERE " + KEY_ID + " = ?",
                new Object[]{delta, accountId});
    }

    public void deleteAccount(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ACCOUNT_ID, 1);
        db.update(TABLE_RECORDS, values, KEY_ACCOUNT_ID + " = ?", new String[]{String.valueOf(id)});
        db.delete(TABLE_ACCOUNTS, KEY_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void updateAccount(Account account) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ACC_NAME, account.getName());
        values.put(KEY_ACC_ICON, account.getIcon());
        values.put(KEY_ACC_TYPE, account.getType());
        values.put(KEY_ACC_INIT_BALANCE, account.getInitBalance());
        values.put(KEY_ACC_REMARK, account.getRemark());
        db.update(TABLE_ACCOUNTS, values, KEY_ID + " = ?", new String[]{String.valueOf(account.getId())});
    }

    public List<Category> getCategories(int type) {
        List<Category> categories = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CATEGORIES + " WHERE " + KEY_CAT_TYPE + " = ? ORDER BY " + KEY_ID;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(type)});

        if (cursor.moveToFirst()) {
            do {
                Category category = new Category();
                category.setId(cursor.getLong(0));
                category.setName(cursor.getString(1));
                category.setIcon(cursor.getString(2));
                category.setColor(cursor.getInt(3));
                category.setType(cursor.getInt(4));
                category.setParentId(cursor.getInt(5));
                categories.add(category);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return categories;
    }

    public void deleteCategory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 删除该分类及其所有子分类
        db.delete(TABLE_CATEGORIES, KEY_ID + " = ? OR " + KEY_CAT_PARENT_ID + " = ?",
                new String[]{String.valueOf(id), String.valueOf(id)});
    }

    public long addRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TYPE, record.getType());
        values.put(KEY_AMOUNT, record.getAmount());
        values.put(KEY_CATEGORY_ID, record.getCategoryId());
        values.put(KEY_PROJECT_ID, record.getProjectId());
        values.put(KEY_ACCOUNT_ID, record.getAccountId());
        values.put(KEY_DATE, record.getDate());
        values.put(KEY_TIMESTAMP, record.getTimestamp());
        values.put(KEY_REMARK, record.getRemark());
        values.put(KEY_TAGS, record.getTags());
        values.put(KEY_IS_IMPORTANT, record.isImportant() ? 1 : 0);
        long id = db.insert(TABLE_RECORDS, null, values);

        // 更新账户余额
        if (record.getAccountId() > 0) {
            double delta = record.getType() == Record.TYPE_EXPENSE ? -record.getAmount() : record.getAmount();
            updateAccountBalance(record.getAccountId(), delta);
        }
        return id;
    }

    public void updateRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 查询原记录以回滚账户余额
        Cursor cursor = db.rawQuery("SELECT " + KEY_TYPE + "," + KEY_AMOUNT + "," + KEY_ACCOUNT_ID + " FROM " + TABLE_RECORDS + " WHERE " + KEY_ID + " = ?",
                new String[]{String.valueOf(record.getId())});
        if (cursor.moveToFirst()) {
            int oldType = cursor.getInt(0);
            double oldAmount = cursor.getDouble(1);
            long oldAccountId = cursor.getLong(2);
            if (oldAccountId > 0) {
                double delta = oldType == Record.TYPE_EXPENSE ? oldAmount : -oldAmount;
                updateAccountBalance(oldAccountId, delta);
            }
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put(KEY_TYPE, record.getType());
        values.put(KEY_AMOUNT, record.getAmount());
        values.put(KEY_CATEGORY_ID, record.getCategoryId());
        values.put(KEY_PROJECT_ID, record.getProjectId());
        values.put(KEY_ACCOUNT_ID, record.getAccountId());
        values.put(KEY_DATE, record.getDate());
        values.put(KEY_TIMESTAMP, record.getTimestamp());
        values.put(KEY_REMARK, record.getRemark());
        values.put(KEY_TAGS, record.getTags());
        values.put(KEY_IS_IMPORTANT, record.isImportant() ? 1 : 0);
        db.update(TABLE_RECORDS, values, KEY_ID + " = ?", new String[]{String.valueOf(record.getId())});

        // 应用新账户余额变动
        if (record.getAccountId() > 0) {
            double delta = record.getType() == Record.TYPE_EXPENSE ? -record.getAmount() : record.getAmount();
            updateAccountBalance(record.getAccountId(), delta);
        }
    }

    public void setRecordImportant(long recordId, boolean important) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_IS_IMPORTANT, important ? 1 : 0);
        db.update(TABLE_RECORDS, values, KEY_ID + " = ?", new String[]{String.valueOf(recordId)});
    }

    public Record getRecord(long id) {
        String selectQuery = "SELECT r.*, c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", p." + KEY_PROJ_NAME
                + ", a." + KEY_ACC_NAME + ", a." + KEY_ACC_ICON
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " LEFT JOIN " + TABLE_PROJECTS + " p ON r." + KEY_PROJECT_ID + " = p." + KEY_ID
                + " LEFT JOIN " + TABLE_ACCOUNTS + " a ON r." + KEY_ACCOUNT_ID + " = a." + KEY_ID
                + " WHERE r." + KEY_ID + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(id)});
        Record record = null;
        if (cursor.moveToFirst()) {
            record = cursorToRecord(cursor);
            fillJoinFields(record, cursor);
        }
        cursor.close();
        return record;
    }

    public List<Record> getRecordsByDate(String date) {
        List<Record> records = new ArrayList<>();
        String selectQuery = "SELECT r.*, c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", p." + KEY_PROJ_NAME
                + ", a." + KEY_ACC_NAME + ", a." + KEY_ACC_ICON
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " LEFT JOIN " + TABLE_PROJECTS + " p ON r." + KEY_PROJECT_ID + " = p." + KEY_ID
                + " LEFT JOIN " + TABLE_ACCOUNTS + " a ON r." + KEY_ACCOUNT_ID + " = a." + KEY_ID
                + " WHERE r." + KEY_DATE + " = ?"
                + " ORDER BY r." + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{date});
        if (cursor.moveToFirst()) {
            do {
                Record record = cursorToRecord(cursor);
                fillJoinFields(record, cursor);
                records.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }

    public List<Record> getRecordsByYear(int year) {
        List<Record> records = new ArrayList<>();
        String selectQuery = "SELECT r.*, c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", p." + KEY_PROJ_NAME
                + ", a." + KEY_ACC_NAME + ", a." + KEY_ACC_ICON
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " LEFT JOIN " + TABLE_PROJECTS + " p ON r." + KEY_PROJECT_ID + " = p." + KEY_ID
                + " LEFT JOIN " + TABLE_ACCOUNTS + " a ON r." + KEY_ACCOUNT_ID + " = a." + KEY_ID
                + " WHERE r." + KEY_DATE + " LIKE ?"
                + " ORDER BY r." + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{year + "%"});
        if (cursor.moveToFirst()) {
            do {
                Record record = cursorToRecord(cursor);
                fillJoinFields(record, cursor);
                records.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }

    /** 4.5 多维度透视：行=月份，列=分类，值=支出金额合计 */
    public java.util.Map<String, java.util.Map<String, Double>> getPivotData(int year, int type) {
        java.util.Map<String, java.util.Map<String, Double>> pivot = new java.util.TreeMap<>();
        String query = "SELECT r." + KEY_DATE + ", c." + KEY_CAT_NAME + ", SUM(r." + KEY_AMOUNT + ") as total"
                + " FROM " + TABLE_RECORDS + " r"
                + " JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " WHERE r." + KEY_DATE + " LIKE ? AND r." + KEY_TYPE + " = ?"
                + " GROUP BY r." + KEY_DATE + ", c." + KEY_CAT_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{year + "%", String.valueOf(type)});
        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(0);
                String catName = cursor.getString(1);
                double total = cursor.getDouble(2);
                // 转为 "yyyy-MM"
                String month = date.length() >= 7 ? date.substring(0, 7) : date;
                java.util.Map<String, Double> row = pivot.computeIfAbsent(month, k -> new java.util.HashMap<>());
                row.merge(catName, total, Double::sum);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return pivot;
    }

    /** 4.5 年度热力图数据：日期 -> 支出金额 */
    public java.util.Map<String, Double> getDailyExpenseForYear(int year) {
        java.util.Map<String, Double> result = new java.util.HashMap<>();
        String query = "SELECT " + KEY_DATE + ", SUM(" + KEY_AMOUNT + ") FROM " + TABLE_RECORDS
                + " WHERE " + KEY_DATE + " LIKE ? AND " + KEY_TYPE + " = ?"
                + " GROUP BY " + KEY_DATE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{year + "%", String.valueOf(Record.TYPE_EXPENSE)});
        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(0);
                double total = cursor.getDouble(1);
                result.put(date, total);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    /** 4.5 最近使用分类（按使用频率） */
    public List<Category> getRecentCategories(int type, int limit) {
        List<Category> result = new ArrayList<>();
        String query = "SELECT c.*, COUNT(r." + KEY_ID + ") as cnt, MAX(r." + KEY_TIMESTAMP + ") as last"
                + " FROM " + TABLE_CATEGORIES + " c"
                + " JOIN " + TABLE_RECORDS + " r ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " WHERE c." + KEY_CAT_TYPE + " = ?"
                + " GROUP BY c." + KEY_ID
                + " ORDER BY last DESC"
                + " LIMIT ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(type), String.valueOf(limit)});
        if (cursor.moveToFirst()) {
            do {
                Category category = new Category();
                category.setId(cursor.getLong(0));
                category.setName(cursor.getString(1));
                category.setIcon(cursor.getString(2));
                category.setColor(cursor.getInt(3));
                category.setType(cursor.getInt(4));
                category.setParentId(cursor.getInt(5));
                result.add(category);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    public void transfer(long fromAccountId, long toAccountId, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_ACCOUNTS + " SET " + KEY_ACC_BALANCE + " = " + KEY_ACC_BALANCE + " - ? WHERE " + KEY_ID + " = ?",
                new Object[]{amount, fromAccountId});
        db.execSQL("UPDATE " + TABLE_ACCOUNTS + " SET " + KEY_ACC_BALANCE + " = " + KEY_ACC_BALANCE + " + ? WHERE " + KEY_ID + " = ?",
                new Object[]{amount, toAccountId});
    }

    private Record cursorToRecord(Cursor cursor) {
        Record record = new Record();
        record.setId(cursor.getLong(0));
        record.setType(cursor.getInt(1));
        record.setAmount(cursor.getDouble(2));
        record.setCategoryId(cursor.getLong(3));
        record.setProjectId(cursor.getLong(4));
        record.setAccountId(cursor.getLong(5));
        record.setDate(cursor.getString(6));
        record.setTimestamp(cursor.getLong(7));
        record.setRemark(cursor.getString(8));
        record.setTags(cursor.getString(9));
        if (cursor.getColumnCount() > 10 && cursor.getColumnIndex(KEY_IS_IMPORTANT) >= 0) {
            record.setImportant(cursor.getInt(cursor.getColumnIndex(KEY_IS_IMPORTANT)) == 1);
        }
        return record;
    }

    private void fillJoinFields(Record record, Cursor cursor) {
        // r.* 包含 11 列：id,type,amount,category_id,project_id,account_id,date,timestamp,remark,tags,is_important
        // 后续 join 字段从索引 11 开始
        int baseIdx = 11;
        record.setCategoryName(cursor.getString(baseIdx));
        record.setCategoryIcon(cursor.getString(baseIdx + 1));
        record.setCategoryColor(cursor.getInt(baseIdx + 2));
        record.setProjectName(cursor.getString(baseIdx + 3));
        record.setAccountName(cursor.getString(baseIdx + 4));
        record.setAccountIcon(cursor.getString(baseIdx + 5));
    }

    public List<Record> getAllRecords() {
        List<Record> records = new ArrayList<>();
        String selectQuery = "SELECT r.*, c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", p." + KEY_PROJ_NAME
                + ", a." + KEY_ACC_NAME + ", a." + KEY_ACC_ICON
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " LEFT JOIN " + TABLE_PROJECTS + " p ON r." + KEY_PROJECT_ID + " = p." + KEY_ID
                + " LEFT JOIN " + TABLE_ACCOUNTS + " a ON r." + KEY_ACCOUNT_ID + " = a." + KEY_ID
                + " ORDER BY r." + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Record record = cursorToRecord(cursor);
                fillJoinFields(record, cursor);
                records.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }

    public List<Record> getRecordsByMonth(String yearMonth) {
        List<Record> records = new ArrayList<>();
        String selectQuery = "SELECT r.*, c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", p." + KEY_PROJ_NAME
                + ", a." + KEY_ACC_NAME + ", a." + KEY_ACC_ICON
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " LEFT JOIN " + TABLE_PROJECTS + " p ON r." + KEY_PROJECT_ID + " = p." + KEY_ID
                + " LEFT JOIN " + TABLE_ACCOUNTS + " a ON r." + KEY_ACCOUNT_ID + " = a." + KEY_ID
                + " WHERE r." + KEY_DATE + " LIKE ?"
                + " ORDER BY r." + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{yearMonth + "%"});

        if (cursor.moveToFirst()) {
            do {
                Record record = cursorToRecord(cursor);
                fillJoinFields(record, cursor);
                records.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }

    public List<Record> getRecordsByProject(long projectId) {
        List<Record> records = new ArrayList<>();
        String selectQuery = "SELECT r.*, c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", p." + KEY_PROJ_NAME
                + ", a." + KEY_ACC_NAME + ", a." + KEY_ACC_ICON
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " LEFT JOIN " + TABLE_PROJECTS + " p ON r." + KEY_PROJECT_ID + " = p." + KEY_ID
                + " LEFT JOIN " + TABLE_ACCOUNTS + " a ON r." + KEY_ACCOUNT_ID + " = a." + KEY_ID
                + " WHERE r." + KEY_PROJECT_ID + " = ?"
                + " ORDER BY r." + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(projectId)});

        if (cursor.moveToFirst()) {
            do {
                Record record = cursorToRecord(cursor);
                fillJoinFields(record, cursor);
                records.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }

    public List<Record> searchRecords(String keyword) {
        List<Record> records = new ArrayList<>();
        String selectQuery = "SELECT r.*, c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", p." + KEY_PROJ_NAME
                + ", a." + KEY_ACC_NAME + ", a." + KEY_ACC_ICON
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " LEFT JOIN " + TABLE_PROJECTS + " p ON r." + KEY_PROJECT_ID + " = p." + KEY_ID
                + " LEFT JOIN " + TABLE_ACCOUNTS + " a ON r." + KEY_ACCOUNT_ID + " = a." + KEY_ID
                + " WHERE r." + KEY_REMARK + " LIKE ? OR r." + KEY_TAGS + " LIKE ?"
                + " OR c." + KEY_CAT_NAME + " LIKE ? OR p." + KEY_PROJ_NAME + " LIKE ?"
                + " OR a." + KEY_ACC_NAME + " LIKE ?"
                + " ORDER BY r." + KEY_TIMESTAMP + " DESC";
        String like = "%" + keyword + "%";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{like, like, like, like, like});

        if (cursor.moveToFirst()) {
            do {
                Record record = cursorToRecord(cursor);
                fillJoinFields(record, cursor);
                records.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }

    public double getMonthlyExpense(String yearMonth) {
        return getSum(yearMonth + "%", Record.TYPE_EXPENSE, -1);
    }

    public double getMonthlyIncome(String yearMonth) {
        return getSum(yearMonth + "%", Record.TYPE_INCOME, -1);
    }

    public double getProjectExpense(long projectId) {
        return getSum(null, Record.TYPE_EXPENSE, projectId);
    }

    /** 5.6 新增：获取项目总收入 */
    public double getProjectIncome(long projectId) {
        return getSum(null, Record.TYPE_INCOME, projectId);
    }

    private double getSum(String dateLike, int type, long projectId) {
        StringBuilder query = new StringBuilder("SELECT SUM(" + KEY_AMOUNT + ") FROM " + TABLE_RECORDS + " WHERE " + KEY_TYPE + " = ?");
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(type));
        if (dateLike != null) {
            query.append(" AND " + KEY_DATE + " LIKE ?");
            args.add(dateLike);
        }
        if (projectId >= 0) {
            query.append(" AND " + KEY_PROJECT_ID + " = ?");
            args.add(String.valueOf(projectId));
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));
        double sum = 0;
        if (cursor.moveToFirst()) {
            sum = cursor.getDouble(0);
        }
        cursor.close();
        return sum;
    }

    public List<DailySum> getDailySumForWeek(Calendar weekStart) {
        List<DailySum> result = new ArrayList<>();
        Calendar cal = (Calendar) weekStart.clone();
        for (int i = 0; i < 7; i++) {
            String date = String.format("%04d-%02d-%02d",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            DailySum ds = new DailySum();
            ds.date = date;
            ds.expense = getDaySum(date, Record.TYPE_EXPENSE);
            ds.income = getDaySum(date, Record.TYPE_INCOME);
            result.add(ds);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return result;
    }

    private double getDaySum(String date, int type) {
        String query = "SELECT SUM(" + KEY_AMOUNT + ") FROM " + TABLE_RECORDS + " WHERE " + KEY_DATE + " = ? AND " + KEY_TYPE + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{date, String.valueOf(type)});
        double sum = 0;
        if (cursor.moveToFirst()) {
            sum = cursor.getDouble(0);
        }
        cursor.close();
        return sum;
    }

    public List<MonthlySum> getMonthlySumForYear(int year) {
        List<MonthlySum> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String ym = String.format("%04d-%02d", year, m);
            MonthlySum ms = new MonthlySum();
            ms.yearMonth = ym;
            ms.month = m;
            ms.expense = getMonthlyExpense(ym);
            ms.income = getMonthlyIncome(ym);
            result.add(ms);
        }
        return result;
    }

    public List<YearlySum> getYearlySum(int startYear, int endYear) {
        List<YearlySum> result = new ArrayList<>();
        for (int y = startYear; y <= endYear; y++) {
            YearlySum ys = new YearlySum();
            ys.year = y;
            ys.expense = getYearlySumByType(y, Record.TYPE_EXPENSE);
            ys.income = getYearlySumByType(y, Record.TYPE_INCOME);
            result.add(ys);
        }
        return result;
    }

    private double getYearlySumByType(int year, int type) {
        String query = "SELECT SUM(" + KEY_AMOUNT + ") FROM " + TABLE_RECORDS + " WHERE " + KEY_DATE + " LIKE ? AND " + KEY_TYPE + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{year + "%", String.valueOf(type)});
        double sum = 0;
        if (cursor.moveToFirst()) {
            sum = cursor.getDouble(0);
        }
        cursor.close();
        return sum;
    }

    public void deleteRecord(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_TYPE + "," + KEY_AMOUNT + "," + KEY_ACCOUNT_ID + " FROM " + TABLE_RECORDS + " WHERE " + KEY_ID + " = ?", new String[]{String.valueOf(id)});
        if (cursor.moveToFirst()) {
            int type = cursor.getInt(0);
            double amount = cursor.getDouble(1);
            long accountId = cursor.getLong(2);
            if (accountId > 0) {
                double delta = type == Record.TYPE_EXPENSE ? amount : -amount;
                updateAccountBalance(accountId, delta);
            }
        }
        cursor.close();
        db.delete(TABLE_RECORDS, KEY_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<CategoryStat> getCategoryStats(String datePattern, int type) {
        List<CategoryStat> stats = new ArrayList<>();
        String query = "SELECT c." + KEY_ID + ", c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", SUM(r." + KEY_AMOUNT + ") as total"
                + " FROM " + TABLE_RECORDS + " r"
                + " JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " WHERE r." + KEY_DATE + " LIKE ? AND r." + KEY_TYPE + " = ?"
                + " GROUP BY c." + KEY_ID
                + " ORDER BY total DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{datePattern, String.valueOf(type)});

        if (cursor.moveToFirst()) {
            do {
                CategoryStat stat = new CategoryStat();
                stat.setCategoryId(cursor.getLong(0));
                stat.setCategoryName(cursor.getString(1));
                stat.setCategoryIcon(cursor.getString(2));
                stat.setCategoryColor(cursor.getInt(3));
                stat.setTotal(cursor.getDouble(4));
                stats.add(stat);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return stats;
    }

    /**
     * 5.4 新增：按日期范围查询分类统计（用于周视图饼状图）
     * 解决原 getCategoryStats 用 LIKE 'yyyy-MM-dd%' 只匹配单日的问题
     */
    public List<CategoryStat> getCategoryStatsForDateRange(String startDate, String endDate, int type) {
        List<CategoryStat> stats = new ArrayList<>();
        String query = "SELECT c." + KEY_ID + ", c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", SUM(r." + KEY_AMOUNT + ") as total"
                + " FROM " + TABLE_RECORDS + " r"
                + " JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " WHERE r." + KEY_DATE + " >= ? AND r." + KEY_DATE + " <= ? AND r." + KEY_TYPE + " = ?"
                + " GROUP BY c." + KEY_ID
                + " ORDER BY total DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{startDate, endDate, String.valueOf(type)});

        if (cursor.moveToFirst()) {
            do {
                CategoryStat stat = new CategoryStat();
                stat.setCategoryId(cursor.getLong(0));
                stat.setCategoryName(cursor.getString(1));
                stat.setCategoryIcon(cursor.getString(2));
                stat.setCategoryColor(cursor.getInt(3));
                stat.setTotal(cursor.getDouble(4));
                stats.add(stat);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return stats;
    }

    /**
     * 5.6 新增：按日期范围查询账单明细
     */
    public List<Record> getRecordsByDateRange(String startDate, String endDate) {
        List<Record> records = new ArrayList<>();
        String selectQuery = "SELECT r.*, c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", p." + KEY_PROJ_NAME
                + ", a." + KEY_ACC_NAME + ", a." + KEY_ACC_ICON
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " LEFT JOIN " + TABLE_PROJECTS + " p ON r." + KEY_PROJECT_ID + " = p." + KEY_ID
                + " LEFT JOIN " + TABLE_ACCOUNTS + " a ON r." + KEY_ACCOUNT_ID + " = a." + KEY_ID
                + " WHERE r." + KEY_DATE + " >= ? AND r." + KEY_DATE + " <= ?"
                + " ORDER BY r." + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{startDate, endDate});
        if (cursor.moveToFirst()) {
            do {
                Record record = cursorToRecord(cursor);
                fillJoinFields(record, cursor);
                records.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }

    /**
     * 5.7 新增：按账户 + 可选日期范围 + 可选项目 + 可选标签 组合查询账单明细
     * 任意条件传 null/0/空字符串表示不限制
     */
    public List<Record> getRecordsByFilter(Long accountId, String startDate, String endDate,
                                           Long projectId, String tag) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT r.*, c.").append(KEY_CAT_NAME).append(", c.").append(KEY_CAT_ICON).append(", c.").append(KEY_CAT_COLOR)
                .append(", p.").append(KEY_PROJ_NAME)
                .append(", a.").append(KEY_ACC_NAME).append(", a.").append(KEY_ACC_ICON)
                .append(" FROM ").append(TABLE_RECORDS).append(" r")
                .append(" LEFT JOIN ").append(TABLE_CATEGORIES).append(" c ON r.").append(KEY_CATEGORY_ID).append(" = c.").append(KEY_ID)
                .append(" LEFT JOIN ").append(TABLE_PROJECTS).append(" p ON r.").append(KEY_PROJECT_ID).append(" = p.").append(KEY_ID)
                .append(" LEFT JOIN ").append(TABLE_ACCOUNTS).append(" a ON r.").append(KEY_ACCOUNT_ID).append(" = a.").append(KEY_ID)
                .append(" WHERE 1=1");

        List<String> args = new ArrayList<>();
        if (accountId != null && accountId > 0) {
            query.append(" AND r.").append(KEY_ACCOUNT_ID).append(" = ?");
            args.add(String.valueOf(accountId));
        }
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            query.append(" AND r.").append(KEY_DATE).append(" >= ? AND r.").append(KEY_DATE).append(" <= ?");
            args.add(startDate);
            args.add(endDate);
        }
        if (projectId != null && projectId > 0) {
            query.append(" AND r.").append(KEY_PROJECT_ID).append(" = ?");
            args.add(String.valueOf(projectId));
        }
        if (tag != null && !tag.isEmpty()) {
            query.append(" AND r.").append(KEY_TAGS).append(" LIKE ?");
            args.add("%" + tag + "%");
        }
        query.append(" ORDER BY r.").append(KEY_TIMESTAMP).append(" DESC");

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]));
        List<Record> records = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                Record record = cursorToRecord(cursor);
                fillJoinFields(record, cursor);
                records.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }

    /** 5.7 新增：获取所有使用过的标签（去重，按字母序） */
    public List<String> getAllTags() {
        List<String> tags = new ArrayList<>();
        String query = "SELECT DISTINCT " + KEY_TAGS + " FROM " + TABLE_RECORDS
                + " WHERE " + KEY_TAGS + " IS NOT NULL AND " + KEY_TAGS + " != ''";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        if (cursor.moveToFirst()) {
            do {
                String tagsStr = cursor.getString(0);
                if (tagsStr == null || tagsStr.isEmpty()) continue;
                String[] arr = tagsStr.trim().split("\\s+");
                for (String t : arr) {
                    if (!t.isEmpty()) set.add(t);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        tags.addAll(set);
        java.util.Collections.sort(tags);
        return tags;
    }

    /**
     * 5.6 新增：按日期范围 + 项目查询账单明细
     */
    public List<Record> getRecordsByDateRangeAndProject(String startDate, String endDate, long projectId) {
        List<Record> records = new ArrayList<>();
        String selectQuery = "SELECT r.*, c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", p." + KEY_PROJ_NAME
                + ", a." + KEY_ACC_NAME + ", a." + KEY_ACC_ICON
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " LEFT JOIN " + TABLE_PROJECTS + " p ON r." + KEY_PROJECT_ID + " = p." + KEY_ID
                + " LEFT JOIN " + TABLE_ACCOUNTS + " a ON r." + KEY_ACCOUNT_ID + " = a." + KEY_ID
                + " WHERE r." + KEY_DATE + " >= ? AND r." + KEY_DATE + " <= ? AND r." + KEY_PROJECT_ID + " = ?"
                + " ORDER BY r." + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{startDate, endDate, String.valueOf(projectId)});
        if (cursor.moveToFirst()) {
            do {
                Record record = cursorToRecord(cursor);
                fillJoinFields(record, cursor);
                records.add(record);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return records;
    }

    /**
     * 5.6 新增：按日期范围查询每个分类的支出和收入
     * 返回列表每个元素包含分类信息和对应的支出、收入金额
     */
    public List<CategoryStatBoth> getCategoryStatsBothForDateRange(String startDate, String endDate) {
        // 先查出所有有记录的分类 ID
        List<CategoryStatBoth> result = new ArrayList<>();
        String query = "SELECT c." + KEY_ID + ", c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", SUM(CASE WHEN r." + KEY_TYPE + " = " + Record.TYPE_EXPENSE
                + " THEN r." + KEY_AMOUNT + " ELSE 0 END) as expense,"
                + " SUM(CASE WHEN r." + KEY_TYPE + " = " + Record.TYPE_INCOME
                + " THEN r." + KEY_AMOUNT + " ELSE 0 END) as income"
                + " FROM " + TABLE_RECORDS + " r"
                + " JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " WHERE r." + KEY_DATE + " >= ? AND r." + KEY_DATE + " <= ?"
                + " GROUP BY c." + KEY_ID
                + " ORDER BY (expense + income) DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{startDate, endDate});
        if (cursor.moveToFirst()) {
            do {
                CategoryStatBoth stat = new CategoryStatBoth();
                stat.categoryId = cursor.getLong(0);
                stat.categoryName = cursor.getString(1);
                stat.categoryIcon = cursor.getString(2);
                stat.categoryColor = cursor.getInt(3);
                stat.expense = cursor.getDouble(4);
                stat.income = cursor.getDouble(5);
                result.add(stat);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    /**
     * 5.6 新增：按日期范围查询每个标签的支出和收入
     * 标签存储格式为 "#标签1 #标签2" 这样的空格分隔字符串
     */
    public List<TagStat> getTagStatsForDateRange(String startDate, String endDate) {
        // 先查出范围内所有带标签的记录
        java.util.Map<String, TagStat> tagMap = new java.util.LinkedHashMap<>();
        String query = "SELECT r." + KEY_TYPE + ", r." + KEY_AMOUNT + ", r." + KEY_TAGS
                + " FROM " + TABLE_RECORDS + " r"
                + " WHERE r." + KEY_DATE + " >= ? AND r." + KEY_DATE + " <= ?"
                + " AND r." + KEY_TAGS + " IS NOT NULL AND r." + KEY_TAGS + " != ''";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{startDate, endDate});
        if (cursor.moveToFirst()) {
            do {
                int type = cursor.getInt(0);
                double amount = cursor.getDouble(1);
                String tagsStr = cursor.getString(2);
                if (tagsStr == null || tagsStr.isEmpty()) continue;
                // 按空格分隔多个标签
                String[] tags = tagsStr.trim().split("\\s+");
                for (String tag : tags) {
                    if (tag.isEmpty()) continue;
                    String key = tag.startsWith("#") ? tag : "#" + tag;
                    TagStat stat = tagMap.get(key);
                    if (stat == null) {
                        stat = new TagStat();
                        stat.tag = key;
                        stat.expense = 0;
                        stat.income = 0;
                        tagMap.put(key, stat);
                    }
                    if (type == Record.TYPE_EXPENSE) {
                        stat.expense += amount;
                    } else {
                        stat.income += amount;
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        // 转换为列表并按总额降序排序
        List<TagStat> result = new ArrayList<>(tagMap.values());
        java.util.Collections.sort(result, (a, b) ->
                Double.compare(b.expense + b.income, a.expense + a.income));
        return result;
    }

    /** 5.6 新增：分类统计（同时包含支出和收入） */
    public static class CategoryStatBoth {
        public long categoryId;
        public String categoryName;
        public String categoryIcon;
        public int categoryColor;
        public double expense;
        public double income;
    }

    /** 5.6 新增：标签统计 */
    public static class TagStat {
        public String tag;
        public double expense;
        public double income;
    }

    public long addProject(Project project) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PROJ_NAME, project.getName());
        values.put(KEY_PROJ_DESC, project.getDescription());
        values.put(KEY_PROJ_TIME, project.getCreateTime());
        return db.insert(TABLE_PROJECTS, null, values);
    }

    public List<Project> getAllProjects() {
        List<Project> projects = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_PROJECTS + " ORDER BY " + KEY_PROJ_TIME + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                Project p = new Project();
                p.setId(cursor.getLong(0));
                p.setName(cursor.getString(1));
                p.setDescription(cursor.getString(2));
                p.setCreateTime(cursor.getLong(3));
                projects.add(p);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return projects;
    }

    public void deleteProject(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PROJECT_ID, 0);
        db.update(TABLE_RECORDS, values, KEY_PROJECT_ID + " = ?", new String[]{String.valueOf(id)});
        db.delete(TABLE_PROJECTS, KEY_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void setBudget(String yearMonth, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_BUDGET_MONTH, yearMonth);
        values.put(KEY_BUDGET_AMOUNT, amount);
        db.insertWithOnConflict(TABLE_BUDGETS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public double getBudget(String yearMonth) {
        String query = "SELECT " + KEY_BUDGET_AMOUNT + " FROM " + TABLE_BUDGETS + " WHERE " + KEY_BUDGET_MONTH + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{yearMonth});
        double budget = 0;
        if (cursor.moveToFirst()) {
            budget = cursor.getDouble(0);
        }
        cursor.close();
        return budget;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + KEY_PROJECT_ID + " INTEGER DEFAULT 0");
            String createProjectsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_PROJECTS + " ("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + KEY_PROJ_NAME + " TEXT NOT NULL, "
                    + KEY_PROJ_DESC + " TEXT, "
                    + KEY_PROJ_TIME + " INTEGER NOT NULL)";
            db.execSQL(createProjectsTable);
            String createBudgetsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_BUDGETS + " ("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + KEY_BUDGET_MONTH + " TEXT UNIQUE NOT NULL, "
                    + KEY_BUDGET_AMOUNT + " REAL NOT NULL)";
            db.execSQL(createBudgetsTable);
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + KEY_ACCOUNT_ID + " INTEGER DEFAULT 1");
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + KEY_TAGS + " TEXT");
            String createAccountsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_ACCOUNTS + " ("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + KEY_ACC_NAME + " TEXT NOT NULL, "
                    + KEY_ACC_ICON + " TEXT NOT NULL, "
                    + KEY_ACC_TYPE + " INTEGER NOT NULL, "
                    + KEY_ACC_INIT_BALANCE + " REAL NOT NULL DEFAULT 0, "
                    + KEY_ACC_BALANCE + " REAL NOT NULL DEFAULT 0, "
                    + KEY_ACC_REMARK + " TEXT)";
            db.execSQL(createAccountsTable);
            initDefaultAccounts(db);
        }
        if (oldVersion < 4) {
            // 4.2 版本：刷新所有默认分类的颜色为降饱和度莫兰迪色系
            refreshDefaultCategoryColors(db);
        }
        if (oldVersion < 5) {
            // 4.5 版本：无限级分类 + 重要标记
            db.execSQL("ALTER TABLE " + TABLE_CATEGORIES + " ADD COLUMN " + KEY_CAT_PARENT_ID + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + KEY_IS_IMPORTANT + " INTEGER DEFAULT 0");
        }
    }

    private void refreshDefaultCategoryColors(SQLiteDatabase db) {
        // 分类名 -> 新颜色映射（与 initDefaultCategories 保持一致）
        String[][] colorMap = {
                {"餐饮", "FFE8B59A"}, {"购物", "FFDBA9A0"}, {"日用", "FFB5A8B8"},
                {"交通", "FFA9BBC4"}, {"蔬菜", "FFA8BBA0"}, {"水果", "FFE0A8A0"},
                {"零食", "FFD4C9A8"}, {"运动", "FFA8C4C4"}, {"娱乐", "FFB5A8C4"},
                {"通讯", "FFA8B5C4"}, {"服饰", "FFD4A8B5"}, {"美容", "FFE0A8B8"},
                {"住房", "FFB8A690"}, {"居家", "FFC4B5A0"}, {"孩子", "FFA8C0D4"},
                {"长辈", "FFB0B8C0"}, {"社交", "FFBBA8C4"}, {"旅行", "FFA8C4D4"},
                {"烟酒", "FFA89080"}, {"数码", "FFA8B8B0"}, {"汽车", "FF9AA8B0"},
                {"医疗", "FFE0A8A8"}, {"书籍", "FFA8A8C4"}, {"学习", "FFA8C0A8"},
                {"宠物", "FFD4BC8C"}, {"礼金", "FFD8A8A0"}, {"礼物", "FFD4A8B8"},
                {"办公", "FFB0B8C0"}, {"维修", "FFD4A890"}, {"捐赠", "FFE0B5B5"},
                {"彩票", "FFC4A8C4"}, {"亲友", "FFA8C4C0"}, {"快递", "FFC4B098"},
                {"其他", "FFB0A8A0"},
                {"工资", "FFA8C0A8"}, {"奖金", "FFC4D4A8"}, {"投资", "FFA8C0B8"},
                {"兼职", "FFA8B8C4"}, {"红包", "FFE0A890"}, {"其他收入", "FFA8C4C4"}
        };
        for (String[] entry : colorMap) {
            ContentValues values = new ContentValues();
            values.put(KEY_CAT_COLOR, (int) Long.parseLong(entry[1], 16));
            db.update(TABLE_CATEGORIES, values, KEY_CAT_NAME + " = ?", new String[]{entry[0]});
        }
    }

    public static class CategoryStat {
        private long categoryId;
        private String categoryName;
        private String categoryIcon;
        private int categoryColor;
        private double total;

        public long getCategoryId() { return categoryId; }
        public void setCategoryId(long categoryId) { this.categoryId = categoryId; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getCategoryIcon() { return categoryIcon; }
        public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }
        public int getCategoryColor() { return categoryColor; }
        public void setCategoryColor(int categoryColor) { this.categoryColor = categoryColor; }
        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }
    }

    public static class DailySum {
        public String date;
        public double expense;
        public double income;
    }

    public static class MonthlySum {
        public String yearMonth;
        public int month;
        public double expense;
        public double income;
    }

    public static class YearlySum {
        public int year;
        public double expense;
        public double income;
    }
}
