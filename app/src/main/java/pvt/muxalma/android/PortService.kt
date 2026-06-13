package pvt.muxalma.android

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.util.ServiceLoader
import java.util.UUID


class PortService : Service() {
    private var portServer: PortServer? = null
    private var currentPort: Int = -1
    private var clientId: String = ""
    private var assetsCacheDir: File? = null
    private var transport: TransportService? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "port_service_channel"
        private const val PREFS_NAME = "port_prefs"
        private const val KEY_PORT = "proxy_port"
        private const val CLIENT_ID_PORT = "client_id"
        
        fun start(context: Context, port: Int, clientId: String) {
            val intent = Intent(context, PortService::class.java).apply {
                putExtra("PORT", port)
                putExtra("CLIENT_ID", clientId)
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

        fun getSavedClientId(context: Context): String  {
            val prefs: SharedPreferences = context.getSharedPreferences("client_prefs", MODE_PRIVATE)
            return prefs.getString(CLIENT_ID_PORT, "") ?: ""
        }

        fun saveClientId(context: Context, clientId: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(CLIENT_ID_PORT, clientId).apply()
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Здесь уже есть context! Создаём кэш директорию
        assetsCacheDir = File(cacheDir, "assets_cache")
        if (!assetsCacheDir!!.exists()) {
            assetsCacheDir!!.mkdirs()
        }

        // Загружаем транспорт
        val transportProviders: ServiceLoader<TransportService?> =
            ServiceLoader.load(TransportService::class.java)
        for (service in transportProviders) {
            transport = service
            Log.i("PortService", "Loaded implementation: " + transport!!.javaClass.name)
            transport!!.prepare(this, assetsCacheDir)
            break
        }
        if (transport == null) {
            Log.e("PortService", "No transport is loaded!")
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentPort = intent?.getIntExtra("PORT", -1) ?: -1
        clientId = intent?.getStringExtra("CLIENT_ID") ?: ""
        
        if (currentPort == -1 || clientId.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }
        
        startForeground(NOTIFICATION_ID, buildNotification())

        portServer = PortServer(transport!!, currentPort, UUID.fromString(clientId), assetsCacheDir)
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
