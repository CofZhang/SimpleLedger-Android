package com.simpleledger.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.preference.PreferenceManager;

/**
 * 4.5 每日记账提醒：使用 AlarmManager 在指定时间触发本地通知。
 *
 * 5.9 彻底修复：
 * - 使用 setAlarmClock 替代 setRepeating：系统级闹钟，Doze/息屏/低电量模式下也能准时触发
 * - 每次触发后自动注册下一次闹钟（setAlarmClock 不支持 repeating）
 * - 通知添加 FULL_SCREEN_INTENT：屏幕息屏时会点亮屏幕并弹出横幅
 * - 使用 WakeLock 保证设备唤醒足够长时间以触发通知
 * - 锁屏显示 VISIBILITY_PUBLIC
 */
public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "reminder_channel";
    public static final String CHANNEL_ID_HIGH = "reminder_channel_high";
    public static final int NOTIFICATION_ID = 1001;
    private static final String PREF_REMINDER_ENABLED = "reminder_enabled";
    private static final String PREF_REMINDER_HOUR = "reminder_hour";
    private static final String PREF_REMINDER_MINUTE = "reminder_minute";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ReminderReceiver", "onReceive 触发，时间=" + System.currentTimeMillis());
        // 5.9 获取 WakeLock 保证 CPU 唤醒足够长时间来显示通知
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CoffeeLedger:Reminder");
            wakeLock.acquire(10 * 1000L); // 最多持有 10 秒
        }

        try {
            // 6.3 检查通知权限，没权限就跳过（避免在某些机型上抛 SecurityException）
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                showNotification(context);
                Log.d("ReminderReceiver", "通知已显示");
            } else {
                Log.w("ReminderReceiver", "通知被系统禁用，跳过显示");
            }
        } catch (Exception e) {
            Log.e("ReminderReceiver", "显示通知失败", e);
        } finally {
            // 5.9 自动注册明天的提醒
            scheduleNextDay(context);
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private void showNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        // 5.9 创建高优先级通知通道（IMPORTANCE_HIGH 会弹出横幅）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_HIGH, "记账提醒（强提醒）",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("每日记账强提醒，会在屏幕上弹出");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 200, 100, 200, 100, 200});
            channel.enableLights(true);
            channel.setLightColor(0xFFD6C7B2);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true); // 绕过勿扰模式
            nm.createNotificationChannel(channel);
        }

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullScreenPi = PendingIntent.getActivity(context, 0, launchIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // 5.9 构建高优先级通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_HIGH)
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentTitle("☕ 咖啡记账提醒")
                .setContentText("今天记账了吗？点击记一笔~")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("今天记账了吗？点击记一笔~\n别忘了记录今天的每一笔收支哦~"))
                .setAutoCancel(true)
                .setContentIntent(fullScreenPi)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(new long[]{0, 200, 100, 200, 100, 200})
                .setLights(0xFFD6C7B2, 1000, 1000)
                .setOngoing(false);

        // 5.9 关键：设置 FULL_SCREEN_INTENT，息屏时会点亮屏幕并弹出全屏界面
        builder.setFullScreenIntent(fullScreenPi, true);

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

    /**
     * 5.9 注册下一次提醒（使用 setAlarmClock，Doze 也能触发）
     */
    public static void scheduleNextDay(Context context) {
        if (!isEnabled(context)) return;

        int hour = getHour(context);
        int minute = getMinute(context);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, NOTIFICATION_ID, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour);
        calendar.set(java.util.Calendar.MINUTE, minute);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        // 如果今天该时间已过，从明天开始
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        // 5.9 使用 setAlarmClock：这是 Android 上最严格的闹钟 API
        // - 在 Doze 模式下也会触发
        // - 系统会在状态栏显示闹钟图标
        // - 等同于系统闹钟应用
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(
                calendar.getTimeInMillis(), pi);
        am.setAlarmClock(info, pi);
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

        // 5.9 调用 scheduleNextDay 注册闹钟
        scheduleNextDay(context);
    }
}
