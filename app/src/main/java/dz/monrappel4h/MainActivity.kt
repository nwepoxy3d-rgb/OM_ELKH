package dz.monrappel4h

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private var selectedHour = 8
    private var selectedMinute = 0
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        buildScreen()
        refreshStatus()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 36, 28, 28)
            setBackgroundColor(0xFFF6F7FB.toInt())
        }
        val title = TextView(this).apply {
            text = "Mon Rappel 4H"
            textSize = 28f
            setTextColor(0xFF172033.toInt())
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Un rappel simple, toutes les 4 heures."
            textSize = 16f
            setTextColor(0xFF5B6475.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 28)
        }
        status = TextView(this).apply {
            textSize = 17f
            setTextColor(0xFF172033.toInt())
            gravity = Gravity.CENTER
            setPadding(12, 22, 12, 22)
        }
        val choose = Button(this).apply {
            text = "Choisir l'heure de la première prise"
            setOnClickListener {
                TimePickerDialog(this@MainActivity, { _, h, m ->
                    selectedHour = h
                    selectedMinute = m
                    scheduleFirstDose()
                }, selectedHour, selectedMinute, true).show()
            }
        }
        val enable = Button(this).apply {
            text = "Activer les rappels"
            setOnClickListener { scheduleFirstDose() }
        }
        val taken = Button(this).apply {
            text = "J'ai pris mon médicament"
            setOnClickListener {
                Scheduler.markTaken(this@MainActivity)
                refreshStatus()
            }
        }
        val disable = Button(this).apply {
            text = "Désactiver les rappels"
            setOnClickListener {
                getSharedPreferences(Scheduler.PREFS, MODE_PRIVATE).edit()
                    .putBoolean(Scheduler.KEY_ENABLED, false).apply()
                Scheduler.cancel(this@MainActivity)
                refreshStatus()
            }
        }
        val permissions = Button(this).apply {
            text = "Autoriser les notifications exactes"
            setOnClickListener { openExactAlarmSettings() }
        }

        root.addView(title, match())
        root.addView(subtitle, match())
        root.addView(status, match())
        root.addView(choose, match())
        root.addView(enable, match())
        root.addView(taken, match())
        root.addView(disable, match())
        root.addView(permissions, match())
        val note = TextView(this).apply {
            text = "Après chaque confirmation « J'ai pris », le prochain rappel est prévu 4 heures plus tard. Si vous ne confirmez pas, un rappel revient toutes les 15 minutes."
            textSize = 13f
            setTextColor(0xFF687386.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }
        root.addView(note, match())
        setContentView(root)
    }

    private fun match() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 10 }

    private fun scheduleFirstDose() {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
            set(java.util.Calendar.MINUTE, selectedMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        val at = cal.timeInMillis
        getSharedPreferences(Scheduler.PREFS, MODE_PRIVATE).edit()
            .putBoolean(Scheduler.KEY_ENABLED, true)
            .putLong(Scheduler.KEY_NEXT, at).apply()
        Scheduler.schedule(this, at)
        refreshStatus()
        if (Build.VERSION.SDK_INT >= 31) openExactAlarmSettingsIfNeeded()
    }

    private fun refreshStatus() {
        val prefs = getSharedPreferences(Scheduler.PREFS, MODE_PRIVATE)
        val enabled = prefs.getBoolean(Scheduler.KEY_ENABLED, false)
        val next = prefs.getLong(Scheduler.KEY_NEXT, 0L)
        status.text = if (enabled && next > 0L) {
            "Rappels activés\nProchain rappel : " +
                SimpleDateFormat("EEE d MMM, HH:mm", Locale.FRENCH).format(Date(next))
        } else "Rappels désactivés"
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4105)
    }

    private fun openExactAlarmSettingsIfNeeded() {
        val manager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 31 && !manager.canScheduleExactAlarms()) openExactAlarmSettings()
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= 31) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:$packageName")))
        }
    }
}
