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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.svg.SVGElement

fun Color.toCssRgba(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return "rgba($r, $g, $b, $alpha)"
}

private var globalClipIdCounter = 0

class DomCanvas(
    val hostElement: HTMLElement? = null
) : Canvas {
    override val drawsChildren: Boolean get() = false
    val nativeCanvas = NativeCanvas(hostElement)

    private var currentTx = 0f
    private var currentTy = 0f
    val translationX: Float get() = currentTx
    val translationY: Float get() = currentTy

    private var defsElement: Element? = null
    private var currentContainer: Element? = null

    private data class CanvasState(
        val tx: Float,
        val ty: Float,
        val container: Element?
    )
    private val stateStack = mutableListOf<CanvasState>()

    override fun save() {
        stateStack.add(CanvasState(currentTx, currentTy, currentContainer))
    }

    override fun restore() {
        if (stateStack.isNotEmpty()) {
            val state = stateStack.removeAt(stateStack.lastIndex)
            currentTx = state.tx
            currentTy = state.ty
            currentContainer = state.container
        } else {
            currentTx = 0f
            currentTy = 0f
            currentContainer = null
        }
    }

    override fun saveLayer(bounds: Rect, paint: Paint) {
        save()
    }

    override fun translate(dx: Float, dy: Float) {
        currentTx += dx
        currentTy += dy
    }

    override fun scale(sx: Float, sy: Float) {}
    override fun rotate(degrees: Float) {}
    override fun skew(sx: Float, sy: Float) {}
    override fun concat(matrix: Matrix) {}

    fun pushLayerClipOutline(outline: Outline?, size: IntSize) {
        val host = hostElement ?: return
        val svg = getOrCreateSvg(host)
        val defs = getOrCreateDefs(svg)
        val clipId = "luta-clip-${++globalClipIdCounter}"
        val clipPath = document.createElementNS("http://www.w3.org/2000/svg", "clipPath")
        clipPath.setAttribute("id", clipId)

        when (outline) {
            is Outline.Rounded -> {
                val rr = outline.roundRect
                val isUniform = rr.topLeftCornerRadius.x == rr.topRightCornerRadius.x &&
                    rr.topLeftCornerRadius.x == rr.bottomRightCornerRadius.x &&
                    rr.topLeftCornerRadius.x == rr.bottomLeftCornerRadius.x &&
                    rr.topLeftCornerRadius.x == rr.topLeftCornerRadius.y
                if (isUniform) {
                    val rect = document.createElementNS("http://www.w3.org/2000/svg", "rect")
                    rect.setAttribute("x", (rr.left + currentTx).toString())
                    rect.setAttribute("y", (rr.top + currentTy).toString())
                    rect.setAttribute("width", rr.width.toString())
                    rect.setAttribute("height", rr.height.toString())
                    rect.setAttribute("rx", rr.topLeftCornerRadius.x.toString())
                    rect.setAttribute("ry", rr.topLeftCornerRadius.y.toString())
                    clipPath.appendChild(rect)
                } else {
                    val domPath = (outline.roundRectPath as? DomPath) ?: DomPath().apply { addRoundRect(rr) }
                    val path = document.createElementNS("http://www.w3.org/2000/svg", "path")
                    path.setAttribute("d", domPath.toSvgPathData())
                    if (currentTx != 0f || currentTy != 0f) {
                        path.setAttribute("transform", "translate($currentTx, $currentTy)")
                    }
                    clipPath.appendChild(path)
                }
            }
            is Outline.Rectangle -> {
                val rect = document.createElementNS("http://www.w3.org/2000/svg", "rect")
                rect.setAttribute("x", (outline.rect.left + currentTx).toString())
                rect.setAttribute("y", (outline.rect.top + currentTy).toString())
                rect.setAttribute("width", outline.rect.width.toString())
                rect.setAttribute("height", outline.rect.height.toString())
                clipPath.appendChild(rect)
            }
            is Outline.Generic -> {
                val domPath = outline.path as? DomPath
                if (domPath != null) {
                    val path = document.createElementNS("http://www.w3.org/2000/svg", "path")
                    path.setAttribute("d", domPath.toSvgPathData())
                    if (currentTx != 0f || currentTy != 0f) {
                        path.setAttribute("transform", "translate($currentTx, $currentTy)")
                    }
                    clipPath.appendChild(path)
                }
            }
            else -> {
                val rect = document.createElementNS("http://www.w3.org/2000/svg", "rect")
                rect.setAttribute("x", currentTx.toString())
                rect.setAttribute("y", currentTy.toString())
                rect.setAttribute("width", size.width.toString())
                rect.setAttribute("height", size.height.toString())
                clipPath.appendChild(rect)
            }
        }
        defs.appendChild(clipPath)

        val g = document.createElementNS("http://www.w3.org/2000/svg", "g")
        g.setAttribute("clip-path", "url(#$clipId)")
        targetContainer().appendChild(g)
        currentContainer = g
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        val host = hostElement ?: return
        if (clipOp == ClipOp.Difference) {
            val b = if (bottom > 0f) bottom else 16f
            val poly = "polygon(0 0, ${left}px 0, ${left}px ${b}px, ${right}px ${b}px, ${right}px 0, 100% 0, 100% 100%, 0 100%)"
            host.style.setProperty("clip-path", poly)
        } else {
            pushLayerClipOutline(
                Outline.Rectangle(Rect(left, top, right, bottom)),
                IntSize((right - left).toInt(), (bottom - top).toInt())
            )
        }
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
        pushLayerClipOutline(Outline.Generic(path), IntSize.Zero)
    }

    private fun targetContainer(): Element {
        val host = hostElement ?: return document.createElement("div")
        return currentContainer ?: getOrCreateSvg(host)
    }

    private fun getOrCreateDefs(svg: Element): Element {
        val existing = defsElement ?: svg.querySelector(":scope > defs")
        if (existing != null) {
            defsElement = existing
            return existing
        }
        val defs = document.createElementNS("http://www.w3.org/2000/svg", "defs")
        svg.insertBefore(defs, svg.firstChild)
        defsElement = defs
        return defs
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        val line = document.createElementNS("http://www.w3.org/2000/svg", "line")
        line.setAttribute("x1", (p1.x + currentTx).toString())
        line.setAttribute("y1", (p1.y + currentTy).toString())
        line.setAttribute("x2", (p2.x + currentTx).toString())
        line.setAttribute("y2", (p2.y + currentTy).toString())
        line.setAttribute("stroke", paint.color.toCssRgba())
        line.setAttribute("stroke-width", paint.strokeWidth.toString())
        targetContainer().appendChild(line)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        val host = hostElement ?: return
        if (paint.color != Color.Unspecified && paint.style != PaintingStyle.Stroke && paint.color.alpha <= 0.001f) {
            return
        }
        val hostW = host.style.width.removeSuffix("px").toDoubleOrNull() ?: host.clientWidth.toDouble()
        val hostH = host.style.height.removeSuffix("px").toDoubleOrNull() ?: host.clientHeight.toDouble()
        val isButton = host.tagName.equals("button", ignoreCase = true) || host.getAttribute("role") == "button"
        val isCheckbox = host.getAttribute("role") == "checkbox" || host.hasAttribute("data-luta-checked")
        val maxInset = if (paint.style == PaintingStyle.Stroke && paint.strokeWidth > 0f) {
            paint.strokeWidth + 0.5f
        } else {
            0.5f
        }
        val isFullElement = (currentContainer == null && !isCheckbox && currentTx == 0f && currentTy == 0f && left <= maxInset && top <= maxInset && (hostW <= 0.0 || right >= (hostW - maxInset - 1.5).toFloat()) && (hostH <= 0.0 || bottom >= (hostH - maxInset - 1.5).toFloat()))
        val shaderGradient = paint.shader?.cssGradient
        if (shaderGradient != null) {
            if (isFullElement) {
                host.style.background = shaderGradient
                host.setAttribute("data-luta-gradient", "true")
                if (paint.alpha < 1f) {
                    host.style.opacity = paint.alpha.toString()
                }
            } else {
                val existingGrad = host.querySelector(":scope > div[data-luta-gradient]") as? HTMLElement
                val gradDiv = existingGrad ?: (document.createElement("div") as HTMLElement).also {
                    it.setAttribute("data-luta-gradient", "true")
                    it.style.position = "absolute"
                    it.style.setProperty("pointer-events", "none")
                    host.appendChild(it)
                }
                gradDiv.style.left = "${left + currentTx}px"
                gradDiv.style.top = "${top + currentTy}px"
                gradDiv.style.width = "${right - left}px"
                gradDiv.style.height = "${bottom - top}px"
                gradDiv.style.background = shaderGradient
                if (paint.alpha < 1f) gradDiv.style.opacity = paint.alpha.toString()
            }
            return
        }
        if (!isFullElement) {
            val rect = document.createElementNS("http://www.w3.org/2000/svg", "rect")
            val x = left + currentTx
            val y = top + currentTy
            val w = right - left
            val h = bottom - top
            rect.setAttribute("x", x.toString())
            rect.setAttribute("y", y.toString())
            rect.setAttribute("width", w.toString())
            rect.setAttribute("height", h.toString())
            if (paint.color != Color.Unspecified) {
                if (paint.style == PaintingStyle.Stroke) {
                    rect.setAttribute("fill", "none")
                    rect.setAttribute("stroke", paint.color.toCssRgba())
                    val sw = if (paint.strokeWidth > 0f) paint.strokeWidth else 1f
                    rect.setAttribute("stroke-width", sw.toString())
                } else {
                    rect.setAttribute("fill", paint.color.toCssRgba())
                    rect.setAttribute("stroke", "none")
                }
            }
            targetContainer().appendChild(rect)
            return
        }
        if (paint.color != Color.Unspecified) {
            if (paint.style == PaintingStyle.Stroke) {
                val strokeW = if (paint.strokeWidth > 0f) paint.strokeWidth else 1f
                host.style.border = "${strokeW}px solid ${paint.color.toCssRgba()}"
            } else {
                host.style.backgroundColor = paint.color.toCssRgba()
            }
        }
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
        paint: Paint,
    ) {
        val host = hostElement ?: return
        if (paint.color != Color.Unspecified && paint.style != PaintingStyle.Stroke && paint.color.alpha <= 0.001f) {
            return
        }
        val hostW = host.style.width.removeSuffix("px").toDoubleOrNull() ?: host.clientWidth.toDouble()
        val hostH = host.style.height.removeSuffix("px").toDoubleOrNull() ?: host.clientHeight.toDouble()
        val isButton = host.tagName.equals("button", ignoreCase = true) || host.getAttribute("role") == "button"
        val isCheckbox = host.getAttribute("role") == "checkbox" || host.hasAttribute("data-luta-checked")
        val maxInset = if (paint.style == PaintingStyle.Stroke && paint.strokeWidth > 0f) {
            paint.strokeWidth + 0.5f
        } else {
            0.5f
        }
        val isFullElement = (currentContainer == null && !isCheckbox && currentTx == 0f && currentTy == 0f && left <= maxInset && top <= maxInset && (hostW <= 0.0 || right >= (hostW - maxInset - 1.5).toFloat()) && (hostH <= 0.0 || bottom >= (hostH - maxInset - 1.5).toFloat()))
        val shaderGradient = paint.shader?.cssGradient
        if (shaderGradient != null) {
            if (isFullElement) {
                host.style.background = shaderGradient
                host.setAttribute("data-luta-gradient", "true")
                host.style.borderRadius = "${radiusX}px"
                host.style.setProperty("overflow", "hidden")
                if (paint.alpha < 1f) {
                    host.style.opacity = paint.alpha.toString()
                }
            } else {
                val existingGrad = host.querySelector(":scope > div[data-luta-gradient]") as? HTMLElement
                val gradDiv = existingGrad ?: (document.createElement("div") as HTMLElement).also {
                    it.setAttribute("data-luta-gradient", "true")
                    it.style.position = "absolute"
                    it.style.setProperty("pointer-events", "none")
                    host.appendChild(it)
                }
                gradDiv.style.left = "${left + currentTx}px"
                gradDiv.style.top = "${top + currentTy}px"
                gradDiv.style.width = "${right - left}px"
                gradDiv.style.height = "${bottom - top}px"
                gradDiv.style.borderRadius = "${radiusX}px"
                gradDiv.style.background = shaderGradient
                if (paint.alpha < 1f) gradDiv.style.opacity = paint.alpha.toString()
            }
            return
        }
        if (!isFullElement) {
            val rect = document.createElementNS("http://www.w3.org/2000/svg", "rect")
            val x = left + currentTx
            val y = top + currentTy
            val w = right - left
            val h = bottom - top
            rect.setAttribute("x", x.toString())
            rect.setAttribute("y", y.toString())
            rect.setAttribute("width", w.toString())
            rect.setAttribute("height", h.toString())
            rect.setAttribute("rx", radiusX.toString())
            rect.setAttribute("ry", radiusY.toString())
            if (paint.color != Color.Unspecified) {
                if (paint.style == PaintingStyle.Stroke) {
                    rect.setAttribute("fill", "none")
                    rect.setAttribute("stroke", paint.color.toCssRgba())
                    val sw = if (paint.strokeWidth > 0f) paint.strokeWidth else 1f
                    rect.setAttribute("stroke-width", sw.toString())
                } else {
                    rect.setAttribute("fill", paint.color.toCssRgba())
                    rect.setAttribute("stroke", "none")
                }
            }
            targetContainer().appendChild(rect)
            return
        }
        if (paint.color != Color.Unspecified) {
            if (paint.style == PaintingStyle.Stroke) {
                val strokeW = if (paint.strokeWidth > 0f) paint.strokeWidth else 1f
                host.style.border = "${strokeW}px solid ${paint.color.toCssRgba()}"
            } else {
                host.style.backgroundColor = paint.color.toCssRgba()
            }
        }
        host.style.borderRadius = "${radiusX}px"
        host.style.setProperty("overflow", "hidden")
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        val host = hostElement ?: return
        if (paint.color != Color.Unspecified && paint.style != PaintingStyle.Stroke && paint.color.alpha <= 0.001f) {
            return
        }
        val ellipse = document.createElementNS("http://www.w3.org/2000/svg", "ellipse")
        val cx = (left + right) / 2f + currentTx
        val cy = (top + bottom) / 2f + currentTy
        val rx = (right - left) / 2f
        val ry = (bottom - top) / 2f
        ellipse.setAttribute("cx", cx.toString())
        ellipse.setAttribute("cy", cy.toString())
        ellipse.setAttribute("rx", rx.toString())
        ellipse.setAttribute("ry", ry.toString())
        if (paint.style == PaintingStyle.Stroke) {
            ellipse.setAttribute("fill", "none")
            ellipse.setAttribute("stroke", paint.color.toCssRgba())
            val sw = if (paint.strokeWidth > 0f) paint.strokeWidth else 1f
            ellipse.setAttribute("stroke-width", sw.toString())
        } else {
            ellipse.setAttribute("fill", paint.color.toCssRgba())
            ellipse.setAttribute("stroke", "none")
        }
        if (paint.alpha < 1f) {
            ellipse.setAttribute("opacity", paint.alpha.toString())
        }
        targetContainer().appendChild(ellipse)
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        val host = hostElement ?: return
        if (paint.color != Color.Unspecified && paint.style != PaintingStyle.Stroke && paint.color.alpha <= 0.001f) {
            return
        }
        val circle = document.createElementNS("http://www.w3.org/2000/svg", "circle")
        val cx = center.x + currentTx
        val cy = center.y + currentTy
        circle.setAttribute("cx", cx.toString())
        circle.setAttribute("cy", cy.toString())
        circle.setAttribute("r", radius.toString())
        if (paint.style == PaintingStyle.Stroke) {
            circle.setAttribute("fill", "none")
            circle.setAttribute("stroke", paint.color.toCssRgba())
            val sw = if (paint.strokeWidth > 0f) paint.strokeWidth else 1f
            circle.setAttribute("stroke-width", sw.toString())
        } else {
            circle.setAttribute("fill", paint.color.toCssRgba())
            circle.setAttribute("stroke", "none")
        }
        if (paint.alpha < 1f) {
            circle.setAttribute("opacity", paint.alpha.toString())
        }
        targetContainer().appendChild(circle)
    }

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint,
    ) {
        val host = hostElement ?: return
        if (paint.color != Color.Unspecified && paint.style != PaintingStyle.Stroke && paint.color.alpha <= 0.001f) {
            return
        }
        val rx = (right - left) / 2f
        val ry = (bottom - top) / 2f
        val cx = left + rx + currentTx
        val cy = top + ry + currentTy

        if (kotlin.math.abs(sweepAngle) >= 360f) {
            val ellipse = document.createElementNS("http://www.w3.org/2000/svg", "ellipse")
            ellipse.setAttribute("cx", cx.toString())
            ellipse.setAttribute("cy", cy.toString())
            ellipse.setAttribute("rx", rx.toString())
            ellipse.setAttribute("ry", ry.toString())
            if (paint.style == PaintingStyle.Stroke) {
                ellipse.setAttribute("fill", "none")
                ellipse.setAttribute("stroke", paint.color.toCssRgba())
                val sw = if (paint.strokeWidth > 0f) paint.strokeWidth else 1f
                ellipse.setAttribute("stroke-width", sw.toString())
            } else {
                ellipse.setAttribute("fill", paint.color.toCssRgba())
                ellipse.setAttribute("stroke", "none")
            }
            targetContainer().appendChild(ellipse)
            return
        }

        val startRad = startAngle * kotlin.math.PI / 180.0
        val endRad = (startAngle + sweepAngle) * kotlin.math.PI / 180.0

        val x1 = cx + rx * kotlin.math.cos(startRad).toFloat()
        val y1 = cy + ry * kotlin.math.sin(startRad).toFloat()
        val x2 = cx + rx * kotlin.math.cos(endRad).toFloat()
        val y2 = cy + ry * kotlin.math.sin(endRad).toFloat()

        val largeArc = if (kotlin.math.abs(sweepAngle) > 180f) 1 else 0
        val sweep = if (sweepAngle > 0f) 1 else 0

        val d = if (useCenter) {
            "M $cx $cy L $x1 $y1 A $rx $ry 0 $largeArc $sweep $x2 $y2 Z"
        } else {
            "M $x1 $y1 A $rx $ry 0 $largeArc $sweep $x2 $y2"
        }

        val pathElement = document.createElementNS("http://www.w3.org/2000/svg", "path")
        pathElement.setAttribute("d", d)
        if (paint.style == PaintingStyle.Stroke) {
            pathElement.setAttribute("fill", "none")
            pathElement.setAttribute("stroke", paint.color.toCssRgba())
            val sw = if (paint.strokeWidth > 0f) paint.strokeWidth else 1f
            pathElement.setAttribute("stroke-width", sw.toString())
            val cap = when (paint.strokeCap) {
                StrokeCap.Round -> "round"
                StrokeCap.Square -> "square"
                else -> "butt"
            }
            pathElement.setAttribute("stroke-linecap", cap)
        } else {
            pathElement.setAttribute("fill", paint.color.toCssRgba())
            pathElement.setAttribute("stroke", "none")
        }
        targetContainer().appendChild(pathElement)
    }

    override fun drawPath(path: Path, paint: Paint) {
        val host = hostElement ?: return
        if (paint.color != Color.Unspecified && paint.style != PaintingStyle.Stroke && paint.color.alpha <= 0.001f) {
            return
        }
        val pathElement = document.createElementNS("http://www.w3.org/2000/svg", "path")
        val d = (path as? DomPath)?.toSvgPathData() ?: ""
        pathElement.setAttribute("d", d)
        if (currentTx != 0f || currentTy != 0f) {
            pathElement.setAttribute("transform", "translate($currentTx, $currentTy)")
        }
        if (paint.style == PaintingStyle.Stroke) {
            pathElement.setAttribute("fill", "none")
            pathElement.setAttribute("stroke", paint.color.toCssRgba())
            val sw = if (paint.strokeWidth > 0f) paint.strokeWidth else 1f
            pathElement.setAttribute("stroke-width", sw.toString())
            val cap = when (paint.strokeCap) {
                StrokeCap.Round -> "round"
                StrokeCap.Square -> "square"
                else -> "butt"
            }
            pathElement.setAttribute("stroke-linecap", cap)
            val join = when (paint.strokeJoin) {
                StrokeJoin.Round -> "round"
                StrokeJoin.Bevel -> "bevel"
                else -> "miter"
            }
            pathElement.setAttribute("stroke-linejoin", join)
        } else {
            pathElement.setAttribute("fill", paint.color.toCssRgba())
            pathElement.setAttribute("stroke", "none")
        }
        targetContainer().appendChild(pathElement)
    }

    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) {
        drawImageRect(
            image,
            IntOffset.Zero,
            IntSize(image.width, image.height),
            IntOffset(topLeftOffset.x.toInt(), topLeftOffset.y.toInt()),
            IntSize(image.width, image.height),
            paint
        )
    }

    override fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint,
    ) {
        val host = hostElement ?: return
        val url = (image as? DomImageBitmap)?.sourceUrl ?: return
        val img = (host.querySelector(":scope > img[data-luta-img]") as? HTMLImageElement)
            ?: (document.createElement("img") as HTMLImageElement).also {
                it.setAttribute("data-luta-img", "true")
                it.style.width = "100%"
                it.style.height = "100%"
                it.style.objectFit = "cover"
                host.appendChild(it)
            }

        if (img.src != url) {
            img.src = url
            img.setAttribute("decoding", "async")
            img.setAttribute("loading", "lazy")
        }
    }

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {}
    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {}
    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {}
    override fun enableZ() {}
    override fun disableZ() {}

    private fun getOrCreateSvg(host: HTMLElement): Element {
        val existing = host.querySelector(":scope > svg[data-luta-svg]")
        if (existing != null) {
            return existing
        }
        return (document.createElementNS("http://www.w3.org/2000/svg", "svg") as SVGElement).also {
            it.setAttribute("data-luta-svg", "true")
            it.style.position = "absolute"
            it.style.left = "0px"
            it.style.top = "0px"
            it.style.width = "100%"
            it.style.height = "100%"
            it.style.setProperty("pointer-events", "none")
            it.style.setProperty("overflow", "visible")
            host.appendChild(it)
        }
    }
}
