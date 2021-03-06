package org.twodee.backlog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    Log.d("FOO", "GOT THE BOOT")
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
      val prefs = PreferenceManager.getDefaultSharedPreferences(context)
      if (prefs.getInt("hour", -1) >= 0) {
        Utilities.scheduleReminder(context, prefs.getInt("hour", 6), prefs.getInt("minute", 0))
      }
    }
  }
}
