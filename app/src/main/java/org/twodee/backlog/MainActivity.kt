package org.twodee.backlog

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.TimePicker

class MainActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener {
  private lateinit var prefs: SharedPreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    createNotificationChannel()

    prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val hour = prefs.getInt("hour", -1)
    if (hour < 0) {
      val builder = AlertDialog.Builder(this)
      builder.setTitle("Set Reminder")
      builder.setMessage("Backlog won't send daily reminders until you schedule a time.")
      builder.setPositiveButton("Schedule") { _, _ ->
        setReminderTime()
      }
      builder.setNegativeButton("Cancel") { dialog, _ ->
        dialog.cancel()
      }
      builder.show()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_action, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
    R.id.remindButton -> {
      setReminderTime()
      true
    }
    R.id.disableReminderButton -> {
      val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
      manager.cancel(Utilities.reminderIntent(applicationContext))
      prefs.edit().apply {
        remove("hour")
        remove("minute")
        apply()
      }
      val receiver = ComponentName(this, BootReceiver::class.java)
      packageManager.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
      true
    }
    else -> {
      super.onOptionsItemSelected(item)
    }
  }

  private fun setReminderTime() {
    val fragment = TimePickerFragment()
    fragment.listener = this
    fragment.hour = prefs.getInt("hour", 6)
    fragment.minute = prefs.getInt("minute", 0)
    fragment.show(supportFragmentManager, null)
  }

  private fun createNotificationChannel() {
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel(Notification.CATEGORY_REMINDER, "Backlog Notifications", importance).apply {
      description = "Show backlog reminders"
    }
    val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
  }

  override fun onTimeSet(picker: TimePicker, hour: Int, minute: Int) {
    prefs.edit().apply {
      putInt("hour", hour)
      putInt("minute", minute)
      apply()
    }

    Utilities.scheduleReminder(applicationContext, hour, minute)

    val receiver = ComponentName(this, BootReceiver::class.java)
    packageManager.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
  }
}
