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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.text.font

internal class DomFontLoader : PlatformFontLoader {
    override fun loadBlocking(font: Font): Any = "sans-serif"
    override suspend fun awaitLoad(font: Font): Any = "sans-serif"
    override val cacheKey: Any = "dom"
}

fun createFontFamilyResolver(): FontFamily.Resolver {
    return FontFamilyResolverImpl(
        DomFontLoader(),
        PlatformResolveInterceptor.Default
    )
}

internal actual class PlatformFontFamilyTypefaceAdapter actual constructor() : FontFamilyTypefaceAdapter {
    actual override fun resolve(
        typefaceRequest: TypefaceRequest,
        platformFontLoader: PlatformFontLoader,
        onAsyncCompletion: (TypefaceResult.Immutable) -> Unit,
        createDefaultTypeface: (TypefaceRequest) -> Any,
    ): TypefaceResult? {
        return TypefaceResult.Immutable("sans-serif")
    }
}

internal actual fun createFontFamilyResolver(
    fontResourceLoader: Font.ResourceLoader
): FontFamily.Resolver = createFontFamilyResolver()
