package dev.liquid.clickgui.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class FloatingButtonConfig(
    initialSizeDp: Int = DEFAULT_SIZE_DP,
) {
    var buttonSizeDp by mutableIntStateOf(coerceSize(initialSizeDp))
        private set

    fun setSize(sizeDp: Int) {
        buttonSizeDp = coerceSize(sizeDp)
    }

    companion object {
        const val MIN_SIZE_DP = 32
        const val DEFAULT_SIZE_DP = 40
        const val MAX_SIZE_DP = 52

        fun coerceSize(sizeDp: Int): Int = sizeDp.coerceIn(MIN_SIZE_DP, MAX_SIZE_DP)
    }
}

@Composable
fun rememberFloatingButtonConfig(
    initialSizeDp: Int = FloatingButtonConfig.DEFAULT_SIZE_DP,
): FloatingButtonConfig = remember(initialSizeDp) {
    FloatingButtonConfig(initialSizeDp)
}
