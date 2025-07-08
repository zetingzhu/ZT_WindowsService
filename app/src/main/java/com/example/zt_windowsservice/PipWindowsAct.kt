package com.example.zt_windowsservice

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner // 用于检测应用前后台切换

class PipWindowsAct : AppCompatActivity() {

    private lateinit var textViewPiPStatus: TextView
    private lateinit var buttonEnterPiP: Button // 可选，用于手动进入 PiP
    var isInPiPMode: Boolean = false

    // 用于 PiP 操作的常量
    private val ACTION_OPEN_APP = "com.example.zt_windowsservice.OPEN_APP"
    private val ACTION_CLOSE_PIP = "com.example.zt_windowsservice.CLOSE_PIP"
    private val REQUEST_CODE_OPEN_APP = 101
    private val REQUEST_CODE_CLOSE_PIP = 102

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, PipWindowsAct::class.java)
            context.startActivity(starter)
        }
    }


    // 在 PipWindowsAct.kt 中
    private val pipActionsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_OPEN_APP -> {
                    val openAppIntent = Intent(
                        this@PipWindowsAct, // <<< 关键点
                        PipWindowsAct::class.java // <<< 假设你想打开 MainActivity
                    ).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(openAppIntent) // <<< 关键点: 这个 startActivity 是从哪里调用的？
                }

                ACTION_CLOSE_PIP -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        finishAndRemoveTask() // <<< 关键点: 这个 finishAndRemoveTask 是 PipWindowsAct 的方法
                    } else {
                        finish() // <<< 关键点: 这个 finish 是 PipWindowsAct 的方法
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pip_windows)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        textViewPiPStatus = findViewById(R.id.textView_pip_status)
        buttonEnterPiP = findViewById(R.id.button_enter_pip)

        buttonEnterPiP.setOnClickListener {
            enterPiPMode()
        }

        // 注册 BroadcastReceiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intentFilter = IntentFilter().apply {
                addAction(ACTION_OPEN_APP)
                addAction(ACTION_CLOSE_PIP)
            }
            registerReceiver(pipActionsReceiver, intentFilter, RECEIVER_EXPORTED)
        }
    }


    // 监听应用进入后台自动进入 PiP
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // 当用户按下 Home 键或切换到其他应用时，此方法会被调用
        // 这是进入 PiP 模式的一个好时机
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isInPictureInPictureMode && supportsPiP()) { // 避免重复进入
                enterPiPMode()
            }
        }
    }

    private fun supportsPiP(): Boolean {
        return packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    private fun enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && supportsPiP()) {
            // 设置 PiP 窗口的宽高比 (例如 16:9)
            val aspectRatio = Rational(16, 9)
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .setActions(getPipActions()) // 设置 PiP 窗口中的操作按钮
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: IllegalStateException) {
                // 通常发生在 Activity 尚未完全启动或处于不合适的状态时
                Toast.makeText(this, "无法进入画中画模式: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun getPipActions(): ArrayList<RemoteAction>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val actions = ArrayList<RemoteAction>()

            // 1. 打开应用按钮
            val openAppIntent = PendingIntent.getBroadcast(
                this,
                REQUEST_CODE_OPEN_APP,
                Intent(ACTION_OPEN_APP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            actions.add(
                RemoteAction(
                    Icon.createWithResource(this, R.drawable.ic_open_in_app), // 替换为你的图标
                    "打开应用",
                    "打开应用",
                    openAppIntent
                )
            )

            // 2. 关闭按钮
            val closePipIntent = PendingIntent.getBroadcast(
                this,
                REQUEST_CODE_CLOSE_PIP,
                Intent(ACTION_CLOSE_PIP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            actions.add(
                RemoteAction(
                    Icon.createWithResource(this, R.drawable.ic_close_pip), // 替换为你的图标
                    "关闭",
                    "关闭画中画",
                    closePipIntent
                )
            )
            return actions
        }
        return null
    }


    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        this.isInPiPMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            // 进入 PiP 模式
            textViewPiPStatus.text = "当前处于画中画模式"
            // 隐藏 Activity 中的非必要 UI 元素
            buttonEnterPiP.visibility = View.GONE
            // 你可能还想隐藏其他视图，只保留 PiP 窗口中需要显示的内容
            // 例如，如果你是在播放视频，此时应该确保视频继续播放，并隐藏其他控制 UI
        } else {
            // 退出 PiP 模式
            textViewPiPStatus.text = "已退出画中画模式"
            // 恢复 Activity 中的 UI 元素
            buttonEnterPiP.visibility = View.VISIBLE
        }
    }

    override fun onStop() {
        super.onStop()
        // 如果应用进入后台（不是因为 PiP），并且当前是 PiP 模式，我们可能希望保持 PiP
        // 如果不是 PiP 模式而进入后台（例如用户按 back 键），则正常 onStop
        // onUserLeaveHint() 已经处理了切换应用时进入 PiP 的情况
    }

    override fun onDestroy() {
        super.onDestroy()
        // 注销 BroadcastReceiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            unregisterReceiver(pipActionsReceiver)
        }
    }
}

