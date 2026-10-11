package com.example.hourlyvoiceclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Calendar
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        if (!Scheduler.isEnabled(app)) return

        Scheduler.schedule(app) // queue the next hour first

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val pending = goAsync()
        val handler = Handler(Looper.getMainLooper())
        var tts: TextToSpeech? = null
        var finished = false

        fun finish() {
            handler.post {
                if (!finished) {
                    finished = true
                    tts?.shutdown()
                    pending.finish()
                }
            }
        }

        tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) = finish()
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) = finish()
                })
                tts?.speak("It's ${Scheduler.label(hour)}", TextToSpeech.QUEUE_FLUSH, null, "hour")
            } else {
                finish()
            }
        }
        handler.postDelayed({ finish() }, 9000) // safety timeout
    }
}
