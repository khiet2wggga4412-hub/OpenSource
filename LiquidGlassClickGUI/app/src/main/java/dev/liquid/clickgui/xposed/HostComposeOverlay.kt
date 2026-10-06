package dev.liquid.clickgui.xposed

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
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
import dev.liquid.clickgui.ui.AnimatedOverlaySwitcher
import dev.liquid.clickgui.ui.ClickGuiController
import dev.liquid.clickgui.ui.ClickGuiConfigStore
import dev.liquid.clickgui.ui.ClickGuiRenderer
import dev.liquid.clickgui.ui.FloatingButtonConfig
import dev.liquid.clickgui.ui.INJECTION_INTRO_START_DELAY_MS
import dev.liquid.clickgui.ui.LiquidClickGuiScreen
import dev.liquid.clickgui.ui.LiquidFloatingButton
import dev.liquid.clickgui.ui.ModuleQuickFloatingButton
import dev.liquid.clickgui.ui.PersistentHudOverlay
import dev.liquid.clickgui.model.ModuleUiModel
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

internal object HostComposeOverlay {
    private var activityRef = WeakReference<Activity>(null)
    private var hostContainer: ViewGroup? = null
    private var layoutParams: FrameLayout.LayoutParams? = null
    private var composeView: ComposeView? = null
    private var persistentHudView: PersistentHudHost? = null
    private val quickButtonViews = linkedMapOf<String, ComposeView>()
    private val quickButtonPositions = mutableMapOf<String, Pair<Int, Int>>()
    private var lifecycleOwner: HostOverlayLifecycleOwner? = null
    private var controller: ClickGuiController? = null
    private var expanded by mutableStateOf(true)
    private var layoutExpanded by mutableStateOf(true)
    private var floatingButtonX: Int? = null
    private var floatingButtonY: Int? = null
    private var customExpandedWidth: Int? = null
    private var customExpandedHeight: Int? = null

    fun isAttachedTo(activity: Activity): Boolean =
        activityRef.get() === activity &&
            composeView?.isAttachedToWindow == true &&
            persistentHudView?.isAttachedToWindow == true &&
            quickButtonViews.values.all { it.isAttachedToWindow }

    @Synchronized
    fun attach(activity: Activity, onComplete: (Result<Unit>) -> Unit) {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Host overlay must be attached on the main thread"
        }
        if (isAttachedTo(activity)) {
            onComplete(Result.success(Unit))
            return
        }
        detachLocked()

        val container = activity.window.decorView as? ViewGroup
            ?: error("MainActivity DecorView is not a ViewGroup")
        val owner = HostOverlayLifecycleOwner().also {
            it.create()
            it.start()
            it.resume()
        }
        container.setViewTreeLifecycleOwner(owner)
        container.setViewTreeSavedStateRegistryOwner(owner)
        container.setViewTreeViewModelStoreOwner(owner)
        val clickGuiController = ClickGuiController(
            configStore = ClickGuiConfigStore(activity.applicationContext ?: activity),
        )
        val params = createLayoutParams(activity, true)
        val completed = AtomicBoolean(false)
        val complete: (Result<Unit>) -> Unit = { result ->
            if (completed.compareAndSet(false, true)) onComplete(result)
        }

