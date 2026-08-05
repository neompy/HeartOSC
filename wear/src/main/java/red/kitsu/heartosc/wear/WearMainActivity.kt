package com.hizkifw.heartosc // Ensure this matches your project

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    private var isTracking = false

    // Listens for updates from our background service to update the UI
    private val heartRateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val bpm = intent?.getIntExtra("bpm", 0) ?: 0
            if (bpm > 0) {
                statusText.text = "Heart Rate: $bpm BPM"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Simple UI Layout
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "Ready to track"
            textSize = 20f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(0, 0, 0, 30)
        }

        toggleButton = Button(this).apply {
            text = "Start Tracking"
            setOnClickListener {
                if (isTracking) stopTracking() else checkPermissionsAndStart()
            }
        }

        layout.addView(statusText)
        layout.addView(toggleButton)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        // Listen for updates from the service while the screen is on
        val filter = IntentFilter("HEART_RATE_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(heartRateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(heartRateReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(heartRateReceiver)
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(Manifest.permission.BODY_SENSORS)
        
        // Android 13+ requires notification permissions for Foreground Services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            startTracking()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startTracking()
            } else {
                statusText.text = "Permissions required\nto track heart rate."
            }
        }
    }

    private fun startTracking() {
        val serviceIntent = Intent(this, HeartRateService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        isTracking = true
        toggleButton.text = "Stop Tracking"
        statusText.text = "Starting sensor..."
    }

    private fun stopTracking() {
        val serviceIntent = Intent(this, HeartRateService::class.java)
        stopService(serviceIntent)
        
        isTracking = false
        toggleButton.text = "Start Tracking"
        statusText.text = "Tracking stopped."
    }
}
