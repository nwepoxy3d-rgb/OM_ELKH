package dz.monrappel4h

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object Scheduler {
    const val PREFS = "rappel_prefs"
    const val KEY_ENABLED = "enabled"
    const val KEY_NEXT = "next_at"
    private const val REQUEST_ALARM = 4101
    const val FOUR_HOURS = 4 * 60 * 60 * 1000L
    const val RETRY_MINUTES = 15 * 60 * 1000L

    fun schedule(context: Context, atMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_ALARM, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        }
    }

    fun cancel(context: Context) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_ALARM, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    }

    fun markTaken(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return
        val next = System.currentTimeMillis() + FOUR_HOURS
        prefs.edit().putLong(KEY_NEXT, next).apply()
        schedule(context, next)
    }

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return
        val next = prefs.getLong(KEY_NEXT, 0L)
        if (next > 0L) schedule(context, next)
    }
}
