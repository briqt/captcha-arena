package com.briqt.captchaarena

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.briqt.captchaarena.captcha.clickword.ClickWordCaptchaView
import com.briqt.captchaarena.captcha.rotate.RotateCaptchaView
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

data class TabItem(val title: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptchaArenaApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        TabItem("滑块", Icons.Default.SwipeRight),
        TabItem("点选", Icons.Default.TouchApp),
        TabItem("旋转", Icons.Default.RotateRight),
        TabItem("极验", Icons.Default.Security),
        TabItem("腾讯", Icons.Default.Verified),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CaptchaArena") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> SliderCaptchaScreen()
                1 -> ClickWordCaptchaScreen()
                2 -> RotateCaptchaScreen()
                3 -> GeeTestScreen()
                4 -> TencentCaptchaScreen()
            }
        }
    }
}

@Composable
fun SliderCaptchaScreen() {
    var resultText by remember { mutableStateOf("拖动滑块完成拼图验证") }
    var trajectoryInfo by remember { mutableStateOf("") }
    var captchaView by remember { mutableStateOf<SliderCaptchaView?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            factory = { context ->
                SliderCaptchaView(context).apply {
                    captchaView = this
                    onCaptchaResult = { success, trajectory ->
                        resultText = if (success) "✅ 验证通过！" else "❌ 验证失败"
                        trajectoryInfo = formatTrajectory(trajectory)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(280.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        ResultCard(resultText, trajectoryInfo)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { captchaView?.reset() }) { Text("重置") }
    }
}

@Composable
fun ClickWordCaptchaScreen() {
    var resultText by remember { mutableStateOf("按顺序点击指定文字") }
    var trajectoryInfo by remember { mutableStateOf("") }
    var captchaView by remember { mutableStateOf<ClickWordCaptchaView?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            factory = { context ->
                ClickWordCaptchaView(context).apply {
                    captchaView = this
                    onCaptchaResult = { success, trajectory ->
                        resultText = if (success) "✅ 验证通过！" else "❌ 点击顺序错误"
                        trajectoryInfo = formatTrajectory(trajectory)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(320.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        ResultCard(resultText, trajectoryInfo)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { captchaView?.reset() }) { Text("重置") }
    }
}

@Composable
fun RotateCaptchaScreen() {
    var resultText by remember { mutableStateOf("旋转内圈使箭头朝上") }
    var trajectoryInfo by remember { mutableStateOf("") }
    var captchaView by remember { mutableStateOf<RotateCaptchaView?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            factory = { context ->
                RotateCaptchaView(context).apply {
                    captchaView = this
                    onCaptchaResult = { success, trajectory ->
                        resultText = if (success) "✅ 验证通过！" else "❌ 角度不对"
                        trajectoryInfo = formatTrajectory(trajectory)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(320.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        ResultCard(resultText, trajectoryInfo)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { captchaView?.reset() }) { Text("重置") }
    }
}

@Composable
fun GeeTestScreen() {
    val context = LocalContext.current
    var resultText by remember { mutableStateOf("点击按钮启动极验验证码") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("极验 GeeTest GT4", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("集成极验行为验证 SDK（滑块/点选/九宫棋）", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            try {
                // GeeTest GT4 SDK integration
                val clazz = Class.forName("com.geetest.captcha.GTCaptcha4Client")
                val getClient = clazz.getMethod("getClient", android.app.Activity::class.java)
                val client = getClient.invoke(null, context)

                // Build config
                val configClazz = Class.forName("com.geetest.captcha.GTCaptcha4Config\$Builder")
                val configBuilder = configClazz.getDeclaredConstructor().newInstance()
                val buildMethod = configClazz.getMethod("build")
                val config = buildMethod.invoke(configBuilder)

                // Init
                val initMethod = clazz.getMethod("init", String::class.java, config.javaClass)
                initMethod.invoke(client, "fcd636b4514bf7ac4143922550b3008b", config)

                // Verify
                val verifyMethod = clazz.getMethod("verifyWithCaptcha")
                verifyMethod.invoke(client)
                resultText = "极验验证码已启动..."
            } catch (e: Exception) {
                resultText = "SDK 加载失败: ${e.message}"
            }
        }) {
            Text("启动极验验证")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(resultText, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun TencentCaptchaScreen() {
    var resultText by remember { mutableStateOf("加载腾讯天御验证码...") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "腾讯天御验证码",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(12.dp)
        )

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onResult(data: String) {
                            (context as? ComponentActivity)?.runOnUiThread {
                                resultText = "结果: $data"
                                Toast.makeText(context, "验证完成", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, "CaptchaBridge")
                    loadUrl("file:///android_asset/tencent_captcha.html")
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        if (resultText.startsWith("结果")) {
            Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(resultText, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ResultCard(result: String, trajectory: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(result, style = MaterialTheme.typography.titleSmall)
            if (trajectory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(trajectory, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatTrajectory(data: TrajectoryData): String {
    return buildString {
        appendLine("采样点: ${data.points.size} | 时长: ${data.duration}ms")
        appendLine("距离: ${"%.0f".format(data.totalDistance)}px | 速度: ${"%.2f".format(data.averageSpeed)}px/ms")
        appendLine("Y偏移: ${"%.1f".format(data.maxYDeviation)}px")
        if (data.points.isNotEmpty()) {
            val p = data.points.map { it.pressure }
            append("压力: ${"%.2f".format(p.min())}-${"%.2f".format(p.max())}")
        }
    }
}
