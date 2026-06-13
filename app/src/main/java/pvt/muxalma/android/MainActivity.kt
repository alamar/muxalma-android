package pvt.muxalma.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.net.ServerSocket
import java.util.UUID
import java.util.UUID.randomUUID

class MainActivity : AppCompatActivity() {
    
    private lateinit var portTextView: TextView
    private lateinit var closeButton: Button
    private var currentPort: Int = -1
    private var clientId: String = ""
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        portTextView = findViewById(R.id.portTextView)
        closeButton = findViewById(R.id.closeButton)
        
        checkAndRequestPermissions()
    }
    
    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                initializeApp()
            }
        } else {
            initializeApp()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            initializeApp()
        }
    }
    
    private fun initializeApp() {
        currentPort = PortService.getSavedPort(this)
        clientId = PortService.getSavedClientId(this)
        
        if (currentPort == -1 || clientId.isEmpty()) {
            // Первый запуск - генерируем UUID и порт
            currentPort = findFreePort()
            clientId = UUID.randomUUID().toString()
            PortService.savePort(this, currentPort)
            PortService.saveClientId(this, clientId)
            android.util.Log.i("MainActivity", "Client UUID: $clientId, Port: $currentPort")
        }
        
        // Запускаем сервис
        PortService.start(this, currentPort, clientId)
        
        // Показываем адрес
        val localAddress = "localhost:$currentPort"
        portTextView.text = localAddress
        
        // Обработка клика для копирования
        portTextView.setOnClickListener {
            copyToClipboard(localAddress)
            Toast.makeText(this, "Copied: $localAddress", Toast.LENGTH_SHORT).show()
        }
        
        // Кнопка закрытия
        closeButton.setOnClickListener {
            PortService.stop(this)
            finishAffinity()
        }
    }
    
    private fun findFreePort(): Int {
        val safeRange = 49152..65535
        for (port in safeRange) {
            if (isPortAvailable(port)) {
                return port
            }
        }
        return 50000 // fallback
    }
    
    private fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { it.localPort == port }
            true
        } catch (e: IOException) {
            false
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Localhost Address", text)
        clipboard.setPrimaryClip(clip)
    }
}
