package com.hizkifw.heartosc // Ensure this matches your actual folder structure

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {

    private val PERMISSION_REQUEST_CODE = 100
    private lateinit var statusText: TextView
    
    // Health Services
    private val measureClient by lazy { HealthServices.getClient(this).measureClient }

    // Callback that receives the modern heart rate data
    private val measureCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
            if (availability is DataTypeAvailability) {
                Log.d("HeartOSC", "Sensor availability: $availability")
            }
        }

        override fun onDataReceived(data: DataPointContainer) {
            val heartRateData = data.getData(DataType.HEART_RATE_BPM)
            if (heartRateData.isNotEmpty()) {
                val latestBpm = heartRateData.last().value.toInt()
                Log.d("HeartOSC", "Heart Rate: $latestBpm")
                
                runOnUiThread {
                    statusText.text = "Heart Rate: $latestBpm BPM"
                }
                
                // Send data to the phone app
                sendHeartRateToPhone(latestBpm)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup a simple text view if you don't have a layout XML file
        statusText = TextView(this).apply {
            text = "Waiting for permission..."
            textSize = 20f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
        setContentView(statusText)

        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, 
                arrayOf(Manifest.permission.BODY_SENSORS), 
                PERMISSION_REQUEST_CODE
            )
        } else {
            startHeartRateTracking()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && 
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startHeartRateTracking()
        } else {
            statusText.text = "Permission Denied.\nCannot read heart rate."
        }
    }

    private fun startHeartRateTracking() {
        statusText.text = "Tracking..."
        // Register the new Jetpack Health Services measure callback
        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, measureCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Always unregister to save watch battery when app closes
        measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, measureCallback)
    }

    private fun sendHeartRateToPhone(bpm: Int) {
        val messageClient = Wearable.getMessageClient(this)
        val nodeIdTask = Wearable.getNodeClient(this).connectedNodes

        nodeIdTask.addOnSuccessListener { nodes ->
            for (node in nodes) {
                // IMPORTANT: Ensure the path "/heartrate" matches what your phone app is expecting!
                messageClient.sendMessage(node.id, "/heartrate", bpm.toString().toByteArray())
            }
        }
    }
}
