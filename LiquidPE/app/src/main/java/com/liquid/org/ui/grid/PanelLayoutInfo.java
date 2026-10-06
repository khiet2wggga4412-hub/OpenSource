
package com.liquid.org.ui.grid;

public final class PanelLayoutInfo {
    public final String panelId;
    public int gridX;
    public int gridY;
    public int spanX;
    public int spanY;
    public boolean visible;
    public boolean hasFreePosition;
    public float freeX;
    public float freeY;

    public PanelLayoutInfo(String panelId, int gridX, int gridY, int spanX, int spanY) {
        this(panelId, gridX, gridY, spanX, spanY, true);
    }

    public PanelLayoutInfo(String panelId, int gridX, int gridY, int spanX, int spanY, boolean visible) {
        this.panelId = panelId == null ? "" : panelId;
        this.gridX = gridX;
        this.gridY = gridY;
        this.spanX = Math.max(1, spanX);
        this.spanY = Math.max(1, spanY);
        this.visible = visible;
    }

    public PanelLayoutInfo copy() {
        PanelLayoutInfo copy = new PanelLayoutInfo(panelId, gridX, gridY, spanX, spanY, visible);
        copy.hasFreePosition = hasFreePosition;
        copy.freeX = freeX;
        copy.freeY = freeY;
        return copy;
    }

    public void setFreePosition(float x, float y) {
        hasFreePosition = true;
        freeX = x;
        freeY = y;
    }
}
