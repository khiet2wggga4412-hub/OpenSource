package com.liquid.org.ui.overlay;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.liquid.org.ui.overlay.LiquidBounceModels.BindSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.ColorSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.DropdownSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.ModuleEntry;
import com.liquid.org.ui.overlay.LiquidBounceModels.MultiSelectSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.RangeSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.SettingEntry;
import com.liquid.org.ui.overlay.LiquidBounceModels.SettingGroup;
import com.liquid.org.ui.overlay.LiquidBounceModels.SliderSetting;
import com.liquid.org.ui.overlay.LiquidBounceModels.ToggleSetting;

public final class ConfigStore {
    private final File directory;
    private final File exportDirectory;

    public ConfigStore(Context context) {
        File root = new File(context.getApplicationContext().getFilesDir(), "liquidpe");
        directory = new File(root, "configs");
        exportDirectory = new File(root, "exports");
        directory.mkdirs();
        exportDirectory.mkdirs();
    }

    public List<String> list() {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return new ArrayList<>();
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        List<String> result = new ArrayList<>();
        for (File file : files) result.add(file.getName().substring(0, file.getName().length() - 5));
        return result;
    }

    public boolean save(String name, LiquidBounceDataStore dataStore, JSONObject uiState) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("name", normalize(name));
            root.put("updatedAt", System.currentTimeMillis());
            root.put("modules", serializeModules(dataStore));
            if (uiState != null) root.put("ui", uiState);
            write(fileFor(name), root.toString(2));
            return true;
        } catch (Exception ignored) { return false; }
    }

    public JSONObject load(String name, LiquidBounceDataStore dataStore) {
        try {
            JSONObject root = new JSONObject(read(fileFor(name)));
            JSONArray modules = root.optJSONArray("modules");
            if (modules != null) applyModules(modules, dataStore);
            return root.optJSONObject("ui");
        } catch (Exception ignored) { return null; }
    }

    public boolean delete(String name) { return fileFor(name).delete(); }

    public File export(String name) {
        try {
            File source = fileFor(name);
            if (!source.isFile()) return null;
            File target = new File(exportDirectory, normalize(name) + "-export.json");
            copy(source, target);
            return target;
        } catch (Exception ignored) { return null; }
    }

    public File fileFor(String name) { return new File(directory, normalize(name) + ".json"); }

    private JSONArray serializeModules(LiquidBounceDataStore dataStore) throws Exception {
        JSONArray result = new JSONArray();
        for (ModuleEntry module : dataStore.getModules()) {
            JSONObject item = new JSONObject();
            item.put("id", module.id);
            item.put("enabled", module.enabled);
            item.put("suffix", module.arrayListSuffix);
            item.put("keyBind", module.keyBind);
            item.put("showInArrayList", module.showInArrayList);
            item.put("showInBinds", module.showInBinds);
            item.put("settingsExpanded", module.settingsExpanded);
            item.put("settings", serializeSettings(module.settings));
            result.put(item);
        }
        return result;
    }

    private JSONArray serializeSettings(List<SettingEntry> settings) throws Exception {
        JSONArray result = new JSONArray();
        for (SettingEntry setting : settings) {
            JSONObject item = new JSONObject();
            item.put("id", setting.id);
            item.put("type", setting.type.name());
            if (setting instanceof ToggleSetting) item.put("value", ((ToggleSetting) setting).value);
            else if (setting instanceof SliderSetting) item.put("value", ((SliderSetting) setting).value);
            else if (setting instanceof RangeSetting) { item.put("low", ((RangeSetting) setting).low); item.put("high", ((RangeSetting) setting).high); }
            else if (setting instanceof DropdownSetting) item.put("value", ((DropdownSetting) setting).value);
            else if (setting instanceof MultiSelectSetting) { JSONArray values = new JSONArray(); for (String value : ((MultiSelectSetting) setting).selected) values.put(value); item.put("selected", values); }
            else if (setting instanceof BindSetting) item.put("value", ((BindSetting) setting).value);
            else if (setting instanceof ColorSetting) item.put("color", ((ColorSetting) setting).color);
            else if (setting instanceof SettingGroup) { SettingGroup group = (SettingGroup) setting; item.put("expanded", group.expanded); item.put("children", serializeSettings(group.children)); }
            result.put(item);
        }
        return result;
    }

    private void applyModules(JSONArray modules, LiquidBounceDataStore dataStore) throws Exception {
        for (int i = 0; i < modules.length(); i++) {
            JSONObject item = modules.optJSONObject(i); if (item == null) continue;
            ModuleEntry module = dataStore.findModule(item.optString("id", "")); if (module == null) continue;
            dataStore.setModuleEnabled(module.id, item.optBoolean("enabled", module.enabled));
            dataStore.setModuleSuffix(module.id, item.optString("suffix", module.arrayListSuffix));
            dataStore.setModuleKeyBind(module.id, item.optString("keyBind", module.keyBind));
            dataStore.setShowInArrayList(module.id, item.optBoolean("showInArrayList", module.showInArrayList));
            dataStore.setShowInBinds(module.id, item.optBoolean("showInBinds", module.showInBinds));
            module.settingsExpanded = item.optBoolean("settingsExpanded", module.settingsExpanded);
            applySettings(module.settings, item.optJSONArray("settings"));
        }
    }

    private void applySettings(List<SettingEntry> settings, JSONArray values) throws Exception {
        if (values == null) return;
        for (int i = 0; i < values.length(); i++) {
            JSONObject item = values.optJSONObject(i); if (item == null) continue;
            SettingEntry setting = null;
            for (SettingEntry candidate : settings) if (candidate.id.equals(item.optString("id"))) { setting = candidate; break; }
            if (setting == null) continue;
            if (setting instanceof ToggleSetting) { ToggleSetting value = (ToggleSetting) setting; value.value = item.optBoolean("value", value.value); value.progress.snapTo(value.value ? 1f : 0f); }
            else if (setting instanceof SliderSetting) ((SliderSetting) setting).value = (float) item.optDouble("value", ((SliderSetting) setting).value);
            else if (setting instanceof RangeSetting) { RangeSetting range = (RangeSetting) setting; range.low = (float) item.optDouble("low", range.low); range.high = (float) item.optDouble("high", range.high); }
            else if (setting instanceof DropdownSetting) ((DropdownSetting) setting).value = item.optString("value", ((DropdownSetting) setting).value);
            else if (setting instanceof MultiSelectSetting) { MultiSelectSetting multi = (MultiSelectSetting) setting; multi.selected.clear(); JSONArray selected = item.optJSONArray("selected"); if (selected != null) for (int j = 0; j < selected.length(); j++) multi.selected.add(selected.optString(j)); }
            else if (setting instanceof BindSetting) ((BindSetting) setting).value = item.optString("value", ((BindSetting) setting).value);
            else if (setting instanceof ColorSetting) { ColorSetting color = (ColorSetting) setting; color.color = item.optInt("color", color.color); }
            else if (setting instanceof SettingGroup) { SettingGroup group = (SettingGroup) setting; group.expanded = item.optBoolean("expanded", group.expanded); group.expansion.snapTo(group.expanded ? 1f : 0f); applySettings(group.children, item.optJSONArray("children")); }
        }
    }

    private static String normalize(String raw) {
        String value = raw == null ? "default" : raw.trim().replaceAll("[^a-zA-Z0-9._-]+", "_");
        if (value.isEmpty()) value = "default";
        return value.length() > 48 ? value.substring(0, 48) : value;
    }

    private static String read(File file) throws Exception {
        FileInputStream input = new FileInputStream(file); byte[] bytes = new byte[(int) file.length()]; int offset = 0, count;
        while (offset < bytes.length && (count = input.read(bytes, offset, bytes.length - offset)) > 0) offset += count;
        input.close(); return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void write(File file, String value) throws Exception {
        file.getParentFile().mkdirs(); FileOutputStream output = new FileOutputStream(file); output.write(value.getBytes(StandardCharsets.UTF_8)); output.close();
    }

    private static void copy(File source, File target) throws Exception {
        FileInputStream input = new FileInputStream(source); FileOutputStream output = new FileOutputStream(target); byte[] buffer = new byte[8192]; int count;
        while ((count = input.read(buffer)) >= 0) { if (count > 0) output.write(buffer, 0, count); }
        input.close(); output.close();
    }
}
