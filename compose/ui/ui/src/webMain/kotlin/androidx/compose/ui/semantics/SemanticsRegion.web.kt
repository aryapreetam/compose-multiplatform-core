/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.semantics

import androidx.compose.ui.unit.IntRect

private class DomSemanticRegion : SemanticsRegion {
    private var rect = IntRect.Zero

    override fun set(rect: IntRect) {
        this.rect = rect
    }

    override val bounds: IntRect
        get() = rect

    override val isEmpty: Boolean
        get() = rect.isEmpty

    override fun intersect(region: SemanticsRegion): Boolean {
        val other = (region as DomSemanticRegion).rect
        rect = rect.intersect(other)
        return !rect.isEmpty
    }

    override fun difference(rect: IntRect): Boolean {
        return !this.rect.isEmpty
    }
}

internal actual fun SemanticsRegion(): SemanticsRegion = DomSemanticRegion()
