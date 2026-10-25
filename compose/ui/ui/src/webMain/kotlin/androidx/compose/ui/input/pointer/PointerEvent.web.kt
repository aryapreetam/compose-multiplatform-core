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

package androidx.compose.ui.input.pointer

import androidx.compose.ui.ExperimentalComposeUiApi
import org.w3c.dom.events.MouseEvent

internal actual typealias NativePointerButtons = Int
internal actual typealias NativePointerKeyboardModifiers = Int

fun PointerButtons(
    isPrimaryPressed: Boolean = false,
    isSecondaryPressed: Boolean = false,
    isTertiaryPressed: Boolean = false,
    isBackPressed: Boolean = false,
    isForwardPressed: Boolean = false
): PointerButtons {
    var res = 0
    if (isPrimaryPressed) res = res or ButtonMasks.Primary
    if (isSecondaryPressed) res = res or ButtonMasks.Secondary
    if (isTertiaryPressed) res = res or ButtonMasks.Tertiary
    if (isBackPressed) res = res or ButtonMasks.Back
    if (isForwardPressed) res = res or ButtonMasks.Forward
    return PointerButtons(res)
}

fun PointerKeyboardModifiers(
    isCtrlPressed: Boolean = false,
    isMetaPressed: Boolean = false,
    isAltPressed: Boolean = false,
    isShiftPressed: Boolean = false,
    isAltGraphPressed: Boolean = false,
    isSymPressed: Boolean = false,
    isFunctionPressed: Boolean = false,
    isCapsLockOn: Boolean = false,
    isScrollLockOn: Boolean = false,
    isNumLockOn: Boolean = false,
): PointerKeyboardModifiers {
    var res = 0
    if (isCtrlPressed) res = res or KeyboardModifierMasks.CtrlPressed
    if (isMetaPressed) res = res or KeyboardModifierMasks.MetaPressed
    if (isAltPressed) res = res or KeyboardModifierMasks.AltPressed
    if (isShiftPressed) res = res or KeyboardModifierMasks.ShiftPressed
    if (isAltGraphPressed) res = res or KeyboardModifierMasks.AltGraphPressed
    if (isSymPressed) res = res or KeyboardModifierMasks.SymPressed
    if (isFunctionPressed) res = res or KeyboardModifierMasks.FunctionPressed
    if (isCapsLockOn) res = res or KeyboardModifierMasks.CapsLockOn
    if (isScrollLockOn) res = res or KeyboardModifierMasks.ScrollLockOn
    if (isNumLockOn) res = res or KeyboardModifierMasks.NumLockOn
    return PointerKeyboardModifiers(res)
}

internal actual fun EmptyPointerKeyboardModifiers() = PointerKeyboardModifiers()

actual class PointerEvent internal actual constructor(
    actual val changes: List<PointerInputChange>,
    internal val internalPointerEvent: InternalPointerEvent?
) {
    actual constructor(changes: List<PointerInputChange>) : this(changes, null)

    actual val buttons: PointerButtons
        get() = internalPointerEvent?.buttons ?: PointerButtons(0)

    actual val keyboardModifiers: PointerKeyboardModifiers
        get() = internalPointerEvent?.keyboardModifiers ?: PointerKeyboardModifiers(0)

    actual var type: PointerEventType =
        internalPointerEvent?.type ?: PointerEventType.Unknown
        internal set

    var nativeEvent: Any? = internalPointerEvent?.nativeEvent
        internal set

    constructor(
        changes: List<PointerInputChange>,
        nativeEvent: Any?
    ) : this(changes, null) {
        this.nativeEvent = nativeEvent
    }
}

actual val PointerButtons.isPrimaryPressed: Boolean
    get() = (packedValue and ButtonMasks.Primary) != 0

actual val PointerButtons.isSecondaryPressed: Boolean
    get() = (packedValue and ButtonMasks.Secondary) != 0

actual val PointerButtons.isTertiaryPressed: Boolean
    get() = (packedValue and ButtonMasks.Tertiary) != 0

actual val PointerButtons.isBackPressed: Boolean
    get() = (packedValue and ButtonMasks.Back) != 0

actual val PointerButtons.isForwardPressed: Boolean
    get() = (packedValue and ButtonMasks.Forward) != 0

actual fun PointerButtons.isPressed(buttonIndex: Int): Boolean =
    when (buttonIndex) {
        0 -> isPrimaryPressed
        1 -> isSecondaryPressed
        2 -> isTertiaryPressed
        3 -> isBackPressed
        4 -> isForwardPressed
        else -> (packedValue and (1 shl buttonIndex)) != 0
    }

actual val PointerButtons.areAnyPressed: Boolean
    get() = packedValue != 0

actual fun PointerButtons.indexOfFirstPressed(): Int {
    if (packedValue == 0) return -1
    return packedValue.countTrailingZeroBits()
}

actual fun PointerButtons.indexOfLastPressed(): Int {
    if (packedValue == 0) return -1
    return 31 - packedValue.countLeadingZeroBits()
}

actual val PointerKeyboardModifiers.isCtrlPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.CtrlPressed) != 0

actual val PointerKeyboardModifiers.isMetaPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.MetaPressed) != 0

actual val PointerKeyboardModifiers.isAltPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.AltPressed) != 0

actual val PointerKeyboardModifiers.isAltGraphPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.AltGraphPressed) != 0

actual val PointerKeyboardModifiers.isSymPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.SymPressed) != 0

actual val PointerKeyboardModifiers.isShiftPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.ShiftPressed) != 0

actual val PointerKeyboardModifiers.isFunctionPressed: Boolean
    get() = (packedValue and KeyboardModifierMasks.FunctionPressed) != 0

actual val PointerKeyboardModifiers.isCapsLockOn: Boolean
    get() = (packedValue and KeyboardModifierMasks.CapsLockOn) != 0

actual val PointerKeyboardModifiers.isScrollLockOn: Boolean
    get() = (packedValue and KeyboardModifierMasks.ScrollLockOn) != 0

actual val PointerKeyboardModifiers.isNumLockOn: Boolean
    get() = (packedValue and KeyboardModifierMasks.NumLockOn) != 0

private object ButtonMasks {
    const val Primary = 1 shl 0
    const val Secondary = 1 shl 1
    const val Tertiary = 1 shl 2
    const val Back = 1 shl 3
    const val Forward = 1 shl 4
}

private object KeyboardModifierMasks {
    const val CtrlPressed = 1 shl 0
    const val MetaPressed = 1 shl 1
    const val AltPressed = 1 shl 2
    const val ShiftPressed = 1 shl 3
    const val AltGraphPressed = 1 shl 4
    const val SymPressed = 1 shl 5
    const val FunctionPressed = 1 shl 6
    const val CapsLockOn = 1 shl 7
    const val ScrollLockOn = 1 shl 8
    const val NumLockOn = 1 shl 9
}

internal val MouseEvent.composeButton get(): PointerButton? {
    when (type) {
        "mousedown", "mouseup" -> Unit
        else -> return null
    }
    return when (val buttonIndex = button.toInt()) {
        0 -> PointerButton.Primary
        1 -> PointerButton.Tertiary
        2 -> PointerButton.Secondary
        3 -> PointerButton.Back
        4 -> PointerButton.Forward
        else -> PointerButton(buttonIndex)
    }
}

internal val MouseEvent.composeButtons get() = PointerButtons(buttons.toInt())
