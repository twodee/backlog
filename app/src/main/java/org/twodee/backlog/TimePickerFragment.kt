package org.twodee.backlog

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class TimePickerFragment : DialogFragment() {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return TimePickerDialog(activity, listener, hour, minute, false)
  }

  var listener: TimePickerDialog.OnTimeSetListener? = null
  var hour: Int = 6
  var minute: Int = 0
}