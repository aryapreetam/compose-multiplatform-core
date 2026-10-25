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

package androidx.compose.ui.input.key

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isAltGraphPressed
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import org.w3c.dom.events.KeyboardEvent

actual typealias NativeKeyEvent = Any

internal data class InternalKeyEvent(
    val key: Key,
    val type: KeyEventType,
    val codePoint: Int,
    val modifiers: PointerKeyboardModifiers,
    val nativeEvent: Any? = null
)

internal val KeyEvent.internal: InternalKeyEvent
    get() = nativeKeyEvent as InternalKeyEvent

actual val KeyEvent.key: Key
    get() = internal.key

actual val KeyEvent.utf16CodePoint: Int
    get() = internal.codePoint

actual val KeyEvent.type: KeyEventType
    get() = internal.type

actual val KeyEvent.isAltPressed: Boolean
    get() = internal.modifiers.isAltPressed || internal.modifiers.isAltGraphPressed

actual val KeyEvent.isCtrlPressed: Boolean
    get() = internal.modifiers.isCtrlPressed

actual val KeyEvent.isMetaPressed: Boolean
    get() = internal.modifiers.isMetaPressed

actual val KeyEvent.isShiftPressed: Boolean
    get() = internal.modifiers.isShiftPressed

@InternalComposeUiApi
fun KeyEvent(
    key: Key,
    type: KeyEventType,
    codePoint: Int = 0,
    isCtrlPressed: Boolean = false,
    isMetaPressed: Boolean = false,
    isAltPressed: Boolean = false,
    isShiftPressed: Boolean = false,
    nativeEvent: Any? = null
) = KeyEvent(
    nativeKeyEvent = InternalKeyEvent(
        key = key,
        type = type,
        codePoint = codePoint,
        modifiers = PointerKeyboardModifiers(
            isCtrlPressed = isCtrlPressed,
            isMetaPressed = isMetaPressed,
            isAltPressed = isAltPressed,
            isShiftPressed = isShiftPressed
        ),
        nativeEvent = nativeEvent
    )
)

internal fun KeyboardEvent.toInputModifiers(): PointerKeyboardModifiers {
    return PointerKeyboardModifiers(
        isAltPressed = altKey,
        isShiftPressed = shiftKey,
        isCtrlPressed = ctrlKey,
        isMetaPressed = metaKey
    )
}

private val codeMap = mapOf(
    "KeyA" to Key.A, "KeyB" to Key.B, "KeyC" to Key.C, "KeyD" to Key.D, "KeyE" to Key.E,
    "KeyF" to Key.F, "KeyG" to Key.G, "KeyH" to Key.H, "KeyI" to Key.I, "KeyJ" to Key.J,
    "KeyK" to Key.K, "KeyL" to Key.L, "KeyM" to Key.M, "KeyN" to Key.N, "KeyO" to Key.O,
    "KeyP" to Key.P, "KeyQ" to Key.Q, "KeyR" to Key.R, "KeyS" to Key.S, "KeyT" to Key.T,
    "KeyU" to Key.U, "KeyV" to Key.V, "KeyW" to Key.W, "KeyX" to Key.X, "KeyY" to Key.Y, "KeyZ" to Key.Z,
    "Digit0" to Key.Zero, "Digit1" to Key.One, "Digit2" to Key.Two, "Digit3" to Key.Three,
    "Digit4" to Key.Four, "Digit5" to Key.Five, "Digit6" to Key.Six, "Digit7" to Key.Seven,
    "Digit8" to Key.Eight, "Digit9" to Key.Nine,
    "Enter" to Key.Enter, "Escape" to Key.Escape, "Backspace" to Key.Backspace, "Tab" to Key.Tab,
    "Space" to Key.Spacebar,
    "ArrowLeft" to Key.DirectionLeft, "ArrowUp" to Key.DirectionUp,
    "ArrowRight" to Key.DirectionRight, "ArrowDown" to Key.DirectionDown
)

private fun KeyboardEvent.toKey(): Key {
    val keyResolved = if (code.isEmpty()) codeMap[key] else codeMap[code]
    return keyResolved ?: Key.Unknown
}

internal fun KeyboardEvent.toComposeEvent(): KeyEvent {
    val composeKey = toKey()
    return KeyEvent(
        nativeKeyEvent = InternalKeyEvent(
            key = composeKey,
            type = when (type) {
                "keydown" -> KeyEventType.KeyDown
                "keyup" -> KeyEventType.KeyUp
                else -> KeyEventType.Unknown
            },
            codePoint = if (key.length == 1) key[0].code else composeKey.keyCode.toInt(),
            modifiers = toInputModifiers(),
            nativeEvent = this
        )
    )
}

val KeyEvent.domEventOrNull: KeyboardEvent?
    get() = internal.nativeEvent as? KeyboardEvent?
