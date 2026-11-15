package dz.monrappel4h

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences(Scheduler.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(Scheduler.KEY_ENABLED, false)) return

        val next = prefs.getLong(Scheduler.KEY_NEXT, 0L)
        if (next <= 0L) return

        createChannel(context)
        val takenIntent = Intent(context, TakenReceiver::class.java)
        val takenPending = PendingIntent.getBroadcast(
            context, 4102, takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = Intent(context, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            context, 4103, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, "rappel_medicament")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("C'est l'heure de votre médicament")
            .setContentText("Quand vous l'avez pris, appuyez sur « J'ai pris mon médicament ».")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_menu_save, "J'ai pris mon médicament", takenPending)
            .build()

        val allowed = Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) NotificationManagerCompat.from(context).notify(4104, notification)

        // Tant que la prise n'est pas confirmée, rappeler toutes les 15 minutes.
        Scheduler.schedule(context, System.currentTimeMillis() + Scheduler.RETRY_MINUTES)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "rappel_medicament", "Rappels de médicament", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Rappel sonore de prise de médicament" }
            manager.createNotificationChannel(channel)
        }
    }
}

class TakenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationManagerCompat.from(context).cancel(4104)
        Scheduler.markTaken(context)
    }
}
