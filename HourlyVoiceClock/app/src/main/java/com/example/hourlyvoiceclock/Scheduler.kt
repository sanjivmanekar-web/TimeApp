package com.example.hourlyvoiceclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object Scheduler {

    /** 0 -> "12 AM", 1 -> "1 AM", 12 -> "12 PM", 17 -> "5 PM", 23 -> "11 PM" */
    fun label(hour24: Int): String {
        val h = if (hour24 % 12 == 0) 12 else hour24 % 12
        return "$h ${if (hour24 < 12) "AM" else "PM"}"
    }

    private fun nextHourMillis(): Long {
        val c = Calendar.getInstance()
        c.add(Calendar.HOUR_OF_DAY, 1)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun pendingIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, 1, Intent(ctx, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun isEnabled(ctx: Context) =
        ctx.getSharedPreferences("p", Context.MODE_PRIVATE).getBoolean("on", false)

    fun setEnabled(ctx: Context, on: Boolean) {
        ctx.getSharedPreferences("p", Context.MODE_PRIVATE).edit().putBoolean("on", on).apply()
        if (on) schedule(ctx) else cancel(ctx)
    }

    fun canBeExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 31 ||
            ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val at = nextHourMillis()
        if (canBeExact(ctx)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(ctx))
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(ctx))
        }
    }

    fun cancel(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pendingIntent(ctx))
    }
}
