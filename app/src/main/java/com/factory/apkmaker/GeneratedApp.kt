package com.factory.apkmaker

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var timePicker: TimePicker
    private lateinit var button: Button
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timePicker = findViewById(R.id.timePicker)
        button = findViewById(R.id.button)

        button.setOnClickListener {
            val hour = timePicker.hour
            val minute = timePicker.minute

            Toast.makeText(this, "Alarm set for $hour:$minute", Toast.LENGTH_SHORT).show()

            Thread {
                while (true) {
                    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    val currentMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)

                    if (currentHour == hour && currentMinute == minute) {
                        runOnUiThread {
                            mediaPlayer = MediaPlayer.create(this, R.raw.alarm)
                            mediaPlayer.start()
                        }
                    }

                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }.start()
        }
    }
}