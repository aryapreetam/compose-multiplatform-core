/*
 * Copyright 2026 Project Luta
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
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.DefaultUiApplier
import androidx.compose.ui.platform.DomComposeOwner
import androidx.compose.ui.platform.DomUriHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.ProvideCommonCompositionLocals
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

import androidx.compose.runtime.Immutable
import androidx.compose.ui.input.key.KeyEvent

@Immutable
actual class PopupProperties actual constructor(
    actual val focusable: Boolean,
    actual val dismissOnBackPress: Boolean,
    actual val dismissOnClickOutside: Boolean,
    actual val clippingEnabled: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PopupProperties) return false
        return focusable == other.focusable &&
            dismissOnBackPress == other.dismissOnBackPress &&
            dismissOnClickOutside == other.dismissOnClickOutside &&
            clippingEnabled == other.clippingEnabled
    }

    override fun hashCode(): Int = 0
}

@Composable
fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    content: @Composable () -> Unit
) {
    val popupPositionProvider = remember(alignment, offset) {
        AlignmentOffsetPositionProvider(alignment, offset)
    }
    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = properties,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
        content = content
    )
}

@Composable
fun Popup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    content: @Composable () -> Unit
) {
    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = properties,
        content = content
    )
}

@Composable
actual fun Popup(
    alignment: Alignment,
    offset: IntOffset,
    onDismissRequest: (() -> Unit)?,
    properties: PopupProperties,
    content: @Composable () -> Unit,
) {
    val popupPositionProvider = remember(alignment, offset) {
        AlignmentOffsetPositionProvider(alignment, offset)
    }
    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = properties,
        content = content
    )
}

@OptIn(InternalComposeUiApi::class)
@Composable
actual fun Popup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)?,
    properties: PopupProperties,
    content: @Composable () -> Unit,
) {
    val rootContainer = LocalDomRootContainer.current ?: document.body as HTMLElement
    val currentContent by rememberUpdatedState(content)
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val windowInfo = LocalWindowInfo.current
    val parentComposition = rememberCompositionContext()

    val anchorBoundsState = remember { mutableStateOf(IntRect.Zero) }
    val onAnchorChanged = remember { mutableStateOf<(() -> Unit)?>(null) }

    // In-tree anchor layout to accurately calculate parent anchor window coordinates
    Layout(
        content = {},
        modifier = Modifier.onPlaced { coordinates ->
            coordinates.parentCoordinates?.let { parentCoords ->
                val pos = parentCoords.positionInWindow().round()
                val size = parentCoords.size
                anchorBoundsState.value = IntRect(pos, size)
            }
        }
    ) { _, _ ->
        layout(0, 0) {}
    }

    DisposableEffect(properties, density, layoutDirection, parentComposition) {
        // Absolute overlay container mounted at rootContainer level with z-index: 10000
        val overlayDiv = (document.createElement("div") as HTMLElement).apply {
            setAttribute("data-luta-popup-overlay", "true")
            style.position = "absolute"
            style.left = "0px"
            style.top = "0px"
            style.zIndex = "10000"
            style.setProperty("pointer-events", "auto")
            addEventListener("pointerdown", { it.stopPropagation() })
        }

        // Transparent backdrop listener when dismissOnClickOutside = true to dispatch onDismissRequest
        var backdropDiv: HTMLElement? = null
        if (properties.dismissOnClickOutside) {
            backdropDiv = (document.createElement("div") as HTMLElement).apply {
                setAttribute("data-luta-popup-backdrop", "true")
                style.position = "fixed"
                style.left = "0px"
                style.top = "0px"
                style.width = "100vw"
                style.height = "100vh"
                style.zIndex = "9999"
                style.background = "transparent"
                style.setProperty("pointer-events", "auto")
                addEventListener("pointerdown", { e ->
                    e.stopPropagation()
                    currentOnDismiss?.invoke()
                })
            }
            rootContainer.appendChild(backdropDiv)
        }

        rootContainer.appendChild(overlayDiv)

        val popupOwner = DomComposeOwner(
            rootContainer = overlayDiv,
            density = density,
            isPopup = true
        )

        fun updatePosition(width: Int, height: Int) {
            val windowSize = windowInfo.containerSize
            val pos = popupPositionProvider.calculatePosition(
                anchorBounds = anchorBoundsState.value,
                windowSize = windowSize,
                layoutDirection = layoutDirection,
                popupContentSize = IntSize(width, height)
            )
            overlayDiv.style.transform = "translate3d(${pos.x}px, ${pos.y}px, 0px)"
            overlayDiv.style.width = "${width}px"
            overlayDiv.style.height = "${height}px"
        }

        popupOwner.onPopupLayoutChange = { w, h ->
            updatePosition(w, h)
        }

        onAnchorChanged.value = {
            if (popupOwner.root.width > 0 && popupOwner.root.height > 0) {
                updatePosition(popupOwner.root.width, popupOwner.root.height)
            }
        }

        val popupComposition = Composition(
            DefaultUiApplier(popupOwner.root),
            parentComposition
        )

        popupComposition.setContent {
            ProvideCommonCompositionLocals(
                owner = popupOwner,
                uriHandler = DomUriHandler,
                content = {
                    CompositionLocalProvider(
                        LocalActiveClipEventsTarget provides { null },
                        LocalDomRootContainer provides rootContainer,
                        LocalSystemTheme provides SystemTheme.Light,
                        content = {
                            currentContent()
                        }
                    )
                }
            )
        }

        popupOwner.requestLayoutPass()

        onDispose {
            onAnchorChanged.value = null
            popupComposition.dispose()
            popupOwner.dispose()
            overlayDiv.remove()
            backdropDiv?.remove()
        }
    }

    LaunchedEffect(anchorBoundsState.value) {
        onAnchorChanged.value?.invoke()
    }
}
