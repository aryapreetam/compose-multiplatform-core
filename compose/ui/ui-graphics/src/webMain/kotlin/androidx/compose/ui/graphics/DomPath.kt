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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import kotlin.math.max
import kotlin.math.min

actual fun Path(): Path = DomPath()

class DomPath : Path {
    override var fillType: PathFillType = PathFillType.NonZero

    private val svgCommands = StringBuilder()
    private var currentX = 0f
    private var currentY = 0f
    private var minX = Float.POSITIVE_INFINITY
    private var minY = Float.POSITIVE_INFINITY
    private var maxX = Float.NEGATIVE_INFINITY
    private var maxY = Float.NEGATIVE_INFINITY
    private var hasPoints = false

    fun toSvgPathData(): String = svgCommands.toString()

    override val isConvex: Boolean get() = true
    override val isEmpty: Boolean get() = !hasPoints

    private fun updateBounds(x: Float, y: Float) {
        hasPoints = true
        minX = min(minX, x)
        minY = min(minY, y)
        maxX = max(maxX, x)
        maxY = max(maxY, y)
    }

    override fun moveTo(x: Float, y: Float) {
        svgCommands.append("M $x $y ")
        currentX = x
        currentY = y
        updateBounds(x, y)
    }

    override fun relativeMoveTo(dx: Float, dy: Float) {
        moveTo(currentX + dx, currentY + dy)
    }

    override fun lineTo(x: Float, y: Float) {
        svgCommands.append("L $x $y ")
        currentX = x
        currentY = y
        updateBounds(x, y)
    }

    override fun relativeLineTo(dx: Float, dy: Float) {
        lineTo(currentX + dx, currentY + dy)
    }

    @Deprecated("Use quadraticTo()", ReplaceWith("quadraticTo(x1, y1, x2, y2)"))
    override fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        quadraticTo(x1, y1, x2, y2)
    }

    override fun quadraticTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        svgCommands.append("Q $x1 $y1, $x2 $y2 ")
        currentX = x2
        currentY = y2
        updateBounds(x1, y1)
        updateBounds(x2, y2)
    }

    @Deprecated("Use relativeQuadraticTo()", ReplaceWith("relativeQuadraticTo(dx1, dy1, dx2, dy2)"))
    override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        relativeQuadraticTo(dx1, dy1, dx2, dy2)
    }

    override fun relativeQuadraticTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        quadraticTo(currentX + dx1, currentY + dy1, currentX + dx2, currentY + dy2)
    }

    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        svgCommands.append("C $x1 $y1, $x2 $y2, $x3 $y3 ")
        currentX = x3
        currentY = y3
        updateBounds(x1, y1)
        updateBounds(x2, y2)
        updateBounds(x3, y3)
    }

    override fun relativeCubicTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float, dx3: Float, dy3: Float) {
        cubicTo(currentX + dx1, currentY + dy1, currentX + dx2, currentY + dy2, currentX + dx3, currentY + dy3)
    }

    override fun arcTo(rect: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float, forceMoveTo: Boolean) {
        val rx = rect.width / 2f
        val ry = rect.height / 2f
        val cx = rect.left + rx
        val cy = rect.top + ry
        if (forceMoveTo) {
            moveTo(cx + rx, cy)
        } else {
            lineTo(cx + rx, cy)
        }
        svgCommands.append("A $rx $ry 0 0 1 ${cx - rx} $cy ")
        svgCommands.append("A $rx $ry 0 0 1 ${cx + rx} $cy ")
        updateBounds(rect.left, rect.top)
        updateBounds(rect.right, rect.bottom)
    }

    @Deprecated("addRect with direction", level = DeprecationLevel.HIDDEN)
    override fun addRect(rect: Rect) {
        addRect(rect, Path.Direction.CounterClockwise)
    }

    override fun addRect(rect: Rect, direction: Path.Direction) {
        moveTo(rect.left, rect.top)
        lineTo(rect.right, rect.top)
        lineTo(rect.right, rect.bottom)
        lineTo(rect.left, rect.bottom)
        close()
    }

    @Deprecated("addOval with direction", level = DeprecationLevel.HIDDEN)
    override fun addOval(oval: Rect) {
        addOval(oval, Path.Direction.CounterClockwise)
    }

    override fun addOval(oval: Rect, direction: Path.Direction) {
        arcTo(oval, 0f, 360f, true)
        close()
    }

    @Deprecated("addRoundRect with direction", level = DeprecationLevel.HIDDEN)
    override fun addRoundRect(roundRect: RoundRect) {
        addRoundRect(roundRect, Path.Direction.CounterClockwise)
    }

    override fun addRoundRect(roundRect: RoundRect, direction: Path.Direction) {
        val rx = roundRect.topLeftCornerRadius.x
        moveTo(roundRect.left + rx, roundRect.top)
        lineTo(roundRect.right - roundRect.topRightCornerRadius.x, roundRect.top)
        quadraticTo(roundRect.right, roundRect.top, roundRect.right, roundRect.top + roundRect.topRightCornerRadius.y)
        lineTo(roundRect.right, roundRect.bottom - roundRect.bottomRightCornerRadius.y)
        quadraticTo(roundRect.right, roundRect.bottom, roundRect.right - roundRect.bottomRightCornerRadius.x, roundRect.bottom)
        lineTo(roundRect.left + roundRect.bottomLeftCornerRadius.x, roundRect.bottom)
        quadraticTo(roundRect.left, roundRect.bottom, roundRect.left, roundRect.bottom - roundRect.bottomLeftCornerRadius.y)
        lineTo(roundRect.left, roundRect.top + roundRect.topLeftCornerRadius.y)
        quadraticTo(roundRect.left, roundRect.top, roundRect.left + rx, roundRect.top)
        close()
    }

    override fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) {
        arcTo(oval, startAngleDegrees, sweepAngleDegrees, true)
    }

    override fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float) {
        arcToRad(oval, startAngleRadians, sweepAngleRadians, true)
    }

    override fun addPath(path: Path, offset: Offset) {
        if (path is DomPath) {
            svgCommands.append(path.svgCommands)
            hasPoints = hasPoints || path.hasPoints
            if (path.hasPoints) {
                updateBounds(path.minX + offset.x, path.minY + offset.y)
                updateBounds(path.maxX + offset.x, path.maxY + offset.y)
            }
        }
    }

    override fun close() {
        svgCommands.append("Z ")
    }

    override fun reset() {
        svgCommands.clear()
        currentX = 0f
        currentY = 0f
        minX = Float.POSITIVE_INFINITY
        minY = Float.POSITIVE_INFINITY
        maxX = Float.NEGATIVE_INFINITY
        maxY = Float.NEGATIVE_INFINITY
        hasPoints = false
    }

    override fun rewind() {
        reset()
    }

    override fun translate(offset: Offset) {
        // Offset bounds
        if (hasPoints) {
            minX += offset.x
            minY += offset.y
            maxX += offset.x
            maxY += offset.y
        }
        currentX += offset.x
        currentY += offset.y
    }

    override fun transform(matrix: Matrix) {}

    override fun getBounds(): Rect {
        return if (hasPoints) {
            Rect(minX, minY, maxX, maxY)
        } else {
            Rect.Zero
        }
    }

    override fun op(path1: Path, path2: Path, operation: PathOperation): Boolean {
        // Fallback: union path commands
        reset()
        addPath(path1)
        addPath(path2)
        return true
    }
}
