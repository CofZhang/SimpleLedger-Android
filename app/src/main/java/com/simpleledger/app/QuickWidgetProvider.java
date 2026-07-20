package com.simpleledger.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * 5.2 桌面快速记账小组件
 *
 * 窄长方形横条，四个按钮从左到右：
 * - 账单 → 进入 APP 明细页
 * - 支出 → 直接开始记支出
 * - 收入 → 直接开始记收入
 * - 统计 → 进入 APP 统计页
 *
 * 通过 PendingIntent 跳转到 MainActivity，并通过 EXTRA_TARGET 指定目标 Fragment。
 */
public class QuickWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quick);

            // 账单 → 明细页
            views.setOnClickPendingIntent(R.id.btnWidgetRecords,
                    buildPendingIntent(context, MainActivity.TARGET_RECORDS));

            // 支出 → 记账页（支出）
            views.setOnClickPendingIntent(R.id.btnWidgetExpense,
                    buildPendingIntent(context, MainActivity.TARGET_ADD_EXPENSE));

            // 收入 → 记账页（收入）
            views.setOnClickPendingIntent(R.id.btnWidgetIncome,
                    buildPendingIntent(context, MainActivity.TARGET_ADD_INCOME));

            // 统计 → 统计页
            views.setOnClickPendingIntent(R.id.btnWidgetStats,
                    buildPendingIntent(context, MainActivity.TARGET_STATS));

            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }

    /**
     * 构建跳转到 MainActivity 的 PendingIntent，携带目标 Fragment extra。
     * 使用 FLAG_IMMUTABLE + FLAG_UPDATE_CURRENT 保证安全性与刷新。
     */
    private PendingIntent buildPendingIntent(Context context, String target) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(MainActivity.EXTRA_TARGET, target);
        // 关键：加上 FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP
        // 如果 MainActivity 已在栈顶，走 onNewIntent；否则新建任务
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                context,
                target.hashCode(),  // requestCode 用 target 字符串的 hashCode 区分四个按钮
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
