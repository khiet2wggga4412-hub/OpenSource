
package com.liquid.org.ui.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.liquid.org.ui.overlay.LiquidBounceModels.TopTab;
import com.liquid.org.ui.overlay.LiquidBounceModels.ModuleEntry;
import com.liquid.org.ui.overlay.LiquidBounceModels.NotificationSpec;

import java.util.Locale;

public class LiquidBounceOverlayView extends FrameLayout implements ClickGuiRenderer.Listener {
    public interface SearchRequestListener { void onSearchRequested(String currentQuery); }

    private static final float CLICK_GUI_MIN_SCALE = 0.85f;
    private static final float CLICK_GUI_TRANSLATION_PX = 8f;
    private static final float CLICK_GUI_SCALE_PIVOT_Y = LiquidBounceUiMetrics.CONTENT_HEIGHT * .5f;
    private static final int CLICK_GUI_BACKDROP_MAX_ALPHA = 120;

    private final ReferenceViewport viewport = new ReferenceViewport();
    private final ResponsiveTypography typography;
    private final LiquidBounceDataStore dataStore;
    private final ClickGuiRenderer clickGuiRenderer;
    private final ConfigStore configStore;
    private final MusicStore musicStore;
    private final MaterialStore materialStore;
    private final HudRenderer hudRenderer;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint debugPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF contentClip = new RectF(0, 0, LiquidBounceUiMetrics.CONTENT_WIDTH, LiquidBounceUiMetrics.CONTENT_HEIGHT);
    private final ClickGuiTransition clickGuiTransition = new ClickGuiTransition(true);
    private boolean clickGuiVisible = true;
    private boolean hudVisible = true;
    private boolean transparentBase;
    private boolean debugBounds;
    private boolean referenceGrid;
    private boolean hudTouchCaptured;
    private float logicalTouchX;
    private float logicalTouchY;
    private SearchRequestListener searchRequestListener;
    private Runnable clickGuiClosedListener;
    private boolean closedCallbackDispatched;
    private float globalUiScale = 1f;
    private static final float FLOATING_ICON_SIZE = 58f;
    private static final float FLOATING_ICON_MARGIN = 26f;
    private long previousFrame;
    private float smoothedFrameMs = 16.67f;
    private final EditText searchInput;
    private final Bitmap floatingLogo;
    private final RectF rectForFloatingIcon = new RectF();
    private boolean syncingSearchInput;
    private boolean musicInputMode;

