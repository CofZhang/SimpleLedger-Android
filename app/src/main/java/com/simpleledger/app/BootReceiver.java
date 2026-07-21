package com.simpleledger.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * 5.9 新增：设备重启后自动重新注册记账提醒闹钟。
 *
 * 系统重启后所有 AlarmManager 闹钟都会丢失，需要重新注册。
 * 监听 BOOT_COMPLETED 和 MY_PACKAGE_REPLACED 事件。
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        // 仅在系统启动完成或应用更新后触发
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {

            // 检查用户是否已开启提醒，若开启则重新注册
            if (ReminderReceiver.isEnabled(context)) {
                ReminderReceiver.scheduleNextDay(context);
            }
        }
    }
}
