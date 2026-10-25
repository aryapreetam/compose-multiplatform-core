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

package androidx.compose.ui.text

import androidx.annotation.IntRange
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DomCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toCssRgba
import androidx.compose.ui.text.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.isSpecified
import kotlinx.browser.document
import org.w3c.dom.HTMLSpanElement

@JvmDefaultWithCompatibility
actual sealed interface Paragraph {
    actual val width: Float
    actual val height: Float
    actual val minIntrinsicWidth: Float
    actual val maxIntrinsicWidth: Float
    actual val firstBaseline: Float
    actual val lastBaseline: Float
    actual val didExceedMaxLines: Boolean
    actual val lineCount: Int
    actual val placeholderRects: List<Rect?>
    actual fun getPathForRange(start: Int, end: Int): Path
    actual fun getCursorRect(offset: Int): Rect
    actual fun getLineLeft(lineIndex: Int): Float
    actual fun getLineRight(lineIndex: Int): Float
    actual fun getLineTop(lineIndex: Int): Float
    actual fun getLineBaseline(lineIndex: Int): Float
    actual fun getLineBottom(lineIndex: Int): Float
    actual fun getLineHeight(lineIndex: Int): Float
    actual fun getLineWidth(lineIndex: Int): Float
    actual fun getLineStart(lineIndex: Int): Int
    actual fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int
    actual fun isLineEllipsized(lineIndex: Int): Boolean
    actual fun getLineForOffset(offset: Int): Int
    actual fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float
    actual fun getParagraphDirection(offset: Int): ResolvedTextDirection
    actual fun getBidiRunDirection(offset: Int): ResolvedTextDirection
    actual fun getLineForVerticalPosition(vertical: Float): Int
    actual fun getOffsetForPosition(position: Offset): Int
    actual fun getRangeForRect(
        rect: Rect,
        granularity: TextGranularity,
        inclusionStrategy: TextInclusionStrategy
    ): TextRange
    actual fun getBoundingBox(offset: Int): Rect
    actual fun fillBoundingBoxes(range: TextRange, array: FloatArray, @IntRange(from = 0) arrayStart: Int)
    actual fun getWordBoundary(offset: Int): TextRange
    actual fun paint(canvas: Canvas, color: Color, shadow: Shadow?, textDecoration: TextDecoration?)
    actual fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    )
    actual fun paint(
        canvas: Canvas,
        brush: Brush,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    )
}

class DomParagraph(
    val text: String,
    val style: TextStyle,
    val maxLines: Int = Int.MAX_VALUE,
    val constraints: Constraints = Constraints()
) : Paragraph {
    private val fontSizePx = if (style.fontSize.isSpecified) style.fontSize.value else 16f
    override val width: Float
    override val height: Float
    override val minIntrinsicWidth: Float get() = 0f
    override val maxIntrinsicWidth: Float
    override val firstBaseline: Float get() = fontSizePx
    override val lastBaseline: Float get() = height - (fontSizePx * 0.2f)
    override val didExceedMaxLines: Boolean get() = false
    override val lineCount: Int get() = 1
    override val placeholderRects: List<Rect?> get() = emptyList()

    init {
        val measured = text.length * fontSizePx * 0.55f
        maxIntrinsicWidth = measured
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else measured
        width = measured.coerceAtMost(maxWidth)
        height = fontSizePx * 1.3f
    }

    override fun getPathForRange(start: Int, end: Int): Path = Path()
    override fun getCursorRect(offset: Int): Rect {
        val x = offset * fontSizePx * 0.55f
        return Rect(x, 0f, x + 2f, height)
    }

    override fun getLineLeft(lineIndex: Int): Float = 0f
    override fun getLineRight(lineIndex: Int): Float = width
    override fun getLineTop(lineIndex: Int): Float = 0f
    override fun getLineBaseline(lineIndex: Int): Float = firstBaseline
    override fun getLineBottom(lineIndex: Int): Float = height
    override fun getLineHeight(lineIndex: Int): Float = height
    override fun getLineWidth(lineIndex: Int): Float = width
    override fun getLineStart(lineIndex: Int): Int = 0
    override fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int = text.length
    override fun isLineEllipsized(lineIndex: Int): Boolean = false
    override fun getLineForOffset(offset: Int): Int = 0
    override fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float =
        offset * fontSizePx * 0.55f
    override fun getParagraphDirection(offset: Int): ResolvedTextDirection = ResolvedTextDirection.Ltr
    override fun getBidiRunDirection(offset: Int): ResolvedTextDirection = ResolvedTextDirection.Ltr
    override fun getLineForVerticalPosition(vertical: Float): Int = 0
    override fun getOffsetForPosition(position: Offset): Int {
        val charWidth = fontSizePx * 0.55f
        return if (charWidth > 0f) {
            (position.x / charWidth).toInt().coerceIn(0, text.length)
        } else {
            0
        }
    }

    override fun getRangeForRect(
        rect: Rect,
        granularity: TextGranularity,
        inclusionStrategy: TextInclusionStrategy
    ): TextRange = TextRange(0, text.length)

    override fun getBoundingBox(offset: Int): Rect {
        val x = offset * fontSizePx * 0.55f
        return Rect(x, 0f, x + (fontSizePx * 0.55f), height)
    }

    override fun fillBoundingBoxes(range: TextRange, array: FloatArray, arrayStart: Int) {}
    override fun getWordBoundary(offset: Int): TextRange = TextRange(0, text.length)

    override fun paint(canvas: Canvas, color: Color, shadow: Shadow?, textDecoration: TextDecoration?) {
        paint(canvas, color, shadow, textDecoration, null, BlendMode.SrcOver)
    }

    override fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    ) {
        val host = (canvas as? DomCanvas)?.hostElement ?: return
        val span = (host.querySelector(":scope > span[data-luta-text]") as? HTMLSpanElement)
            ?: (document.createElement("span") as HTMLSpanElement).also {
                it.setAttribute("data-luta-text", "true")
                it.style.display = "inline-block"
                it.style.setProperty("white-space", "pre-wrap")
                it.style.setProperty("word-break", "break-word")
                it.style.setProperty("pointer-events", "none")
                host.appendChild(it)
            }

        span.style.font = "${fontSizePx}px sans-serif"
        if (color.isSpecified) {
            span.style.color = color.toCssRgba()
        }
        if (span.textContent != text) {
            span.textContent = text
        }
    }

    override fun paint(
        canvas: Canvas,
        brush: Brush,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    ) {
        paint(canvas, Color.Black, shadow, textDecoration, drawStyle, blendMode)
    }
}

class DomParagraphIntrinsics(
    val text: String,
    val style: TextStyle,
    val density: Density
) : ParagraphIntrinsics {
    private val fontSizePx = if (style.fontSize.isSpecified) style.fontSize.value else 16f
    override val minIntrinsicWidth: Float get() = 0f
    override val maxIntrinsicWidth: Float = text.length * fontSizePx * 0.55f
    override val hasStaleResolvedFonts: Boolean get() = false
}
