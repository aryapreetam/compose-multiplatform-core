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

class DomCanvas(
    val hostElement: HTMLElement? = null
) : Canvas {
    override val drawsChildren: Boolean get() = false
    val nativeCanvas = NativeCanvas(hostElement)

    override fun save() {}
    override fun restore() {}
    override fun saveLayer(bounds: Rect, paint: Paint) {}
    override fun translate(dx: Float, dy: Float) {}
    override fun scale(sx: Float, sy: Float) {}
    override fun rotate(degrees: Float) {}
    override fun skew(sx: Float, sy: Float) {}
    override fun concat(matrix: Matrix) {}

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        hostElement?.style?.setProperty("overflow", "hidden")
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
        hostElement?.style?.setProperty("overflow", "hidden")
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        val host = hostElement ?: return
        val svg = getOrCreateSvg(host)
        val line = document.createElementNS("http://www.w3.org/2000/svg", "line")
        line.setAttribute("x1", p1.x.toString())
        line.setAttribute("y1", p1.y.toString())
        line.setAttribute("x2", p2.x.toString())
        line.setAttribute("y2", p2.y.toString())
        line.setAttribute("stroke", paint.color.toCssRgba())
        line.setAttribute("stroke-width", paint.strokeWidth.toString())
        svg.appendChild(line)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        val host = hostElement ?: return
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
        if (paint.color != Color.Unspecified) {
            if (paint.style == PaintingStyle.Stroke) {
                val strokeW = if (paint.strokeWidth > 0f) paint.strokeWidth else 1f
                host.style.border = "${strokeW}px solid ${paint.color.toCssRgba()}"
            } else {
                host.style.backgroundColor = paint.color.toCssRgba()
            }
        }
        host.style.borderRadius = "${radiusX}px"
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        val host = hostElement ?: return
        val svg = getOrCreateSvg(host)
        val ellipse = document.createElementNS("http://www.w3.org/2000/svg", "ellipse")
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        val rx = (right - left) / 2f
        val ry = (bottom - top) / 2f
        ellipse.setAttribute("cx", cx.toString())
        ellipse.setAttribute("cy", cy.toString())
        ellipse.setAttribute("rx", rx.toString())
        ellipse.setAttribute("ry", ry.toString())
        ellipse.setAttribute("fill", paint.color.toCssRgba())
        if (paint.alpha < 1f) {
            ellipse.setAttribute("opacity", paint.alpha.toString())
        }
        svg.appendChild(ellipse)
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        val host = hostElement ?: return
        val svg = getOrCreateSvg(host)
        val circle = document.createElementNS("http://www.w3.org/2000/svg", "circle")
        circle.setAttribute("cx", center.x.toString())
        circle.setAttribute("cy", center.y.toString())
        circle.setAttribute("r", radius.toString())
        circle.setAttribute("fill", paint.color.toCssRgba())
        if (paint.alpha < 1f) {
            circle.setAttribute("opacity", paint.alpha.toString())
        }
        svg.appendChild(circle)
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
    ) {}

    override fun drawPath(path: Path, paint: Paint) {
        val host = hostElement ?: return
        val svg = getOrCreateSvg(host)
        val pathElement = document.createElementNS("http://www.w3.org/2000/svg", "path")
        val d = (path as? DomPath)?.toSvgPathData() ?: ""
        pathElement.setAttribute("d", d)
        if (paint.style == PaintingStyle.Stroke) {
            pathElement.setAttribute("fill", "none")
            pathElement.setAttribute("stroke", paint.color.toCssRgba())
            pathElement.setAttribute("stroke-width", paint.strokeWidth.toString())
        } else {
            pathElement.setAttribute("fill", paint.color.toCssRgba())
        }
        svg.appendChild(pathElement)
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
            existing.innerHTML = ""
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
            host.appendChild(it)
        }
    }
}
