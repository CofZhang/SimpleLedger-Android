package com.simpleledger.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private Calendar currentMonth;
    private Calendar selectedDate;
    private TextView tvMonth, tvSelectedDate, tvDayTotal, tvEmptyDay;
    private GridView gvCalendar;
    private RecyclerView rvDayRecords;
    private RecordAdapter recordAdapter;
    private List<Record> dayRecords;
    private Map<String, Double> dailyExpenseMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        dbHelper = new DatabaseHelper(this);
        currentMonth = Calendar.getInstance();
        selectedDate = Calendar.getInstance();

        int year = getIntent().getIntExtra("year", currentMonth.get(Calendar.YEAR));
        int month = getIntent().getIntExtra("month", currentMonth.get(Calendar.MONTH));
        currentMonth.set(Calendar.YEAR, year);
        currentMonth.set(Calendar.MONTH, month);
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);

        tvMonth = findViewById(R.id.tvMonth);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvDayTotal = findViewById(R.id.tvDayTotal);
        tvEmptyDay = findViewById(R.id.tvEmptyDay);
        gvCalendar = findViewById(R.id.gvCalendar);
        rvDayRecords = findViewById(R.id.rvDayRecords);

        dayRecords = new ArrayList<>();
        rvDayRecords.setLayoutManager(new LinearLayoutManager(this));
        recordAdapter = new RecordAdapter(dayRecords);
        rvDayRecords.setAdapter(recordAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            loadCalendarData();
        });
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            loadCalendarData();
        });

        loadCalendarData();
        loadDayRecords();
    }

    private void loadCalendarData() {
        String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentMonth.getTime());
        String displayMonth = new SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(currentMonth.getTime());
        tvMonth.setText(displayMonth);

        dailyExpenseMap = new HashMap<>();
        List<Record> monthRecords = dbHelper.getRecordsByMonth(yearMonth);
        for (Record r : monthRecords) {
            if (r.getType() == Record.TYPE_EXPENSE) {
                String date = r.getDate();
                dailyExpenseMap.put(date, dailyExpenseMap.getOrDefault(date, 0.0) + r.getAmount());
            }
        }

        gvCalendar.setAdapter(new CalendarAdapter());
    }

    private void loadDayRecords() {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());
        String displayDate = new SimpleDateFormat("M月d日 EEEE", Locale.getDefault()).format(selectedDate.getTime());
        tvSelectedDate.setText(displayDate);

        dayRecords.clear();
        for (Record r : dbHelper.getRecordsByMonth(dateStr.substring(0, 7))) {
            if (dateStr.equals(r.getDate())) {
                dayRecords.add(r);
            }
        }
        recordAdapter.updateData(dayRecords);

        double dayTotal = 0;
        for (Record r : dayRecords) {
            if (r.getType() == Record.TYPE_EXPENSE) dayTotal += r.getAmount();
        }
        tvDayTotal.setText(String.format("支出 ¥%.2f", dayTotal));

        if (dayRecords.isEmpty()) {
            tvEmptyDay.setVisibility(View.VISIBLE);
            rvDayRecords.setVisibility(View.GONE);
        } else {
            tvEmptyDay.setVisibility(View.GONE);
            rvDayRecords.setVisibility(View.VISIBLE);
        }
    }

    private class CalendarAdapter extends BaseAdapter {
        private List<CalendarDay> days;

        CalendarAdapter() {
            days = new ArrayList<>();
            Calendar cal = (Calendar) currentMonth.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            if (firstDayOfWeek < 0) firstDayOfWeek = 6;

            Calendar prevMonth = (Calendar) cal.clone();
            prevMonth.add(Calendar.MONTH, -1);
            int prevMonthMaxDay = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int i = firstDayOfWeek - 1; i >= 0; i--) {
                CalendarDay day = new CalendarDay();
                prevMonth.set(Calendar.DAY_OF_MONTH, prevMonthMaxDay - i);
                day.date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(prevMonth.getTime());
                day.day = prevMonth.get(Calendar.DAY_OF_MONTH);
                day.isCurrentMonth = false;
                days.add(day);
            }

            int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            Calendar today = Calendar.getInstance();
            for (int i = 1; i <= maxDay; i++) {
                cal.set(Calendar.DAY_OF_MONTH, i);
                CalendarDay day = new CalendarDay();
                day.date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
                day.day = i;
                day.isCurrentMonth = true;
                day.isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                        && cal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                        && cal.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);
                days.add(day);
            }

            int remaining = 42 - days.size();
            Calendar nextMonth = (Calendar) currentMonth.clone();
            nextMonth.add(Calendar.MONTH, 1);
            nextMonth.set(Calendar.DAY_OF_MONTH, 1);
            for (int i = 0; i < remaining; i++) {
                CalendarDay day = new CalendarDay();
                day.date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(nextMonth.getTime());
                day.day = nextMonth.get(Calendar.DAY_OF_MONTH);
                day.isCurrentMonth = false;
                days.add(day);
                nextMonth.add(Calendar.DAY_OF_MONTH, 1);
            }
        }

        @Override
        public int getCount() { return days.size(); }

        @Override
        public Object getItem(int position) { return days.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(CalendarActivity.this).inflate(R.layout.item_calendar_day, parent, false);
            }

            CalendarDay day = days.get(position);
            TextView tvDay = convertView.findViewById(R.id.tvDay);
            TextView tvAmount = convertView.findViewById(R.id.tvAmount);
            View viewBg = convertView.findViewById(R.id.viewBg);

            tvDay.setText(String.valueOf(day.day));

            if (!day.isCurrentMonth) {
                tvDay.setTextColor(getResources().getColor(R.color.calendar_day_other));
            } else if (day.isToday) {
                viewBg.setVisibility(View.VISIBLE);
                viewBg.setBackgroundResource(R.drawable.bg_calendar_day);
                tvDay.setTextColor(getResources().getColor(R.color.white));
            } else {
                tvDay.setTextColor(getResources().getColor(R.color.calendar_day_normal));
                viewBg.setVisibility(View.GONE);
            }

            String selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());
            if (day.date.equals(selectedDateStr) && day.isCurrentMonth) {
                viewBg.setVisibility(View.VISIBLE);
                viewBg.setBackgroundResource(R.drawable.bg_calendar_selected);
                tvDay.setTextColor(getResources().getColor(R.color.white));
            }

            Double expense = dailyExpenseMap.get(day.date);
            if (expense != null && expense > 0 && day.isCurrentMonth) {
                tvAmount.setVisibility(View.VISIBLE);
                tvAmount.setText(String.format("¥%.0f", expense));
            } else {
                tvAmount.setVisibility(View.GONE);
            }

            convertView.setOnClickListener(v -> {
                if (day.isCurrentMonth) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        selectedDate.setTime(sdf.parse(day.date));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    notifyDataSetChanged();
                    loadDayRecords();
                }
            });

            return convertView;
        }
    }

    private static class CalendarDay {
        String date;
        int day;
        boolean isCurrentMonth;
        boolean isToday;
    }
}
