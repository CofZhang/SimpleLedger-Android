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
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_RECORDS = "records";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_PROJECTS = "projects";
    private static final String TABLE_BUDGETS = "budgets";

    private static final String KEY_ID = "id";
    private static final String KEY_TYPE = "type";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_CATEGORY_ID = "category_id";
    private static final String KEY_PROJECT_ID = "project_id";
    private static final String KEY_DATE = "date";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_REMARK = "remark";

    private static final String KEY_CAT_NAME = "name";
    private static final String KEY_CAT_ICON = "icon";
    private static final String KEY_CAT_COLOR = "color";
    private static final String KEY_CAT_TYPE = "type";

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
                + KEY_CAT_TYPE + " INTEGER NOT NULL)";
        db.execSQL(createCategoriesTable);

        String createRecordsTable = "CREATE TABLE " + TABLE_RECORDS + " ("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + KEY_TYPE + " INTEGER NOT NULL, "
                + KEY_AMOUNT + " REAL NOT NULL, "
                + KEY_CATEGORY_ID + " INTEGER NOT NULL, "
                + KEY_PROJECT_ID + " INTEGER DEFAULT 0, "
                + KEY_DATE + " TEXT NOT NULL, "
                + KEY_TIMESTAMP + " INTEGER NOT NULL, "
                + KEY_REMARK + " TEXT)";
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

        initDefaultCategories(db);
    }

    private void initDefaultCategories(SQLiteDatabase db) {
        addCategory(db, new Category("餐饮", "🍜", 0xFFF44336, Record.TYPE_EXPENSE));
        addCategory(db, new Category("购物", "🛒", 0xFFE91E63, Record.TYPE_EXPENSE));
        addCategory(db, new Category("日用", "🧴", 0xFF9C27B0, Record.TYPE_EXPENSE));
        addCategory(db, new Category("交通", "🚗", 0xFFFF9800, Record.TYPE_EXPENSE));
        addCategory(db, new Category("蔬菜", "🥬", 0xFF4CAF50, Record.TYPE_EXPENSE));
        addCategory(db, new Category("水果", "🍎", 0xFF8BC34A, Record.TYPE_EXPENSE));
        addCategory(db, new Category("零食", "🍪", 0xFFCDDC39, Record.TYPE_EXPENSE));
        addCategory(db, new Category("运动", "⚽", 0xFF00BCD4, Record.TYPE_EXPENSE));
        addCategory(db, new Category("娱乐", "🎮", 0xFF673AB7, Record.TYPE_EXPENSE));
        addCategory(db, new Category("通讯", "📱", 0xFF3F51B5, Record.TYPE_EXPENSE));
        addCategory(db, new Category("服饰", "👕", 0xFFE91E63, Record.TYPE_EXPENSE));
        addCategory(db, new Category("美容", "💄", 0xFFF06292, Record.TYPE_EXPENSE));
        addCategory(db, new Category("住房", "🏠", 0xFF795548, Record.TYPE_EXPENSE));
        addCategory(db, new Category("居家", "🛋️", 0xFF8D6E63, Record.TYPE_EXPENSE));
        addCategory(db, new Category("孩子", "👶", 0xFF42A5F5, Record.TYPE_EXPENSE));
        addCategory(db, new Category("长辈", "👴", 0xFF78909C, Record.TYPE_EXPENSE));
        addCategory(db, new Category("社交", "👥", 0xFFAB47BC, Record.TYPE_EXPENSE));
        addCategory(db, new Category("旅行", "✈️", 0xFF29B6F6, Record.TYPE_EXPENSE));
        addCategory(db, new Category("烟酒", "🚬", 0xFF5D4037, Record.TYPE_EXPENSE));
        addCategory(db, new Category("数码", "💻", 0xFF26A69A, Record.TYPE_EXPENSE));
        addCategory(db, new Category("汽车", "🚙", 0xFF455A64, Record.TYPE_EXPENSE));
        addCategory(db, new Category("医疗", "💊", 0xFFEF5350, Record.TYPE_EXPENSE));
        addCategory(db, new Category("书籍", "📚", 0xFF5C6BC0, Record.TYPE_EXPENSE));
        addCategory(db, new Category("学习", "✏️", 0xFF66BB6A, Record.TYPE_EXPENSE));
        addCategory(db, new Category("宠物", "🐱", 0xFFFFB300, Record.TYPE_EXPENSE));
        addCategory(db, new Category("礼金", "🧧", 0xFFEF5350, Record.TYPE_EXPENSE));
        addCategory(db, new Category("礼物", "🎁", 0xFFEC407A, Record.TYPE_EXPENSE));
        addCategory(db, new Category("办公", "📎", 0xFF78909C, Record.TYPE_EXPENSE));
        addCategory(db, new Category("维修", "🔧", 0xFFFF7043, Record.TYPE_EXPENSE));
        addCategory(db, new Category("捐赠", "❤️", 0xFFEF9A9A, Record.TYPE_EXPENSE));
        addCategory(db, new Category("彩票", "🎰", 0xFFBA68C8, Record.TYPE_EXPENSE));
        addCategory(db, new Category("亲友", "👨‍👩‍👧", 0xFF26C6DA, Record.TYPE_EXPENSE));
        addCategory(db, new Category("快递", "📦", 0xFF8D6E63, Record.TYPE_EXPENSE));
        addCategory(db, new Category("其他", "📋", 0xFF607D8B, Record.TYPE_EXPENSE));

        addCategory(db, new Category("工资", "💰", 0xFF4CAF50, Record.TYPE_INCOME));
        addCategory(db, new Category("奖金", "🏆", 0xFF8BC34A, Record.TYPE_INCOME));
        addCategory(db, new Category("投资", "📈", 0xFF009688, Record.TYPE_INCOME));
        addCategory(db, new Category("兼职", "💼", 0xFF2196F3, Record.TYPE_INCOME));
        addCategory(db, new Category("红包", "🧧", 0xFFFF5722, Record.TYPE_INCOME));
        addCategory(db, new Category("其他收入", "💵", 0xFF00BCD4, Record.TYPE_INCOME));
    }

    private long addCategory(SQLiteDatabase db, Category category) {
        ContentValues values = new ContentValues();
        values.put(KEY_CAT_NAME, category.getName());
        values.put(KEY_CAT_ICON, category.getIcon());
        values.put(KEY_CAT_COLOR, category.getColor());
        values.put(KEY_CAT_TYPE, category.getType());
        return db.insert(TABLE_CATEGORIES, null, values);
    }

    public long addCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();
        return addCategory(db, category);
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
                categories.add(category);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return categories;
    }

    public void deleteCategory(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CATEGORIES, KEY_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public long addRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TYPE, record.getType());
        values.put(KEY_AMOUNT, record.getAmount());
        values.put(KEY_CATEGORY_ID, record.getCategoryId());
        values.put(KEY_PROJECT_ID, record.getProjectId());
        values.put(KEY_DATE, record.getDate());
        values.put(KEY_TIMESTAMP, record.getTimestamp());
        values.put(KEY_REMARK, record.getRemark());
        return db.insert(TABLE_RECORDS, null, values);
    }

    private Record cursorToRecord(Cursor cursor) {
        Record record = new Record();
        record.setId(cursor.getLong(0));
        record.setType(cursor.getInt(1));
        record.setAmount(cursor.getDouble(2));
        record.setCategoryId(cursor.getLong(3));
        record.setProjectId(cursor.getLong(4));
        record.setDate(cursor.getString(5));
        record.setTimestamp(cursor.getLong(6));
        record.setRemark(cursor.getString(7));
        return record;
    }

    public List<Record> getAllRecords() {
        List<Record> records = new ArrayList<>();
        String selectQuery = "SELECT r.*, c." + KEY_CAT_NAME + ", c." + KEY_CAT_ICON + ", c." + KEY_CAT_COLOR
                + ", p." + KEY_PROJ_NAME
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " LEFT JOIN " + TABLE_PROJECTS + " p ON r." + KEY_PROJECT_ID + " = p." + KEY_ID
                + " ORDER BY r." + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Record record = cursorToRecord(cursor);
                record.setCategoryName(cursor.getString(8));
                record.setCategoryIcon(cursor.getString(9));
                record.setCategoryColor(cursor.getInt(10));
                record.setProjectName(cursor.getString(11));
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
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " LEFT JOIN " + TABLE_PROJECTS + " p ON r." + KEY_PROJECT_ID + " = p." + KEY_ID
                + " WHERE r." + KEY_DATE + " LIKE ?"
                + " ORDER BY r." + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{yearMonth + "%"});

        if (cursor.moveToFirst()) {
            do {
                Record record = cursorToRecord(cursor);
                record.setCategoryName(cursor.getString(8));
                record.setCategoryIcon(cursor.getString(9));
                record.setCategoryColor(cursor.getInt(10));
                record.setProjectName(cursor.getString(11));
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
                + " FROM " + TABLE_RECORDS + " r"
                + " LEFT JOIN " + TABLE_CATEGORIES + " c ON r." + KEY_CATEGORY_ID + " = c." + KEY_ID
                + " LEFT JOIN " + TABLE_PROJECTS + " p ON r." + KEY_PROJECT_ID + " = p." + KEY_ID
                + " WHERE r." + KEY_PROJECT_ID + " = ?"
                + " ORDER BY r." + KEY_TIMESTAMP + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(projectId)});

        if (cursor.moveToFirst()) {
            do {
                Record record = cursorToRecord(cursor);
                record.setCategoryName(cursor.getString(8));
                record.setCategoryIcon(cursor.getString(9));
                record.setCategoryColor(cursor.getInt(10));
                record.setProjectName(cursor.getString(11));
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
