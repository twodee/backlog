package org.twodee.backlog

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.twodee.rattler.PermittedActivity
import java.io.File
import java.util.*

private const val REQUEST_CAMERA = 110
private const val REQUEST_GALLERY = 111

class MainActivity : PermittedActivity(), TimePickerDialog.OnTimeSetListener {
  private lateinit var prefs: SharedPreferences
  private lateinit var photosList: RecyclerView
  private lateinit var monthSpinner: Spinner
  private lateinit var daySpinner: Spinner

  private val year
    get() = Calendar.getInstance().get(Calendar.YEAR)

  private val month
    get() = monthSpinner.selectedItemPosition + 1

  private val day
    get() = daySpinner.selectedItemPosition + 1

  private val photoDirectory
    get() = File(Environment.getExternalStorageDirectory(), "backlog")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    createNotificationChannel()
    prefs = PreferenceManager.getDefaultSharedPreferences(this)

    // Set up UI.
    photosList = findViewById(R.id.photosList)
    monthSpinner = findViewById(R.id.monthSpinner)
    daySpinner = findViewById(R.id.daySpinner)
    val leftButton: ImageButton = findViewById(R.id.leftButton)
    val rightButton: ImageButton = findViewById(R.id.rightButton)

    photosList.layoutManager = LinearLayoutManager(this)
    leftButton.setOnClickListener { previousDay() }
    rightButton.setOnClickListener { nextDay() }

    monthSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.months))

    // I need to disable animations on the spinner.
    val today = Calendar.getInstance()
    monthSpinner.setSelection(today.get(Calendar.MONTH), false)
    syncDays()
    daySpinner.setSelection(today.get(Calendar.DAY_OF_MONTH) - 1, false)
    loadDayPhotos()

    // Register callbacks.
    monthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onNothingSelected(p0: AdapterView<*>?) {}
      override fun onItemSelected(adapter: AdapterView<*>?, view: View?, i: Int, id: Long) {
        syncDays()
      }
    }

    daySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onNothingSelected(p0: AdapterView<*>?) {}
      override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        loadDayPhotos()
      }
    }

    // Fire prompts.
    val permissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    requestPermissions(permissions, 100, {
      promptForTodo()
    }, {
      Toast.makeText(this, "Unable to store photos.", Toast.LENGTH_LONG).show()
    })
  }

  private fun daysInMonth(month: Int) = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    2 -> 29
    else -> 30
  }

  private fun previousDay() {
    if (day == 1) {
      monthSpinner.setSelection((month - 2 + 12) % 12, false)
      syncDays()
      daySpinner.setSelection(daysInMonth(month) - 1)
    } else {
      daySpinner.setSelection(day - 2, false)
    }
  }

  private fun nextDay() {
    if (day == daysInMonth(month)) {
      monthSpinner.setSelection((month + 12) % 12, false)
      syncDays()
      daySpinner.setSelection(0)
    } else {
      daySpinner.setSelection(day, false)
    }
  }

  private fun syncDays() {
    val nDays = daysInMonth(month)
    if (daySpinner.adapter != null && nDays == daySpinner.adapter.count) {
      return
    }

    val days = (1..nDays).map { it.toString() }
    daySpinner.adapter = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, days.toTypedArray())
  }

  private fun promptForTodo() {
    if (intent.getBooleanExtra("isAdd", false)) {
      promptForAdd()
    } else {
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
    R.id.addButton -> {
      promptForAdd()
      true
    }
    else -> {
      super.onOptionsItemSelected(item)
    }
  }

  private fun promptForAdd() {
    val builder = AlertDialog.Builder(this).apply {
      setTitle("Choose source")
      setMessage("Where is the photo?")
      setPositiveButton("Camera") { _, _ ->
        takePictureFromCamera()
      }
      setNegativeButton("Gallery") { _, _ ->
        takePictureFromGallery()
      }
    }
    builder.show()
  }

  // Exercise 1
  private fun loadDayPhotos() {
  }

  // Exercise 2
  private fun dayFile(year: Int, month: Int, day: Int): File {
    TODO()
  }

  // Exercise 3 - FileProvider XML

  // Exercise 4
  private fun dayUri(year: Int, month: Int, day: Int): Uri {
    TODO()
  }

  // Exercise 5
  private fun takePictureFromCamera() {
  }

  // Exercise 6
  private fun takePictureFromGallery() {
  }

  // Exercise 7
  private fun copyUriToUri(from: Uri, to: Uri) {
  }

  // Exercise 8
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
