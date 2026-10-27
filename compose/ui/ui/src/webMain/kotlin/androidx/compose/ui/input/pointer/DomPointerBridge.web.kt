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

package androidx.compose.ui.input.pointer

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.MouseEvent
import kotlin.math.max

internal object DomPointerBridge {
    private var rippleStylesInjected = false
    private val lastRippleTimes = mutableMapOf<HTMLElement, Double>()

    private fun ensureRippleStyles() {
        if (rippleStylesInjected) return
        val head = document.head ?: return
        rippleStylesInjected = true
        val style = document.createElement("style")
        style.textContent = """
            @keyframes luta-ripple-kf {
                0% {
                    transform: translate(-50%, -50%) scale(0);
                    opacity: 0.16;
                }
                100% {
                    transform: translate(-50%, -50%) scale(2.5);
                    opacity: 0;
                }
            }
            .luta-ripple-effect {
                position: absolute;
                border-radius: 50%;
                pointer-events: none;
                background-color: currentColor;
                transform: translate(-50%, -50%) scale(0);
                animation: luta-ripple-kf 600ms cubic-bezier(0.2, 0, 0, 1) forwards;
            }
        """.trimIndent()
        head.appendChild(style)
    }

    fun attachRipple(element: HTMLElement, clientX: Double, clientY: Double) {
        val now = window.performance.now()
        val last = lastRippleTimes[element] ?: 0.0
        if (now - last < 80.0) return
        lastRippleTimes[element] = now

        ensureRippleStyles()
        if (element.style.borderRadius.isNotEmpty() && element.style.borderRadius != "0px") {
            element.style.setProperty("overflow", "hidden")
        }

        val rect = element.getBoundingClientRect()
        val x = if (clientX > 0.0) (clientX - rect.left) else (rect.width / 2.0)
        val y = if (clientY > 0.0) (clientY - rect.top) else (rect.height / 2.0)
        val dim = max(rect.width, rect.height)
        val size = if (dim > 0.0) dim else 48.0

        val ripple = document.createElement("span") as HTMLElement
        ripple.className = "luta-ripple-effect"
        ripple.style.width = "${size}px"
        ripple.style.height = "${size}px"
        ripple.style.left = "${x}px"
        ripple.style.top = "${y}px"

        element.appendChild(ripple)
        ripple.addEventListener("animationend", { ripple.remove() })
        window.setTimeout({
            ripple.remove()
            null
        }, 650)
    }

    fun bindNodeClick(element: HTMLElement, onClick: () -> Unit) {
        element.style.cursor = "pointer"
        element.onclick = { event ->
            onClick()
        }
    }

    fun bindContinuousDrag(
        element: HTMLElement,
        onDrag: (fraction: Float) -> Unit
    ) {
        element.style.cursor = "pointer"
        var isDragging = false

        val computeFraction = { clientX: Double ->
            val rect = element.getBoundingClientRect()
            val w = rect.width
            if (w > 0.0) {
                val frac = ((clientX - rect.left) / w).coerceIn(0.0, 1.0).toFloat()
                onDrag(frac)
            }
        }

        element.addEventListener("pointerdown", { e ->
            val me = e as? MouseEvent ?: return@addEventListener
            isDragging = true
            computeFraction(me.clientX.toDouble())
        })

        window.addEventListener("pointermove", { e ->
            if (isDragging) {
                val me = e as? MouseEvent ?: return@addEventListener
                computeFraction(me.clientX.toDouble())
            }
        })

        val endDrag: (org.w3c.dom.events.Event) -> Unit = {
            if (isDragging) {
                isDragging = false
            }
        }
        window.addEventListener("pointerup", endDrag)
        window.addEventListener("pointercancel", endDrag)
    }

    fun bindNodeInput(element: HTMLInputElement, onInput: (String) -> Unit) {
        element.addEventListener("pointerdown", { it.stopPropagation() })
        element.addEventListener("pointerup", { it.stopPropagation() })
        var isComposing = false
        element.addEventListener("compositionstart", {
            isComposing = true
        })
        element.addEventListener("compositionend", {
            isComposing = false
            onInput(element.value)
        })
        val listener: (org.w3c.dom.events.Event) -> Unit = { event ->
            event.stopPropagation()
            if (!isComposing) {
                onInput(element.value)
            }
        }
        element.addEventListener("input", listener)
        element.addEventListener("change", listener)
        installInputValueTracker(element)
    }
}

private fun installInputValueTracker(element: HTMLInputElement): Unit =
    js(
        """{
        if (!element.__luta_tracked) {
            element.__luta_tracked = true;
            var proto = HTMLInputElement.prototype;
            var desc = Object.getOwnPropertyDescriptor(proto, 'value');
            if (desc && desc.set) {
                Object.defineProperty(element, 'value', {
                    get: function() { return desc.get.call(this); },
                    set: function(val) {
                        if (val !== desc.get.call(this)) {
                            desc.set.call(this, val);
                            element.dispatchEvent(new Event('input', { bubbles: true }));
                        }
                    }
                });
            }
        }
    }"""
    )
