package com.example.zt_windowsservice // 确保包名正确

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlin.io.path.name

class MyApplication : Application() { // Removed LifecycleObserver, will use LifecycleEventObserver

    companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
        Log.d(TAG, "MyApplication onCreate, ProcessLifecycleObserver added.")
    }

    private inner class AppLifecycleObserver : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_START -> {
                    Log.d(TAG, "App is coming to FOREGROUND")
                    // 应用返回前台，关闭悬浮窗 Service
                    if (isFloatingWindowServiceRunning()) {
                        Log.d(TAG, "FloatingWindowService is running, stopping it.")
                        stopService(Intent(applicationContext, FloatingWindowService::class.java))
                    } else {
                        Log.d(TAG, "FloatingWindowService is not running.")
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    Log.d(TAG, "App is going to BACKGROUND")
                    // 应用进入后台
                    // 检查悬浮窗权限
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(applicationContext)) {
                            Log.d(TAG, "Overlay permission granted.")
                            if (!isFloatingWindowServiceRunning()) {
                                Log.d(TAG, "FloatingWindowService not running, starting it.")
                                startService(Intent(applicationContext, FloatingWindowService::class.java))
                            } else {
                                Log.d(TAG, "FloatingWindowService already running.")
                            }
                        } else {
                            Log.w(TAG, "Overlay permission NOT granted. Cannot show floating window.")
                            // 你可以在这里考虑发送一个通知或者给用户一个提示，
                            // 告知他们需要悬浮窗权限才能使用此功能。
                            // 但通常情况下，权限请求应该由 Activity 发起。
                        }
                    } else {
                        // Android M 以下版本，权限在安装时授予 (如果 Manifest 中声明了)
                        Log.d(TAG, "Below Android M, assuming permission if declared.")
                        if (!isFloatingWindowServiceRunning()) {
                            Log.d(TAG, "FloatingWindowService not running, starting it.")
                            startService(Intent(applicationContext, FloatingWindowService::class.java))
                        } else {
                            Log.d(TAG, "FloatingWindowService already running.")
                        }
                    }
                }
                else -> {
                    // 其他生命周期事件，例如 ON_CREATE, ON_RESUME, ON_PAUSE, ON_DESTROY
                    // Log.d(TAG, "Lifecycle event: $event")
                }
            }
        }
    }

    // 辅助方法检查 Service 是否正在运行
    private fun isFloatingWindowServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        try {
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (FloatingWindowService::class.java.name == service.service.className) {
                    Log.d(TAG, "Found running service: ${service.service.className}")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking running services: ", e)
            // Fallback or rethrow, depending on how critical this check is
        }
        Log.d(TAG, "FloatingWindowService not found in running services.")
        return false
    }
}

