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

actual class PlatformTextStyle {
    actual val spanStyle: PlatformSpanStyle?
    actual val paragraphStyle: PlatformParagraphStyle?

    constructor(
        spanStyle: PlatformSpanStyle?,
        paragraphStyle: PlatformParagraphStyle?
    ) {
        this.spanStyle = spanStyle
        this.paragraphStyle = paragraphStyle
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlatformTextStyle) return false
        if (paragraphStyle != other.paragraphStyle) return false
        if (spanStyle != other.spanStyle) return false
        return true
    }

    override fun hashCode(): Int {
        var result = spanStyle?.hashCode() ?: 0
        result = 31 * result + (paragraphStyle?.hashCode() ?: 0)
        return result
    }
}

internal actual fun createPlatformTextStyle(
    spanStyle: PlatformSpanStyle?,
    paragraphStyle: PlatformParagraphStyle?
): PlatformTextStyle = PlatformTextStyle(spanStyle, paragraphStyle)

actual class PlatformParagraphStyle {
    actual companion object {
        actual val Default: PlatformParagraphStyle = PlatformParagraphStyle()
    }

    actual fun merge(other: PlatformParagraphStyle?): PlatformParagraphStyle {
        return other ?: this
    }

    override fun equals(other: Any?): Boolean = other is PlatformParagraphStyle
    override fun hashCode(): Int = 0
}

actual class PlatformSpanStyle {
    actual companion object {
        actual val Default: PlatformSpanStyle = PlatformSpanStyle()
    }

    actual fun merge(other: PlatformSpanStyle?): PlatformSpanStyle {
        return other ?: this
    }

    override fun equals(other: Any?): Boolean = other is PlatformSpanStyle
    override fun hashCode(): Int = 0
}

actual fun lerp(
    start: PlatformParagraphStyle,
    stop: PlatformParagraphStyle,
    fraction: Float
): PlatformParagraphStyle = start

actual fun lerp(
    start: PlatformSpanStyle,
    stop: PlatformSpanStyle,
    fraction: Float
): PlatformSpanStyle = start
