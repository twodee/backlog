package org.twodee.backlog

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import java.util.*

object Utilities {
  fun scheduleReminder(context: Context, hour: Int, minute: Int) {
    val today = Calendar.getInstance().apply {
      timeInMillis = System.currentTimeMillis()
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.setInexactRepeating(AlarmManager.RTC, today.timeInMillis, AlarmManager.INTERVAL_DAY, reminderIntent(context))
  }

  fun reminderIntent(context: Context): PendingIntent {
    return Intent(context, AlarmReceiver::class.java).let {
      PendingIntent.getBroadcast(context, 0, it, 0)
    }
  }

  fun logExtras(bundle: Bundle) {
    bundle.keySet().forEach { key ->
      Log.d("FOO", "$key -> ${bundle[key]}")
    }
  }
}
