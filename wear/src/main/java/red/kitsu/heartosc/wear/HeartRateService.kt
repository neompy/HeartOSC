package red.kitsu.heartosc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import com.google.android.gms.wearable.Wearable

class HeartRateService : Service() {

    private val CHANNEL_ID = "HeartOSC_Background_Tracking"
    private var wakeLock: PowerManager.WakeLock? = null
    private val measureClient by lazy { HealthServices.getClient(this).measureClient }

    private val measureCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {}

        override fun onDataReceived(data: DataPointContainer) {
            val heartRateData = data.getData(DataType.HEART_RATE_BPM)
            if (heartRateData.isNotEmpty()) {
                val latestBpm = heartRateData.last().value.toInt()
                
                // Send heart rate data to phone app
                sendHeartRateToPhone(latestBpm)
                
                // Broadcast to MainActivity if it is open
                val intent = Intent("HEART_RATE_UPDATE")
                intent.putExtra("bpm", latestBpm)
                sendBroadcast(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Acquire Partial WakeLock to keep the watch CPU running when the screen turns off
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HeartOSC::BackgroundTracking"
        )
        wakeLock?.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create persistent notification required by Android for background tracking
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HeartOSC")
            .setContentText("Tracking heart rate for VR...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        // Start listening to the sensor
        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, measureCallback)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Unregister sensor callback
        measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, measureCallback)
        
        // Release WakeLock so the CPU can go back to sleep
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun sendHeartRateToPhone(bpm: Int) {
        val messageClient = Wearable.getMessageClient(this)
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                messageClient.sendMessage(node.id, "/heartrate", bpm.toString().toByteArray())
            }
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Background Heart Rate Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
