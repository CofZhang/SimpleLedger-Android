package com.simpleledger.app;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.preference.PreferenceManager;

/**
 * 4.5 触觉反馈工具类：所有点击操作可触发轻微震动，可在设置中关闭。
 *
 * 5.8 修复：
 * - 提升震动时长（15ms 部分机型无感知），light=30ms/medium=50ms/strong=80ms
 * - 使用 createWaveform 替代 createOneShot，兼容性更好
 * - 不再缓存 enabled，每次实时读取，避免重启应用后状态不一致
 */
public class HapticHelper {

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("haptic_enabled", true);
    }

    public static void setEnabled(Context context, boolean on) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean("haptic_enabled", on).apply();
    }

    public static void light(Context context) {
        if (!isEnabled(context)) return;
        vibrate(context, 30);
    }

    public static void medium(Context context) {
        if (!isEnabled(context)) return;
        vibrate(context, 50);
    }

    public static void strong(Context context) {
        if (!isEnabled(context)) return;
        vibrate(context, 80);
    }

    private static void vibrate(Context context, int ms) {
        if (context == null) return;
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm != null ? vm.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (vibrator == null || !vibrator.hasVibrator()) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 5.8 使用 createWaveform 替代 createOneShot，提升兼容性
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, ms}, -1));
            } else {
                vibrator.vibrate(ms);
            }
        } catch (Exception ignored) {
        }
    }
}
