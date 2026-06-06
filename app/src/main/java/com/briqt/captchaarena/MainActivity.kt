package com.briqt.captchaarena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.briqt.captchaarena.captcha.slider.SliderCaptchaView
import com.briqt.captchaarena.model.TrajectoryData

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CaptchaArenaApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptchaArenaApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("滑块拼图", "文字点选", "旋转验证")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("CaptchaArena") })
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {},
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> SliderCaptchaScreen()
                1 -> PlaceholderScreen("文字点选验证码 - 开发中")
                2 -> PlaceholderScreen("旋转验证码 - 开发中")
            }
        }
    }
}

@Composable
fun SliderCaptchaScreen() {
    var resultText by remember { mutableStateOf("拖动滑块完成验证") }
    var trajectoryInfo by remember { mutableStateOf("") }
    var captchaView by remember { mutableStateOf<SliderCaptchaView?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Captcha View
        AndroidView(
            factory = { context ->
                SliderCaptchaView(context).apply {
                    captchaView = this
                    onCaptchaResult = { success, trajectory ->
                        resultText = if (success) "✅ 验证通过！" else "❌ 验证失败"
                        trajectoryInfo = buildTrajectoryInfo(trajectory)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Result
        Text(
            text = resultText,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Trajectory analysis info
        if (trajectoryInfo.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("轨迹分析", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(trajectoryInfo, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset button
        Button(onClick = { captchaView?.reset() }) {
            Text("重置验证码")
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun buildTrajectoryInfo(data: TrajectoryData): String {
    return buildString {
        appendLine("采样点数: ${data.points.size}")
        appendLine("总时长: ${data.duration}ms")
        appendLine("总距离: ${"%.1f".format(data.totalDistance)}px")
        appendLine("平均速度: ${"%.3f".format(data.averageSpeed)}px/ms")
        appendLine("Y轴最大偏移: ${"%.1f".format(data.maxYDeviation)}px")
        if (data.points.isNotEmpty()) {
            val pressures = data.points.map { it.pressure }
            appendLine("压力范围: ${"%.3f".format(pressures.min())} - ${"%.3f".format(pressures.max())}")
        }
    }
}
