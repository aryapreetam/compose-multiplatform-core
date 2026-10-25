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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.DomComposeOwner
import kotlinx.browser.document
import org.w3c.dom.DocumentReadyState
import org.w3c.dom.HTMLElement
import org.w3c.dom.LOADING

@ExperimentalComposeUiApi
fun ComposeViewport(
    viewportContainerId: String? = null,
    configure: ComposeViewportConfiguration.() -> Unit = {},
    content: @Composable () -> Unit = { }
) {
    InternalComposeViewport(
        viewportContainerId = viewportContainerId,
        configure = configure,
        content = content
    )
}

@ExperimentalComposeUiApi
fun ComposeViewport(
    viewportContainer: HTMLElement,
    configure: ComposeViewportConfiguration.() -> Unit = {},
    content: @Composable () -> Unit = { }
) {
    val owner = DomComposeOwner(viewportContainer)
    owner.setContent(content)
}

internal fun InternalComposeViewport(
    viewportContainerId: String? = null,
    configure: ComposeViewportConfiguration.() -> Unit = {},
    content: @Composable () -> Unit = { }
) {
    onDomReady {
        val container = if (viewportContainerId != null) {
            document.getElementById(viewportContainerId) as? HTMLElement
                ?: error("failed to find element by viewportContainerId: '$viewportContainerId'")
        } else {
            document.body ?: error("failed to find <body> element")
        }
        ComposeViewport(container, configure, content)
    }
}

internal fun onSkikoReady(block: () -> Unit) {
    onDomReady(block)
}

internal fun onDomReady(block: () -> Unit) {
    if (document.readyState == DocumentReadyState.Companion.LOADING) {
        document.addEventListener("DOMContentLoaded", {
            block()
        })
    } else {
        block()
    }
}
