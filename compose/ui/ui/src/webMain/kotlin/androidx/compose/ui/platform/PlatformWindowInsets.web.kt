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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path

@InternalComposeUiApi
interface PlatformInsets {
    val left: Int get() = 0
    val top: Int get() = 0
    val right: Int get() = 0
    val bottom: Int get() = 0

    companion object {
        val Zero: PlatformInsets = object : PlatformInsets {}
    }
}

@InternalComposeUiApi
fun PlatformInsets(
    getLeft: () -> Int = { 0 },
    getTop: () -> Int = { 0 },
    getRight: () -> Int = { 0 },
    getBottom: () -> Int = { 0 }
): PlatformInsets = object : PlatformInsets {
    override val left: Int get() = getLeft()
    override val top: Int get() = getTop()
    override val right: Int get() = getRight()
    override val bottom: Int get() = getBottom()
}

@InternalComposeUiApi
interface PlatformWindowInsets {
    val displayCutouts: List<Rect> get() = emptyList()
    val cutoutPath: Path? get() = null
    val captionBar: PlatformInsets get() = PlatformInsets.Zero
    val displayCutout: PlatformInsets get() = PlatformInsets.Zero
    val ime: PlatformInsets get() = PlatformInsets.Zero
    val mandatorySystemGestures: PlatformInsets get() = PlatformInsets.Zero
    val navigationBars: PlatformInsets get() = PlatformInsets.Zero
    val statusBars: PlatformInsets get() = PlatformInsets.Zero
    val systemBars: PlatformInsets get() = PlatformInsets.Zero
    val systemGestures: PlatformInsets get() = PlatformInsets.Zero
    val tappableElement: PlatformInsets get() = PlatformInsets.Zero
    val waterfall: PlatformInsets get() = PlatformInsets.Zero
    fun excluding(
        safeInsets: Boolean = true,
        ime: Boolean = true
    ): PlatformWindowInsets = this
}

@InternalComposeUiApi
object EmptyPlatformWindowInsets : PlatformWindowInsets

@InternalComposeUiApi
val LocalPlatformWindowInsets = staticCompositionLocalOf<PlatformWindowInsets> {
    EmptyPlatformWindowInsets
}

@InternalComposeUiApi
val PlatformWindowInsets.safeDrawing: PlatformInsets get() = PlatformInsets.Zero

@InternalComposeUiApi
val PlatformWindowInsets.safeGestures: PlatformInsets get() = PlatformInsets.Zero

@InternalComposeUiApi
val PlatformWindowInsets.safeContent: PlatformInsets get() = PlatformInsets.Zero

@Composable
internal fun PlatformWindowInsets.exclude(
    safeInsets: Boolean,
    ime: Boolean,
    content: @Composable () -> Unit
) {
    val windowInsets = LocalPlatformWindowInsets.current.excluding(safeInsets, ime)

    CompositionLocalProvider(
        LocalPlatformWindowInsets provides windowInsets,
        content = content
    )
}
