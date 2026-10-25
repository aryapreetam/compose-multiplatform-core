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

actual class NativePaint

actual fun Paint(): Paint = DomPaint()

class DomPaint : Paint {
    private val native = NativePaint()
    override fun asFrameworkPaint(): NativePaint = native

    override var alpha: Float = 1.0f
    override var isAntiAlias: Boolean = true
    override var color: Color = Color.Black
    override var blendMode: BlendMode = BlendMode.SrcOver
    override var style: PaintingStyle = PaintingStyle.Fill
    override var strokeWidth: Float = 0.0f
    override var strokeCap: StrokeCap = StrokeCap.Butt
    override var strokeJoin: StrokeJoin = StrokeJoin.Miter
    override var strokeMiterLimit: Float = 4.0f
    override var filterQuality: FilterQuality = FilterQuality.Low
    override var shader: Shader? = null
    override var colorFilter: ColorFilter? = null
    override var pathEffect: PathEffect? = null
}
