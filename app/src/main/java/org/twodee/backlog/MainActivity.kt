package org.twodee.backlog

import android.Manifest
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
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.twodee.rattler.PermittedActivity
import java.io.File
import java.util.*

private const val REQUEST_CAMERA = 110
private const val REQUEST_GALLERY = 111
private const val REQUEST_SPEECH_RECOGNITION = 112

class MainActivity : PermittedActivity(), TimePickerDialog.OnTimeSetListener {
  private lateinit var prefs: SharedPreferences
  private lateinit var photosList: RecyclerView
  private lateinit var monthSpinner: Spinner
  private lateinit var daySpinner: Spinner
  private var speechRecognizer: SpeechRecognizer? = null

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

    intent?.extras?.let { Utilities.logExtras(intent.extras) }

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
    val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
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

  var openDialogs: MutableList<AlertDialog> = mutableListOf()

  private fun promptForTodo() {
    if (intent.getBooleanExtra("isAdd", false)) {
      promptForAdd()
    } else {
      val hour = prefs.getInt("hour", -1)
      if (hour < 0) {
        AlertDialog.Builder(this).apply {
          setTitle("Set Reminder")
          setMessage("Backlog won't send daily reminders until you schedule a time.")
          setPositiveButton("Schedule") { _, _ ->
            setReminderTime()
          }
          setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
          }

          val dialog = show()
          setOnDismissListener { openDialogs.remove(dialog) }
          openDialogs.add(dialog)
        }
      }
    }
  }

  override fun onStop() {
    super.onStop()
    openDialogs.forEach { it.dismiss() }
    openDialogs.clear()
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
    R.id.speechButton -> {
      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      }
      startActivityForResult(intent, REQUEST_SPEECH_RECOGNITION)

      if (speechRecognizer == null) {

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
          setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onPartialResults(p0: Bundle?) {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onEndOfSpeech() {}
            override fun onError(p0: Int) {}

            override fun onResults(bundle: Bundle?) {
              bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.forEach {
                if (it == "next") {
                  nextDay()
                } else if (it == "previous") {
                  previousDay()
                }
              }
              startListening(intent)
            }
          })
          startListening(intent)
        }
      } else {
        speechRecognizer?.stopListening()
      }

      true
    }
    else -> {
      super.onOptionsItemSelected(item)
    }
  }

  private fun promptForAdd() {
    if (dayFile(year, month, day).exists()) {
      return
    }

    AlertDialog.Builder(this).apply {
      setTitle("Choose source")
      setMessage("Where is the photo?")
      setPositiveButton("Camera") { _, _ ->
        takePictureFromCamera()
      }
      setNegativeButton("Gallery") { _, _ ->
        takePictureFromGallery()
      }

      val dialog = show()
      setOnDismissListener { openDialogs.remove(dialog) }
      openDialogs.add(dialog)
    }
  }

  // Chunk 0
  private fun loadDayPhotos() {
    if (photoDirectory.exists()) {
      val photos = photoDirectory
        .listFiles { file, _ -> file.isDirectory }
        .map { Photo(File(it, String.format("%02d_%02d.jpg", month, day))) }
        .filter { it.file.exists() }

      photosList.adapter = PhotoAdapter(photos)
    }
  }

  // Chunk 1
  private fun dayFile(year: Int, month: Int, day: Int): File {
    val file = File(photoDirectory, String.format("$year/%02d_%02d.jpg", month, day))
    file.parentFile.mkdirs()
    return file
  }

  // Chunk 2 - FileProvider XML

  // Chunk 3
  private fun dayUri(year: Int, month: Int, day: Int): Uri {
    val file = dayFile(year, month, day)
    val uri = FileProvider.getUriForFile(this, "org.twodee.backlog.fileprovider", file)
    return uri
  }

  // Chunk 4
  private fun takePictureFromCamera() {
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    intent.resolveActivity(packageManager)?.let {
      val uri = dayUri(year, month, day)
      intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
      startActivityForResult(intent, REQUEST_CAMERA)
    }
  }

  // Chunk 5
  private fun takePictureFromGallery() {
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "image/*"
    startActivityForResult(intent, REQUEST_GALLERY)
  }

  // Chunk 6
  private fun copyUriToUri(from: Uri, to: Uri) {
    contentResolver.openInputStream(from).use { input ->
      contentResolver.openOutputStream(to).use { output ->
        input.copyTo(output)
      }
    }
  }

  // Chunk 7
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CAMERA -> {
        if (resultCode == Activity.RESULT_OK) {
          loadDayPhotos()
        }
      }
      REQUEST_GALLERY -> {
        if (resultCode == Activity.RESULT_OK) {
          data?.data?.let { uri ->
            copyUriToUri(uri, dayUri(year, month, day))
            loadDayPhotos()
          }
        }
      }
      REQUEST_SPEECH_RECOGNITION -> {
        if (resultCode == Activity.RESULT_OK) {
          data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.forEach {
            if (it == "next") {
              nextDay()
            } else if (it == "previous") {
              previousDay()
            }
          }
        }
      }
      else -> {
        super.onActivityResult(requestCode, resultCode, data)
      }
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
