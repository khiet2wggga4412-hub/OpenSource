
package com.liquid.org.ui.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.liquid.org.ui.overlay.LiquidBounceModels.BindSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.CategoryPanel;
import com.liquid.org.ui.overlay.LiquidBounceModels.ColorSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.DropdownSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.ModuleEntry;
import com.liquid.org.ui.overlay.LiquidBounceModels.MultiSelectSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.RangeSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.SettingEntry;
import com.liquid.org.ui.overlay.LiquidBounceModels.SettingGroup;
import com.liquid.org.ui.overlay.LiquidBounceModels.SliderSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.ToggleSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.TopTab;
import com.liquid.org.ui.grid.PanelLayoutInfo;
import com.liquid.org.ui.grid.PanelLayoutStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;

public final class ClickGuiRenderer {

    private static final int PANEL_GRID_COLUMNS = 7;
    private static final float PANEL_GRID_LEFT = 30f;
    private static final float PANEL_GRID_TOP = 194f;
    private static final float PANEL_GRID_CELL_WIDTH = 375f;
    private static final float PANEL_GRID_CELL_HEIGHT = 451f;
    private static final float PANEL_GRID_GAP_X = 30f;
    private static final float PANEL_GRID_GAP_Y = 30f;
    private static final float PANEL_CONTENT_TOP = 60f;
    private static final float PANEL_CONTENT_BOTTOM_PADDING = 8f;
    private static final float PANEL_SCREEN_BOTTOM_INSET = 30f;
    private static final float UI_SCALE_MIN = .75f;
    private static final float UI_SCALE_MAX = 2.0f;
    private static final float UI_SCALE_PIVOT_X = LiquidBounceUiMetrics.CONTENT_WIDTH * .5f;
    private static final float UI_SCALE_PIVOT_Y = LiquidBounceUiMetrics.CONTENT_HEIGHT * .5f;
    private static final float HUD_LEFT = 420f;
    private static final float HUD_TOP = 180f;
    private static final float HUD_RIGHT = 2520f;
    private static final float HUD_BOTTOM = 1450f;
    private static final float HUD_PREVIEW_LEFT = 500f;
    private static final float HUD_PREVIEW_RIGHT = 2440f;
    private static final float HUD_PREVIEW_BOTTOM = 1360f;
    private static final float SETTINGS_LEFT = 650f;
    private static final float SETTINGS_TOP = 220f;
    private static final float SETTINGS_RIGHT = 2290f;
    private static final float SETTINGS_BOTTOM = 1320f;
    private static final float UTILITY_LEFT = 30f;
    private static final float UTILITY_TOP = 96f;
    private static final float UTILITY_RIGHT = 990f;
    private static final float UTILITY_BOTTOM = 178f;
    private static final float CONFIG_LEFT = 520f;
    private static final float CONFIG_TOP = 170f;
    private static final float CONFIG_RIGHT = 2420f;
    private static final float CONFIG_BOTTOM = 1580f;
    public interface Listener {
        void onTopTabChanged(TopTab tab);
        void onSearchRequested();
        void onLanguageChanged();

        default void onHudComponentVisibilityChanged(String component, boolean visible) {}
        default void onOverlaySettingChanged(String setting, boolean enabled) {}
        default void onUiScaleChanged(float scale) {}
        default void onFontChanged(String fontName) {}
        default void onConfigSave(String name) {}
        default void onConfigExport(String name) {}
        default void onConfigLoad(String name) {}
        default void onConfigDelete(String name) {}
        default void onMusicSearch(String query) {}
        default void onMusicInputRequested() {}
        default void onMusicPlay(int index) {}
        default void onMusicStop() {}
        default void onMaterialLoad(String name) {}
        default void onMaterialRefresh() {}
    }

    private static final int HIT_TOGGLE = 1;
    private static final int HIT_SLIDER = 2;
    private static final int HIT_RANGE_LOW = 3;
    private static final int HIT_RANGE_HIGH = 4;
    private static final int HIT_DROPDOWN = 5;
    private static final int HIT_DROPDOWN_OPTION = 6;
    private static final int HIT_MULTI_OPTION = 7;
    private static final int HIT_BIND = 8;
    private static final int HIT_COLOR = 9;
    private static final int HIT_GROUP = 10;
    private static final int HIT_SV = 11;
    private static final int HIT_HUE = 12;
    private static final int HIT_ALPHA = 13;

    private static final class SettingHit {
        final RectF bounds = new RectF();
        SettingEntry setting;
        int action;
        int index;
    }

    private final LiquidBounceDataStore dataStore;
    private final ResponsiveTypography typography;
    private final List<CategoryPanel> panels;
    private final Listener listener;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final SettingHit[] settingHits = new SettingHit[128];
    private int settingHitCount;

    private CategoryPanel hitPanel;
    private float hitScale = 1f;
    private SettingHit activeHit;
    private DropdownSetting overlayDropdown;
    private CategoryPanel overlayPanel;
    private final RectF overlayDropdownBounds = new RectF();
    private TopTab activeTab = TopTab.CLICK_GUI;
    private TopTab previousTab = TopTab.CLICK_GUI;
    private final AnimationState tabTransition = new AnimationState(1f);
    private String searchQuery = "";
    private int searchSelectedIndex;
    private boolean hudArrayVisible = true;
    private boolean hudBindsVisible = true;
    private boolean hudNotificationsVisible = true;
    private boolean showGrid;
    private boolean snappingEnabled = true;
    private boolean darkenOverlay = true;
    private float uiScale = 1f;
    private int fontIndex;
    private final List<String> configNames = new ArrayList<>();
    private String selectedConfig = "default";
    private String configStatus = "";
    private long configStatusUntil;
    private String musicQuery = "";
    private final List<MusicStore.Track> musicTracks = new ArrayList<>();
    private final List<MaterialStore.Material> materials = new ArrayList<>();
    private String materialStatus = "";
    private String musicStatus = "";
    private boolean settingsScaleDragging;
    private CategoryPanel scrollPanel;
    private CategoryPanel pressedPanel;
    private CategoryPanel layoutDraggingPanel;
    private float downY;
    private float lastY;
    private float downX;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean moved;
    private boolean isEditMode;
    private boolean longPressTriggered;
    private float originalPanelX;
    private float originalPanelY;
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private final PanelLayoutStorage layoutStorage;
    private final Runnable longPressRunnable = new Runnable() {
        @Override public void run() {
            if (pressedPanel != null && !moved && activeHit == null) beginPanelDrag(pressedPanel, downX, downY);
        }
    };

