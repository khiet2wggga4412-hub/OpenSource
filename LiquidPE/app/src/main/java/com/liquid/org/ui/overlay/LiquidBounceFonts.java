
package com.liquid.org.ui.overlay;

import android.content.Context;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

import com.liquid.org.R;

public final class LiquidBounceFonts {
    private static Typeface regular;
    private static Typeface medium;
    private static Typeface bold;
    private static String family = "Inter";

    private static Typeface bindsRegular;
    private static Typeface bindsMedium;
    private static Typeface bindsBold;

    private LiquidBounceFonts() {}

    public static void initialize(Context context) {
        if (regular != null) return;
        Typeface bundled = ResourcesCompat.getFont(context, R.font.inter_regular);
        if (bundled == null) bundled = Typeface.DEFAULT;
        regular = Typeface.create(bundled, Typeface.NORMAL);
        Typeface mediumBundled = ResourcesCompat.getFont(context, R.font.inter_medium);
        Typeface boldBundled = ResourcesCompat.getFont(context, R.font.inter_semibold);
        medium = Typeface.create(mediumBundled == null ? bundled : mediumBundled, Typeface.NORMAL);
        bold = Typeface.create(boldBundled == null ? bundled : boldBundled, Typeface.NORMAL);

        Typeface bindsBundled = ResourcesCompat.getFont(context, R.font.inter_regular);
        if (bindsBundled == null) bindsBundled = Typeface.DEFAULT;
        bindsRegular = Typeface.create(bindsBundled, Typeface.NORMAL);
        Typeface bindsMediumBundled = ResourcesCompat.getFont(context, R.font.inter_medium);
        Typeface bindsBoldBundled = ResourcesCompat.getFont(context, R.font.inter_semibold);
        bindsMedium = Typeface.create(bindsMediumBundled == null ? bindsBundled : bindsMediumBundled, Typeface.NORMAL);
        bindsBold = Typeface.create(bindsBoldBundled == null ? bindsBundled : bindsBoldBundled, Typeface.NORMAL);
    }

    public static Typeface regular() { return regular != null ? regular : Typeface.DEFAULT; }
    public static Typeface medium() { return medium != null ? medium : Typeface.DEFAULT_BOLD; }
    public static Typeface bold() { return bold != null ? bold : Typeface.DEFAULT_BOLD; }

    public static Typeface bindsRegular() { return bindsRegular != null ? bindsRegular : Typeface.DEFAULT; }
    public static Typeface bindsMedium() { return bindsMedium != null ? bindsMedium : Typeface.DEFAULT_BOLD; }
    public static Typeface bindsBold() { return bindsBold != null ? bindsBold : Typeface.DEFAULT_BOLD; }
    public static String familyName() { return family; }
    public static int familyCount() { return 3; }
    public static String familyAt(int index) { return index == 1 ? "Roboto" : index == 2 ? "Minecraft" : "Inter"; }
    public static void setFamily(Context context, String name) {
        if (name == null) return;
        family = name;
        Typeface base;
        if ("Roboto".equals(name)) {
            Typeface roboto = ResourcesCompat.getFont(context, R.font.roboto_regular);
            base = roboto == null ? Typeface.create("sans", Typeface.NORMAL) : roboto;
        }
        else if ("Minecraft".equals(name)) {
            Typeface minecraft = ResourcesCompat.getFont(context, R.font.minecraft);
            base = minecraft == null ? Typeface.DEFAULT : minecraft;
        } else base = ResourcesCompat.getFont(context, R.font.inter_regular);
        if (base == null) base = Typeface.DEFAULT;
        regular = Typeface.create(base, Typeface.NORMAL);
        medium = Typeface.create(base, Typeface.BOLD);
        bold = Typeface.create(base, Typeface.BOLD);
        bindsRegular = regular; bindsMedium = medium; bindsBold = bold;
    }
}
