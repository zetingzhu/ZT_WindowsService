package com.example.zt_windowsservice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class SystemWindowsAct : AppCompatActivity() {
    private val REQUEST_CODE_SYSTEM_ALERT_WINDOW = 101

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, SystemWindowsAct::class.java)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_system_windows)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // You might want to trigger the permission check when a button is clicked
        // For example, if you have a button with id 'button_show_floating_window':
        // findViewById<Button>(R.id.button_show_floating_window).setOnClickListener {
        //     checkAndRequestOverlayPermission()
        // }
        // Or call it directly for testing:
        checkAndRequestOverlayPermission() // Call this to initiate the process
    }


    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE_SYSTEM_ALERT_WINDOW)
            } else {
                // 权限已授予，可以创建悬浮窗

            }
        } else {
            // Android M 以下版本，权限在安装时授予
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SYSTEM_ALERT_WINDOW) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // 权限已授予
                    showFloatingWindow()
                } else {
                    // 权限被拒绝
                    Toast.makeText(this, "悬浮窗权限未授予", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Defines the action to take when permission is granted to show the floating window.
     * This typically involves starting the FloatingWindowService.
     */
    private fun showFloatingWindow() {
        // First, ensure the service is not already running if you don't want multiple instances
        // (You might need a more robust check depending on your app's logic)
        // For simplicity, we'll just start it here.
        // If you have the isFloatingWindowServiceRunning() method in your Application class,
        // you might not need it here directly, as the Application class could handle
        // the logic of when to show/hide it based on app foreground/background state.

        // However, if this Activity is specifically for launching it on demand:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "悬浮窗权限未授予，无法显示悬浮窗。", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "权限已授予，正在尝试启动悬浮窗服务...", Toast.LENGTH_SHORT).show()
        // Assuming FloatingWindowService is in the same package
        val intent = Intent(this, FloatingWindowService::class.java)
        startService(intent)

        // Optionally, if this activity's only purpose is to launch the window, you can finish it.
        // finish()
    }
}