    public LiquidBounceOverlayView(Context context) { this(context, null); }
    public LiquidBounceOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        searchInput = new EditText(context);
        floatingLogo = BitmapFactory.decodeResource(context.getResources(), com.liquid.org.R.drawable.clickgui);
        searchInput.setSingleLine(true);
        searchInput.setFocusable(true);
        searchInput.setFocusableInTouchMode(true);
        searchInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchInput.setTextColor(Color.TRANSPARENT);
        searchInput.setHintTextColor(Color.TRANSPARENT);
        searchInput.setCursorVisible(false);
        searchInput.setBackgroundColor(Color.TRANSPARENT);
        searchInput.setAlpha(0.01f);
        searchInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        FrameLayout.LayoutParams searchParams = new FrameLayout.LayoutParams(2, 2, Gravity.TOP | Gravity.START);
        searchParams.leftMargin = 1;
        searchParams.topMargin = 1;
        addView(searchInput, searchParams);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!syncingSearchInput) {
                    if (musicInputMode) clickGuiRenderer.setMusicQuery(s == null ? "" : s.toString());
                    else clickGuiRenderer.setSearchQuery(s == null ? "" : s.toString());
                    invalidate();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        LiquidBounceFonts.initialize(context);
        typography = new ResponsiveTypography(context);
        dataStore = LiquidBounceDataStore.createDemo();
        configStore = new ConfigStore(context);
        musicStore = new MusicStore(context);
        materialStore = new MaterialStore(context);
        clickGuiRenderer = new ClickGuiRenderer(context, dataStore, this, typography);
        clickGuiRenderer.setConfigNames(configStore.list());
        clickGuiRenderer.setMaterials(materialStore.list());
        hudRenderer = new HudRenderer(dataStore, typography);
        dataStore.addModuleStateListener(new LiquidBounceDataStore.ModuleStateListener() {
            @Override public void onModuleEnabledChanged(ModuleEntry module, boolean enabled) {
                hudRenderer.onModuleChanged(module);
                hudRenderer.pushModuleNotification(module, enabled);
                invalidate();
            }

            @Override public void onModuleMetadataChanged(ModuleEntry module) {
                hudRenderer.onModuleMetadataChanged(module);
                invalidate();
            }
        });
        setFocusable(true);
        setFocusableInTouchMode(true);
        debugPaint.setTypeface(LiquidBounceFonts.regular());
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        viewport.update(w, h);
        typography.update(getContext(), w, h);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        float clickGuiProgress = clickGuiTransition.get(now);
        if (previousFrame != 0) smoothedFrameMs = smoothedFrameMs * .9f + (now - previousFrame) * .1f;
        previousFrame = now;
        if (transparentBase) canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        else canvas.drawColor(Color.WHITE);
        int save = canvas.save();
        viewport.apply(canvas);
        if (hudVisible) hudRenderer.draw(canvas, debugBounds, now);
        if (clickGuiProgress > 0f) drawAnimatedClickGui(canvas, clickGuiProgress, now);
        if (hudVisible) hudRenderer.drawBindsOverlay(canvas, debugBounds);
        if (clickGuiProgress <= 0.01f) drawFloatingToggle(canvas);
        if (referenceGrid) drawReferenceGrid(canvas);
        if (debugBounds || referenceGrid) drawDebugInfo(canvas, now);
        canvas.restoreToCount(save);
        boolean transitionRunning = clickGuiTransition.isRunning(now);
        if (!transitionRunning && !clickGuiVisible && clickGuiProgress <= 0f && !closedCallbackDispatched) {
            closedCallbackDispatched = true;
            if (clickGuiClosedListener != null) post(clickGuiClosedListener);
        }
        if (transitionRunning || clickGuiRenderer.hasActiveAnimations(now) || hudRenderer.hasActiveAnimations(now)) {
            postInvalidateOnAnimation();
        }
    }

    private void drawAnimatedClickGui(Canvas canvas, float progress, long now) {

        float translationY = CLICK_GUI_TRANSLATION_PX * (1f - progress) / Math.max(0.001f, viewport.getScale());
        int transformSave = canvas.save();
        canvas.translate(0f, translationY);

        int alphaLayer = canvas.saveLayerAlpha(null, Math.round(255f * progress));
        clickGuiRenderer.draw(canvas, debugBounds, now);
        canvas.restoreToCount(alphaLayer);
        canvas.restoreToCount(transformSave);
    }

    private void drawFloatingToggle(Canvas canvas) {
        if (floatingLogo != null) {
            rectForFloatingIcon.set(FLOATING_ICON_MARGIN, FLOATING_ICON_MARGIN,
                    FLOATING_ICON_MARGIN + FLOATING_ICON_SIZE, FLOATING_ICON_MARGIN + FLOATING_ICON_SIZE);
            canvas.drawBitmap(floatingLogo, null, rectForFloatingIcon, debugPaint);
        }
    }

    private void drawReferenceGrid(Canvas canvas) {
        debugPaint.setStyle(Paint.Style.STROKE); debugPaint.setStrokeWidth(1); debugPaint.setColor(0x3F55D8FF);
        for (int x = 0; x <= 2940; x += 100) canvas.drawLine(x, 0, x, 1837, debugPaint);
        for (int y = 0; y <= 1837; y += 100) canvas.drawLine(0, y, 2940, y, debugPaint);
        debugPaint.setColor(0xA0FF3CAC); debugPaint.setStrokeWidth(2);
        canvas.drawLine(logicalTouchX - 12, logicalTouchY, logicalTouchX + 12, logicalTouchY, debugPaint);
        canvas.drawLine(logicalTouchX, logicalTouchY - 12, logicalTouchX, logicalTouchY + 12, debugPaint);
    }

    private void drawDebugInfo(Canvas canvas, long now) {
        debugPaint.setStyle(Paint.Style.FILL); debugPaint.setColor(0xD9000000); canvas.drawRoundRect(18, 1730, 620, 1818, 8, 8, debugPaint);
        debugPaint.setColor(Color.WHITE); debugPaint.setTextSize(typography.size(18));
        float fps = 1000f / Math.max(1f, smoothedFrameMs);
        String line1 = String.format(Locale.US, "scale %.4f  fps %.1f  frame %.2f ms", viewport.getScale(), fps, smoothedFrameMs);
        String line2 = String.format(Locale.US, "logical %.1f, %.1f  uptime %d", logicalTouchX, logicalTouchY, now);
        canvas.drawText(line1, 32, 1765, debugPaint); canvas.drawText(line2, 32, 1798, debugPaint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        PointF p = viewport.toLogical(event.getX(), event.getY());
        if (clickGuiVisible) {
            long now = SystemClock.uptimeMillis();
            float progress = clickGuiTransition.get(now);
            float translationY = CLICK_GUI_TRANSLATION_PX * (1f - progress) / Math.max(0.001f, viewport.getScale());
            p.y -= translationY;
        }
        logicalTouchX = p.x; logicalTouchY = p.y;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (!clickGuiVisible && !isFloatingToggleHit(p.x, p.y)) return false;
            if (clickGuiVisible && !clickGuiRenderer.isInsideUi(p.x, p.y)) {
                setClickGuiVisible(false);
                return false;
            }
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            requestFocus();
            hudTouchCaptured = !clickGuiVisible && hudVisible && hudRenderer.onTouchDown(p.x, p.y);
            if (!hudTouchCaptured && clickGuiVisible) clickGuiRenderer.onTouchDown(p.x, p.y);
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (hudTouchCaptured) hudRenderer.onTouchMove(p.x, p.y);
            else if (clickGuiVisible) clickGuiRenderer.onTouchMove(p.x, p.y);
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (hudTouchCaptured) hudRenderer.onTouchUp();
            else if (clickGuiVisible) {
                clickGuiRenderer.onTouchUp(p.x, p.y);
            } else if (isFloatingToggleHit(p.x, p.y)) {
        setClickGuiVisible(true);
            }
            hudTouchCaptured = false;
            performClick();
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            if (hudTouchCaptured) hudRenderer.onTouchUp();
            else if (clickGuiVisible) clickGuiRenderer.onTouchUp(p.x, p.y);
            hudTouchCaptured = false;
        }
        invalidate();
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override public void onTopTabChanged(TopTab tab) {
        invalidate();
    }

    @Override public void onSearchRequested() {
        musicInputMode = false;
        syncingSearchInput = true;
        searchInput.setText(clickGuiRenderer.getSearchQuery());
        searchInput.setSelection(searchInput.length());
        syncingSearchInput = false;
        searchInput.post(() -> {
            clearFocus();
            searchInput.requestFocusFromTouch();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.restartInput(searchInput);
                imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        if (searchRequestListener != null) searchRequestListener.onSearchRequested(clickGuiRenderer.getSearchQuery());
    }

    @Override public void onMusicInputRequested() {
        musicInputMode = true;
        syncingSearchInput = true;
        searchInput.setText(clickGuiRenderer.getMusicQuery());
        searchInput.setSelection(searchInput.length());
        syncingSearchInput = false;
        searchInput.post(() -> {
            clearFocus(); searchInput.requestFocusFromTouch();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) { imm.restartInput(searchInput); imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT); }
        });
    }

    @Override public void onLanguageChanged() {
        hudRenderer.onLanguageChanged();
        invalidate();
    }

    @Override public void onHudComponentVisibilityChanged(String component, boolean visible) {
        hudRenderer.setComponentVisible(component, visible);
        invalidate();
    }

    @Override public void onOverlaySettingChanged(String setting, boolean enabled) {
        if ("grid".equals(setting)) referenceGrid = enabled;
        invalidate();
    }

    @Override public void onUiScaleChanged(float scale) {
        globalUiScale = Math.max(.75f, Math.min(2.0f, scale));
        invalidate();
    }

    @Override public void onFontChanged(String fontName) {
        LiquidBounceFonts.setFamily(getContext(), fontName);
        invalidate();
    }

    @Override public void onConfigSave(String name) {
        String configName = name == null || name.trim().isEmpty() ? "default" : name;
        boolean saved = configStore.save(configName, dataStore, clickGuiRenderer.exportUiState());
        clickGuiRenderer.setSelectedConfig(configName);
        clickGuiRenderer.setConfigNames(configStore.list());
        clickGuiRenderer.showConfigStatus(saved ? "Saved " + configName : "Save failed");
        invalidate();
    }

    @Override public void onConfigExport(String name) {
        String configName = name == null || name.trim().isEmpty() ? "default" : name;
        if (!configStore.fileFor(configName).isFile()) onConfigSave(configName);
        java.io.File exported = configStore.export(configName);
        clickGuiRenderer.showConfigStatus(exported == null ? "Export failed" : "Exported " + exported.getName());
        invalidate();
    }

    @Override public void onConfigLoad(String name) {
        String configName = name == null || name.trim().isEmpty() ? "default" : name;
        org.json.JSONObject ui = configStore.load(configName, dataStore);
        if (ui == null) { clickGuiRenderer.showConfigStatus("Load failed"); invalidate(); return; }
        clickGuiRenderer.setSelectedConfig(configName);
        clickGuiRenderer.importUiState(ui);
        onHudComponentVisibilityChanged("arraylist", ui.optBoolean("hudArrayVisible", true));
        onHudComponentVisibilityChanged("binds", ui.optBoolean("hudBindsVisible", true));
        onHudComponentVisibilityChanged("notifications", ui.optBoolean("hudNotificationsVisible", true));
        onOverlaySettingChanged("grid", ui.optBoolean("showGrid", referenceGrid));
        hudRenderer.onLanguageChanged();
        clickGuiRenderer.showConfigStatus("Loaded " + configName);
        invalidate();
    }

    @Override public void onConfigDelete(String name) {
        String configName = name == null || name.trim().isEmpty() ? "default" : name;
        boolean deleted = configStore.delete(configName);
        java.util.List<String> names = configStore.list();
        clickGuiRenderer.setConfigNames(names);
        if (!names.isEmpty()) clickGuiRenderer.setSelectedConfig(names.get(0));
        clickGuiRenderer.showConfigStatus(deleted ? "Deleted " + configName : "Delete failed");
        invalidate();
    }

    @Override public void onMusicSearch(String query) {
        String normalized = query == null || query.trim().isEmpty() ? "网易云音乐" : query.trim();
        clickGuiRenderer.showMusicStatus("Searching");
        musicStore.search(normalized, new MusicStore.Callback() {
            @Override public void onResult(java.util.List<MusicStore.Track> tracks) { clickGuiRenderer.setMusicTracks(tracks); clickGuiRenderer.showMusicStatus(tracks.isEmpty() ? "No search results" : "Search complete"); invalidate(); }
            @Override public void onError(String message) { clickGuiRenderer.showMusicStatus("Music search failed"); invalidate(); }
        });
    }

    @Override public void onMusicPlay(int index) {
        java.util.List<MusicStore.Track> tracks = clickGuiRenderer.getMusicTracks();
        if (index < 0 || index >= tracks.size()) return;
        musicStore.play(tracks.get(index), new MusicStore.Callback() {
            @Override public void onResult(java.util.List<MusicStore.Track> ignored) { clickGuiRenderer.showMusicStatus("Playing"); invalidate(); }
            @Override public void onError(String message) { clickGuiRenderer.showMusicStatus("Playback unavailable"); invalidate(); }
        });
    }

    @Override public void onMusicStop() { musicStore.stop(); clickGuiRenderer.showMusicStatus("Stopped"); invalidate(); }

    @Override public void onMaterialLoad(String name) {
        boolean loaded = materialStore.load(name);
        clickGuiRenderer.showMaterialStatus(loaded ? name : "");
        clickGuiRenderer.showConfigStatus(loaded ? "Material loaded" : "Material load failed");
        invalidate();
    }

    @Override public void onMaterialRefresh() { clickGuiRenderer.setMaterials(materialStore.list()); invalidate(); }

    private boolean isFloatingToggleHit(float x, float y) {
        return x >= FLOATING_ICON_MARGIN && x <= FLOATING_ICON_MARGIN + FLOATING_ICON_SIZE
                && y >= FLOATING_ICON_MARGIN && y <= FLOATING_ICON_MARGIN + FLOATING_ICON_SIZE;
    }

    public void setGlobalUiScale(float scale) {
        globalUiScale = Math.max(.75f, Math.min(2.0f, scale));
        clickGuiRenderer.setUiScale(globalUiScale);
        invalidate();
    }

    public float getGlobalUiScale() { return globalUiScale; }

    public void setClickGuiVisible(boolean visible) {
        if (clickGuiVisible == visible && clickGuiTransition.getTarget() == (visible ? 1f : 0f)) return;
        clickGuiVisible = visible;
        closedCallbackDispatched = false;
        clickGuiTransition.animateTo(visible ? 1f : 0f,
                visible ? LiquidBounceUiDurations.CLICK_GUI_OPEN : LiquidBounceUiDurations.CLICK_GUI_CLOSE);
        postInvalidateOnAnimation();
    }

    public void setClickGuiVisibleImmediately(boolean visible) {
        clickGuiVisible = visible;
        closedCallbackDispatched = !visible;
        clickGuiTransition.snapTo(visible ? 1f : 0f);
        invalidate();
    }

    public boolean isClickGuiVisibleRequested() { return clickGuiVisible; }
    public void setTransparentBase(boolean transparent) { transparentBase = transparent; invalidate(); }
    public void setOnClickGuiClosedListener(@Nullable Runnable listener) { clickGuiClosedListener = listener; }
    public void setHudVisible(boolean visible) { hudVisible = visible; invalidate(); }
    public void setActiveTopTab(TopTab tab) { clickGuiRenderer.setActiveTab(tab); }
    public void saveConfig(String name) { onConfigSave(name); }
    public void exportConfig(String name) { onConfigExport(name); }
    public void loadConfig(String name) { onConfigLoad(name); }
    public void deleteConfig(String name) { onConfigDelete(name); }
    public java.util.List<String> getConfigNames() { return configStore.list(); }
    public void setMusicQuery(String query) { clickGuiRenderer.setMusicQuery(query); invalidate(); }
    public void searchMusic() { onMusicSearch(clickGuiRenderer.getMusicQuery()); }
    public void playMusic(int index) { onMusicPlay(index); }
    public void stopMusic() { onMusicStop(); }
    public java.io.File getMaterialDirectory() { return materialStore.getDirectory(); }
    public void refreshMaterials() { clickGuiRenderer.setMaterials(materialStore.list()); invalidate(); }
    public void setModuleEnabled(String name, boolean enabled) { dataStore.setModuleEnabled(name, enabled); }
    public void setModuleSuffix(String name, String suffix) { dataStore.setModuleSuffix(name, suffix); }
    public void setModuleKeyBind(String name, String keyName) { dataStore.setModuleKeyBind(name, keyName); }
    public void setModuleShownInArrayList(String name, boolean shown) { dataStore.setShowInArrayList(name, shown); }
    public void setModuleShownInBinds(String name, boolean shown) { dataStore.setShowInBinds(name, shown); }
    public void setCategoryExpanded(String categoryName, boolean expanded) { clickGuiRenderer.setCategoryExpanded(categoryName, expanded); invalidate(); }
    public void setModuleSettingsExpanded(String categoryName, String moduleName, boolean expanded) { clickGuiRenderer.setModuleSettingsExpanded(categoryName, moduleName, expanded); invalidate(); }
    public void pushModuleNotification(String moduleName, boolean enabled) { ModuleEntry module = dataStore.findModule(moduleName); if (module != null) hudRenderer.pushModuleNotification(module, enabled); invalidate(); }
    public void pushNotification(NotificationSpec notification) { hudRenderer.pushNotification(notification); invalidate(); }
    public void setDebugBoundsEnabled(boolean enabled) { debugBounds = enabled; invalidate(); }
    public void setReferenceGridEnabled(boolean enabled) { referenceGrid = enabled; invalidate(); }

    public void enterEditMode() { clickGuiRenderer.enterEditMode(); invalidate(); }
    public void exitEditMode() { clickGuiRenderer.exitEditMode(); invalidate(); }
    public boolean isEditMode() { return clickGuiRenderer.isEditMode(); }
    public void saveLayout() { clickGuiRenderer.savePanelLayout(); }
    public void restoreLayout() { clickGuiRenderer.restorePanelLayout(); invalidate(); }
    public void resetLayout() { clickGuiRenderer.resetPanelLayout(); invalidate(); }
    public void setSearchQuery(String query) {
        String normalized = query == null ? "" : query;
        syncingSearchInput = true;
        searchInput.setText(normalized);
        searchInput.setSelection(searchInput.length());
        syncingSearchInput = false;
        clickGuiRenderer.setSearchQuery(normalized);
        invalidate();
    }
    public void setSearchRequestListener(SearchRequestListener listener) { searchRequestListener = listener; }
    public LiquidBounceDataStore getDataStore() { return dataStore; }
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && clickGuiRenderer.onBackPressed()) {
            invalidate();
            return true;
        }
        String keyName = KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "").replace('_', ' ');
        ModuleEntry changed = clickGuiRenderer.onKeyPressed(keyName);
        if (changed != null) {
            invalidate();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private static final class ClickGuiTransition {
        private float startValue;
        private float value;
        private float targetValue;
        private long startTime;
        private long duration;

        ClickGuiTransition(boolean visible) {
            startValue = value = targetValue = visible ? 1f : 0f;
        }

        float get(long now) {
            if (value == targetValue || duration <= 0L) return targetValue;
            float t = Math.max(0f, Math.min(1f, (now - startTime) / (float) duration));
            float eased = targetValue > startValue ? easeOutCubic(t) : easeInCubic(t);
            value = startValue + (targetValue - startValue) * eased;
            if (t >= 1f) value = targetValue;
            return value;
        }

        void animateTo(float target, long fullDuration) {
            long now = SystemClock.uptimeMillis();
            float current = get(now);
            startValue = current;
            targetValue = target;
            startTime = now;
            duration = Math.max(1L, Math.round(fullDuration * Math.abs(target - current)));
        }

        void snapTo(float target) {
            startValue = value = targetValue = target;
            startTime = 0L;
            duration = 0L;
        }

        boolean isRunning(long now) {
            return value != targetValue && now - startTime < duration;
        }

        float getTarget() { return targetValue; }

        private static float easeOutCubic(float t) {
            float p = 1f - t;
            return 1f - p * p * p;
        }

        private static float easeInCubic(float t) { return t * t * t; }
    }
}
