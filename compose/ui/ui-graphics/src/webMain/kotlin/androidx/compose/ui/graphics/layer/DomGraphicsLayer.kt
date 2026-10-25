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

package androidx.compose.ui.graphics.layer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ActualImageBitmap
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

actual class GraphicsLayer {
    actual var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
    actual var topLeft: IntOffset = IntOffset.Zero
    actual var size: IntSize = IntSize.Zero
        private set
    actual var pivotOffset: Offset = Offset.Unspecified
    actual var alpha: Float = 1.0f
    actual var scaleX: Float = 1.0f
    actual var scaleY: Float = 1.0f
    actual var translationX: Float = 0.0f
    actual var translationY: Float = 0.0f
    actual var shadowElevation: Float = 0.0f
    actual var ambientShadowColor: Color = Color.Black
    actual var spotShadowColor: Color = Color.Black
    actual var blendMode: BlendMode = BlendMode.SrcOver
    actual var colorFilter: ColorFilter? = null
    actual var rotationX: Float = 0.0f
    actual var rotationY: Float = 0.0f
    actual var rotationZ: Float = 0.0f
    actual var cameraDistance: Float = 8.0f
    actual var clip: Boolean = false
    actual var renderEffect: RenderEffect? = null
    actual var isReleased: Boolean = false
        private set

    private var internalOutline: Outline? = null
    actual val outline: Outline
        get() = internalOutline ?: Outline.Rectangle(
            Rect(Offset.Zero, Size(size.width.toFloat(), size.height.toFloat()))
        )

    actual fun setPathOutline(path: Path) {
        internalOutline = Outline.Generic(path)
    }

    actual fun setRoundRectOutline(topLeft: Offset, size: Size, cornerRadius: Float) {
        internalOutline = Outline.Rounded(
            RoundRect(
                topLeft.x,
                topLeft.y,
                topLeft.x + size.width,
                topLeft.y + size.height,
                CornerRadius(cornerRadius, cornerRadius)
            )
        )
    }

    actual fun setRectOutline(topLeft: Offset, size: Size) {
        internalOutline = Outline.Rectangle(Rect(topLeft, size))
    }

    private var drawBlock: (DrawScope.() -> Unit)? = null
    private val drawScope = CanvasDrawScope()

    actual fun record(
        density: Density,
        layoutDirection: LayoutDirection,
        size: IntSize,
        block: DrawScope.() -> Unit
    ) {
        this.size = size
        this.drawBlock = block
    }

    actual suspend fun toImageBitmap(): ImageBitmap {
        return ActualImageBitmap(size.width, size.height, ImageBitmapConfig.Argb8888, true, ColorSpaces.Srgb)
    }

    internal actual fun draw(canvas: Canvas, parentLayer: GraphicsLayer?) {
        val block = drawBlock ?: return
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(size.width.toFloat(), size.height.toFloat())
        ) {
            block()
        }
    }
}