    public ClickGuiRenderer(Context context, LiquidBounceDataStore dataStore, Listener listener,
                            ResponsiveTypography typography) {
        this.dataStore = dataStore;
        this.panels = dataStore.getCategories();
        this.listener = listener;
        this.typography = typography;
        this.layoutStorage = new PanelLayoutStorage(context);
        text.setTypeface(LiquidBounceFonts.medium());
        stroke.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < settingHits.length; i++) settingHits[i] = new SettingHit();
        restorePanelLayout();
    }

    public void draw(Canvas canvas, boolean debugBounds, long now) {
        settingHitCount = 0;
        overlayDropdown = null;
        overlayPanel = null;
        fill.setShader(null);
        float transition = tabTransition.get(now);
        drawTopTabs(canvas, transition);
        int pageSave = canvas.save();
        float direction = activeTab.ordinal() >= previousTab.ordinal() ? 1f : -1f;
        canvas.translate(direction * (1f - transition) * 120f, 0f);
        int pageLayer = canvas.saveLayerAlpha(null, Math.round(255f * transition));
        if (activeTab == TopTab.CLICK_GUI) {
            drawSearch(canvas);
            for (CategoryPanel panel : panels) drawPanel(canvas, panel, debugBounds, now);
        } else if (activeTab == TopTab.CONFIG) {
            drawConfigPage(canvas, debugBounds, now);
        } else if (activeTab == TopTab.MUSIC) {
            drawMusicPage(canvas, debugBounds);
        } else {
            drawMaterialsPage(canvas, debugBounds);
        }
        if (overlayDropdown != null && overlayPanel != null) drawDropdownOverlay(canvas, overlayPanel, overlayDropdown);
        canvas.restoreToCount(pageLayer);
        canvas.restoreToCount(pageSave);
    }

    private void drawIntegratedControls(Canvas canvas, long now) {
        setFill(LiquidBounceUiColors.PANEL_DEEP); rect.set(UTILITY_LEFT, UTILITY_TOP, UTILITY_RIGHT, UTILITY_BOTTOM); canvas.drawRoundRect(rect, 9, 9, fill);
        setFill(LiquidBounceUiColors.ACCENT); canvas.drawRect(UTILITY_LEFT, UTILITY_BOTTOM - 3, UTILITY_RIGHT, UTILITY_BOTTOM, fill);
        setText(Color.WHITE, 18, LiquidBounceFonts.bold()); drawBaseline(canvas, LiquidBounceI18n.t("HUD"), UTILITY_LEFT + 18, UTILITY_TOP + 7, 30, text);
        float x = UTILITY_LEFT + 90;
        x = drawUtilitySwitch(canvas, "ArrayList", x, hudArrayVisible, "arraylist");
        x = drawUtilitySwitch(canvas, "Binds", x, hudBindsVisible, "binds");
        x = drawUtilitySwitch(canvas, "Notifications", x, hudNotificationsVisible, "notifications");
        setText(LiquidBounceUiColors.TEXT_MUTED, 16, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t("Interface"), x + 18, UTILITY_TOP + 10, 24, text);
        x += 85;
        x = drawUtilitySwitch(canvas, "Grid", x, showGrid, "grid");
        x = drawUtilitySwitch(canvas, "Snap", x, snappingEnabled, "snapping");
        x = drawUtilitySwitch(canvas, "Dim", x, darkenOverlay, "darken");
        setText(LiquidBounceUiColors.TEXT_MUTED, 16, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t("Font"), UTILITY_RIGHT - 255, UTILITY_TOP + 10, 24, text);
        setText(Color.WHITE, 14, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceFonts.familyName(), UTILITY_RIGHT - 255, UTILITY_TOP + 35, 20, text);
        drawMiniScale(canvas, UTILITY_RIGHT - 180, UTILITY_TOP + 47);
    }

    private float drawUtilitySwitch(Canvas canvas, String label, float x, boolean enabled, String id) {
        setText(LiquidBounceUiColors.TEXT_NORMAL, 15, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t(label), x, UTILITY_TOP + 10, 24, text);
        drawSwitch(canvas, x + 49, UTILITY_TOP + 10, enabled);
        return x + 105;
    }

    private void drawMiniScale(Canvas canvas, float x, float y) {
        setText(LiquidBounceUiColors.TEXT_MUTED, 15, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t("UI Scale"), x, y - 10, 22, text);
        setText(Color.WHITE, 15, LiquidBounceFonts.medium()); drawRightBaseline(canvas, String.format(Locale.US, "%.0f%%", uiScale * 100f), x + 155, y - 10, 22, text);
        float left = x, right = x + 155, progress = (uiScale - UI_SCALE_MIN) / (UI_SCALE_MAX - UI_SCALE_MIN);
        setFill(LiquidBounceUiColors.TRACK); canvas.drawRoundRect(left, y + 22, right, y + 27, 3, 3, fill);
        setFill(LiquidBounceUiColors.ACCENT); canvas.drawRoundRect(left, y + 22, left + (right - left) * progress, y + 27, 3, 3, fill);
        setFill(Color.WHITE); canvas.drawCircle(left + (right - left) * progress, y + 24.5f, 8, fill);
    }

    private void drawConfigPage(Canvas canvas, boolean debugBounds, long now) {
        drawWindow(canvas, CONFIG_LEFT, CONFIG_TOP, CONFIG_RIGHT, CONFIG_BOTTOM, "Config");
        drawBaselineLabel(canvas, "Configuration management", CONFIG_LEFT + 70, CONFIG_TOP + 92, 30, LiquidBounceUiColors.TEXT_MUTED);
        float buttonY = CONFIG_TOP + 122;
        drawConfigButton(canvas, "Save current", CONFIG_LEFT + 70, buttonY, 280, 58);
        drawConfigButton(canvas, "Export", CONFIG_LEFT + 370, buttonY, 220, 58);
        drawConfigButton(canvas, "Load selected", CONFIG_LEFT + 610, buttonY, 280, 58);
        drawConfigButton(canvas, "Delete", CONFIG_LEFT + 910, buttonY, 220, 58);
        drawBaselineLabel(canvas, LiquidBounceI18n.t("Selected") + ": " + selectedConfig, CONFIG_LEFT + 70, CONFIG_TOP + 228, 28, LiquidBounceUiColors.ACCENT_SOFT);
        float listTop = CONFIG_TOP + 270;
        if (configNames.isEmpty()) {
            drawBaselineLabel(canvas, "No saved configurations", CONFIG_LEFT + 70, listTop, 34, LiquidBounceUiColors.TEXT_MUTED);
        } else {
            for (int i = 0; i < configNames.size(); i++) {
                String name = configNames.get(i);
                float y = listTop + i * 66;
                setFill(name.equals(selectedConfig) ? 0xB2182A58 : LiquidBounceUiColors.PANEL_ALT); rect.set(CONFIG_LEFT + 70, y, CONFIG_RIGHT - 70, y + 54); canvas.drawRoundRect(rect, 7, 7, fill);
                drawBaselineLabel(canvas, name, CONFIG_LEFT + 92, y + 8, 38, Color.WHITE);
                drawBaselineLabel(canvas, name.equals(selectedConfig) ? "Selected" : "Tap to select", CONFIG_RIGHT - 330, y + 8, 38, LiquidBounceUiColors.TEXT_MUTED);
            }
        }
        if (now < configStatusUntil) drawBaselineLabel(canvas, translateStatus(configStatus), CONFIG_LEFT + 70, CONFIG_BOTTOM - 72, 30, LiquidBounceUiColors.ACCENT_SOFT);
        if (debugBounds) drawDebugRect(canvas, CONFIG_LEFT, CONFIG_TOP, CONFIG_RIGHT - CONFIG_LEFT, CONFIG_BOTTOM - CONFIG_TOP);
    }

    private void drawConfigButton(Canvas canvas, String label, float x, float y, float width, float height) {
        setFill(LiquidBounceUiColors.PANEL_ALT); rect.set(x, y, x + width, y + height); canvas.drawRoundRect(rect, 7, 7, fill);
        setText(Color.WHITE, 17, LiquidBounceFonts.medium()); drawCentered(canvas, LiquidBounceI18n.t(label), x, y, width, height, text);
    }

    private void drawMusicPage(Canvas canvas, boolean debugBounds) {
        drawWindow(canvas, CONFIG_LEFT, CONFIG_TOP, CONFIG_RIGHT, CONFIG_BOTTOM, "Music");
        drawBaselineLabel(canvas, "Search music", CONFIG_LEFT + 70, CONFIG_TOP + 92, 30, LiquidBounceUiColors.TEXT_MUTED);
        setFill(LiquidBounceUiColors.PANEL_ALT); rect.set(CONFIG_LEFT + 70, CONFIG_TOP + 122, CONFIG_LEFT + 950, CONFIG_TOP + 184); canvas.drawRoundRect(rect, 7, 7, fill);
        setText(Color.WHITE, 18, LiquidBounceFonts.medium()); drawBaseline(canvas, musicQuery.isEmpty() ? LiquidBounceI18n.t("Search") : musicQuery, CONFIG_LEFT + 92, CONFIG_TOP + 122, 62, text);
        drawConfigButton(canvas, "Search", CONFIG_LEFT + 980, CONFIG_TOP + 122, 220, 62);
        drawConfigButton(canvas, "Stop", CONFIG_LEFT + 1220, CONFIG_TOP + 122, 180, 62);
        if (!musicStatus.isEmpty()) drawBaselineLabel(canvas, musicStatus, CONFIG_LEFT + 1430, CONFIG_TOP + 132, 38, LiquidBounceUiColors.ACCENT_SOFT);
        if (musicTracks.isEmpty()) drawBaselineLabel(canvas, "No search results", CONFIG_LEFT + 70, CONFIG_TOP + 270, 34, LiquidBounceUiColors.TEXT_MUTED);
        for (int i = 0; i < Math.min(16, musicTracks.size()); i++) { MusicStore.Track track = musicTracks.get(i); float y = CONFIG_TOP + 220 + i * 66; setFill(LiquidBounceUiColors.PANEL_ALT); rect.set(CONFIG_LEFT + 70, y, CONFIG_RIGHT - 70, y + 54); canvas.drawRoundRect(rect, 7, 7, fill); drawBaselineLabel(canvas, (i + 1) + ". " + track.name, CONFIG_LEFT + 92, y + 6, 30, Color.WHITE); drawBaselineLabel(canvas, track.artist, CONFIG_RIGHT - 500, y + 6, 30, LiquidBounceUiColors.TEXT_MUTED); }
        if (debugBounds) drawDebugRect(canvas, CONFIG_LEFT, CONFIG_TOP, CONFIG_RIGHT - CONFIG_LEFT, CONFIG_BOTTOM - CONFIG_TOP);
    }

    private void drawMaterialsPage(Canvas canvas, boolean debugBounds) {
        drawWindow(canvas, CONFIG_LEFT, CONFIG_TOP, CONFIG_RIGHT, CONFIG_BOTTOM, "Materials");
        drawBaselineLabel(canvas, "Local material packs", CONFIG_LEFT + 70, CONFIG_TOP + 92, 30, LiquidBounceUiColors.TEXT_MUTED);
        drawBaselineLabel(canvas, "Directory: files/materials", CONFIG_LEFT + 70, CONFIG_TOP + 142, 25, LiquidBounceUiColors.TEXT_DIM);
        drawConfigButton(canvas, "Refresh", CONFIG_RIGHT - 320, CONFIG_TOP + 102, 250, 62);
        if (materials.isEmpty()) drawBaselineLabel(canvas, "No material packs", CONFIG_LEFT + 70, CONFIG_TOP + 220, 34, LiquidBounceUiColors.TEXT_MUTED);
        for (int i = 0; i < materials.size(); i++) { MaterialStore.Material material = materials.get(i); float y = CONFIG_TOP + 200 + i * 66; setFill(material.name.equals(materialStatus) ? 0xB2182A58 : LiquidBounceUiColors.PANEL_ALT); rect.set(CONFIG_LEFT + 70, y, CONFIG_RIGHT - 70, y + 54); canvas.drawRoundRect(rect, 7, 7, fill); drawBaselineLabel(canvas, material.name, CONFIG_LEFT + 92, y + 6, 30, Color.WHITE); drawBaselineLabel(canvas, "Load", CONFIG_RIGHT - 240, y + 6, 30, LiquidBounceUiColors.ACCENT_SOFT); }
        if (debugBounds) drawDebugRect(canvas, CONFIG_LEFT, CONFIG_TOP, CONFIG_RIGHT - CONFIG_LEFT, CONFIG_BOTTOM - CONFIG_TOP);
    }

    private void drawHudEditor(Canvas canvas, boolean debugBounds) {
        drawWindow(canvas, HUD_LEFT, HUD_TOP, HUD_RIGHT, HUD_BOTTOM, "HUD Editor");
        drawBaselineLabel(canvas, "Components", HUD_LEFT + 80, HUD_TOP + 100, 34, LiquidBounceUiColors.TEXT_MUTED);
        drawComponentRow(canvas, "ArrayList", HUD_LEFT + 80, HUD_TOP + 150, hudArrayVisible, "arraylist");
        drawComponentRow(canvas, "Binds", HUD_LEFT + 80, HUD_TOP + 225, hudBindsVisible, "binds");
        drawComponentRow(canvas, "Notifications", HUD_LEFT + 80, HUD_TOP + 300, hudNotificationsVisible, "notifications");
        setFill(0x22000000); canvas.drawRect(HUD_PREVIEW_LEFT, 620, HUD_PREVIEW_RIGHT, HUD_PREVIEW_BOTTOM, fill);
        if (showGrid) drawEditorGrid(canvas, HUD_PREVIEW_LEFT, 620, HUD_PREVIEW_RIGHT, HUD_PREVIEW_BOTTOM);
        drawPreviewMarker(canvas, HUD_PREVIEW_LEFT + 100, 700, "ArrayList", hudArrayVisible);
        drawPreviewMarker(canvas, HUD_PREVIEW_LEFT + 100, 810, "Binds", hudBindsVisible);
        drawPreviewMarker(canvas, HUD_PREVIEW_LEFT + 100, 920, "Notifications", hudNotificationsVisible);
        if (debugBounds) drawDebugRect(canvas, HUD_LEFT, HUD_TOP, HUD_RIGHT - HUD_LEFT, HUD_BOTTOM - HUD_TOP);
    }

    private void drawSettings(Canvas canvas, boolean debugBounds) {
        drawWindow(canvas, SETTINGS_LEFT, SETTINGS_TOP, SETTINGS_RIGHT, SETTINGS_BOTTOM, "Global Settings");
        drawBaselineLabel(canvas, "Interface", SETTINGS_LEFT + 90, SETTINGS_TOP + 100, 34, LiquidBounceUiColors.TEXT_MUTED);
        float rowX = SETTINGS_LEFT + 90;
        float rowY = SETTINGS_TOP + 160;
        drawSettingRow(canvas, "Language", LiquidBounceI18n.isChinese() ? "Chinese" : "English", rowX, rowY, 0);
        drawSettingRow(canvas, "Show HUD grid", showGrid ? "On" : "Off", rowX, rowY + 70, 1);
        drawSettingRow(canvas, "Snap to grid", snappingEnabled ? "On" : "Off", rowX, rowY + 140, 2);
        drawSettingRow(canvas, "Darken overlay", darkenOverlay ? "On" : "Off", rowX, rowY + 210, 3);
        drawUiScaleRow(canvas, rowX, rowY + 280);
        drawSettingRow(canvas, "Font", LiquidBounceFonts.familyName(), rowX, rowY + 395, 5);
        drawBaselineLabel(canvas, "Changes apply immediately", rowX, rowY + 320, 24, LiquidBounceUiColors.TEXT_MUTED);
        if (debugBounds) drawDebugRect(canvas, SETTINGS_LEFT, SETTINGS_TOP, SETTINGS_RIGHT - SETTINGS_LEFT, SETTINGS_BOTTOM - SETTINGS_TOP);
    }

    private void drawWindow(Canvas canvas, float left, float top, float right, float bottom, String title) {
        setFill(LiquidBounceUiColors.PANEL); rect.set(left, top, right, bottom); canvas.drawRoundRect(rect, 10, 10, fill);
        setFill(LiquidBounceUiColors.PANEL_DEEP); rect.set(left, top, right, top + 62); canvas.drawRoundRect(rect, 10, 10, fill);
        canvas.drawRect(left, top + 42, right, top + 62, fill);
        setFill(LiquidBounceUiColors.ACCENT); canvas.drawRect(left, top + 60, right, top + 63, fill);
        setText(Color.WHITE, 25, LiquidBounceFonts.bold()); drawBaseline(canvas, LiquidBounceI18n.t(title), left + 26, top + 8, 52, text);
    }

    private void drawComponentRow(Canvas canvas, String label, float x, float y, boolean enabled, String id) {
        setFill(LiquidBounceUiColors.PANEL_ALT); rect.set(x, y, x + 1500, y + 64); canvas.drawRoundRect(rect, 7, 7, fill);
        drawBaselineLabel(canvas, label, x + 20, y + 10, 34, LiquidBounceUiColors.TEXT_NORMAL);
        drawSwitch(canvas, x + 1440, y + 19, enabled);
    }

    private void drawPreviewMarker(Canvas canvas, float x, float y, String label, boolean enabled) {
        setFill(enabled ? 0x663C69FC : 0x33222222); rect.set(x, y, x + 420, y + 64); canvas.drawRoundRect(rect, 8, 8, fill);
        drawBaselineLabel(canvas, label, x + 18, y + 14, 36, enabled ? Color.WHITE : LiquidBounceUiColors.TEXT_DIM);
        drawBaselineLabel(canvas, enabled ? "Visible" : "Hidden", x + 270, y + 14, 36, enabled ? LiquidBounceUiColors.ACCENT_SOFT : LiquidBounceUiColors.TEXT_DIM);
    }

    private void drawSettingRow(Canvas canvas, String label, String value, float x, float y, int index) {
        float width = SETTINGS_RIGHT - SETTINGS_LEFT - 68;
        setFill(LiquidBounceUiColors.PANEL_ALT); rect.set(x, y, x + width, y + 58); canvas.drawRoundRect(rect, 7, 7, fill);
        drawBaselineLabel(canvas, LiquidBounceI18n.t(label), x + 22, y + 11, 36, LiquidBounceUiColors.TEXT_NORMAL);
        setText(LiquidBounceUiColors.ACCENT_SOFT, 20, LiquidBounceFonts.medium());
        drawRightBaseline(canvas, LiquidBounceI18n.t(value), x + width - 56, y + 11, 36, text);
        drawChevron(canvas, x + width - 22, y + 29, false);
    }

    private void drawUiScaleRow(Canvas canvas, float x, float y) {
        float width = SETTINGS_RIGHT - SETTINGS_LEFT - 68;
        setFill(LiquidBounceUiColors.PANEL_ALT); rect.set(x, y, x + width, y + 88); canvas.drawRoundRect(rect, 7, 7, fill);
        drawBaselineLabel(canvas, "UI Scale", x + 22, y + 9, 30, LiquidBounceUiColors.TEXT_NORMAL);
        setText(LiquidBounceUiColors.ACCENT_SOFT, 20, LiquidBounceFonts.medium());
        drawRightBaseline(canvas, String.format(Locale.US, "%.0f%%", uiScale * 100f), x + width - 56, y + 9, 30, text);
        float left = x + 22, right = x + width - 130, ty = y + 60;
        setFill(LiquidBounceUiColors.TRACK); canvas.drawRoundRect(left, ty, right, ty + 5, 3, 3, fill);
        float progress = (uiScale - UI_SCALE_MIN) / (UI_SCALE_MAX - UI_SCALE_MIN);
        setFill(LiquidBounceUiColors.ACCENT); canvas.drawRoundRect(left, ty, left + (right - left) * progress, ty + 5, 3, 3, fill);
        setFill(Color.WHITE); canvas.drawCircle(left + (right - left) * progress, ty + 2.5f, 10, fill);
    }

    private void drawSwitch(Canvas canvas, float x, float y, boolean enabled) {
        float width = control(49), height = control(26);
        setFill(enabled ? LiquidBounceUiColors.ACCENT : LiquidBounceUiColors.TOGGLE_OFF); rect.set(x, y, x + width, y + height); canvas.drawRoundRect(rect, height * .5f, height * .5f, fill);
        setFill(Color.WHITE); canvas.drawCircle(x + (enabled ? width - control(11) : control(11)), y + height * .5f, control(10), fill);
    }

    private void drawBaselineLabel(Canvas canvas, String value, float x, float y, float height, int color) {
        setText(color, 20, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t(value), x, y, height, text);
    }

    private void drawEditorGrid(Canvas canvas, float left, float top, float right, float bottom) {
        setFill(0x2AFFFFFF);
        for (float x = left; x <= right; x += 80) canvas.drawRect(x, top, x + 1, bottom, fill);
        for (float y = top; y <= bottom; y += 80) canvas.drawRect(left, y, right, y + 1, fill);
    }

    private void drawTopTabs(Canvas canvas, float transition) {
        setFill(LiquidBounceUiColors.PANEL_DEEP);
        rect.set(LiquidBounceUiMetrics.TOP_TABS_X, LiquidBounceUiMetrics.TOP_TABS_Y,
                LiquidBounceUiMetrics.TOP_TABS_X + LiquidBounceUiMetrics.TOP_TABS_WIDTH,
                LiquidBounceUiMetrics.TOP_TABS_Y + LiquidBounceUiMetrics.TOP_TABS_HEIGHT);
        canvas.drawRoundRect(rect, LiquidBounceUiMetrics.TOP_TABS_RADIUS, LiquidBounceUiMetrics.TOP_TABS_RADIUS, fill);
        String[] labels = {LiquidBounceI18n.t("ClickGUI"), LiquidBounceI18n.t("Config"), LiquidBounceI18n.t("Music"), LiquidBounceI18n.t("Materials")};
        float tabWidth = LiquidBounceUiMetrics.TOP_TABS_WIDTH / labels.length;
        float[] widths = {tabWidth, tabWidth, tabWidth, tabWidth};
        float x = LiquidBounceUiMetrics.TOP_TABS_X;
        int oldIndex = previousTab.ordinal();
        int newIndex = activeTab.ordinal();
        float indicatorX = LiquidBounceUiMetrics.TOP_TABS_X
                + tabOffset(oldIndex, widths) * (1f - transition)
                + tabOffset(newIndex, widths) * transition;
        float indicatorWidth = widths[oldIndex] * (1f - transition) + widths[newIndex] * transition;
        setFill(0x334677FF);
        rect.set(indicatorX + 3, LiquidBounceUiMetrics.TOP_TABS_Y + 3,
                indicatorX + indicatorWidth - 3,
                LiquidBounceUiMetrics.TOP_TABS_Y + LiquidBounceUiMetrics.TOP_TABS_HEIGHT - 3);
        float indicatorRadius = (LiquidBounceUiMetrics.TOP_TABS_HEIGHT - 6f) * .5f;
        canvas.drawRoundRect(rect, indicatorRadius, indicatorRadius, fill);
        stroke.setColor(LiquidBounceUiColors.ACCENT); stroke.setStrokeWidth(2f);
        canvas.drawRoundRect(rect, indicatorRadius, indicatorRadius, stroke);
        for (int i = 0; i < labels.length; i++) {
            boolean active = activeTab.ordinal() == i;
            setText(active ? Color.WHITE : 0xFFB7B8BD, 20, active ? LiquidBounceFonts.bold() : LiquidBounceFonts.medium());
            drawCentered(canvas, labels[i], x, LiquidBounceUiMetrics.TOP_TABS_Y, widths[i], LiquidBounceUiMetrics.TOP_TABS_HEIGHT, text);
            x += widths[i];
        }
    }

    private float tabOffset(int index, float[] widths) {
        float offset = 0f;
        for (int i = 0; i < index; i++) offset += widths[i];
        return offset;
    }

    private void drawSearch(Canvas canvas) {
        setFill(0xE6000000); rect.set(LiquidBounceUiMetrics.SEARCH_X, LiquidBounceUiMetrics.SEARCH_Y,
                LiquidBounceUiMetrics.SEARCH_X + LiquidBounceUiMetrics.SEARCH_WIDTH,
                LiquidBounceUiMetrics.SEARCH_Y + LiquidBounceUiMetrics.SEARCH_HEIGHT);
        canvas.drawRoundRect(rect, LiquidBounceUiMetrics.SEARCH_RADIUS, LiquidBounceUiMetrics.SEARCH_RADIUS, fill);
        setText(searchQuery.isEmpty() ? LiquidBounceUiColors.TEXT_MUTED : LiquidBounceUiColors.TEXT_NORMAL, 30, LiquidBounceFonts.regular());
        drawBaseline(canvas, searchQuery.isEmpty() ? LiquidBounceI18n.t("Search") : searchQuery,
                LiquidBounceUiMetrics.SEARCH_X + 46, LiquidBounceUiMetrics.SEARCH_Y,
                LiquidBounceUiMetrics.SEARCH_HEIGHT, text);
        if (!searchQuery.isEmpty()) {
            stroke.setColor(LiquidBounceUiColors.TEXT_MUTED); stroke.setStrokeWidth(3);
            float cx = LiquidBounceUiMetrics.SEARCH_X + LiquidBounceUiMetrics.SEARCH_WIDTH - 34;
            float cy = LiquidBounceUiMetrics.SEARCH_Y + LiquidBounceUiMetrics.SEARCH_HEIGHT * .5f;
            canvas.drawLine(cx - 7, cy - 7, cx + 7, cy + 7, stroke);
            canvas.drawLine(cx + 7, cy - 7, cx - 7, cy + 7, stroke);
        }
        drawSearchResults(canvas);
    }

    private void drawSearchResults(Canvas canvas) {
        List<ModuleEntry> results = getSearchResults();
        if (results.isEmpty()) return;
        float left = LiquidBounceUiMetrics.SEARCH_X;
        float top = LiquidBounceUiMetrics.SEARCH_Y + LiquidBounceUiMetrics.SEARCH_HEIGHT;
        float width = LiquidBounceUiMetrics.SEARCH_WIDTH;
        float rowHeight = 53f;
        float bottom = top + Math.min(7, results.size()) * rowHeight + 10;
        setFill(0xE6000000); rect.set(left, top, left + width, bottom); canvas.drawRoundRect(rect, 10, 10, fill);
        setFill(LiquidBounceUiColors.ACCENT); canvas.drawRect(left, top, left + width, top + 2, fill);
        for (int i = 0; i < Math.min(7, results.size()); i++) {
            ModuleEntry module = results.get(i);
            float y = top + 5 + i * rowHeight;
            if (i == searchSelectedIndex) {
                setFill(0x301F56C8); rect.set(left + 8, y, left + width - 8, y + rowHeight - 2); canvas.drawRoundRect(rect, 4, 4, fill);
            }
            setText(module.enabled ? LiquidBounceUiColors.ACCENT : LiquidBounceUiColors.TEXT_MUTED, 18, LiquidBounceFonts.medium());
            drawBaseline(canvas, LiquidBounceI18n.moduleName(module.id, module.name), left + 18, y, rowHeight - 2, text);
            if (!module.aliases.isEmpty()) {
                setText(LiquidBounceUiColors.TEXT_DIM, 14, LiquidBounceFonts.regular());
                drawBaseline(canvas, LiquidBounceI18n.t("aka") + " " + joinAliases(module), left + 190, y, rowHeight - 2, text);
            }
        }
    }

    private String joinAliases(ModuleEntry module) {
        StringBuilder result = new StringBuilder();
        for (String alias : module.aliases) {
            if (result.length() > 0) result.append(", ");
            result.append(alias);
        }
        return result.toString();
    }

    private List<ModuleEntry> getSearchResults() {
        List<ModuleEntry> results = new ArrayList<>();
        if (searchQuery.isEmpty()) return results;
        for (ModuleEntry module : dataStore.getModules()) if (matches(module)) results.add(module);
        searchSelectedIndex = Math.max(0, Math.min(searchSelectedIndex, results.size() - 1));
        return results;
    }

    private void drawPanel(Canvas canvas, CategoryPanel panel, boolean debugBounds, long now) {
        int save = canvas.save();
        float panelScale = panelScale(panel);
        canvas.scale(panelScale, panelScale, panelAnchorX(panel), panelAnchorY(panel));
        hitPanel = panel;
        hitScale = panelScale;
        if (panel == layoutDraggingPanel) {
            canvas.scale(1.04f, 1.04f, panel.x + 187.5f, panel.y + 120f);
        }
        float progress = panel.expansion.get(now);
        panel.contentHeight = calculateContentHeight(panel, now);
        float availableBodyHeight = calculateAvailableBodyHeight(panel);
        float bodyViewportHeight = Math.min(panel.contentHeight, availableBodyHeight);
        float bodyHeight = bodyViewportHeight * progress;
        float panelHeight = PANEL_CONTENT_TOP + bodyHeight;
        setFill(LiquidBounceUiColors.PANEL);
        rect.set(panel.x, panel.y, panel.x + LiquidBounceUiMetrics.PANEL_WIDTH, panel.y + panelHeight);
        canvas.drawRoundRect(rect, LiquidBounceUiMetrics.PANEL_RADIUS, LiquidBounceUiMetrics.PANEL_RADIUS, fill);
        setFill(LiquidBounceUiColors.PANEL_DEEP);
        rect.set(panel.x, panel.y, panel.x + LiquidBounceUiMetrics.PANEL_WIDTH,
                panel.y + LiquidBounceUiMetrics.PANEL_HEADER_HEIGHT);
        canvas.drawRoundRect(rect, LiquidBounceUiMetrics.PANEL_RADIUS, LiquidBounceUiMetrics.PANEL_RADIUS, fill);
        canvas.drawRect(panel.x,
                panel.y + LiquidBounceUiMetrics.PANEL_HEADER_HEIGHT - LiquidBounceUiMetrics.PANEL_RADIUS,
                panel.x + LiquidBounceUiMetrics.PANEL_WIDTH,
                panel.y + LiquidBounceUiMetrics.PANEL_HEADER_HEIGHT, fill);
        setFill(LiquidBounceUiColors.PANEL_HIGHLIGHT);
        rect.set(panel.x + 3f, panel.y + 1f,
                panel.x + LiquidBounceUiMetrics.PANEL_WIDTH - 3f, panel.y + 2f);
        canvas.drawRect(rect, fill);
        setFill(LiquidBounceUiColors.ACCENT);
        canvas.drawRect(panel.x,
                panel.y + LiquidBounceUiMetrics.PANEL_HEADER_HEIGHT - 2f,
                panel.x + LiquidBounceUiMetrics.PANEL_WIDTH,
                panel.y + LiquidBounceUiMetrics.PANEL_HEADER_HEIGHT + 1f, fill);
        drawCategoryIcon(canvas, panel.name, panel.x + 34, panel.y + 29);
        setText(Color.WHITE, 22, LiquidBounceFonts.bold());
        drawBaseline(canvas, LiquidBounceI18n.t(panel.name), panel.x + 62, panel.y, 58, text);
        setText(Color.WHITE, 26, LiquidBounceFonts.regular());
        drawCentered(canvas, panel.expanded ? "−" : "+", panel.x + 326, panel.y, 44, 58, text);
        if (bodyHeight > .5f) {
            int bodySave = canvas.save();
            canvas.clipRect(panel.x, panel.y + PANEL_CONTENT_TOP,
                    panel.x + LiquidBounceUiMetrics.PANEL_WIDTH, panel.y + panelHeight);
            float maxScroll = Math.max(0, panel.contentHeight - bodyViewportHeight);

            panel.scrollOffset = clamp(panel.scrollOffset, 0, maxScroll);
            panel.targetScrollOffset = clamp(panel.targetScrollOffset, 0, maxScroll);
            panel.scrollOffset += (panel.targetScrollOffset - panel.scrollOffset) * .28f;
            float y = panel.y + PANEL_CONTENT_TOP - panel.scrollOffset;
            for (ModuleEntry module : panel.modules) {
                if (!matches(module.name)) continue;
                drawModuleRow(canvas, panel, module, y, now);
                y += 53f;
                float expansion = module.settingsProgress.get(now);
                if (module.hasSettings() && expansion > .001f) {
                    float fullHeight = measureSettings(module.settings, now);
                    int nested = canvas.save();
                    canvas.clipRect(panel.x, y, panel.x + 375, y + fullHeight * expansion);
                    drawSettings(canvas, panel, module.settings, y, 0, now);
                    canvas.restoreToCount(nested);
                    y += fullHeight * expansion;
                }
            }
            if (panel.contentHeight > bodyViewportHeight) drawScrollbar(canvas, panel, bodyHeight, bodyViewportHeight);
            canvas.restoreToCount(bodySave);
        }
        if (debugBounds) drawDebugRect(canvas, panel.x, panel.y, 375, panelHeight);
        hitPanel = null;
        hitScale = 1f;
        canvas.restoreToCount(save);
    }

    private float calculateContentHeight(CategoryPanel panel, long now) {
        float height = 0;
        for (ModuleEntry module : panel.modules) if (matches(module.name)) {
            height += 53f;
            if (module.hasSettings()) height += measureSettings(module.settings, now) * module.settingsProgress.get(now);
        }
        return height > 0 ? height + PANEL_CONTENT_BOTTOM_PADDING : 0;
    }

    private float calculateAvailableBodyHeight(CategoryPanel panel) {

        float scale = panelScale(panel);
        float anchorY = panelAnchorY(panel);
        return Math.max(0, (LiquidBounceUiMetrics.CONTENT_HEIGHT - PANEL_SCREEN_BOTTOM_INSET - anchorY) / scale
                + anchorY - panel.y - PANEL_CONTENT_TOP);
    }

    private float panelScale(CategoryPanel panel) {
        return clamp(uiScale, UI_SCALE_MIN, UI_SCALE_MAX);
    }

    private float panelAnchorX(CategoryPanel panel) {
        return panel.x + LiquidBounceUiMetrics.PANEL_WIDTH * .5f;
    }

    private float panelAnchorY(CategoryPanel panel) {
        return panel.y + PANEL_GRID_CELL_HEIGHT * .5f;
    }

    private float scalePanelX(CategoryPanel panel, float x) {
        return panelAnchorX(panel) + (x - panelAnchorX(panel)) * panelScale(panel);
    }

    private float scalePanelY(CategoryPanel panel, float y) {
        return panelAnchorY(panel) + (y - panelAnchorY(panel)) * panelScale(panel);
    }

    private float unscalePanelX(CategoryPanel panel, float x) {
        return panelAnchorX(panel) + (x - panelAnchorX(panel)) / panelScale(panel);
    }

    private float unscalePanelY(CategoryPanel panel, float y) {
        return panelAnchorY(panel) + (y - panelAnchorY(panel)) / panelScale(panel);
    }

    private float calculatePanelHeight(CategoryPanel panel, long now) {
        panel.contentHeight = calculateContentHeight(panel, now);
        return PANEL_CONTENT_TOP + Math.min(panel.contentHeight, calculateAvailableBodyHeight(panel)) * panel.expansion.get(now);
    }

    private float calculateMaxScroll(CategoryPanel panel, long now) {
        panel.contentHeight = calculateContentHeight(panel, now);
        return Math.max(0, panel.contentHeight - Math.min(panel.contentHeight, calculateAvailableBodyHeight(panel)));
    }

    private float measureSettings(List<SettingEntry> settings, long now) {
        float height = 10;
        for (SettingEntry setting : settings) if (setting.visible) height += measureSetting(setting, now);
        return height + 8;
    }

    private float measureSetting(SettingEntry setting, long now) {
        switch (setting.type) {
            case TOGGLE: return control(43);
            case SLIDER: return control(69);
            case RANGE: return control(72);
            case DROPDOWN: return control(61);
            case BIND: return control(79);
            case MULTI_SELECT:
                MultiSelectSetting multi = (MultiSelectSetting) setting;
                return control(77 + Math.max(0, (multi.options.size() - 1) / 3) * 38);
            case COLOR: return control(55 + (((ColorSetting) setting).expanded ? 216 : 0));
            case GROUP:
                SettingGroup group = (SettingGroup) setting;
                return control(46) + measureSettings(group.children, now) * group.expansion.get(now);
            default: return 0;
        }
    }

    private void drawModuleRow(Canvas canvas, CategoryPanel panel, ModuleEntry module, float y, long now) {
        int color = LiquidBounceUiColors.blend(LiquidBounceUiColors.TEXT_NORMAL, LiquidBounceUiColors.ACCENT, module.enabledProgress.get(now));
        setText(color, 19, LiquidBounceFonts.medium());
        drawCentered(canvas, LiquidBounceI18n.moduleName(module.id, module.name), panel.x + 8, y, 332, 53, text);
        if (!module.hasSettings()) return;
        stroke.setColor(0xFF8A8B91); stroke.setStrokeWidth(2.4f);
        float cx = panel.x + 344, cy = y + 26;
        if (module.settingsExpanded) {
            canvas.drawLine(cx - 6, cy - 3, cx, cy + 3, stroke); canvas.drawLine(cx, cy + 3, cx + 6, cy - 3, stroke);
        } else {
            canvas.drawLine(cx - 3, cy - 6, cx + 3, cy, stroke); canvas.drawLine(cx + 3, cy, cx - 3, cy + 6, stroke);
        }
    }

    private float drawSettings(Canvas canvas, CategoryPanel panel, List<SettingEntry> settings, float startY, int level, long now) {
        float fullHeight = measureSettings(settings, now);
        float railX = panel.x + 17 + level * 13;
        setFill(0x4A06173E); canvas.drawRect(railX - 7, startY, railX + 6, startY + fullHeight, fill);
        setFill(LiquidBounceUiColors.ACCENT); canvas.drawRect(railX, startY, railX + 3, startY + fullHeight, fill);
        float y = startY + 10;
        for (SettingEntry setting : settings) {
            if (!setting.visible) continue;
            y = drawSetting(canvas, panel, setting, y, level, now);
        }
        return y + 8;
    }

    private float drawSetting(Canvas canvas, CategoryPanel panel, SettingEntry setting, float y, int level, long now) {
        float x = panel.x + 27 + level * 13;
        float right = panel.x + 358;
        switch (setting.type) {
            case BIND: return drawBind(canvas, (BindSetting) setting, x, right, y);
            case TOGGLE: return drawToggle(canvas, (ToggleSetting) setting, x, right, y, now);
            case SLIDER: return drawSlider(canvas, (SliderSetting) setting, x, right, y);
            case RANGE: return drawRange(canvas, (RangeSetting) setting, x, right, y);
            case DROPDOWN: return drawDropdown(canvas, panel, (DropdownSetting) setting, x, right, y);
            case MULTI_SELECT: return drawMulti(canvas, (MultiSelectSetting) setting, x, right, y);
            case COLOR: return drawColor(canvas, (ColorSetting) setting, x, right, y);
            case GROUP:
                SettingGroup group = (SettingGroup) setting;
                float groupHeight = control(46);
                setText(Color.WHITE, 19, LiquidBounceFonts.bold()); drawBaseline(canvas, LiquidBounceI18n.t(group.name), x, y, groupHeight, text); drawChevron(canvas, right - control(8), y + groupHeight * .5f, group.expanded);
                addHit(group, HIT_GROUP, -1, x - control(5), y, right, y + groupHeight);
                float childHeight = measureSettings(group.children, now) * group.expansion.get(now);
                if (childHeight > .5f) {
                    int save = canvas.save(); canvas.clipRect(panel.x, y + groupHeight,
                            panel.x + 375, y + groupHeight + childHeight);
                    drawSettings(canvas, panel, group.children, y + groupHeight, level + 1, now); canvas.restoreToCount(save);
                }
                return y + groupHeight + childHeight;
            default: return y;
        }
    }

    private float drawBind(Canvas canvas, BindSetting setting, float x, float right, float y) {
        float height = control(63), radius = control(6);
        rect.set(x, y, right, y + height); setFill(0xFF010205); canvas.drawRoundRect(rect, radius, radius, fill); stroke.setColor(LiquidBounceUiColors.ACCENT); stroke.setStrokeWidth(control(2.5f)); canvas.drawRoundRect(rect, radius, radius, stroke);
        setText(Color.WHITE, 17, LiquidBounceFonts.medium()); drawCentered(canvas, LiquidBounceI18n.t(setting.name), x, y + control(5), right - x, control(25), text);
        setText(setting.listening ? LiquidBounceUiColors.ACCENT : LiquidBounceUiColors.TEXT_MUTED, 16, LiquidBounceFonts.regular()); drawCentered(canvas, setting.listening ? LiquidBounceI18n.t("Press a key") : setting.value, x, y + control(30), right - x, control(25), text);
        addHit(setting, HIT_BIND, -1, x, y, right, y + height); return y + control(79);
    }

    private float drawToggle(Canvas canvas, ToggleSetting setting, float x, float right, float y, long now) {
        float progress = setting.progress.get(now);
        float width = control(49), height = control(21), knobRadius = control(10.5f), top = y + control(10);
        setFill(LiquidBounceUiColors.blend(LiquidBounceUiColors.TOGGLE_OFF, LiquidBounceUiColors.ACCENT_DEEP, progress));
        rect.set(x, top, x + width, top + height); canvas.drawRoundRect(rect, height * .5f, height * .5f, fill);
        setFill(LiquidBounceUiColors.blend(0xFFF4F4F4, LiquidBounceUiColors.ACCENT, progress));
        canvas.drawCircle(x + control(11) + control(27) * progress, top + height * .5f, knobRadius, fill);
        setText(LiquidBounceUiColors.TEXT_NORMAL, 18, LiquidBounceFonts.medium());
        drawBaseline(canvas, LiquidBounceI18n.t(setting.name), x + control(59), y, control(40), text);
        addHit(setting, HIT_TOGGLE, -1, x - control(5), y, right, y + control(42)); return y + control(43);
    }

    private float drawSlider(Canvas canvas, SliderSetting setting, float x, float right, float y) {
        float progress = (setting.value - setting.min) / Math.max(.0001f, setting.max - setting.min);
        setText(LiquidBounceUiColors.TEXT_NORMAL, 18, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t(setting.name), x, y, control(29), text);
        setText(LiquidBounceUiColors.TEXT_NORMAL, 17, LiquidBounceFonts.regular()); drawRightBaseline(canvas, formatValue(setting.value) + (setting.unit.isEmpty() ? "" : " " + LiquidBounceI18n.t(setting.unit)), right, y, control(29), text);
        float ty = y + control(42), trackHeight = control(4), thumbRadius = control(9); setFill(LiquidBounceUiColors.TRACK); canvas.drawRoundRect(x, ty, right, ty + trackHeight, trackHeight * .5f, trackHeight * .5f, fill); setFill(LiquidBounceUiColors.ACCENT); canvas.drawRoundRect(x, ty, x + (right - x) * progress, ty + trackHeight, trackHeight * .5f, trackHeight * .5f, fill); canvas.drawCircle(x + (right - x) * progress, ty + trackHeight * .5f, thumbRadius, fill);
        addHit(setting, HIT_SLIDER, -1, x, y + control(25), right, y + control(59)); return y + control(69);
    }

    private float drawRange(Canvas canvas, RangeSetting setting, float x, float right, float y) {
        float span = Math.max(.0001f, setting.max - setting.min), low = (setting.low - setting.min) / span, high = (setting.high - setting.min) / span;
        setText(LiquidBounceUiColors.TEXT_NORMAL, 18, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t(setting.name), x, y, control(29), text);
        setText(LiquidBounceUiColors.TEXT_NORMAL, 17, LiquidBounceFonts.regular()); drawRightBaseline(canvas, formatValue(setting.low) + " - " + formatValue(setting.high), right, y, control(29), text);
        float ty = y + control(42), trackHeight = control(4), thumbRadius = control(9); setFill(LiquidBounceUiColors.TRACK); canvas.drawRoundRect(x, ty, right, ty + trackHeight, trackHeight * .5f, trackHeight * .5f, fill); setFill(LiquidBounceUiColors.ACCENT); canvas.drawRoundRect(x + (right - x) * low, ty, x + (right - x) * high, ty + trackHeight, trackHeight * .5f, trackHeight * .5f, fill); canvas.drawCircle(x + (right - x) * low, ty + trackHeight * .5f, thumbRadius, fill); canvas.drawCircle(x + (right - x) * high, ty + trackHeight * .5f, thumbRadius, fill);
        float mid = x + (right - x) * (low + high) * .5f; addHit(setting, HIT_RANGE_LOW, -1, x, y + control(25), mid, y + control(60)); addHit(setting, HIT_RANGE_HIGH, -1, mid, y + control(25), right, y + control(60)); return y + control(72);
    }

    private float drawDropdown(Canvas canvas, CategoryPanel panel, DropdownSetting setting, float x, float right, float y) {
        float height = control(42), radius = control(6);
        rect.set(x, y, right, y + height); setFill(LiquidBounceUiColors.ACCENT_SOFT); canvas.drawRoundRect(rect, radius, radius, fill);
        setText(Color.WHITE, 18, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t(setting.name) + " • " + LiquidBounceI18n.t(setting.value), x + control(14), y, height, text); drawChevron(canvas, right - control(18), y + height * .5f, false);
        addHit(setting, HIT_DROPDOWN, -1, x, y, right, y + height);
        if (setting.open) {
            overlayDropdown = setting;
            overlayPanel = panel;
            overlayDropdownBounds.set(scalePanelX(panel, x), scalePanelY(panel, y),
                    scalePanelX(panel, right), scalePanelY(panel, y + height));
        }
        return y + control(61);
    }

    private float drawMulti(Canvas canvas, MultiSelectSetting setting, float x, float right, float y) {
        setText(Color.WHITE, 18, LiquidBounceFonts.bold()); drawBaseline(canvas, LiquidBounceI18n.t(setting.name), x, y, control(35), text);
        setText(LiquidBounceUiColors.TEXT_NORMAL, 17, LiquidBounceFonts.medium()); drawRightBaseline(canvas, setting.selected.size() + " / " + setting.options.size(), right, y, control(35), text);
        float tx = x + control(22), ty = y + control(39);
        for (int i = 0; i < setting.options.size(); i++) {
            String option = setting.options.get(i); String display = LiquidBounceI18n.t(option); setText(setting.selected.contains(option) ? LiquidBounceUiColors.ACCENT_SOFT : LiquidBounceUiColors.TEXT_MUTED, 17, LiquidBounceFonts.medium()); float w = text.measureText(display) + control(20);
            if (tx + w > right) { tx = x + control(22); ty += control(38); }
            float chipHeight = control(31), radius = control(5);
            rect.set(tx, ty, tx + w, ty + chipHeight); setFill(setting.selected.contains(option) ? 0xB2182A58 : 0x78101425); canvas.drawRoundRect(rect, radius, radius, fill); drawCentered(canvas, display, tx, ty, w, chipHeight, text);
            addHit(setting, HIT_MULTI_OPTION, i, tx, ty, tx + w, ty + chipHeight); tx += w + control(8);
        }
        return Math.max(y + control(77), ty + control(38));
    }

    private float drawColor(Canvas canvas, ColorSetting setting, float x, float right, float y) {
        setText(LiquidBounceUiColors.TEXT_NORMAL, 18, LiquidBounceFonts.medium()); drawBaseline(canvas, LiquidBounceI18n.t(setting.name), x, y, control(42), text);
        setText(LiquidBounceUiColors.TEXT_NORMAL, 17, LiquidBounceFonts.regular()); drawRightBaseline(canvas, toRgbaHex(setting.color), right - control(64), y, control(42), text);
        setFill(setting.color); rect.set(right - control(46), y + control(7), right, y + control(37)); canvas.drawRoundRect(rect, control(5), control(5), fill); stroke.setColor(LiquidBounceUiColors.ACCENT); stroke.setStrokeWidth(control(2)); canvas.drawRoundRect(rect, control(5), control(5), stroke);
        addHit(setting, HIT_COLOR, -1, x, y, right, y + control(48));
        if (!setting.expanded) return y + control(55);
        float pickerY = y + control(55), svRight = right - control(62), pickerHeight = control(192);
        RectF sv = addHit(setting, HIT_SV, -1, x, pickerY, svRight, pickerY + pickerHeight).bounds; drawChecker(canvas, sv, control(10));
        int pure = Color.HSVToColor(new float[]{setting.hue, 1, 1}); fill.setShader(new LinearGradient(sv.left, 0, sv.right, 0, Color.WHITE, pure, Shader.TileMode.CLAMP)); canvas.drawRect(sv, fill); fill.setShader(new LinearGradient(0, sv.top, 0, sv.bottom, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)); canvas.drawRect(sv, fill); fill.setShader(null);
        stroke.setColor(Color.WHITE); stroke.setStrokeWidth(control(3)); canvas.drawCircle(sv.left + setting.saturation * sv.width(), sv.top + (1 - setting.brightness) * sv.height(), control(11), stroke);
        RectF hue = addHit(setting, HIT_HUE, -1, right - control(48), pickerY, right - control(34), pickerY + pickerHeight).bounds; int[] rainbow = {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED}; fill.setShader(new LinearGradient(0, hue.top, 0, hue.bottom, rainbow, null, Shader.TileMode.CLAMP)); canvas.drawRoundRect(hue, control(2), control(2), fill); fill.setShader(null); canvas.drawCircle(hue.centerX(), hue.top + setting.hue / 360f * hue.height(), control(11), stroke);
        RectF alpha = addHit(setting, HIT_ALPHA, -1, right - control(20), pickerY, right - control(6), pickerY + pickerHeight).bounds; drawChecker(canvas, alpha, control(7)); int opaque = Color.HSVToColor(new float[]{setting.hue, setting.saturation, setting.brightness}); fill.setShader(new LinearGradient(0, alpha.top, 0, alpha.bottom, opaque, Color.TRANSPARENT, Shader.TileMode.CLAMP)); canvas.drawRoundRect(alpha, control(2), control(2), fill); fill.setShader(null); canvas.drawCircle(alpha.centerX(), alpha.top + (1 - setting.alpha) * alpha.height(), control(11), stroke);
        return pickerY + control(216);
    }

    private void drawDropdownOverlay(Canvas canvas, CategoryPanel panel, DropdownSetting setting) {
        float panelBottom = scalePanelY(panel, panel.y + calculatePanelHeight(panel, android.os.SystemClock.uptimeMillis()));
        int save = canvas.save();
        float panelTop = scalePanelY(panel, panel.y + PANEL_CONTENT_TOP);
        float panelLeft = scalePanelX(panel, panel.x);
        float panelRight = scalePanelX(panel, panel.x + 375);
        canvas.clipRect(Math.min(panelLeft, panelRight), Math.min(panelTop, panelBottom),
                Math.max(panelLeft, panelRight), Math.max(panelTop, panelBottom));
        float s = panelScale(panel);
        float rowHeight = control(41) * s, top = overlayDropdownBounds.bottom + control(3) * s, h = setting.options.size() * rowHeight;
        setFill(0xFF030507); rect.set(overlayDropdownBounds.left, top, overlayDropdownBounds.right, top + h); canvas.drawRoundRect(rect, control(6) * s, control(6) * s, fill); stroke.setColor(LiquidBounceUiColors.ACCENT); stroke.setStrokeWidth(control(2) * s); canvas.drawRoundRect(rect, control(6) * s, control(6) * s, stroke);
        for (int i = 0; i < setting.options.size(); i++) {
            String option = setting.options.get(i); String display = LiquidBounceI18n.t(option); setText(option.equals(setting.value) ? LiquidBounceUiColors.ACCENT : LiquidBounceUiColors.TEXT_NORMAL, 18 * s, LiquidBounceFonts.medium()); drawCentered(canvas, display, rect.left, top + i * rowHeight, rect.width(), rowHeight, text);
            addHit(setting, HIT_DROPDOWN_OPTION, i, rect.left, top + i * rowHeight, rect.right, top + (i + 1) * rowHeight);
        }
        canvas.restoreToCount(save);
    }

    private SettingHit addHit(SettingEntry setting, int action, int index, float left, float top, float right, float bottom) {
        if (hitPanel != null) {
            left = scalePanelX(hitPanel, left);
            right = scalePanelX(hitPanel, right);
            top = scalePanelY(hitPanel, top);
            bottom = scalePanelY(hitPanel, bottom);
        }
        SettingHit hit = settingHits[Math.min(settingHitCount++, settingHits.length - 1)]; hit.setting = setting; hit.action = action; hit.index = index; hit.bounds.set(left, top, right, bottom); return hit;
    }

    public boolean onTouchDown(float x, float y) {
        cancelPanelLongPress();
        moved = false; longPressTriggered = false; downX = x; downY = lastY = y; activeHit = findHit(x, y);
        if (activeTab != TopTab.CLICK_GUI) return true;
        if (activeHit != null && (activeHit.action == HIT_SLIDER || activeHit.action == HIT_RANGE_LOW || activeHit.action == HIT_RANGE_HIGH || activeHit.action == HIT_SV || activeHit.action == HIT_HUE || activeHit.action == HIT_ALPHA)) { updateDrag(activeHit, x, y); return true; }
        for (CategoryPanel panel : panels) {
            float height = calculatePanelHeight(panel, android.os.SystemClock.uptimeMillis());
            if (containsPanelPoint(panel, x, y, height)) {
                scrollPanel = panel;

                if (unscalePanelY(panel, y) <= panel.y + LiquidBounceUiMetrics.PANEL_HEADER_HEIGHT) {
                    pressedPanel = panel;
                    longPressHandler.postDelayed(longPressRunnable, android.view.ViewConfiguration.getLongPressTimeout());
                }
                return true;
            }
        }
        return true;
    }

    public boolean onTouchMove(float x, float y) {
        if (activeTab != TopTab.CLICK_GUI) {
            if (Math.hypot(x - downX, y - downY) > 7) moved = true;
            return true;
        }
        if (layoutDraggingPanel != null) { updatePanelDrag(x, y); return true; }
        if (activeHit != null && (activeHit.action == HIT_SLIDER || activeHit.action == HIT_RANGE_LOW || activeHit.action == HIT_RANGE_HIGH || activeHit.action == HIT_SV || activeHit.action == HIT_HUE || activeHit.action == HIT_ALPHA)) { moved = true; updateDrag(activeHit, x, y); return true; }
        if (Math.hypot(x - downX, y - downY) > 7) { moved = true; cancelPanelLongPress(); }
        if (scrollPanel != null && Math.abs(y - downY) > 7) { moved = true; scrollPanel.targetScrollOffset -= (y - lastY) / panelScale(scrollPanel); lastY = y; return true; }
        return false;
    }

    public boolean onTouchUp(float x, float y) {
        cancelPanelLongPress();
        settingsScaleDragging = false;
        if (layoutDraggingPanel != null) finishPanelDrag(true);
        else if (!moved) {
            if (isEditMode && pressedPanel == null) exitEditMode();
            else handleTap(x, y);
        }
        activeHit = null;
        scrollPanel = null;
        pressedPanel = null;
        return true;
    }

    private void handleTap(float x, float y) {
        if (y >= LiquidBounceUiMetrics.TOP_TABS_Y
                && y <= LiquidBounceUiMetrics.TOP_TABS_Y + LiquidBounceUiMetrics.TOP_TABS_HEIGHT
                && x >= LiquidBounceUiMetrics.TOP_TABS_X
                && x <= LiquidBounceUiMetrics.TOP_TABS_X + LiquidBounceUiMetrics.TOP_TABS_WIDTH) {
            float local = x - LiquidBounceUiMetrics.TOP_TABS_X;
            float tabWidth = LiquidBounceUiMetrics.TOP_TABS_WIDTH / TopTab.values().length;
            int tabIndex = Math.max(0, Math.min(TopTab.values().length - 1, (int) (local / tabWidth)));
            setActiveTab(TopTab.values()[tabIndex]);
            return;
        }
        if (activeTab == TopTab.CONFIG) {
            handleConfigTap(x, y);
            return;
        }
        if (activeTab == TopTab.MUSIC) { handleMusicTap(x, y); return; }
        if (activeTab == TopTab.MATERIALS) { handleMaterialsTap(x, y); return; }
        SettingHit hit = findHit(x, y);
        if (hit != null) { handleSettingTap(hit); return; }
        for (CategoryPanel panel : panels) for (ModuleEntry module : panel.modules) for (SettingEntry setting : module.settings) closeDropdowns(setting);
        if (x >= LiquidBounceUiMetrics.SEARCH_X
                && x <= LiquidBounceUiMetrics.SEARCH_X + LiquidBounceUiMetrics.SEARCH_WIDTH
                && y >= LiquidBounceUiMetrics.SEARCH_Y
                && y <= LiquidBounceUiMetrics.SEARCH_Y + LiquidBounceUiMetrics.SEARCH_HEIGHT) {
            if (!searchQuery.isEmpty() && x > LiquidBounceUiMetrics.SEARCH_X + LiquidBounceUiMetrics.SEARCH_WIDTH - 55) setSearchQuery("");
            else listener.onSearchRequested();
            return;
        }
        if (!searchQuery.isEmpty()) {
            List<ModuleEntry> results = getSearchResults();
            float resultTop = LiquidBounceUiMetrics.SEARCH_Y + LiquidBounceUiMetrics.SEARCH_HEIGHT;
            int index = (int) ((y - resultTop - 5) / 53f);
            if (x >= LiquidBounceUiMetrics.SEARCH_X && x <= LiquidBounceUiMetrics.SEARCH_X + LiquidBounceUiMetrics.SEARCH_WIDTH
                    && index >= 0 && index < Math.min(7, results.size())) {
                dataStore.toggleModule(results.get(index).id);
                return;
            }
        }
        for (CategoryPanel panel : panels) {
            float panelHeight = calculatePanelHeight(panel, android.os.SystemClock.uptimeMillis());
            if (!containsPanelPoint(panel, x, y, panelHeight)) continue;
            float localX = unscalePanelX(panel, x);
            float localY = unscalePanelY(panel, y);
            if (localY < panel.y || localY > panel.y + panelHeight) continue;
            if (localY >= panel.y && localY <= panel.y + LiquidBounceUiMetrics.PANEL_HEADER_HEIGHT) { panel.expanded = !panel.expanded; panel.expansion.animateTo(panel.expanded ? 1 : 0, LiquidBounceUiDurations.PANEL); return; }
            float cy = panel.y + PANEL_CONTENT_TOP - panel.scrollOffset;
            for (ModuleEntry module : panel.modules) {
                if (!matches(module)) continue;
                if (localY >= cy && localY < cy + LiquidBounceUiMetrics.MODULE_ROW_HEIGHT) {
                    if (module.hasSettings() && localX > panel.x + LiquidBounceUiMetrics.PANEL_WIDTH - 55) { module.settingsExpanded = !module.settingsExpanded; module.settingsProgress.animateTo(module.settingsExpanded ? 1 : 0, LiquidBounceUiDurations.GROUP); }
                    else { dataStore.toggleModule(module.id); }
                    return;
                }
                cy += LiquidBounceUiMetrics.MODULE_ROW_HEIGHT + (module.hasSettings() ? measureSettings(module.settings, android.os.SystemClock.uptimeMillis()) * module.settingsProgress.get() : 0);
            }
        }
    }

    private void toggleHudComponent(String component) {
        boolean visible;
        if ("arraylist".equals(component)) {
            hudArrayVisible = !hudArrayVisible;
            visible = hudArrayVisible;
        } else if ("binds".equals(component)) {
            hudBindsVisible = !hudBindsVisible;
            visible = hudBindsVisible;
        } else {
            hudNotificationsVisible = !hudNotificationsVisible;
            visible = hudNotificationsVisible;
        }
        listener.onHudComponentVisibilityChanged(component, visible);
    }

    private void handleUtilityTap(float x, float y) {
        float local = x - (UTILITY_LEFT + 90);
        if (local >= 0 && local < 105) toggleHudComponent("arraylist");
        else if (local < 210) toggleHudComponent("binds");
        else if (local < 315) toggleHudComponent("notifications");
        else if (local >= 400 && local < 505) { showGrid = !showGrid; listener.onOverlaySettingChanged("grid", showGrid); }
        else if (local < 610) { snappingEnabled = !snappingEnabled; listener.onOverlaySettingChanged("snapping", snappingEnabled); }
        else if (local < 710) { darkenOverlay = !darkenOverlay; listener.onOverlaySettingChanged("darken", darkenOverlay); }
        else if (local < 790) { fontIndex = (fontIndex + 1) % Math.max(1, LiquidBounceFonts.familyCount()); listener.onFontChanged(LiquidBounceFonts.familyAt(fontIndex)); }
        invalidateState();
    }

    private void handleConfigTap(float x, float y) {
        float buttonY = CONFIG_TOP + 122;
        if (y >= buttonY && y <= buttonY + 58) {
            if (x >= CONFIG_LEFT + 70 && x < CONFIG_LEFT + 350) { listener.onConfigSave(selectedConfig); return; }
            if (x >= CONFIG_LEFT + 370 && x < CONFIG_LEFT + 590) { listener.onConfigExport(selectedConfig); return; }
            if (x >= CONFIG_LEFT + 610 && x < CONFIG_LEFT + 890) { listener.onConfigLoad(selectedConfig); return; }
            if (x >= CONFIG_LEFT + 910 && x < CONFIG_LEFT + 1130) { listener.onConfigDelete(selectedConfig); return; }
        }
        float listTop = CONFIG_TOP + 270;
        int index = (int) ((y - listTop) / 66f);
        if (x >= CONFIG_LEFT + 70 && x <= CONFIG_RIGHT - 70 && index >= 0 && index < configNames.size()) {
            selectedConfig = configNames.get(index);
            configStatus = "Selected " + selectedConfig;
            configStatusUntil = android.os.SystemClock.uptimeMillis() + 1600L;
            listener.onTopTabChanged(activeTab);
        }
    }

    private void handleMusicTap(float x, float y) {
        if (y >= CONFIG_TOP + 122 && y <= CONFIG_TOP + 184) {
            if (x >= CONFIG_LEFT + 70 && x <= CONFIG_LEFT + 950) listener.onMusicInputRequested();
            else if (x >= CONFIG_LEFT + 980 && x <= CONFIG_LEFT + 1200) listener.onMusicSearch(musicQuery);
            else if (x >= CONFIG_LEFT + 1220 && x <= CONFIG_LEFT + 1400) listener.onMusicStop();
            return;
        }
        int index = (int) ((y - (CONFIG_TOP + 220)) / 66f);
        if (index >= 0 && index < musicTracks.size()) listener.onMusicPlay(index);
    }

    private void handleMaterialsTap(float x, float y) {
        if (x >= CONFIG_RIGHT - 320 && x <= CONFIG_RIGHT - 70 && y >= CONFIG_TOP + 102 && y <= CONFIG_TOP + 164) { listener.onMaterialRefresh(); return; }
        int index = (int) ((y - (CONFIG_TOP + 200)) / 66f);
        if (index >= 0 && index < materials.size()) { materialStatus = materials.get(index).name; listener.onMaterialLoad(materialStatus); }
    }

    private void updateUiScaleFromUtility(float x) {
        float left = UTILITY_RIGHT - 178f, right = UTILITY_RIGHT - 23f;
        float p = clamp((x - left) / Math.max(1f, right - left), 0f, 1f);
        uiScale = UI_SCALE_MIN + (UI_SCALE_MAX - UI_SCALE_MIN) * p;
        listener.onUiScaleChanged(uiScale);
    }

    private void invalidateState() {
        listener.onTopTabChanged(activeTab);
    }

    private void handleSettingTap(SettingHit hit) {
        switch (hit.action) {
            case HIT_TOGGLE:
                ToggleSetting toggle = (ToggleSetting) hit.setting; toggle.value = !toggle.value; toggle.progress.animateTo(toggle.value ? 1 : 0, LiquidBounceUiDurations.TOGGLE); syncSpecialSetting(toggle); break;
            case HIT_DROPDOWN:
                DropdownSetting dropdown = (DropdownSetting) hit.setting; dropdown.open = !dropdown.open; break;
            case HIT_DROPDOWN_OPTION:
                DropdownSetting select = (DropdownSetting) hit.setting; select.value = select.options.get(hit.index); select.open = false;
                if ("lang".equals(select.id)) {
                    LiquidBounceI18n.setChinese("Chinese".equals(select.value));
                    if (listener != null) listener.onLanguageChanged();
                }
                syncSpecialSetting(select);
                break;
            case HIT_MULTI_OPTION:
                MultiSelectSetting multi = (MultiSelectSetting) hit.setting; String option = multi.options.get(hit.index); if (!multi.selected.remove(option)) multi.selected.add(option); syncSpecialSetting(multi); break;
            case HIT_BIND:
                BindSetting bind = (BindSetting) hit.setting; bind.listening = !bind.listening; break;
            case HIT_COLOR:
                ColorSetting color = (ColorSetting) hit.setting; color.expanded = !color.expanded; break;
            case HIT_GROUP:
                SettingGroup group = (SettingGroup) hit.setting; group.expanded = !group.expanded; group.expansion.animateTo(group.expanded ? 1 : 0, LiquidBounceUiDurations.GROUP); break;
            default: break;
        }
    }

    private void syncSpecialSetting(SettingEntry setting) {
        if (setting instanceof ToggleSetting) {
            boolean value = ((ToggleSetting) setting).value;
            if ("hud_array".equals(setting.id)) { hudArrayVisible = value; listener.onHudComponentVisibilityChanged("arraylist", value); }
            else if ("hud_binds".equals(setting.id)) { hudBindsVisible = value; listener.onHudComponentVisibilityChanged("binds", value); }
            else if ("hud_notifications".equals(setting.id)) { hudNotificationsVisible = value; listener.onHudComponentVisibilityChanged("notifications", value); }
            else if ("show_grid".equals(setting.id)) { showGrid = value; listener.onOverlaySettingChanged("grid", value); }
            else if ("snapping".equals(setting.id)) { snappingEnabled = value; listener.onOverlaySettingChanged("snapping", value); }
            else if ("darken".equals(setting.id)) { darkenOverlay = value; listener.onOverlaySettingChanged("darken", value); }
        } else if (setting instanceof SliderSetting && "ui_scale".equals(setting.id)) {
            uiScale = clamp(((SliderSetting) setting).value, UI_SCALE_MIN, UI_SCALE_MAX);
            listener.onUiScaleChanged(uiScale);
        } else if (setting instanceof DropdownSetting && "font".equals(setting.id)) {
            DropdownSetting font = (DropdownSetting) setting;
            for (int i = 0; i < LiquidBounceFonts.familyCount(); i++) if (LiquidBounceFonts.familyAt(i).equals(font.value)) { fontIndex = i; break; }
            listener.onFontChanged(font.value);
        }
    }

    private void updateDrag(SettingHit hit, float x, float y) {
        float p = clamp((x - hit.bounds.left) / Math.max(1, hit.bounds.width()), 0, 1);
        if (hit.action == HIT_SLIDER) { SliderSetting s = (SliderSetting) hit.setting; s.value = s.min + (s.max - s.min) * p; syncSpecialSetting(s); }
        else if (hit.action == HIT_RANGE_LOW) { RangeSetting s = (RangeSetting) hit.setting; s.low = Math.min(s.high, s.min + (s.max - s.min) * p); }
        else if (hit.action == HIT_RANGE_HIGH) { RangeSetting s = (RangeSetting) hit.setting; s.high = Math.max(s.low, s.min + (s.max - s.min) * p); }
        else {
            ColorSetting c = (ColorSetting) hit.setting;
            if (hit.action == HIT_SV) { c.saturation = p; c.brightness = 1 - clamp((y - hit.bounds.top) / hit.bounds.height(), 0, 1); }
            else if (hit.action == HIT_HUE) c.hue = 360 * clamp((y - hit.bounds.top) / hit.bounds.height(), 0, 1);
            else if (hit.action == HIT_ALPHA) c.alpha = 1 - clamp((y - hit.bounds.top) / hit.bounds.height(), 0, 1);
            c.updateColor();
        }
    }

    private void beginPanelDrag(CategoryPanel panel, float x, float y) {
        if (panel == null || panel.layoutInfo == null) return;
        isEditMode = true;
        longPressTriggered = true;
        layoutDraggingPanel = panel;
        originalPanelX = panel.x;
        originalPanelY = panel.y;
        dragOffsetX = x - panel.x;
        dragOffsetY = y - panel.y;
        updatePanelDrag(x, y);
    }

    private void updatePanelDrag(float x, float y) {
        if (layoutDraggingPanel == null || layoutDraggingPanel.layoutInfo == null) return;
        CategoryPanel panel = layoutDraggingPanel;
        panel.x = clamp(x - dragOffsetX, 0, LiquidBounceUiMetrics.CONTENT_WIDTH - LiquidBounceUiMetrics.PANEL_WIDTH);
        panel.y = clamp(y - dragOffsetY, PANEL_GRID_TOP, LiquidBounceUiMetrics.CONTENT_HEIGHT - PANEL_CONTENT_TOP);
        moved = true;
    }

    private void finishPanelDrag(boolean commit) {
        CategoryPanel panel = layoutDraggingPanel;
        if (panel == null || panel.layoutInfo == null) return;
        if (!commit) {
            panel.x = originalPanelX;
            panel.y = originalPanelY;
        }
        panel.layoutInfo.setFreePosition(panel.x, panel.y);
        layoutDraggingPanel = null;
        savePanelLayout();
    }

    public void restorePanelLayout() {
        Map<String, PanelLayoutInfo> persisted = layoutStorage.load();
        for (int index = 0; index < panels.size(); index++) {
            CategoryPanel panel = panels.get(index);
            PanelLayoutInfo saved = persisted.get(panel.id);
            int defaultSpanY = "player".equals(panel.id) ? 2 : 1;
            if (saved == null) saved = new PanelLayoutInfo(panel.id, index < PANEL_GRID_COLUMNS ? index : 0, index < PANEL_GRID_COLUMNS ? 0 : 1, 1, defaultSpanY, true);
            panel.layoutInfo = new PanelLayoutInfo(panel.id, saved.gridX, saved.gridY,
                    Math.min(PANEL_GRID_COLUMNS, Math.max(1, saved.spanX)), Math.max(1, saved.spanY), saved.visible);
            boolean legacyPosition = saved.hasFreePosition
                    && (saved.freeX < PANEL_GRID_LEFT || saved.freeY < PANEL_GRID_TOP
                    || saved.freeX + LiquidBounceUiMetrics.PANEL_WIDTH > LiquidBounceUiMetrics.CONTENT_WIDTH);
            if (saved.hasFreePosition && !legacyPosition) {
                panel.x = clamp(saved.freeX, 0, LiquidBounceUiMetrics.CONTENT_WIDTH - LiquidBounceUiMetrics.PANEL_WIDTH);
                panel.y = clamp(saved.freeY, PANEL_GRID_TOP, LiquidBounceUiMetrics.CONTENT_HEIGHT - PANEL_CONTENT_TOP);
                panel.layoutInfo.setFreePosition(panel.x, panel.y);
            } else {

                panel.x = gridToX(Math.max(0, Math.min(PANEL_GRID_COLUMNS - panel.layoutInfo.spanX, saved.gridX)));
                panel.y = gridToY(Math.max(0, saved.gridY));
                panel.layoutInfo.setFreePosition(panel.x, panel.y);
            }

            panel.expanded = true;
            panel.expansion.snapTo(1f);
        }
        savePanelLayout();
    }

    public void savePanelLayout() {
        List<PanelLayoutInfo> infos = new ArrayList<>();
        for (CategoryPanel panel : panels) if (panel.layoutInfo != null) infos.add(panel.layoutInfo.copy());
        layoutStorage.save(infos);
    }

    public void resetPanelLayout() {
        layoutStorage.clear();
        for (int index = 0; index < panels.size(); index++) {
            CategoryPanel panel = panels.get(index);
            int spanY = "player".equals(panel.id) ? 2 : 1;
            panel.layoutInfo = new PanelLayoutInfo(panel.id, -1, -1, 1, spanY, true);
            panel.x = gridToX(index < PANEL_GRID_COLUMNS ? index : 0);
            panel.y = gridToY(index < PANEL_GRID_COLUMNS ? 0 : 1);
            panel.layoutInfo.setFreePosition(panel.x, panel.y);
            panel.expanded = true;
            panel.expansion.snapTo(1f);
        }
        savePanelLayout();
    }

    public void enterEditMode() { isEditMode = true; }
    public void exitEditMode() {
        cancelPanelLongPress();
        if (layoutDraggingPanel != null) finishPanelDrag(false);
        isEditMode = false;
        savePanelLayout();
    }
    public boolean isEditMode() { return isEditMode; }
    public boolean onBackPressed() { if (!isEditMode) return false; exitEditMode(); return true; }

    private float gridToX(int gridX) { return PANEL_GRID_LEFT + gridX * (PANEL_GRID_CELL_WIDTH + PANEL_GRID_GAP_X); }
    private float gridToY(int gridY) { return PANEL_GRID_TOP + gridY * (PANEL_GRID_CELL_HEIGHT + PANEL_GRID_GAP_Y); }
    private float spanWidth(int spanX) { return spanX * PANEL_GRID_CELL_WIDTH + Math.max(0, spanX - 1) * PANEL_GRID_GAP_X; }
    private float spanHeight(int spanY) { return spanY * PANEL_GRID_CELL_HEIGHT + Math.max(0, spanY - 1) * PANEL_GRID_GAP_Y; }
    private void cancelPanelLongPress() { longPressHandler.removeCallbacks(longPressRunnable); }

    private SettingHit findHit(float x, float y) {
        if (!isInsideVisiblePanel(x, y)) return null;
        for (int i = Math.min(settingHitCount, settingHits.length) - 1; i >= 0; i--) if (settingHits[i].bounds.contains(x, y)) return settingHits[i];
        return null;
    }

    private boolean isInsideVisiblePanel(float x, float y) {
        long now = android.os.SystemClock.uptimeMillis();
        for (CategoryPanel panel : panels) {
            if (containsPanelPoint(panel, x, y, calculatePanelHeight(panel, now))) return true;
        }
        return false;
    }

    private boolean containsPanelPoint(CategoryPanel panel, float x, float y, float height) {
        float left = scalePanelX(panel, panel.x);
        float right = scalePanelX(panel, panel.x + LiquidBounceUiMetrics.PANEL_WIDTH);
        float top = scalePanelY(panel, panel.y);
        float bottom = scalePanelY(panel, panel.y + height);
        return x >= Math.min(left, right) && x <= Math.max(left, right)
                && y >= Math.min(top, bottom) && y <= Math.max(top, bottom);
    }

    public boolean isInsideUi(float x, float y) {
        if (x >= LiquidBounceUiMetrics.TOP_TABS_X && x <= LiquidBounceUiMetrics.TOP_TABS_X + LiquidBounceUiMetrics.TOP_TABS_WIDTH
                && y >= LiquidBounceUiMetrics.TOP_TABS_Y && y <= LiquidBounceUiMetrics.TOP_TABS_Y + LiquidBounceUiMetrics.TOP_TABS_HEIGHT) return true;
        if (activeTab == TopTab.CLICK_GUI) {
            if (x >= LiquidBounceUiMetrics.SEARCH_X && x <= LiquidBounceUiMetrics.SEARCH_X + LiquidBounceUiMetrics.SEARCH_WIDTH
                    && y >= LiquidBounceUiMetrics.SEARCH_Y && y <= LiquidBounceUiMetrics.SEARCH_Y + LiquidBounceUiMetrics.SEARCH_HEIGHT) return true;
            if (!searchQuery.isEmpty()) {
                int resultCount = Math.min(7, getSearchResults().size());
                if (x >= LiquidBounceUiMetrics.SEARCH_X && x <= LiquidBounceUiMetrics.SEARCH_X + LiquidBounceUiMetrics.SEARCH_WIDTH
                        && y >= LiquidBounceUiMetrics.SEARCH_Y + LiquidBounceUiMetrics.SEARCH_HEIGHT
                        && y <= LiquidBounceUiMetrics.SEARCH_Y + LiquidBounceUiMetrics.SEARCH_HEIGHT + resultCount * 53f + 10f) return true;
            }
            if (overlayDropdown != null && overlayPanel != null) {
                float top = overlayDropdownBounds.bottom + control(3);
                float bottom = top + overlayDropdown.options.size() * control(41) * panelScale(overlayPanel);
                if (x >= overlayDropdownBounds.left && x <= overlayDropdownBounds.right && y >= overlayDropdownBounds.top && y <= bottom) return true;
            }
            return isInsideVisiblePanel(x, y);
        }
        return x >= CONFIG_LEFT && x <= CONFIG_RIGHT && y >= CONFIG_TOP && y <= CONFIG_BOTTOM;
    }

    private void updateUiScale(float x) {
        float left = SETTINGS_LEFT + 34f + 22f;
        float right = SETTINGS_LEFT + 34f + (SETTINGS_RIGHT - SETTINGS_LEFT - 68f) - 130f;
        float p = clamp((x - left) / Math.max(1f, right - left), 0f, 1f);
        uiScale = UI_SCALE_MIN + (UI_SCALE_MAX - UI_SCALE_MIN) * p;
        listener.onUiScaleChanged(uiScale);
    }
    public void setUiScale(float scale) {
        uiScale = clamp(scale, UI_SCALE_MIN, UI_SCALE_MAX);
    }
    public float getUiScale() { return uiScale; }

    public void setConfigNames(List<String> names) {
        configNames.clear();
        if (names != null) configNames.addAll(names);
        if (!configNames.contains(selectedConfig) && !configNames.isEmpty()) selectedConfig = configNames.get(0);
    }
    public void setMusicQuery(String query) { musicQuery = query == null ? "" : query; }
    public String getMusicQuery() { return musicQuery; }
    public void setMusicTracks(List<MusicStore.Track> tracks) { musicTracks.clear(); if (tracks != null) musicTracks.addAll(tracks); listener.onTopTabChanged(activeTab); }
    public List<MusicStore.Track> getMusicTracks() { return Collections.unmodifiableList(musicTracks); }
    public void setMaterials(List<MaterialStore.Material> values) { materials.clear(); if (values != null) materials.addAll(values); listener.onTopTabChanged(activeTab); }
    public void showMaterialStatus(String value) { materialStatus = value == null ? "" : value; listener.onTopTabChanged(activeTab); }
    public void showMusicStatus(String value) { musicStatus = value == null ? "" : LiquidBounceI18n.t(value); listener.onTopTabChanged(activeTab); }
    public List<String> getConfigNames() { return Collections.unmodifiableList(configNames); }
    public String getSelectedConfig() { return selectedConfig; }
    public void setSelectedConfig(String name) { if (name != null && !name.trim().isEmpty()) selectedConfig = name.trim(); }
    public void showConfigStatus(String value) { configStatus = value == null ? "" : value; configStatusUntil = android.os.SystemClock.uptimeMillis() + 2200L; listener.onTopTabChanged(activeTab); }
    private String translateStatus(String value) {
        if (value == null) return "";
        String[] keys = {"Saved", "Save failed", "Exported", "Export failed", "Loaded", "Load failed", "Deleted", "Delete failed", "Selected"};
        for (String key : keys) if (value.equals(key) || value.startsWith(key + " ")) return LiquidBounceI18n.t(key) + value.substring(key.length());
        return LiquidBounceI18n.t(value);
    }

    public JSONObject exportUiState() {
        try {
            JSONObject state = new JSONObject();
            state.put("hudArrayVisible", hudArrayVisible); state.put("hudBindsVisible", hudBindsVisible); state.put("hudNotificationsVisible", hudNotificationsVisible);
            state.put("showGrid", showGrid); state.put("snappingEnabled", snappingEnabled); state.put("darkenOverlay", darkenOverlay); state.put("uiScale", uiScale); state.put("fontIndex", fontIndex);
            JSONArray panelsJson = new JSONArray();
            for (CategoryPanel panel : panels) { JSONObject item = new JSONObject(); item.put("id", panel.id); item.put("x", panel.x); item.put("y", panel.y); item.put("expanded", panel.expanded); item.put("scroll", panel.scrollOffset); panelsJson.put(item); }
            state.put("panels", panelsJson);
            return state;
        } catch (Exception ignored) { return new JSONObject(); }
    }

    public void importUiState(JSONObject state) {
        if (state == null) return;
        hudArrayVisible = state.optBoolean("hudArrayVisible", hudArrayVisible); hudBindsVisible = state.optBoolean("hudBindsVisible", hudBindsVisible); hudNotificationsVisible = state.optBoolean("hudNotificationsVisible", hudNotificationsVisible);
        showGrid = state.optBoolean("showGrid", showGrid); snappingEnabled = state.optBoolean("snappingEnabled", snappingEnabled); darkenOverlay = state.optBoolean("darkenOverlay", darkenOverlay); uiScale = clamp((float) state.optDouble("uiScale", uiScale), UI_SCALE_MIN, UI_SCALE_MAX); fontIndex = state.optInt("fontIndex", fontIndex);
        JSONArray panelsJson = state.optJSONArray("panels");
        if (panelsJson != null) for (int i = 0; i < panelsJson.length(); i++) { JSONObject item = panelsJson.optJSONObject(i); if (item == null) continue; CategoryPanel panel = dataStore.findCategory(item.optString("id", "")); if (panel == null) continue; panel.x = (float) item.optDouble("x", panel.x); panel.y = (float) item.optDouble("y", panel.y); panel.expanded = item.optBoolean("expanded", panel.expanded); panel.expansion.snapTo(panel.expanded ? 1f : 0f); panel.scrollOffset = panel.targetScrollOffset = Math.max(0f, (float) item.optDouble("scroll", panel.scrollOffset)); }
        listener.onUiScaleChanged(uiScale); listener.onFontChanged(LiquidBounceFonts.familyAt(fontIndex % Math.max(1, LiquidBounceFonts.familyCount()))); listener.onTopTabChanged(activeTab);
    }
    private void closeDropdowns(SettingEntry setting) { if (setting instanceof DropdownSetting) ((DropdownSetting) setting).open = false; else if (setting instanceof SettingGroup) for (SettingEntry child : ((SettingGroup) setting).children) closeDropdowns(child); }

    public void setActiveTab(TopTab tab) {
        if (tab == null || tab == activeTab) return;
        previousTab = activeTab;
        activeTab = tab;
        tabTransition.snapTo(0f);
        tabTransition.animateTo(1f, 220L);
        listener.onTopTabChanged(tab);
    }
    public TopTab getActiveTab() { return activeTab; }
        public void setSearchQuery(String query) { searchQuery = query == null ? "" : query.trim(); searchSelectedIndex = 0; }
    public String getSearchQuery() { return searchQuery; }
    public void setCategoryExpanded(String name, boolean expanded) { CategoryPanel p = dataStore.findCategory(name); if (p != null) { p.expanded = expanded; p.expansion.animateTo(expanded ? 1 : 0, LiquidBounceUiDurations.PANEL); } }
    public void setModuleEnabled(String name, boolean enabled) { dataStore.setModuleEnabled(name, enabled); }
    public void setModuleSuffix(String name, String suffix) { dataStore.setModuleSuffix(name, suffix); }
    public void setModuleKeyBind(String name, String bind) { dataStore.setModuleKeyBind(name, bind); }
    public void setModuleSettingsExpanded(String category, String moduleName, boolean expanded) { ModuleEntry m = dataStore.findModule(moduleName); if (m != null && m.categoryId.equals(dataStore.findCategory(category) == null ? category : dataStore.findCategory(category).id) && m.hasSettings()) { m.settingsExpanded = expanded; m.settingsProgress.animateTo(expanded ? 1 : 0, LiquidBounceUiDurations.GROUP); } }
    public void setPanelScroll(String category, float offset) {
        CategoryPanel panel = dataStore.findCategory(category);
        if (panel != null) panel.scrollOffset = panel.targetScrollOffset = clamp(Math.max(0, offset), 0, calculateMaxScroll(panel, android.os.SystemClock.uptimeMillis()));
    }
    public void setDropdownOpen(String module, String settingId, boolean open) { SettingEntry setting = findSetting(dataStore.findModule(module), settingId); if (setting instanceof DropdownSetting) ((DropdownSetting) setting).open = open; }
    public void setColorExpanded(String module, String settingId, boolean expanded) { SettingEntry setting = findSetting(dataStore.findModule(module), settingId); if (setting instanceof ColorSetting) ((ColorSetting) setting).expanded = expanded; }

    public ModuleEntry onKeyPressed(String keyName) {
        for (CategoryPanel panel : panels) for (ModuleEntry module : panel.modules) {
            BindSetting listening = findListeningBind(module.settings);
            if (listening != null) {
                listening.value = keyName == null || keyName.isEmpty() ? "None" : keyName;
                listening.listening = false;
                dataStore.setModuleKeyBind(module.id, listening.value);
                return module;
            }
        }
        return null;
    }

    private SettingEntry findSetting(ModuleEntry module, String id) { if (module == null) return null; return findSetting(module.settings, id); }
    private SettingEntry findSetting(List<SettingEntry> list, String id) { for (SettingEntry s : list) { if (s.id.equals(id)) return s; if (s instanceof SettingGroup) { SettingEntry child = findSetting(((SettingGroup) s).children, id); if (child != null) return child; } } return null; }
    private BindSetting findListeningBind(List<SettingEntry> list) { for (SettingEntry setting : list) { if (setting instanceof BindSetting && ((BindSetting) setting).listening) return (BindSetting) setting; if (setting instanceof SettingGroup) { BindSetting child = findListeningBind(((SettingGroup) setting).children); if (child != null) return child; } } return null; }
    public boolean hasActiveAnimations(long now) { if (tabTransition.isRunning(now) || now < configStatusUntil) return true; for (CategoryPanel p : panels) { if (p.expansion.isRunning(now) || Math.abs(p.targetScrollOffset - p.scrollOffset) > .5f) return true; for (ModuleEntry m : p.modules) { if (m.enabledProgress.isRunning(now) || m.settingsProgress.isRunning(now)) return true; if (settingsAnimating(m.settings, now)) return true; } } return false; }
    private boolean settingsAnimating(List<SettingEntry> list, long now) { for (SettingEntry s : list) { if (s instanceof ToggleSetting && ((ToggleSetting) s).progress.isRunning(now)) return true; if (s instanceof SettingGroup && (((SettingGroup) s).expansion.isRunning(now) || settingsAnimating(((SettingGroup) s).children, now))) return true; } return false; }

    private void drawScrollbar(Canvas canvas, CategoryPanel panel, float bodyHeight, float bodyMax) { float ratio = bodyMax / panel.contentHeight, thumb = Math.max(40, bodyHeight * ratio), maxScroll = panel.contentHeight - bodyMax, travel = Math.max(1, bodyHeight - thumb), top = panel.y + 60 + travel * panel.scrollOffset / Math.max(1, maxScroll); setFill(LiquidBounceUiColors.ACCENT); canvas.drawRoundRect(panel.x + 371, top, panel.x + 374, top + thumb, 2, 2, fill); }
    private void drawChevron(Canvas canvas, float cx, float cy, boolean down) { float s = control(1); stroke.setColor(Color.WHITE); stroke.setStrokeWidth(control(2)); if (down) { canvas.drawLine(cx - 5 * s, cy - 3 * s, cx, cy + 2 * s, stroke); canvas.drawLine(cx, cy + 2 * s, cx + 5 * s, cy - 3 * s, stroke); } else { canvas.drawLine(cx - 3 * s, cy - 5 * s, cx + 2 * s, cy, stroke); canvas.drawLine(cx + 2 * s, cy, cx - 3 * s, cy + 5 * s, stroke); } }
    private void drawChecker(Canvas canvas, RectF b, float size) { for (float yy = b.top; yy < b.bottom; yy += size) for (float xx = b.left; xx < b.right; xx += size) { int row = (int) ((yy - b.top) / size), col = (int) ((xx - b.left) / size); setFill(((row + col) & 1) == 0 ? 0xFFB8B8B8 : 0xFF6D6D6D); canvas.drawRect(xx, yy, Math.min(xx + size, b.right), Math.min(yy + size, b.bottom), fill); } }
    private void drawCategoryIcon(Canvas canvas, String name, float cx, float cy) { stroke.setColor(Color.WHITE); stroke.setStrokeWidth(3); stroke.setStrokeCap(Paint.Cap.ROUND); path.reset(); if ("Render".equals(name)) { rect.set(cx - 10, cy - 7, cx + 10, cy + 7); canvas.drawOval(rect, stroke); setFill(Color.WHITE); canvas.drawCircle(cx, cy, 3, fill); } else if ("Combat".equals(name)) { canvas.drawLine(cx - 9, cy - 9, cx + 9, cy + 9, stroke); canvas.drawLine(cx + 9, cy - 9, cx - 9, cy + 9, stroke); } else if ("Player".equals(name)) { setFill(Color.WHITE); canvas.drawCircle(cx, cy - 7, 6, fill); canvas.drawRoundRect(cx - 9, cy + 1, cx + 9, cy + 12, 4, 4, fill); } else if ("Movement".equals(name)) { path.moveTo(cx - 10, cy + 8); path.lineTo(cx - 2, cy); path.lineTo(cx + 1, cy - 8); path.lineTo(cx + 9, cy - 10); path.moveTo(cx, cy - 2); path.lineTo(cx + 10, cy + 7); path.moveTo(cx - 2, cy + 1); path.lineTo(cx - 7, cy + 12); canvas.drawPath(path, stroke); } else if ("World".equals(name)) { canvas.drawCircle(cx, cy, 10, stroke); canvas.drawLine(cx, cy - 10, cx, cy + 10, stroke); canvas.drawOval(cx - 6, cy - 10, cx + 6, cy + 10, stroke); } else { canvas.drawCircle(cx, cy, 8, stroke); canvas.drawCircle(cx + 5, cy - 7, 3, stroke); } stroke.setStrokeCap(Paint.Cap.BUTT); }
    private boolean matches(ModuleEntry module) {
        if (searchQuery.isEmpty()) return true;
        String query = searchQuery.toLowerCase(Locale.ROOT).replace(" ", "");
        if (module.name.toLowerCase(Locale.ROOT).replace(" ", "").contains(query)) return true;
        for (String alias : module.aliases) if (alias.toLowerCase(Locale.ROOT).replace(" ", "").contains(query)) return true;
        return false;
    }
    private boolean matches(String name) {
        if (searchQuery.isEmpty()) return true;
        return name != null && name.toLowerCase(Locale.ROOT).contains(searchQuery.toLowerCase(Locale.ROOT));
    }
    private static String formatValue(float value) { if (Math.abs(value - Math.round(value)) < .0001f) return Integer.toString(Math.round(value)); return String.format(Locale.US, value < 1 ? "%.3f" : "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", ""); }
    private static String toRgbaHex(int color) { return String.format(Locale.US, "#%02X%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color)); }
    private float control(float referenceSize) { return typography.control(referenceSize); }
    private static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    private void setFill(int color) { fill.setShader(null); fill.setStyle(Paint.Style.FILL); fill.setColor(color); }
    private void setText(int color, float size, Typeface face) { text.setColor(color); text.setTextSize(typography.size(size)); text.setTypeface(face); }
    private void drawCentered(Canvas c, String s, float x, float y, float w, float h, Paint p) { Paint.FontMetrics fm = p.getFontMetrics(); c.drawText(s, x + (w - p.measureText(s)) * .5f, y + (h - (fm.descent - fm.ascent)) * .5f - fm.ascent, p); }
    private void drawBaseline(Canvas c, String s, float x, float y, float h, Paint p) { Paint.FontMetrics fm = p.getFontMetrics(); c.drawText(s, x, y + (h - (fm.descent - fm.ascent)) * .5f - fm.ascent, p); }
    private void drawRightBaseline(Canvas c, String s, float right, float y, float h, Paint p) { drawBaseline(c, s, right - p.measureText(s), y, h, p); }
    private void drawDebugRect(Canvas c, float x, float y, float w, float h) { stroke.setColor(0xCCFF3CAC); stroke.setStrokeWidth(2); c.drawRect(x, y, x + w, y + h, stroke); setFill(0xFFFF3CAC); c.drawCircle(x + w * .5f, y + h * .5f, 4, fill); }
}
