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
    private var path: DomPath? = null
    private var forceClosed: Boolean = false

    private data class LinearSegment(
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val length: Float,
        val startDistance: Float,
        val endDistance: Float,
    )

    private val segments = mutableListOf<LinearSegment>()
    private var totalLength: Float = 0f

    override val length: Float get() = totalLength

    override fun setPath(path: Path?, forceClosed: Boolean) {
        this.path = path as? DomPath
        this.forceClosed = forceClosed
        segments.clear()
        totalLength = 0f

        val ops = this.path?.operations ?: return
        var startX = 0f
        var startY = 0f
        var currX = 0f
        var currY = 0f

        for (op in ops) {
            when (op) {
                is DomPathOp.MoveTo -> {
                    currX = op.x
                    currY = op.y
                    startX = op.x
                    startY = op.y
                }
                is DomPathOp.LineTo -> {
                    val dx = op.x - currX
                    val dy = op.y - currY
                    val segLen = kotlin.math.hypot(dx, dy)
                    if (segLen > 0f) {
                        segments.add(
                            LinearSegment(
                                x0 = currX,
                                y0 = currY,
                                x1 = op.x,
                                y1 = op.y,
                                length = segLen,
                                startDistance = totalLength,
                                endDistance = totalLength + segLen
                            )
                        )
                        totalLength += segLen
                    }
                    currX = op.x
                    currY = op.y
                }
                is DomPathOp.QuadTo -> {
                    val steps = 8
                    var prevX = currX
                    var prevY = currY
                    for (i in 1..steps) {
                        val t = i.toFloat() / steps
                        val invT = 1f - t
                        val x = invT * invT * currX + 2 * invT * t * op.x1 + t * t * op.x2
                        val y = invT * invT * currY + 2 * invT * t * op.y1 + t * t * op.y2
                        val segLen = kotlin.math.hypot(x - prevX, y - prevY)
                        if (segLen > 0f) {
                            segments.add(
                                LinearSegment(
                                    x0 = prevX,
                                    y0 = prevY,
                                    x1 = x,
                                    y1 = y,
                                    length = segLen,
                                    startDistance = totalLength,
                                    endDistance = totalLength + segLen
                                )
                            )
                            totalLength += segLen
                        }
                        prevX = x
                        prevY = y
                    }
                    currX = op.x2
                    currY = op.y2
                }
                is DomPathOp.CubicTo -> {
                    val steps = 12
                    var prevX = currX
                    var prevY = currY
                    for (i in 1..steps) {
                        val t = i.toFloat() / steps
                        val invT = 1f - t
                        val x = invT * invT * invT * currX + 3 * invT * invT * t * op.x1 + 3 * invT * t * t * op.x2 + t * t * t * op.x3
                        val y = invT * invT * invT * currY + 3 * invT * invT * t * op.y1 + 3 * invT * t * t * op.y2 + t * t * t * op.y3
                        val segLen = kotlin.math.hypot(x - prevX, y - prevY)
                        if (segLen > 0f) {
                            segments.add(
                                LinearSegment(
                                    x0 = prevX,
                                    y0 = prevY,
                                    x1 = x,
                                    y1 = y,
                                    length = segLen,
                                    startDistance = totalLength,
                                    endDistance = totalLength + segLen
                                )
                            )
                            totalLength += segLen
                        }
                        prevX = x
                        prevY = y
                    }
                    currX = op.x3
                    currY = op.y3
                }
                is DomPathOp.Close -> {
                    val dx = startX - currX
                    val dy = startY - currY
                    val segLen = kotlin.math.hypot(dx, dy)
                    if (segLen > 0f) {
                        segments.add(
                            LinearSegment(
                                x0 = currX,
                                y0 = currY,
                                x1 = startX,
                                y1 = startY,
                                length = segLen,
                                startDistance = totalLength,
                                endDistance = totalLength + segLen
                            )
                        )
                        totalLength += segLen
                    }
                    currX = startX
                    currY = startY
                }
            }
        }

        if (forceClosed && (currX != startX || currY != startY)) {
            val dx = startX - currX
            val dy = startY - currY
            val segLen = kotlin.math.hypot(dx, dy)
            if (segLen > 0f) {
                segments.add(
                    LinearSegment(
                        x0 = currX,
                        y0 = currY,
                        x1 = startX,
                        y1 = startY,
                        length = segLen,
                        startDistance = totalLength,
                        endDistance = totalLength + segLen
                    )
                )
                totalLength += segLen
            }
        }
    }

    override fun getSegment(
        startDistance: Float,
        stopDistance: Float,
        destination: Path,
        startWithMoveTo: Boolean,
    ): Boolean {
        if (segments.isEmpty() || totalLength <= 0f) return false
        val startD = startDistance.coerceIn(0f, totalLength)
        val stopD = stopDistance.coerceIn(0f, totalLength)
        if (startD >= stopD) return false

        var firstPoint = true
        for (seg in segments) {
            if (seg.endDistance <= startD) continue
            if (seg.startDistance >= stopD) break

            val segStartT = if (startD > seg.startDistance) {
                (startD - seg.startDistance) / seg.length
            } else {
                0f
            }
            val segEndT = if (stopD < seg.endDistance) {
                (stopD - seg.startDistance) / seg.length
            } else {
                1f
            }

            val pStartX = seg.x0 + (seg.x1 - seg.x0) * segStartT
            val pStartY = seg.y0 + (seg.y1 - seg.y0) * segStartT
            val pEndX = seg.x0 + (seg.x1 - seg.x0) * segEndT
            val pEndY = seg.y0 + (seg.y1 - seg.y0) * segEndT

            if (firstPoint) {
                if (startWithMoveTo) {
                    destination.moveTo(pStartX, pStartY)
                } else {
                    destination.lineTo(pStartX, pStartY)
                }
                firstPoint = false
            }
            destination.lineTo(pEndX, pEndY)
        }
        return !firstPoint
    }

    override fun getPosition(distance: Float): Offset {
        if (segments.isEmpty() || totalLength <= 0f) return Offset.Unspecified
        val d = distance.coerceIn(0f, totalLength)
        for (seg in segments) {
            if (d in seg.startDistance..seg.endDistance) {
                val t = if (seg.length > 0f) (d - seg.startDistance) / seg.length else 0f
                val x = seg.x0 + (seg.x1 - seg.x0) * t
                val y = seg.y0 + (seg.y1 - seg.y0) * t
                return Offset(x, y)
            }
        }
        return Offset.Unspecified
    }

    override fun getTangent(distance: Float): Offset {
        if (segments.isEmpty() || totalLength <= 0f) return Offset.Unspecified
        val d = distance.coerceIn(0f, totalLength)
        for (seg in segments) {
            if (d in seg.startDistance..seg.endDistance) {
                val dx = seg.x1 - seg.x0
                val dy = seg.y1 - seg.y0
                val len = seg.length
                return if (len > 0f) Offset(dx / len, dy / len) else Offset.Zero
            }
        }
        return Offset.Unspecified
    }
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