        expanded = true
        layoutExpanded = true
        val view = ComposeView(activity).apply {
            id = View.generateViewId()
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            elevation = dp(activity, 24).toFloat()
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val backgroundMode = ClickGuiRenderer.backgroundMode
                val blurRadius = ClickGuiRenderer.gaussianBlurRadiusDp
                val buttonSizeDp = clickGuiController.floatingButtonConfig.buttonSizeDp
                val quickModules = clickGuiController.quickOverlayModules()
                val quickModuleIds = quickModules.map { it.id }
                LaunchedEffect(layoutExpanded, backgroundMode, blurRadius) {
                    ClickGuiRenderer.updateHostEffect(activity, layoutExpanded)
                }
                LaunchedEffect(layoutExpanded, buttonSizeDp) {
                    updateFloatingButtonLayout(activity, buttonSizeDp)
                }
                LaunchedEffect(layoutExpanded, buttonSizeDp, quickModuleIds) {
                    syncQuickButtonViews(
                        activity = activity,
                        owner = owner,
                        clickGuiController = clickGuiController,
                        modules = quickModules,
                        buttonSizeDp = buttonSizeDp,
                    )
                }
                DisposableEffect(activity) {
                    onDispose {
                        ClickGuiRenderer.clearHostEffect()
                    }
                }
                AnimatedOverlaySwitcher(
                    expanded = expanded,
                    onExpandedLayoutRequired = { applyLayoutMode(activity, true) },
                    onCollapsedLayoutRequired = { applyLayoutMode(activity, false) },
                    playInjectionIntro = true,
                    expandedContent = {
                        LiquidClickGuiScreen(
                            controller = clickGuiController,
                            modifier = Modifier.clip(RoundedCornerShape(26.dp)),
                            backgroundMode = backgroundMode,
                            onCollapse = { updateExpandedState(activity, false) },
                            onExit = { updateExpandedState(activity, false) },
                            onResize = { delta ->
                                resizeExpandedPanel(activity, delta.x, delta.y)
                            },
                        )
                    },
                    collapsedContent = {
                        LiquidFloatingButton(
                            onExpand = { updateExpandedState(activity, true) },
                            sizeDp = buttonSizeDp,
                            onDrag = { delta ->
                                dragMainFloatingButton(activity, delta.x, delta.y)
                            },
                        )
                    },
                )
            }
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(attachedView: View) {
                    attachedView.removeOnAttachStateChangeListener(this)
                    attachedView.post { complete(Result.success(Unit)) }
                }

