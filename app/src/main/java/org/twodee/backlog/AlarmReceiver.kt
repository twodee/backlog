package org.twodee.backlog

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val intent: PendingIntent = Intent(context, MainActivity::class.java).run {
      this.putExtra("isAdd", true)
//      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      PendingIntent.getActivity(context, 0, this, 0)
    }

    val notification = Notification.Builder(context, Notification.CATEGORY_REMINDER).run {
      setSmallIcon(R.drawable.camera)
      setContentTitle("A new day, a new photo")
      setContentText("Just a friendly reminder to take today's picture.")
      setContentIntent(intent)
      setAutoCancel(true)
      build()
    }

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(0, notification)
  }
}
