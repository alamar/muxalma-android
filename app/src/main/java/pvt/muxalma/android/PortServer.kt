package pvt.muxalma.android

import android.util.Log
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.util.UUID
import kotlin.concurrent.thread


class PortServer(
    private val transport: TransportService,
    private val port: Int,
    private val clientId: UUID,
    private val assetsCacheDir: File?
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    
    fun start(onStarted: () -> Unit = {}) {
        thread {
            try {
                transport.runTransport(assetsCacheDir, clientId, port)
            } catch (e: IOException) {
                if (isRunning) {
                    Log.e("PortServer", "Server error: ${e.message}")
                }
            }
        }
    }
    
    fun stop() {
        isRunning = false
        try {
            transport.terminate(assetsCacheDir)
        } catch (e: IOException) {
            Log.e("PortServer", "Error closing server: ${e.message}")
        }
    }
    
    fun isActive(): Boolean = isRunning && serverSocket != null && !serverSocket!!.isClosed
}
