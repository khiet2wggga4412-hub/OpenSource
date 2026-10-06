package dev.liquid.clickgui.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.liquid.clickgui.MainActivity
import dev.liquid.clickgui.ui.AnimatedOverlaySwitcher
import dev.liquid.clickgui.ui.ClickGuiController
import dev.liquid.clickgui.ui.ClickGuiConfigStore
import dev.liquid.clickgui.ui.LiquidClickGuiScreen
import dev.liquid.clickgui.ui.LiquidFloatingButton
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private lateinit var controller: ClickGuiController

    private var expanded by mutableStateOf(false)
    private var layoutExpanded = false
    private var customExpandedWidth: Int? = null
    private var customExpandedHeight: Int? = null

    override fun onCreate() {
        super.onCreate()
        controller = ClickGuiController(configStore = ClickGuiConfigStore(this))
        createNotificationChannel()
        startForeground()
        windowManager = getSystemService(WindowManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val initialExpanded = intent?.action != ACTION_SHOW_COLLAPSED
        if (composeView == null) {
            createOverlay(initialExpanded)
        } else {
            updateExpandedState(initialExpanded)
        }
        return START_STICKY
    }

    private fun createOverlay(initialExpanded: Boolean) {
        expanded = initialExpanded
        layoutExpanded = initialExpanded
        lifecycleOwner = OverlayLifecycleOwner().also {
            it.create()
            it.start()
            it.resume()
        }

        val size = if (expanded) expandedSize() else collapsedSize()
        params = WindowManager.LayoutParams(
            size.first,
            size.second,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            windowFlags(expanded),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            positionWindow(this, expanded)
        }

        val owner = requireNotNull(lifecycleOwner)
        composeView = ComposeView(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnimatedOverlaySwitcher(
                    expanded = expanded,
                    onExpandedLayoutRequired = { applyWindowLayout(true) },
                    onCollapsedLayoutRequired = { applyWindowLayout(false) },
                    expandedContent = {
                        LiquidClickGuiScreen(
                            controller = controller,
                            modifier = Modifier.clip(RoundedCornerShape(26.dp)),
                            onCollapse = { updateExpandedState(false) },
                            onExit = { stopSelf() },
                            onResize = { delta -> resizeExpandedPanel(delta.x, delta.y) },
                        )
                    },
                    collapsedContent = {
                        LiquidFloatingButton(
                            onExpand = { updateExpandedState(true) },
                        )
                    },
                )
            }
        }
        windowManager.addView(composeView, params)
    }

    private fun updateExpandedState(value: Boolean) {
        if (composeView == null || expanded == value) return
        expanded = value
    }

    private fun applyWindowLayout(value: Boolean) {
        val view = composeView ?: return
        val size = if (value) expandedSize() else collapsedSize()
        params.width = size.first
        params.height = size.second
        params.flags = windowFlags(value)
        positionWindow(params, value)
        layoutExpanded = value
        windowManager.updateViewLayout(view, params)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val view = composeView ?: return
        val size = if (layoutExpanded) expandedSize() else collapsedSize()
        params.width = size.first
        params.height = size.second
        params.flags = windowFlags(layoutExpanded)
        positionWindow(params, layoutExpanded)
        windowManager.updateViewLayout(view, params)
    }

    override fun onDestroy() {
        composeView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        composeView = null
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun expandedSize(): Pair<Int, Int> {
        val bounds = currentBounds()
        val maxWidth = (bounds.width() * 0.95f).roundToInt().coerceAtMost(dp(1080))
        val maxHeight = (bounds.height() * 0.90f).roundToInt().coerceAtMost(dp(720))
        val minWidth = dp(640).coerceAtMost(maxWidth)
        val minHeight = dp(390).coerceAtMost(maxHeight)
        return (customExpandedWidth?.coerceIn(minWidth, maxWidth) ?: maxWidth) to
            (customExpandedHeight?.coerceIn(minHeight, maxHeight)
                ?: (bounds.height() * 0.86f).roundToInt().coerceAtMost(maxHeight))
    }

    private fun resizeExpandedPanel(deltaX: Float, deltaY: Float) {
        val view = composeView ?: return
        if (!layoutExpanded) return
        val bounds = currentBounds()
        val maxWidth = (bounds.width() * 0.95f).roundToInt().coerceAtMost(dp(1080))
        val maxHeight = (bounds.height() * 0.90f).roundToInt().coerceAtMost(dp(720))
        val minWidth = dp(640).coerceAtMost(maxWidth)
        val minHeight = dp(390).coerceAtMost(maxHeight)
        customExpandedWidth = (params.width + deltaX * 2f).roundToInt()
            .coerceIn(minWidth, maxWidth)
        customExpandedHeight = (params.height + deltaY * 2f).roundToInt()
            .coerceIn(minHeight, maxHeight)
        params.width = requireNotNull(customExpandedWidth)
        params.height = requireNotNull(customExpandedHeight)
        positionWindow(params, true)
        windowManager.updateViewLayout(view, params)
    }

    private fun collapsedSize(): Pair<Int, Int> = dp(60) to dp(60)

    private fun windowFlags(isExpanded: Boolean): Int {
        val base = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        return if (isExpanded) base else base or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    }

    private fun positionWindow(layoutParams: WindowManager.LayoutParams, isExpanded: Boolean) {
        val bounds = currentBounds()
        layoutParams.x = if (isExpanded) {
            ((bounds.width() - layoutParams.width) / 2).coerceAtLeast(0)
        } else {
            (bounds.width() - layoutParams.width - dp(18)).coerceAtLeast(0)
        }
        layoutParams.y = ((bounds.height() - layoutParams.height) / 2).coerceAtLeast(0)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun currentBounds(): android.graphics.Rect = windowManager.currentWindowMetrics.bounds

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Glass Client overlay",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps the user-invoked ClickGUI overlay available"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startForeground() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("Glass Client")
            .setContentText("Overlay is available")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        const val ACTION_SHOW_COLLAPSED = "dev.liquid.clickgui.action.SHOW_COLLAPSED"
        const val ACTION_SHOW_EXPANDED = "dev.liquid.clickgui.action.SHOW_EXPANDED"
        private const val CHANNEL_ID = "liquid_clickgui_overlay"
        private const val NOTIFICATION_ID = 0x1901
    }
}

private class OverlayLifecycleOwner :
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = registry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    fun create() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun start() = registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    fun resume() = registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

    fun destroy() {
        if (registry.currentState == Lifecycle.State.DESTROYED) return
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}
