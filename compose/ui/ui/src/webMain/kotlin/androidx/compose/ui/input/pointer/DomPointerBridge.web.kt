/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement

internal object DomPointerBridge {
    fun bindNodeClick(element: HTMLElement, onClick: () -> Unit) {
        element.style.cursor = "pointer"
        element.addEventListener("pointerdown", { it.stopPropagation() })
        element.addEventListener("pointerup", { it.stopPropagation() })
        element.onclick = { event ->
            event.stopPropagation()
            onClick()
        }
    }

    fun bindNodeInput(element: HTMLInputElement, onInput: (String) -> Unit) {
        element.addEventListener("pointerdown", { it.stopPropagation() })
        element.addEventListener("pointerup", { it.stopPropagation() })
        val listener: (org.w3c.dom.events.Event) -> Unit = { event ->
            event.stopPropagation()
            onInput(element.value)
        }
        element.addEventListener("input", listener)
        element.addEventListener("change", listener)
    }
}
