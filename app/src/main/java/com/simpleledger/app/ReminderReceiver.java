package com.simpleledger.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import android.preference.PreferenceManager;

/**
 * 4.5 每日记账提醒：使用 AlarmManager 在指定时间触发本地通知。
 */
public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "reminder_channel";
    public static final int NOTIFICATION_ID = 1001;
    private static final String PREF_REMINDER_ENABLED = "reminder_enabled";
    private static final String PREF_REMINDER_HOUR = "reminder_hour";
    private static final String PREF_REMINDER_MINUTE = "reminder_minute";

    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification(context);
    }

    private void showNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "记账提醒",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("每日记账提醒");
            channel.enableVibration(true);
            nm.createNotificationChannel(channel);
        }

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, launchIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentTitle("咖啡记账提醒")
                .setContentText("今天记账了吗？点击记一笔~")
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        nm.notify(NOTIFICATION_ID, builder.build());
    }

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_REMINDER_ENABLED, false);
    }

    public static int getHour(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_REMINDER_HOUR, 20);
    }

    public static int getMinute(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_REMINDER_MINUTE, 0);
    }

    public static void setReminder(Context context, boolean enabled, int hour, int minute) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_REMINDER_ENABLED, enabled)
                .putInt(PREF_REMINDER_HOUR, hour)
                .putInt(PREF_REMINDER_MINUTE, minute)
                .apply();

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, NOTIFICATION_ID, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (!enabled) {
            if (am != null) am.cancel(pi);
            return;
        }

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour);
        calendar.set(java.util.Calendar.MINUTE, minute);
        calendar.set(java.util.Calendar.SECOND, 0);
        // 如果今天该时间已过，从明天开始
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        if (am != null) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pi);
        }
    }
}
