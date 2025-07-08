package com.example.zt_windowsservice

/**
 * @author: zeting
 * @date: 2025/7/8
 *
 */
// FloatingWindowService.kt
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.semantics.text
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zt_windowsservice.R // 假设你的包名

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: ViewGroup? = null
    private lateinit var params: WindowManager.LayoutParams

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0.toFloat()
    private var initialTouchY: Float = 0.toFloat()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initLayoutParams()
    }

    private fun initLayoutParams() {
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, // 宽度
            WindowManager.LayoutParams.WRAP_CONTENT, // 高度
            // Android O 及以上需要 TYPE_APPLICATION_OVERLAY，其他版本用 TYPE_PHONE 或 TYPE_SYSTEM_ALERT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or // 不获取焦点，这样底层应用才能继续交互
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, // 可以监听窗口外的触摸事件
            PixelFormat.TRANSLUCENT // 透明背景
        )
        params.gravity = Gravity.TOP or Gravity.START // 初始位置
        params.x = 100 // 初始 X 坐标
        params.y = 100 // 初始 Y 坐标
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null) {
            showFloatingWindowInternal()
        }
        return START_STICKY // 或者根据需求选择其他返回值
    }

    private fun showFloatingWindowInternal() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // 创建悬浮窗的布局
        floatingView = inflater.inflate(R.layout.layout_floating_window, null) as ViewGroup

        // 1. 设置列表 (RecyclerView)
        val recyclerView = floatingView?.findViewById<RecyclerView>(R.id.recyclerView_floating)
        recyclerView?.layoutManager = LinearLayoutManager(this)
        // 假设你有一个 Adapter 和数据
        val data =
            mutableListOf("列表项 1", "列表项 2", "列表项 3", "列表项 4", "列表项 5", "列表项 6")
        val adapter = FloatingListAdapter(data)
        recyclerView?.adapter = adapter

        // 2. 设置关闭按钮
        val closeButton = floatingView?.findViewById<ImageView>(R.id.button_close_window)
        closeButton?.setOnClickListener {
            stopSelf() // 关闭 Service，从而移除悬浮窗
        }

        // 3. 设置打开应用按钮
        val openAppButton = floatingView?.findViewById<ImageView>(R.id.button_open_app)
        openAppButton?.setOnClickListener {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            startActivity(launchIntent?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
            stopSelf() // 打开应用后也关闭悬浮窗
        }

        // 4. 实现拖动逻辑
        val dragHandle =
            floatingView?.findViewById<LinearLayout>(R.id.drag_handle_layout) // 假设有一个拖动区域
        dragHandle?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }

                else -> false
            }
        }
        // 将悬浮窗添加到 WindowManager
        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            try {
                windowManager.removeView(floatingView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            floatingView = null
        }
    }
}

// 悬浮窗列表的 Adapter 示例
class FloatingListAdapter(private val items: List<String>) :
    RecyclerView.Adapter<FloatingListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1) // 假设使用简单的 text1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = items[position]
        holder.textView.setOnClickListener {
            // 处理列表项点击事件，例如打开应用并传递参数
        }
    }

    override fun getItemCount() = items.size
}
