package scenic.rd.util01

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import scenic.rd.util01.ui.theme.FloatClockTheme

class MainActivity : ComponentActivity() {

    private var canDraw by mutableStateOf(false)
    private var running by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 13+ 请求通知权限，前台服务的常驻通知才能显示（不授也不影响悬浮窗本身）
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        setContent {
            FloatClockTheme {
                ControlScreen(
                    canDraw = canDraw,
                    running = running,
                    onToggle = ::onToggle
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从系统授权页返回后刷新权限状态；悬浮窗被轻点关闭后刷新开关状态
        canDraw = Settings.canDrawOverlays(this)
        running = FloatClockService.isRunning(this)

        onToggle(true)
    }

    private fun onToggle(enabled: Boolean) {
        if (enabled) {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
                return
            }
            val intent = Intent(this, FloatClockService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent)
            else startService(intent)
            running = true

            finish()
        } else {
            stopService(Intent(this, FloatClockService::class.java))
            running = false
        }
    }
}

@Composable
private fun ControlScreen(
    canDraw: Boolean,
    running: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.float_clock_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (canDraw) {
                    if (running) stringResource(R.string.status_running)
                    else stringResource(R.string.status_permission_ok)
                } else {
                    stringResource(R.string.status_need_permission)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onToggle(!running) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (running) stringResource(R.string.stop_float_clock)
                    else stringResource(R.string.start_float_clock)
                )
            }

            if (running) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.hint_drag_tap),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

