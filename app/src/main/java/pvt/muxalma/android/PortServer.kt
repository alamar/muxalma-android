package pvt.muxalma.android

import android.util.Log
import java.io.IOException
import java.net.ServerSocket
import kotlin.concurrent.thread

class PortServer(private val port: Int) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    
    fun start(onStarted: () -> Unit = {}) {
        thread {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                Log.i("PortServer", "Server listening on port $port")
                onStarted()
                
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    clientSocket.use { socket ->
                        val response = "Hello! Your request was received.\n"
                        socket.getOutputStream().write(response.toByteArray())
                        socket.close()
                    }
                }
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
            serverSocket?.close()
            serverSocket = null
            Log.i("PortServer", "Server stopped")
        } catch (e: IOException) {
            Log.e("PortServer", "Error closing server: ${e.message}")
        }
    }
    
    fun isActive(): Boolean = isRunning && serverSocket != null && !serverSocket!!.isClosed
}
