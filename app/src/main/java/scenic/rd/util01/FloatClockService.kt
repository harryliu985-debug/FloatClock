package scenic.rd.util01

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs

/**
 * 前台服务承载系统级悬浮时钟：
 * - TYPE_APPLICATION_OVERLAY 悬浮在所有应用之上（需"显示在其他应用上层"权限）
 * - 每 100ms 对齐刷新北京时间，精确到十分之一秒
 * - 拖动改位置，轻点关闭
 */
class FloatClockService : Service() {

    companion object {
        const val ACTION_STOP = "scenic.rd.util01.STOP_FLOAT_CLOCK"
        private const val CHANNEL_ID = "float_clock"
        private const val NOTIFICATION_ID = 1
        private const val RUN_PREFS = "float_clock_state"
        private const val KEY_RUNNING = "running"
        private const val TAP_SLOP_PX = 12

        /** 供控制页查询悬浮窗是否在跑 */
        fun isRunning(context: Context): Boolean =
            context.getSharedPreferences(RUN_PREFS, MODE_PRIVATE)
                .getBoolean(KEY_RUNNING, false)
    }

    private lateinit var windowManager: WindowManager
    private var clockView: TextView? = null
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())

    /** 刷新始终对齐下一个 100ms 边界（留 5ms 余量），保证十分位秒平滑跳变 */
    private val ticker = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            clockView?.text = BeijingTime.format(now)
            handler.postDelayed(this, 105 - now % 100)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        showFloatingClock()
        markRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        clockView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        clockView = null
        markRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.float_clock_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FloatClockService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val notification = builder
            .setContentTitle(getString(R.string.float_clock_running))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.stop), stopIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun showFloatingClock() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val tv = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            val hPad = dp(14)
            val vPad = dp(8)
            setPadding(hPad, vPad, hPad, vPad)
            background = GradientDrawable().apply {
                setColor(0xCC1C1C1E.toInt())
                cornerRadius = dp(16).toFloat()
            }
        }
        tv.text = BeijingTime.format(System.currentTimeMillis())
        attachDragAndTap(tv)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(120)
        }

        windowManager.addView(tv, params)
        clockView = tv
        handler.post(ticker)
    }

    /** 拖动移动悬浮窗；位移小于阈值视为轻点 → 关闭 */
    private fun attachDragAndTap(view: TextView) {
        var downRawX = 0f
        var downRawY = 0f
        var startParamX = 0
        var startParamY = 0
        var moved = false

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startParamX = params.x
                    startParamY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > TAP_SLOP_PX || abs(dy) > TAP_SLOP_PX) moved = true
                    if (moved) {
                        params.x = startParamX + dx.toInt()
                        params.y = startParamY + dy.toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) v.performClick() else stopSelf()
                    true
                }
                else -> false
            }
        }
    }

    private fun markRunning(running: Boolean) {
        getSharedPreferences(RUN_PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RUNNING, running)
            .apply()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
