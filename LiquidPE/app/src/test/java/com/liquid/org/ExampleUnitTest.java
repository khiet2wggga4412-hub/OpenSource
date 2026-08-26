/*
 * LiquidPE 开源作者
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.liquid.org;

import com.liquid.org.ui.overlay.LiquidBounceUiMetrics;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void enlargedTopControlsStayCenteredAndClearOfPanels() {
        assertEquals(LiquidBounceUiMetrics.CONTENT_WIDTH * .5f,
                LiquidBounceUiMetrics.TOP_TABS_X + LiquidBounceUiMetrics.TOP_TABS_WIDTH * .5f, 0.001f);
        assertEquals(LiquidBounceUiMetrics.CONTENT_WIDTH * .5f,
                LiquidBounceUiMetrics.SEARCH_X + LiquidBounceUiMetrics.SEARCH_WIDTH * .5f, 0.001f);
        assertTrue(LiquidBounceUiMetrics.SEARCH_Y + LiquidBounceUiMetrics.SEARCH_HEIGHT
                < LiquidBounceUiMetrics.PANEL_Y);
        assertTrue(LiquidBounceUiMetrics.TOP_TABS_WIDTH / 4f >= 120f);
    }
}
