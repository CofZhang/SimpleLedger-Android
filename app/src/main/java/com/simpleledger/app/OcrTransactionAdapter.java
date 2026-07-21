package com.simpleledger.app;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * 6.4 OCR 多笔交易列表 Adapter
 * 每行支持：选中、编辑日期、分类、金额、备注、切换收入/支出
 */
public class OcrTransactionAdapter extends RecyclerView.Adapter<OcrTransactionAdapter.VH> {

    public interface OnTypeClickListener {
        void onTypeClick(int position);
    }

    private final List<OcrTransaction> list;
    private final OnTypeClickListener typeListener;

    public OcrTransactionAdapter(List<OcrTransaction> list, OnTypeClickListener listener) {
        this.list = list;
        this.typeListener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ocr_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        OcrTransaction t = list.get(position);
        h.bind(t, position, typeListener);
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        EditText etDate, etCategory, etAmount, etRemark;
        TextView btnType;
        TextWatcher dateWatcher, categoryWatcher, amountWatcher, remarkWatcher;

        VH(@NonNull View v) {
            super(v);
            cbSelect = v.findViewById(R.id.cbSelect);
            etDate = v.findViewById(R.id.etDate);
            etCategory = v.findViewById(R.id.etCategory);
            etAmount = v.findViewById(R.id.etAmount);
            etRemark = v.findViewById(R.id.etRemark);
            btnType = v.findViewById(R.id.btnType);
        }

        void bind(OcrTransaction t, int pos, OnTypeClickListener listener) {
            // 清掉旧的 TextWatcher，避免回收复用导致错位
            if (dateWatcher != null) etDate.removeTextChangedListener(dateWatcher);
            if (categoryWatcher != null) etCategory.removeTextChangedListener(categoryWatcher);
            if (amountWatcher != null) etAmount.removeTextChangedListener(amountWatcher);
            if (remarkWatcher != null) etRemark.removeTextChangedListener(remarkWatcher);

            cbSelect.setOnCheckedChangeListener(null);
            cbSelect.setChecked(t.isSelected());
            cbSelect.setOnCheckedChangeListener((b, c) -> t.setSelected(c));

            etDate.setText(t.getDate() != null ? t.getDate() : "");
            etCategory.setText(t.getCategoryName() != null ? t.getCategoryName() : "");
            etAmount.setText(t.getAmount() > 0 ?
                    String.format(Locale.getDefault(), "%.2f", t.getAmount()) : "");
            etRemark.setText(t.getRemark() != null ? t.getRemark() : "");

            btnType.setText(t.getType() == Record.TYPE_INCOME ? "收入" : "支出");
            btnType.setOnClickListener(v -> {
                if (listener != null) listener.onTypeClick(pos);
            });

            dateWatcher = new SimpleTextWatcher() {
                @Override public void afterTextChanged(Editable s) { t.setDate(s.toString().trim()); }
            };
            categoryWatcher = new SimpleTextWatcher() {
                @Override public void afterTextChanged(Editable s) {
                    t.setCategoryName(s.toString().trim());
                    t.setCategoryId(0); // 用户改了分类，重置匹配
                }
            };
            amountWatcher = new SimpleTextWatcher() {
                @Override public void afterTextChanged(Editable s) {
                    try { t.setAmount(Double.parseDouble(s.toString().trim())); }
                    catch (Exception e) { t.setAmount(0); }
                }
            };
            remarkWatcher = new SimpleTextWatcher() {
                @Override public void afterTextChanged(Editable s) { t.setRemark(s.toString().trim()); }
            };
            etDate.addTextChangedListener(dateWatcher);
            etCategory.addTextChangedListener(categoryWatcher);
            etAmount.addTextChangedListener(amountWatcher);
            etRemark.addTextChangedListener(remarkWatcher);
        }
    }

    private static abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
