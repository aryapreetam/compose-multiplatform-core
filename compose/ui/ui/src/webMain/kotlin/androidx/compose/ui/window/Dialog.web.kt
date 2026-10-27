/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

private val DefaultScrimColor = Color(0f, 0f, 0f, 0.6f)

@Immutable
actual class DialogProperties constructor(
    actual val dismissOnBackPress: Boolean = true,
    actual val dismissOnClickOutside: Boolean = true,
    actual val usePlatformDefaultWidth: Boolean = true,
    val usePlatformInsets: Boolean = true,
    val useSoftwareKeyboardInset: Boolean = true,
    val scrimColor: Color = DefaultScrimColor,
) {
    actual constructor(
        dismissOnBackPress: Boolean,
        dismissOnClickOutside: Boolean,
        usePlatformDefaultWidth: Boolean,
    ) : this(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
        usePlatformInsets = true,
        useSoftwareKeyboardInset = true,
        scrimColor = DefaultScrimColor,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DialogProperties) return false
        return dismissOnBackPress == other.dismissOnBackPress &&
            dismissOnClickOutside == other.dismissOnClickOutside &&
            usePlatformDefaultWidth == other.usePlatformDefaultWidth &&
            usePlatformInsets == other.usePlatformInsets &&
            useSoftwareKeyboardInset == other.useSoftwareKeyboardInset &&
            scrimColor == other.scrimColor
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + usePlatformDefaultWidth.hashCode()
        result = 31 * result + usePlatformInsets.hashCode()
        result = 31 * result + useSoftwareKeyboardInset.hashCode()
        result = 31 * result + scrimColor.hashCode()
        return result
    }
}

internal object DialogPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = ((windowSize.width - popupContentSize.width) / 2).coerceAtLeast(0)
        val y = ((windowSize.height - popupContentSize.height) / 2).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

@Composable
actual fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    content: @Composable () -> Unit,
) {
    Popup(
        popupPositionProvider = DialogPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = properties.dismissOnClickOutside,
            dismissOnBackPress = properties.dismissOnBackPress,
            clippingEnabled = false,
            hasScrim = true
        ),
        content = content
    )
}
