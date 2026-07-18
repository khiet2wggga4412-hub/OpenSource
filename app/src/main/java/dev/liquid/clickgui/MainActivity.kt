package dev.liquid.clickgui

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * 模块桌面入口按要求保持为空白。
 * ClickGUI 只会在目标进程完成壳后 ClassLoader 初始化并确认 Hook 成功后载入。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
