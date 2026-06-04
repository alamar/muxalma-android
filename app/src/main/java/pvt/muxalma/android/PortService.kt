package pvt.muxalma.android

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.net.ServerSocket

class PortService : Service() {
    private var portServer: PortServer? = null
    private var currentPort: Int = 0
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "port_service_channel"
        private const val PREFS_NAME = "port_prefs"
        private const val KEY_PORT = "server_port"
        
        fun start(context: Context, port: Int) {
            val intent = Intent(context, PortService::class.java).apply {
                putExtra("PORT", port)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, PortService::class.java)
            context.stopService(intent)
        }
        
        fun getSavedPort(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_PORT, -1)
        }
        
        fun savePort(context: Context, port: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_PORT, port).apply()
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentPort = intent?.getIntExtra("PORT", -1) ?: -1
        
        if (currentPort == -1) {
            stopSelf()
            return START_NOT_STICKY
        }
        
        startForeground(NOTIFICATION_ID, buildNotification())
        
        portServer = PortServer(currentPort)
        portServer?.start()
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        portServer?.stop()
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Port Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the port server running"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Port Server Active")
            .setContentText("Listening on port $currentPort")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
