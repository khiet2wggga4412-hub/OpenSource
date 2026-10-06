
package com.liquid.org.ui.overlay;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

public final class ResponsiveTypography {
    private static final float MIN_SCALE = .82f;
    private static final float MAX_SCALE = 1.25f;

    private float scale = 1f;
    private float controlScale = 1f;

    public ResponsiveTypography(Context context) {
        update(context, 0, 0);
    }

    public void update(Context context, int widthPx, int heightPx) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        Configuration configuration = context.getResources().getConfiguration();
        float density = Math.max(1f, metrics.density);
        float shortestWidthDp = Math.min(widthPx, heightPx) / density;
        if (shortestWidthDp <= 0f) shortestWidthDp = configuration.smallestScreenWidthDp;

        float deviceScale;
        if (shortestWidthDp <= 360f) {
            deviceScale = .90f;
        } else if (shortestWidthDp < 600f) {
            deviceScale = .90f + (shortestWidthDp - 360f) / 240f * .10f;
        } else {
            deviceScale = Math.min(1.12f, 1f + (shortestWidthDp - 600f) / 1200f * .12f);
        }

        controlScale = clamp(deviceScale, .90f, 1.12f);
        float systemFontScale = clamp(configuration.fontScale, .85f, 1.30f);
        scale = clamp(deviceScale * systemFontScale, MIN_SCALE, MAX_SCALE);
    }

    public float size(float referenceSize) {
        return referenceSize * scale;
    }

    public float getScale() {
        return scale;
    }

    public float control(float referenceSize) {
        return referenceSize * controlScale;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
