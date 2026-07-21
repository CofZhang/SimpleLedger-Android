package com.simpleledger.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 6.0 周期账单管理：自动记录房租/订阅/工资等固定收支
 */
public class RecurringActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private RecurringAdapter adapter;
    private List<RecurringBill> bills = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring);

        dbHelper = new DatabaseHelper(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAdd).setOnClickListener(v -> showEditDialog(null));

        RecyclerView rv = findViewById(R.id.rvList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecurringAdapter();
        rv.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        bills = dbHelper.getAllRecurringBills();
        adapter.notifyDataSetChanged();
        findViewById(R.id.tvEmpty).setVisibility(bills.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class RecurringAdapter extends RecyclerView.Adapter<RecurringAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recurring, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            RecurringBill bill = bills.get(position);
            holder.tvTitle.setText(bill.getTitle());
            holder.tvAmount.setText(String.format(Locale.getDefault(), "¥%.2f", bill.getAmount()));
            holder.tvAmount.setTextColor(getColor(bill.getType() == Record.TYPE_EXPENSE
                    ? R.color.expense_color : R.color.income_color));

            // 类型标签
            holder.tvType.setText(bill.getType() == Record.TYPE_EXPENSE ? "支出" : "收入");
            holder.tvType.setBackgroundColor(getColor(bill.getType() == Record.TYPE_EXPENSE
                    ? R.color.expense_color : R.color.income_color));

            Category cat = dbHelper.getCategoryById(bill.getCategoryId());
            String catName = cat != null ? cat.getName() : "未分类";
            holder.tvDesc.setText(String.format("%s · %s", bill.getPeriodText(), catName));

            holder.tvNextDate.setText("下次：" + bill.getNextDate());
            holder.swEnabled.setChecked(bill.isEnabled());
            holder.swEnabled.setOnCheckedChangeListener((b, checked) -> {
                bill.setEnabled(checked);
                dbHelper.updateRecurringBill(bill);
            });

            holder.itemView.setOnClickListener(v -> showEditDialog(bill));
            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(RecurringActivity.this)
                        .setTitle("删除周期账单")
                        .setMessage("确定删除「" + bill.getTitle() + "」？")
                        .setPositiveButton("删除", (d, w) -> {
                            dbHelper.deleteRecurringBill(bill.getId());
                            loadData();
                            Toast.makeText(RecurringActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return bills.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvAmount, tvType, tvDesc, tvNextDate;
            Switch swEnabled;

            VH(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvType = itemView.findViewById(R.id.tvType);
                tvDesc = itemView.findViewById(R.id.tvDesc);
                tvNextDate = itemView.findViewById(R.id.tvNextDate);
                swEnabled = itemView.findViewById(R.id.swEnabled);
            }
        }
    }

    /** 添加或编辑周期账单对话框。bill=null 表示新增 */
    private void showEditDialog(RecurringBill bill) {
        final boolean isEdit = bill != null;
        final RecurringBill b = isEdit ? bill : new RecurringBill();

        // 构建对话框视图
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_recurring_edit, null);
        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextView tvType = dialogView.findViewById(R.id.tvType);
        TextView tvCategory = dialogView.findViewById(R.id.tvCategory);
        TextView tvPeriod = dialogView.findViewById(R.id.tvPeriod);
        TextView tvNextDate = dialogView.findViewById(R.id.tvNextDate);
        EditText etRemark = dialogView.findViewById(R.id.etRemark);

        // 默认值
        if (isEdit) {
            etTitle.setText(b.getTitle());
            etAmount.setText(String.valueOf(b.getAmount()));
            tvType.setText(b.getType() == Record.TYPE_EXPENSE ? "支出" : "收入");
            tvType.setTag(b.getType());
            Category cat = dbHelper.getCategoryById(b.getCategoryId());
            if (cat != null) {
                tvCategory.setText(cat.getName());
                tvCategory.setTag(cat);
            }
            tvPeriod.setText(b.getPeriodText());
            tvPeriod.setTag(b.getPeriod());
            tvNextDate.setText(b.getNextDate());
            tvNextDate.setTag(b.getNextDate());
            etRemark.setText(b.getRemark() != null ? b.getRemark() : "");
        } else {
            b.setType(Record.TYPE_EXPENSE);
            tvType.setText("支出");
            tvType.setTag(Record.TYPE_EXPENSE);
            tvPeriod.setText("每月");
            tvPeriod.setTag(3);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().getTime());
            tvNextDate.setText(today);
            tvNextDate.setTag(today);
        }

        // 类型切换
        tvType.setOnClickListener(v -> {
            int type = (int) tvType.getTag() == Record.TYPE_EXPENSE ? Record.TYPE_INCOME : Record.TYPE_EXPENSE;
            tvType.setTag(type);
            tvType.setText(type == Record.TYPE_EXPENSE ? "支出" : "收入");
            // 重置分类选择
            tvCategory.setText("点击选择分类");
            tvCategory.setTag(null);
        });

        // 分类选择
        tvCategory.setOnClickListener(v -> {
            int type = (int) tvType.getTag();
            List<Category> cats = dbHelper.getCategories(type);
            String[] names = new String[cats.size()];
            for (int i = 0; i < cats.size(); i++) names[i] = cats.get(i).getIcon() + " " + cats.get(i).getName();
            new AlertDialog.Builder(this)
                    .setTitle("选择分类")
                    .setItems(names, (d, w) -> {
                        tvCategory.setText(cats.get(w).getName());
                        tvCategory.setTag(cats.get(w));
                    })
                    .show();
        });

        // 周期选择
        tvPeriod.setOnClickListener(v -> {
            String[] periods = {"每日", "每周", "每月", "每年"};
            int[] codes = {1, 2, 3, 4};
            new AlertDialog.Builder(this)
                    .setTitle("选择周期")
                    .setItems(periods, (d, w) -> {
                        tvPeriod.setText(periods[w]);
                        tvPeriod.setTag(codes[w]);
                    })
                    .show();
        });

        // 日期选择
        tvNextDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            String current = (String) tvNextDate.getTag();
            if (current != null && !current.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    cal.setTime(sdf.parse(current));
                } catch (Exception ignored) {}
            }
            new android.app.DatePickerDialog(this, (view, year, month, day) -> {
                Calendar c = Calendar.getInstance();
                c.set(year, month, day);
                String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.getTime());
                tvNextDate.setText(dateStr);
                tvNextDate.setTag(dateStr);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "编辑周期账单" : "添加周期账单")
                .setView(dialogView)
                .setPositiveButton("保存", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String amountStr = etAmount.getText().toString().trim();
                    if (title.isEmpty() || amountStr.isEmpty()) {
                        Toast.makeText(this, "请填写标题和金额", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Category cat = (Category) tvCategory.getTag();
                    if (cat == null) {
                        Toast.makeText(this, "请选择分类", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amount;
                    try { amount = Double.parseDouble(amountStr); }
                    catch (Exception e) {
                        Toast.makeText(this, "金额格式错误", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    b.setTitle(title);
                    b.setAmount(amount);
                    b.setType((int) tvType.getTag());
                    b.setCategoryId(cat.getId());
                    b.setPeriod((int) tvPeriod.getTag());
                    b.setNextDate((String) tvNextDate.getTag());
                    b.setRemark(etRemark.getText().toString().trim());
                    b.setEnabled(true);

                    if (isEdit) dbHelper.updateRecurringBill(b);
                    else dbHelper.addRecurringBill(b);

                    loadData();
                    Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
