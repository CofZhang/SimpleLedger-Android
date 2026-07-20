package com.simpleledger.app;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.preference.PreferenceManager;

/**
 * 4.5 触觉反馈工具类：所有点击操作可触发轻微震动，可在设置中关闭。
 */
public class HapticHelper {
    private static Boolean enabled = null;

    public static boolean isEnabled(Context context) {
        if (enabled == null) {
            enabled = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("haptic_enabled", true);
        }
        return enabled;
    }

    public static void setEnabled(Context context, boolean on) {
        enabled = on;
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean("haptic_enabled", on).apply();
    }

    public static void light(Context context) {
        if (!isEnabled(context)) return;
        vibrate(context, 15);
    }

    public static void medium(Context context) {
        if (!isEnabled(context)) return;
        vibrate(context, 30);
    }

    public static void strong(Context context) {
        if (!isEnabled(context)) return;
        vibrate(context, 50);
    }

    private static void vibrate(Context context, int ms) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null && vm.getDefaultVibrator().hasVibrator()) {
                vm.getDefaultVibrator().vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } else {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }
}
