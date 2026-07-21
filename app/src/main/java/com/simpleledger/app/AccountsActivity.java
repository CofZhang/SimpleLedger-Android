package com.simpleledger.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class AccountsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private RecyclerView rvAccounts;
    private AccountAdapter adapter;
    private List<Account> accounts;

    private final String[] accountTypes = {"现金 💵", "银行卡 🏦", "支付宝 📱", "微信 💬", "信用卡 💳", "其他 💼"};
    private final String[] defaultIcons = {"💵", "🏦", "📱", "💬", "💳", "💼"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);

        dbHelper = new DatabaseHelper(this);
        accounts = new ArrayList<>();

        initViews();
        loadAccounts();
    }

    private void initViews() {
        rvAccounts = findViewById(R.id.rvAccounts);
        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AccountAdapter();
        rvAccounts.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> showAddAccountDialog());
    }

    private void loadAccounts() {
        accounts.clear();
        accounts.addAll(dbHelper.getAllAccounts());
        adapter.notifyDataSetChanged();
    }

    private void showAddAccountDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_account, null);
        EditText etAccountName = dialogView.findViewById(R.id.etAccountName);
        Spinner spinnerAccountType = dialogView.findViewById(R.id.spinnerAccountType);
        EditText etInitBalance = dialogView.findViewById(R.id.etInitBalance);
        EditText etAccountRemark = dialogView.findViewById(R.id.etAccountRemark);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, accountTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAccountType.setAdapter(typeAdapter);

        new AlertDialog.Builder(this)
                .setTitle("添加账户")
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String name = etAccountName.getText().toString().trim();
                    int type = spinnerAccountType.getSelectedItemPosition();
                    String icon = defaultIcons[type];
                    String balanceStr = etInitBalance.getText().toString().trim();
                    String remark = etAccountRemark.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "请输入账户名称", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double initBalance = 0;
                    if (!balanceStr.isEmpty()) {
                        try {
                            initBalance = Double.parseDouble(balanceStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "金额格式不正确", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    Account account = new Account(name, icon, type, initBalance, remark);
                    dbHelper.addAccount(account);
                    loadAccounts();
                    Toast.makeText(this, "账户添加成功", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDeleteAccount(Account account) {
        new AlertDialog.Builder(this)
                .setTitle("删除账户")
                .setMessage(String.format("确定要删除账户「%s」吗？该账户下的记录将转移到默认账户。", account.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deleteAccount(account.getId());
                    loadAccounts();
                    Toast.makeText(this, "账户已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAccounts();
    }

    private class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {

        @Override
        public AccountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_account, parent, false);
            return new AccountViewHolder(view);
        }

        @Override
        public void onBindViewHolder(AccountViewHolder holder, int position) {
            Account account = accounts.get(position);
            holder.tvAccountIcon.setText(account.getIcon());
            holder.tvAccountName.setText(account.getName());
            holder.tvBalance.setText(String.format("¥%.2f", account.getBalance()));

            String remark = account.getRemark();
            if (remark != null && !remark.isEmpty()) {
                holder.tvAccountRemark.setText(remark);
                holder.tvAccountRemark.setVisibility(View.VISIBLE);
            } else {
                holder.tvAccountRemark.setVisibility(View.GONE);
            }

            holder.itemView.setOnLongClickListener(v -> {
                confirmDeleteAccount(account);
                return true;
            });

            // 5.7 点击账户跳转到账户详情页，查看明细和总收支
            holder.itemView.setOnClickListener(v -> {
                HapticHelper.light(AccountsActivity.this);
                Intent intent = new Intent(AccountsActivity.this, AccountDetailActivity.class);
                intent.putExtra("account_id", account.getId());
                intent.putExtra("account_name", account.getName());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return accounts.size();
        }

        class AccountViewHolder extends RecyclerView.ViewHolder {
            TextView tvAccountIcon;
            TextView tvAccountName;
            TextView tvAccountRemark;
            TextView tvBalance;

            AccountViewHolder(View itemView) {
                super(itemView);
                tvAccountIcon = itemView.findViewById(R.id.tvAccountIcon);
                tvAccountName = itemView.findViewById(R.id.tvAccountName);
                tvAccountRemark = itemView.findViewById(R.id.tvAccountRemark);
                tvBalance = itemView.findViewById(R.id.tvBalance);
            }
        }
    }
}
