package com.example.hourlyvoiceclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arms the hourly alarm after reboot, app update, or a clock/timezone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Scheduler.isEnabled(context)) Scheduler.schedule(context)
    }
}
