package com.example.zt_windowsservice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.zt_windowsservice.ui.theme.ZT_WindowsServiceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZT_WindowsServiceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val current = LocalContext.current
    Column {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        Button(onClick = {
            SystemWindowsAct.start(current)
        }) {
            Text(text = "系统悬浮窗模式")
        }
        Button(onClick = {
            PipWindowsAct.start(current)
        }) {
            Text(text = "普通文本 画中画交互")
        }
        Button(onClick = {
            DownTimeAct.start(current)
        }) {
            Text(text = "倒计时 画中画")
        }
        Button(onClick = {
            MovieActivity.start(current)
        }) {
            Text(text = "视频 画中画")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ZT_WindowsServiceTheme {
        Greeting("Android")
    }
}