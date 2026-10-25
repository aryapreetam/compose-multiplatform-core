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

package androidx.compose.ui.graphics

import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces

internal actual fun ActualImageBitmap(
    width: Int,
    height: Int,
    config: ImageBitmapConfig,
    hasAlpha: Boolean,
    colorSpace: ColorSpace,
): ImageBitmap = DomImageBitmap(width, height, config, hasAlpha, colorSpace)

internal actual fun createImageBitmap(bytes: ByteArray): ImageBitmap {
    return DomImageBitmap(1, 1, ImageBitmapConfig.Argb8888, true, ColorSpaces.Srgb)
}

class DomImageBitmap(
    override val width: Int,
    override val height: Int,
    override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888,
    override val hasAlpha: Boolean = true,
    override val colorSpace: ColorSpace = ColorSpaces.Srgb,
    var sourceUrl: String? = null
) : ImageBitmap {
    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int,
    ) {}

    override fun prepareToDraw() {}
}