                override fun onViewDetachedFromWindow(detachedView: View) = Unit
            })
        }
        val hudView = createPersistentHudView(
            activity = activity,
            owner = owner,
            clickGuiController = clickGuiController,
        )
        val hudParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        activityRef = WeakReference(activity)
        hostContainer = container
        layoutParams = params
        composeView = view
        persistentHudView = hudView
        lifecycleOwner = owner
        controller = clickGuiController

        try {
            container.addView(view, params)
            container.addView(hudView, hudParams)
        } catch (error: Throwable) {
            detachLocked()
            throw error
        }

        container.postDelayed(
            {
                if (activityRef.get() === activity && hudView.isAttachedToWindow) {
                    hudView.bringToFront()
                    hudView.invalidate()
                }
            },
            PERSISTENT_HUD_REELEVATION_DELAY_MS,
        )

        container.postDelayed(
            {
                if (!view.isAttachedToWindow || !hudView.isAttachedToWindow) {
                    detach(activity)
                    complete(Result.failure(IllegalStateException("ClickGUI view attach timed out")))
                }
            },
            VIEW_ATTACH_TIMEOUT_MS,
        )
    }

    @Synchronized
    fun detach(activity: Activity) {
        if (activityRef.get() !== activity) return
        detachLocked()
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return false
        return controller?.handleShortcutKey(event.keyCode) == true
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val activity = activityRef.get() ?: return false
        val overlay = composeView ?: return false
        if (!layoutExpanded || overlay.visibility != View.VISIBLE) return false
        val overlayLocation = IntArray(2)
        overlay.getLocationOnScreen(overlayLocation)
        if (event.rawX < overlayLocation[0] || event.rawX > overlayLocation[0] + overlay.width ||
            event.rawY < overlayLocation[1] || event.rawY > overlayLocation[1] + overlay.height
        ) return false

        val gameContent = activity.findViewById<ViewGroup>(android.R.id.content) ?: return false
        val contentLocation = IntArray(2)
        gameContent.getLocationInWindow(contentLocation)
        val forwarded = MotionEvent.obtain(event)
        forwarded.offsetLocation(-contentLocation[0].toFloat(), -contentLocation[1].toFloat())
        runCatching { gameContent.dispatchTouchEvent(forwarded) }
        forwarded.recycle()
        return false
    }

    private fun updateExpandedState(activity: Activity, value: Boolean) {
        if (activityRef.get() !== activity) return
        if (expanded == value) return
        expanded = value
    }

    private fun applyLayoutMode(activity: Activity, value: Boolean) {
        if (activityRef.get() !== activity) return
        val view = composeView ?: return
        val params = layoutParams ?: return
        val buttonSizeDp = controller?.floatingButtonConfig?.buttonSizeDp
            ?: FloatingButtonConfig.DEFAULT_SIZE_DP
        val size = if (value) expandedSize(activity) else collapsedSize(activity, buttonSizeDp)

        params.width = size.first
        params.height = size.second
        if (value) {
            params.gravity = Gravity.CENTER
            params.leftMargin = 0
            params.topMargin = 0
            params.rightMargin = 0
        } else {
            val position = resolveMainFloatingPosition(activity, size.first, size.second)
            params.gravity = Gravity.TOP or Gravity.START
            params.leftMargin = position.first
            params.topMargin = position.second
            params.rightMargin = 0
        }
        quickButtonViews.values.forEach { quickView ->
            quickView.visibility = if (value) View.GONE else View.VISIBLE
        }
        ClickGuiRenderer.updateHostEffect(activity, value)
        layoutExpanded = value
        view.layoutParams = params
        view.requestLayout()
    }

    private fun createLayoutParams(
        activity: Activity,
        isExpanded: Boolean,
    ): FrameLayout.LayoutParams {
        val buttonSizeDp = controller?.floatingButtonConfig?.buttonSizeDp
            ?: FloatingButtonConfig.DEFAULT_SIZE_DP
        val size = if (isExpanded) expandedSize(activity) else collapsedSize(activity, buttonSizeDp)
        return FrameLayout.LayoutParams(size.first, size.second).apply {
            if (isExpanded) {
                gravity = Gravity.CENTER
            } else {
                val position = resolveMainFloatingPosition(activity, size.first, size.second)
                gravity = Gravity.TOP or Gravity.START
                leftMargin = position.first
                topMargin = position.second
            }
        }
    }

    private fun expandedSize(activity: Activity): Pair<Int, Int> {
        val decor = activity.window.decorView
        val bounds = activity.windowManager.currentWindowMetrics.bounds
        val width = decor.width.takeIf { it > 0 } ?: bounds.width()
        val height = decor.height.takeIf { it > 0 } ?: bounds.height()
        val maxWidth = (width * 0.95f).roundToInt().coerceAtMost(dp(activity, 1080))
        val maxHeight = (height * 0.90f).roundToInt().coerceAtMost(dp(activity, 720))
        val minWidth = dp(activity, 640).coerceAtMost(maxWidth)
        val minHeight = dp(activity, 390).coerceAtMost(maxHeight)
        return (customExpandedWidth?.coerceIn(minWidth, maxWidth) ?: maxWidth) to
            (customExpandedHeight?.coerceIn(minHeight, maxHeight)
                ?: (height * 0.86f).roundToInt().coerceAtMost(maxHeight))
    }

    private fun resizeExpandedPanel(activity: Activity, deltaX: Float, deltaY: Float) {
        if (activityRef.get() !== activity || !layoutExpanded) return
        val view = composeView ?: return
        val params = layoutParams ?: return
        val bounds = activity.windowManager.currentWindowMetrics.bounds
        val maxWidth = (bounds.width() * 0.95f).roundToInt().coerceAtMost(dp(activity, 1080))
        val maxHeight = (bounds.height() * 0.90f).roundToInt().coerceAtMost(dp(activity, 720))
        val minWidth = dp(activity, 640).coerceAtMost(maxWidth)
        val minHeight = dp(activity, 390).coerceAtMost(maxHeight)
        customExpandedWidth = (params.width + deltaX * 2f).roundToInt()
            .coerceIn(minWidth, maxWidth)
        customExpandedHeight = (params.height + deltaY * 2f).roundToInt()
            .coerceIn(minHeight, maxHeight)
        params.width = requireNotNull(customExpandedWidth)
        params.height = requireNotNull(customExpandedHeight)
        params.gravity = Gravity.CENTER
        view.layoutParams = params
        view.requestLayout()
    }

    private fun collapsedSize(activity: Activity, buttonSizeDp: Int): Pair<Int, Int> {
        val containerSizeDp = FloatingButtonConfig.coerceSize(buttonSizeDp) + 16
        return dp(activity, containerSizeDp) to dp(activity, containerSizeDp)
    }

    private fun updateFloatingButtonLayout(activity: Activity, buttonSizeDp: Int) {
        if (activityRef.get() !== activity || layoutExpanded) return
        val view = composeView ?: return
        val params = layoutParams ?: return
        val size = collapsedSize(activity, buttonSizeDp)
        val position = resolveMainFloatingPosition(activity, size.first, size.second)
        params.width = size.first
        params.height = size.second
        params.gravity = Gravity.TOP or Gravity.START
        params.leftMargin = position.first
        params.topMargin = position.second
        params.rightMargin = 0
        view.layoutParams = params
        view.requestLayout()
    }

    private fun syncQuickButtonViews(
        activity: Activity,
        owner: HostOverlayLifecycleOwner,
        clickGuiController: ClickGuiController,
        modules: List<ModuleUiModel>,
        buttonSizeDp: Int,
    ) {
        if (activityRef.get() !== activity) return
        val container = hostContainer ?: return
        val desiredIds = modules.mapTo(linkedSetOf()) { it.id }
        val obsoleteIds = quickButtonViews.keys.filterNot(desiredIds::contains)
        obsoleteIds.forEach { moduleId ->
            val removed = quickButtonViews.remove(moduleId) ?: return@forEach
            runCatching { removed.disposeComposition() }
            runCatching { container.removeView(removed) }
        }

        val coercedSize = FloatingButtonConfig.coerceSize(buttonSizeDp)
        val hostSize = hostSize(activity)
        val itemExtent = dp(activity, coercedSize + 20)
        val defaultStartY = ((hostSize.second - modules.size * itemExtent) / 2).coerceAtLeast(0)

        modules.forEachIndexed { index, module ->
            val existing = quickButtonViews[module.id]
            val quickView = existing ?: createQuickButtonView(
                activity = activity,
                owner = owner,
                clickGuiController = clickGuiController,
                module = module,
            )
            val params = (quickView.layoutParams as? FrameLayout.LayoutParams)
                ?: FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            val estimatedWidth = quickView.width.takeIf { it > 0 }
                ?: estimatedQuickButtonWidth(activity, module, coercedSize)
            val estimatedHeight = quickView.height.takeIf { it > 0 }
                ?: dp(activity, coercedSize + 12)
            val defaultPosition = dp(activity, 12) to (defaultStartY + index * itemExtent)
            val storedPosition = quickButtonPositions[module.id] ?: defaultPosition
            val x = storedPosition.first.coerceIn(
                0,
                (hostSize.first - estimatedWidth).coerceAtLeast(0),
            )
            val y = storedPosition.second.coerceIn(
                0,
                (hostSize.second - estimatedHeight).coerceAtLeast(0),
            )
            quickButtonPositions[module.id] = x to y
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            params.gravity = Gravity.TOP or Gravity.START
            params.leftMargin = x
            params.topMargin = y
            quickView.visibility = if (layoutExpanded) View.GONE else View.VISIBLE

            if (existing == null) {
                val added = runCatching { container.addView(quickView, params) }.isSuccess
                if (added) {
                    quickButtonViews[module.id] = quickView
                } else {
                    runCatching { quickView.disposeComposition() }
                    return@forEachIndexed
                }
            } else {
                quickView.layoutParams = params
                quickView.requestLayout()
            }
            quickView.post { clampQuickButtonPosition(activity, module.id) }
        }
    }

    private fun createQuickButtonView(
        activity: Activity,
        owner: HostOverlayLifecycleOwner,
        clickGuiController: ClickGuiController,
        module: ModuleUiModel,
    ): ComposeView = ComposeView(activity).apply {
        id = View.generateViewId()
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        elevation = dp(activity, 26).toFloat()
        setViewTreeLifecycleOwner(owner)
        setViewTreeSavedStateRegistryOwner(owner)
        setViewTreeViewModelStoreOwner(owner)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ModuleQuickFloatingButton(
                module = module,
                controller = clickGuiController,
                sizeDp = clickGuiController.floatingButtonConfig.buttonSizeDp,
                onDrag = { delta ->
                    dragQuickButton(activity, module.id, delta.x, delta.y)
                },
                modifier = Modifier.padding(6.dp),
            )
        }
    }

    private fun createPersistentHudView(
        activity: Activity,
        owner: HostOverlayLifecycleOwner,
        clickGuiController: ClickGuiController,
    ): PersistentHudHost = PersistentHudHost(
        context = activity,
        islandVisible = { true },
    ).apply {
        elevation = dp(activity, 30).toFloat()
        setViewTreeLifecycleOwner(owner)
        setViewTreeSavedStateRegistryOwner(owner)
        setViewTreeViewModelStoreOwner(owner)
        composeView.apply {
            id = View.generateViewId()
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            isFocusable = false
            isFocusableInTouchMode = false
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PersistentHudOverlay(controller = clickGuiController)
            }
        }
    }

    private fun dragMainFloatingButton(activity: Activity, deltaX: Float, deltaY: Float) {
        if (activityRef.get() !== activity || expanded || layoutExpanded) return
        val view = composeView ?: return
        val params = layoutParams ?: return
        val hostSize = hostSize(activity)
        val width = view.width.takeIf { it > 0 } ?: params.width
        val height = view.height.takeIf { it > 0 } ?: params.height
        val x = (params.leftMargin + deltaX.roundToInt())
            .coerceIn(0, (hostSize.first - width).coerceAtLeast(0))
        val y = (params.topMargin + deltaY.roundToInt())
            .coerceIn(0, (hostSize.second - height).coerceAtLeast(0))
        floatingButtonX = x
        floatingButtonY = y
        params.gravity = Gravity.TOP or Gravity.START
        params.leftMargin = x
        params.topMargin = y
        params.rightMargin = 0
        view.layoutParams = params
    }

    private fun dragQuickButton(
        activity: Activity,
        moduleId: String,
        deltaX: Float,
        deltaY: Float,
    ) {
        if (activityRef.get() !== activity || expanded || layoutExpanded) return
        val view = quickButtonViews[moduleId] ?: return
        val params = view.layoutParams as? FrameLayout.LayoutParams ?: return
        val hostSize = hostSize(activity)
        val width = view.width.takeIf { it > 0 } ?: params.width
        val height = view.height.takeIf { it > 0 } ?: params.height
        val x = (params.leftMargin + deltaX.roundToInt())
            .coerceIn(0, (hostSize.first - width).coerceAtLeast(0))
        val y = (params.topMargin + deltaY.roundToInt())
            .coerceIn(0, (hostSize.second - height).coerceAtLeast(0))
        quickButtonPositions[moduleId] = x to y
        params.gravity = Gravity.TOP or Gravity.START
        params.leftMargin = x
        params.topMargin = y
        view.layoutParams = params
    }

    private fun resolveMainFloatingPosition(
        activity: Activity,
        width: Int,
        height: Int,
    ): Pair<Int, Int> {
        val hostSize = hostSize(activity)
        val defaultX = hostSize.first - width - dp(activity, 18)
        val defaultY = (hostSize.second - height) / 2
        val x = (floatingButtonX ?: defaultX)
            .coerceIn(0, (hostSize.first - width).coerceAtLeast(0))
        val y = (floatingButtonY ?: defaultY)
            .coerceIn(0, (hostSize.second - height).coerceAtLeast(0))
        floatingButtonX = x
        floatingButtonY = y
        return x to y
    }

    private fun estimatedQuickButtonWidth(
        activity: Activity,
        module: ModuleUiModel,
        buttonSizeDp: Int,
    ): Int {
        val fontSizeDp = (buttonSizeDp * 0.30f).coerceIn(10.5f, 15f)
        val longestNameLength = maxOf(module.name.length, module.englishName.length)
        val textWidthDp = longestNameLength * fontSizeDp * 0.62f
        val horizontalPaddingDp = (buttonSizeDp * 0.68f).coerceAtLeast(22f)
        return dp(
            activity,
            (textWidthDp + horizontalPaddingDp + 12f).roundToInt(),
        )
    }

    private fun clampQuickButtonPosition(activity: Activity, moduleId: String) {
        if (activityRef.get() !== activity) return
        val view = quickButtonViews[moduleId] ?: return
        val params = view.layoutParams as? FrameLayout.LayoutParams ?: return
        val hostSize = hostSize(activity)
        val width = view.width.takeIf { it > 0 } ?: return
        val height = view.height.takeIf { it > 0 } ?: return
        val stored = quickButtonPositions[moduleId] ?: (params.leftMargin to params.topMargin)
        val x = stored.first.coerceIn(0, (hostSize.first - width).coerceAtLeast(0))
        val y = stored.second.coerceIn(0, (hostSize.second - height).coerceAtLeast(0))
        quickButtonPositions[moduleId] = x to y
        if (params.leftMargin != x || params.topMargin != y) {
            params.leftMargin = x
            params.topMargin = y
            view.layoutParams = params
        }
    }

    private fun hostSize(activity: Activity): Pair<Int, Int> {
        val bounds = activity.windowManager.currentWindowMetrics.bounds
        val container = hostContainer
        return (container?.width?.takeIf { it > 0 } ?: bounds.width()) to
            (container?.height?.takeIf { it > 0 } ?: bounds.height())
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).roundToInt()

    private fun detachLocked() {
        val view = composeView
        val hudView = persistentHudView
        val quickViews = quickButtonViews.values.toList()
        val container = hostContainer
        ClickGuiRenderer.clearHostEffect()
        if (view != null && container != null) {
            runCatching { container.removeView(view) }
        }
        if (hudView != null) {
            runCatching { hudView.disposeComposition() }
            if (container != null) {
                runCatching { container.removeView(hudView) }
            }
        }
        quickViews.forEach { quickView ->
            runCatching { quickView.disposeComposition() }
            if (container != null) {
                runCatching { container.removeView(quickView) }
            }
        }
        container?.setViewTreeLifecycleOwner(null)
        container?.setViewTreeSavedStateRegistryOwner(null)
        container?.setViewTreeViewModelStoreOwner(null)
        composeView = null
        persistentHudView = null
        quickButtonViews.clear()
        layoutParams = null
        hostContainer = null
        activityRef.clear()
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        controller = null
        expanded = true
        layoutExpanded = true
    }

    private const val VIEW_ATTACH_TIMEOUT_MS = 1_000L
    private const val PERSISTENT_HUD_REELEVATION_DELAY_MS =
        INJECTION_INTRO_START_DELAY_MS - 250L
}

private class PersistentHudHost(
    context: Context,
    private val islandVisible: () -> Boolean,
) : FrameLayout(context) {
    val composeView = ComposeView(context)

    init {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        isClickable = false
        isFocusable = false
        addView(
            composeView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (!islandVisible()) return false
        val density = resources.displayMetrics.density
        val halfWidth = 320f * density
        val maxHeight = 205f * density
        if (event.x < width / 2f - halfWidth || event.x > width / 2f + halfWidth) {
            return false
        }
        if (event.y > maxHeight) return false
        return super.dispatchTouchEvent(event)
    }

    fun disposeComposition() {
        composeView.disposeComposition()
    }
}

private class HostOverlayLifecycleOwner :
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
