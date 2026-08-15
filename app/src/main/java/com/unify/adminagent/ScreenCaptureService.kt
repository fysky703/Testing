// File: ScreenCaptureService.kt
package com.unify.adminagent

import android.app.Service
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream
import java.net.URI
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import javax.imageio.ImageIO

class ScreenCaptureService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var webSocket: WebSocketClient? = null
    private var isStreaming = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(2, NotificationCompat.Builder(this, "screen_channel")
            .setContentTitle("Screen Streaming")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build())

        val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>("data")
        if (resultCode != -1 && data != null) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            startStreaming()
        }
        return START_STICKY
    }

    private fun startStreaming() {
        val metrics = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, android.graphics.ImageFormat.JPEG, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()

            // Send via WebSocket to Admin
            webSocket?.send(bytes)
        }, null)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenStream",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        // Connect WebSocket to Admin server (IP from Firebase)
        connectWebSocket()
    }

    private fun connectWebSocket() {
        val serverUrl = "ws://192.168.1.100:8080" // Replace with Admin's IP from Firebase
        webSocket = object : WebSocketClient(URI(serverUrl)) {
            override fun onOpen(handshake: ServerHandshake?) {
                isStreaming = true
            }
            override fun onMessage(message: String?) {}
            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                isStreaming = false
            }
            override fun onError(ex: Exception?) { ex?.printStackTrace() }
        }
        webSocket?.connect()
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        webSocket?.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}