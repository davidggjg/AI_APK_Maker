package com.factory.apkmaker

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import java.util.*

class GeneratedApp : AppCompatActivity() {
    private lateinit var timeTextView: TextView
    private lateinit var setTimeButton: Button
    private lateinit var alarmManager: AlarmManager
    private var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeTextView = findViewById(R.id.time_text_view)
        setTimeButton = findViewById(R.id.set_time_button)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        setTimeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this, object : TimePickerDialog.OnTimeSetListener {
                override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
                    timeTextView.text = String.format("%02d:%02d", hourOfDay, minute)
                    setAlarm(hourOfDay, minute)
                }
            }, hour, minute, true)
            timePickerDialog.show()
        }
    }

    private fun setAlarm(hour: Int, minute: Int) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)

        val intent = Intent(this, AlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}