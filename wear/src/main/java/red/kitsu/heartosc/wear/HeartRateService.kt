package red.kitsu.heartosc // Ensure this matches your project

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
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
    private val measureClient by lazy { HealthServices.getClient(this).measureClient }

    private val measureCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {}

        override fun onDataReceived(data: DataPointContainer) {
            val heartRateData = data.getData(DataType.HEART_RATE_BPM)
            if (heartRateData.isNotEmpty()) {
                val latestBpm = heartRateData.last().value.toInt()
                sendHeartRateToPhone(latestBpm)
                
                // Broadcast to MainActivity just to update the watch UI if it happens to be open
                val intent = Intent("HEART_RATE_UPDATE")
                intent.putExtra("bpm", latestBpm)
                sendBroadcast(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create a persistent notification required by Android for background services
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HeartOSC")
            .setContentText("Tracking heart rate for VR...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details) // Default icon, replace with yours if needed
            .setOngoing(true)
            .build()

        startForeground(1, notification)

        // Start listening to the sensor
        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, measureCallback)

        return START_STICKY // Keeps the service running if the system briefly kills it
    }

    override fun onDestroy() {
        super.onDestroy()
        measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, measureCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

   private fun sendHeartRateToPhone(bpm: Int) {
        val messageClient = Wearable.getMessageClient(this)
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                // Try sending as a raw Integer byte array instead of a String
                val buffer = java.nio.ByteBuffer.allocate(4).putInt(bpm)
                messageClient.sendMessage(node.id, "/heartrate", buffer.array())
            }
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Background Heart Rate Tracking",
            NotificationManager.IMPORTANCE_LOW // Low importance prevents constant buzzing on the watch
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
