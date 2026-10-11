package com.example.hourlyvoiceclock

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var toggle: Button
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        val pad = (24 * d).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad * 3, pad, pad)
        }
        val title = TextView(this).apply {
            text = "Hourly Voice Clock"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
        }
        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, pad / 2, 0, pad)
        }
        toggle = Button(this).apply { setOnClickListener { onToggle() } }
        val test = Button(this).apply {
            text = "Test voice"
            setOnClickListener { speakNow() }
        }
        val battery = Button(this).apply {
            text = "Allow background running"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
        val note = TextView(this).apply {
            text = "Speaks the time on the hour, 12 AM to 11 PM, even when the screen is off. " +
                "If an hour is missed, set this app to \"Unrestricted\" under battery settings."
            textSize = 13f
            setPadding(0, pad, 0, 0)
        }
        listOf(title, status, toggle, test, battery, note).forEach { root.addView(it) }
        setContentView(root)

        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault() }
    }

    override fun onResume() {
        super.onResume()
        // Re-arm (also upgrades to exact alarms once the permission is granted).
        if (Scheduler.isEnabled(this)) Scheduler.schedule(this)
        refresh()
    }

    private fun onToggle() {
        val turnOn = !Scheduler.isEnabled(this)
        Scheduler.setEnabled(this, turnOn)
        if (turnOn && !Scheduler.canBeExact(this) && Build.VERSION.SDK_INT >= 31) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
            )
        }
        refresh()
    }

    private fun speakNow() {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tts?.speak("It's ${Scheduler.label(h)}", TextToSpeech.QUEUE_FLUSH, null, "test")
    }

    private fun refresh() {
        val on = Scheduler.isEnabled(this)
        toggle.text = if (on) "Stop announcing" else "Start announcing every hour"
        status.text = when {
            !on -> "Off"
            Scheduler.canBeExact(this) -> "On. Next announcement at the top of the hour."
            else -> "On, but timing may drift. Allow \"Alarms & reminders\" for this app."
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
