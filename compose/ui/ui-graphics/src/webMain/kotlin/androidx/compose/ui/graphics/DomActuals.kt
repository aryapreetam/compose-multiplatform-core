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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import org.w3c.dom.HTMLElement

actual class NativeCanvas(val element: HTMLElement? = null)

actual val Canvas.nativeCanvas: NativeCanvas
    get() = (this as? DomCanvas)?.nativeCanvas ?: NativeCanvas()

internal actual fun ActualCanvas(image: ImageBitmap): Canvas = DomCanvas()

actual fun BlendMode.isSupported(): Boolean = true

actual fun TileMode.isSupported(): Boolean = true

actual class NativeColorFilter

internal actual fun actualTintColorFilter(color: Color, blendMode: BlendMode): NativeColorFilter =
    NativeColorFilter()

internal actual fun actualColorMatrixColorFilter(colorMatrix: ColorMatrix): NativeColorFilter =
    NativeColorFilter()

internal actual fun actualLightingColorFilter(multiply: Color, add: Color): NativeColorFilter =
    NativeColorFilter()

internal actual fun actualColorMatrixFromFilter(filter: NativeColorFilter): ColorMatrix =
    ColorMatrix()

internal actual fun actualCornerPathEffect(radius: Float): PathEffect = object : PathEffect {}

internal actual fun actualDashPathEffect(intervals: FloatArray, phase: Float): PathEffect =
    object : PathEffect {}

internal actual fun actualChainPathEffect(outer: PathEffect, inner: PathEffect): PathEffect =
    object : PathEffect {}

internal actual fun actualStampedPathEffect(
    shape: Path,
    advance: Float,
    phase: Float,
    style: StampedPathEffectStyle,
): PathEffect = object : PathEffect {}

actual class Shader

internal actual class TransformShader actual constructor() {
    actual var shader: Shader? = null
    actual fun transform(matrix: Matrix?) {}
}

internal actual fun ActualLinearGradientShader(
    from: Offset,
    to: Offset,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode,
): Shader = Shader()

internal actual fun ActualRadialGradientShader(
    center: Offset,
    radius: Float,
    colors: List<Color>,
    colorStops: List<Float>?,
    tileMode: TileMode,
): Shader = Shader()

internal actual fun ActualSweepGradientShader(
    center: Offset,
    colors: List<Color>,
    colorStops: List<Float>?,
): Shader = Shader()

internal actual fun ActualImageShader(
    image: ImageBitmap,
    tileModeX: TileMode,
    tileModeY: TileMode,
): Shader = Shader()

internal actual fun ActualCompositeShader(
    dst: Shader,
    src: Shader,
    blendMode: BlendMode,
): Shader = Shader()

@Immutable
actual sealed class RenderEffect actual constructor() {
    actual open fun isSupported(): Boolean = true
}

@Immutable
actual class BlurEffect actual constructor(
    val renderEffect: RenderEffect?,
    val radiusX: Float,
    val radiusY: Float,
    val edgeTreatment: TileMode,
) : RenderEffect()

@Immutable
actual class OffsetEffect actual constructor(
    val renderEffect: RenderEffect?,
    val offset: Offset,
) : RenderEffect()

actual fun PathMeasure(): PathMeasure = DomPathMeasure()

class DomPathMeasure : PathMeasure {
    override val length: Float get() = 0f
    override fun getSegment(
        startDistance: Float,
        stopDistance: Float,
        destination: Path,
        startWithMoveTo: Boolean,
    ): Boolean = false

    override fun setPath(path: Path?, forceClosed: Boolean) {}
    override fun getPosition(distance: Float): Offset = Offset.Unspecified
    override fun getTangent(distance: Float): Offset = Offset.Unspecified
}

actual fun PathIterator(
    path: Path,
    conicEvaluation: PathIterator.ConicEvaluation,
    tolerance: Float,
): PathIterator = DomPathIterator(path, conicEvaluation, tolerance)

class DomPathIterator(
    override val path: Path,
    override val conicEvaluation: PathIterator.ConicEvaluation = PathIterator.ConicEvaluation.AsQuadratics,
    override val tolerance: Float = 0.25f,
) : PathIterator {
    override fun calculateSize(includeConvertedConics: Boolean): Int = 0
    override fun hasNext(): Boolean = false
    override fun next(outPoints: FloatArray, offset: Int): PathSegment.Type = PathSegment.Type.Done
    override fun next(): PathSegment = DoneSegment
}
