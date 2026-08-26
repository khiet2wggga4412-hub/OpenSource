package com.liquid.org.ui.overlay;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Local material-pack manager. It scans app-private .apk/.zip packs and can add them to AssetManager. */
public final class MaterialStore {
    public static final class Material {
        public final String name;
        public final File file;
        Material(String name, File file) { this.name = name; this.file = file; }
    }

    private final Context context;
    private final File directory;
    private String selected;

    public MaterialStore(Context context) {
        this.context = context.getApplicationContext();
        directory = new File(this.context.getFilesDir(), "materials");
        directory.mkdirs();
        selected = this.context.getSharedPreferences("materials", Context.MODE_PRIVATE).getString("selected", "");
    }

    public File getDirectory() { directory.mkdirs(); return directory; }

    public List<Material> list() {
        File[] files = directory.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".apk") || lower.endsWith(".zip");
        });
        if (files == null) return new ArrayList<>();
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        List<Material> result = new ArrayList<>();
        for (File file : files) result.add(new Material(stripExtension(file.getName()), file));
        return result;
    }

    public boolean load(String name) {
        for (Material material : list()) if (material.name.equals(name)) {
            boolean loaded = addAssetPath(context.getAssets(), material.file.getAbsolutePath());
            if (loaded) { selected = material.name; context.getSharedPreferences("materials", Context.MODE_PRIVATE).edit().putString("selected", selected).apply(); }
            return loaded;
        }
        return false;
    }

    public String getSelected() { return selected; }

    private static boolean addAssetPath(AssetManager assets, String path) {
        try {
            Method method = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            method.setAccessible(true);
            return ((Integer) method.invoke(assets, path)) != 0;
        } catch (Exception ignored) { return false; }
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.'); return dot > 0 ? name.substring(0, dot) : name;
    }
}
