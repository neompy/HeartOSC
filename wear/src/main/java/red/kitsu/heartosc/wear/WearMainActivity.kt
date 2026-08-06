package red.kitsu.heartosc

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            // Set background to pure black to save OLED battery
            setBackgroundColor(android.graphics.Color.BLACK) 
        }

        statusText = TextView(this).apply {
            text = "Ready to track"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
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

        // VR HACK: Force screen to stay awake but dim it to absolute minimum
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val params = window.attributes
        params.screenBrightness = 0.01f
        window.attributes = params
    }

    private fun stopTracking() {
        val serviceIntent = Intent(this, HeartRateService::class.java)
        stopService(serviceIntent)
        
        isTracking = false
        toggleButton.text = "Start Tracking"
        statusText.text = "Tracking stopped."

        // VR HACK: Allow screen to turn off normally and restore brightness
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params
    }
}
