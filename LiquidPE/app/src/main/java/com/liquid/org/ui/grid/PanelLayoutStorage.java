
package com.liquid.org.ui.grid;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PanelLayoutStorage {

    private static final String PREFS_NAME = "draggable_panel_layouts_v5";
    private static final String KEY_LAYOUT = "layout";
    private static final int VERSION = 3;
    private final SharedPreferences preferences;

    public PanelLayoutStorage(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void save(List<PanelLayoutInfo> infos) {
        JSONArray panels = new JSONArray();
        if (infos != null) for (PanelLayoutInfo info : infos) {
            if (info == null || info.panelId.isEmpty()) continue;
            JSONObject item = new JSONObject();
            try {
                item.put("id", info.panelId);
                item.put("x", info.gridX);
                item.put("y", info.gridY);
                item.put("spanX", Math.max(1, info.spanX));
                item.put("spanY", Math.max(1, info.spanY));
                item.put("visible", info.visible);
                item.put("freePosition", info.hasFreePosition);
                if (info.hasFreePosition) {
                    item.put("freeX", info.freeX);
                    item.put("freeY", info.freeY);
                }
                panels.put(item);
            } catch (JSONException ignored) {

            }
        }
        JSONObject root = new JSONObject();
        try {
            root.put("version", VERSION);
            root.put("panels", panels);
            preferences.edit().putString(KEY_LAYOUT, root.toString()).apply();
        } catch (JSONException ignored) {

        }
    }

    public Map<String, PanelLayoutInfo> load() {
        Map<String, PanelLayoutInfo> result = new LinkedHashMap<>();
        String raw = preferences.getString(KEY_LAYOUT, null);
        if (raw == null || raw.isEmpty()) return result;
        try {
            JSONArray panels = new JSONObject(raw).optJSONArray("panels");
            if (panels == null) return result;
            for (int i = 0; i < panels.length(); i++) {
                JSONObject item = panels.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "").trim();
                if (id.isEmpty() || result.containsKey(id)) continue;
                int spanX = Math.max(1, item.optInt("spanX", 1));
                int spanY = Math.max(1, item.optInt("spanY", 1));
                int x = item.optInt("x", -1);
                int y = item.optInt("y", -1);
                PanelLayoutInfo info = new PanelLayoutInfo(id, x, y, spanX, spanY, item.optBoolean("visible", true));
                if (item.optBoolean("freePosition", false)) {
                    info.setFreePosition((float) item.optDouble("freeX", 0), (float) item.optDouble("freeY", 0));
                }
                result.put(id, info);
            }
        } catch (JSONException ignored) {

        }
        return result;
    }

    public void clear() { preferences.edit().remove(KEY_LAYOUT).apply(); }

    public List<PanelLayoutInfo> loadList() { return new ArrayList<>(load().values()); }
}
