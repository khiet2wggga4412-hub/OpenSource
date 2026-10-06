
package com.liquid.org.ui.overlay;

import android.graphics.Color;

public final class LiquidBounceUiColors {
    private LiquidBounceUiColors() {}

    public static final int ACCENT = 0xFF4677FF;
    public static final int ACCENT_SOFT = 0xFF6C92FF;
    public static final int ACCENT_DEEP = 0xFF1C2F66;

    public static final int PANEL = 0xCC000000;
    public static final int PANEL_DEEP = 0xE6000000;
    public static final int PANEL_HIGHLIGHT = 0x26FFFFFF;
    public static final int PANEL_ALT = 0xB8000000;
    public static final int HUD_PANEL = 0xAD000000;
    public static final int TEXT_PRIMARY = 0xFFFDFDFD;
    public static final int TEXT_NORMAL = 0xFFFFFFFF;
    public static final int TEXT_MUTED = 0xFFD3D3D3;
    public static final int TEXT_DIM = 0xFFA0A0A0;
    public static final int SUCCESS = 0xFF4DAC68;
    public static final int ERROR = 0xFFFC4130;
    public static final int TRACK = 0xFF303237;
    public static final int TOGGLE_OFF = 0xFF55575E;

    public static int blend(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return Color.argb(
                Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t),
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t));
    }
}
