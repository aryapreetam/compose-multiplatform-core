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

package androidx.compose.ui.platform

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.retain.ForgetfulRetainedValuesStore
import androidx.compose.runtime.retain.RetainedValuesStore
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.layout.RootMeasurePolicy
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.draganddrop.DragAndDropManager
import androidx.compose.ui.draganddrop.DragAndDropNode
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.focus.FocusOwnerImpl
import androidx.compose.ui.focus.PlatformFocusOwner
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.DomCanvas
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.BrowserCursor
import androidx.compose.ui.input.pointer.EmptyPointerKeyboardModifiers
import androidx.compose.ui.input.pointer.InternalPointerEvent
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerInputEventData
import androidx.compose.ui.input.pointer.PointerInputEventProcessor
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.PositionCalculator
import androidx.compose.ui.input.pointer.composeButtons
import androidx.compose.ui.input.pointer.composeButton
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.modifier.ModifierLocalManager
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNodeDrawScope
import androidx.compose.ui.node.MeasureAndLayoutDelegate
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.OwnerSnapshotObserver
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.EmptySemanticsModifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.input.pointer.DomPointerBridge
import org.w3c.dom.HTMLInputElement
import androidx.compose.ui.spatial.RectManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.InteropView
import kotlin.coroutines.CoroutineContext
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.compose.ui.SessionMutex
import androidx.compose.runtime.snapshots.Snapshot
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.KeyboardEvent
import androidx.compose.ui.input.key.toComposeEvent

@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
internal class DomComposeOwner(
    val rootContainer: HTMLElement,
    override val density: Density = Density(1f),
    val isPopup: Boolean = false
) : Owner {
    var onPopupLayoutChange: ((width: Int, height: Int) -> Unit)? = null
    override val root: LayoutNode = LayoutNode().apply { measurePolicy = RootMeasurePolicy }
    private val _layoutNodes: MutableIntObjectMap<LayoutNode> = mutableIntObjectMapOf()
    override val layoutNodes: IntObjectMap<LayoutNode> get() = _layoutNodes

    override val sharedDrawScope: LayoutNodeDrawScope = LayoutNodeDrawScope()
    override val rootForTest: RootForTest
        get() = error("RootForTest is not supported in DomComposeOwner")

    override val hapticFeedBack: HapticFeedback = object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {}
    }

    override val inputModeManager: InputModeManager = object : InputModeManager {
        override val inputMode: InputMode = InputMode.Touch
        override fun requestInputMode(inputMode: InputMode): Boolean = true
    }

    @Suppress("DEPRECATION")
    override val clipboardManager: ClipboardManager = object : ClipboardManager {
        override fun getText(): AnnotatedString? = null
        override fun setText(annotatedString: AnnotatedString) {}
        override fun hasText(): Boolean = false
        override fun getClip(): ClipEntry? = null
        override fun setClip(clipEntry: ClipEntry?) {}
    }

    override val clipboard: Clipboard = object : Clipboard {
        override suspend fun getClipEntry(): ClipEntry? = null
        override suspend fun setClipEntry(clipEntry: ClipEntry?) {}
        override val nativeClipboard: NativeClipboard get() = error("No native clipboard")
    }

    override val accessibilityManager: AccessibilityManager = object : AccessibilityManager {
        override fun calculateRecommendedTimeoutMillis(
            originalTimeoutMillis: Long,
            containsIcons: Boolean,
            containsText: Boolean,
            containsControls: Boolean
        ): Long = originalTimeoutMillis
    }

    override val graphicsContext: GraphicsContext = object : GraphicsContext {
        override fun createGraphicsLayer(): GraphicsLayer = GraphicsLayer()
        override fun releaseGraphicsLayer(layer: GraphicsLayer) {}
    }

    override val textToolbar: TextToolbar = WebTextToolbar()

    @Suppress("DEPRECATION")
    override val autofillTree: AutofillTree = AutofillTree()
    @Suppress("DEPRECATION")
    override val autofill: Autofill? = null
    override val autofillManager: AutofillManager? = null

    private val domTextInputService = DomTextInputService(rootContainer)
    @Suppress("DEPRECATION")
    override val textInputService: TextInputService = TextInputService(domTextInputService)

    override val softwareKeyboardController: SoftwareKeyboardController =
        DelegatingSoftwareKeyboardController(textInputService)

    override val pointerIconService: PointerIconService = object : PointerIconService {
        private var currentIcon: PointerIcon = PointerIcon.Default

        override fun getIcon(): PointerIcon = currentIcon

        override fun setIcon(value: PointerIcon?) {
            val icon = value ?: PointerIcon.Default
            currentIcon = icon
            rootContainer.style.cursor = when (icon) {
                is BrowserCursor -> icon.id
                PointerIcon.Hand -> "pointer"
                PointerIcon.Text -> "text"
                PointerIcon.Crosshair -> "crosshair"
                else -> "default"
            }
        }

        override fun getStylusHoverIcon(): PointerIcon? = null
        override fun setStylusHoverIcon(value: PointerIcon?) {}
    }

    override val semanticsOwner: SemanticsOwner =
        SemanticsOwner(root, EmptySemanticsModifier(), _layoutNodes)

    private val platformFocusOwner = object : PlatformFocusOwner {
        override fun requestOwnerFocus(focusDirection: FocusDirection?, previouslyFocusedRect: Rect?): Boolean = true
        override fun clearOwnerFocus() {}
        override fun moveFocusInChildren(focusDirection: FocusDirection): Boolean = false
        override fun getEmbeddedViewFocusRect(): Rect? = null
    }

    override val focusOwner: FocusOwner = FocusOwnerImpl(platformFocusOwner, this)

    override val windowInfo: WindowInfo = object : WindowInfo {
        override val isWindowFocused: Boolean get() = true
        override val keyboardModifiers: PointerKeyboardModifiers get() = PointerKeyboardModifiers(0)
        override val containerSize: IntSize
            get() = IntSize(
                if (rootContainer.clientWidth > 0) rootContainer.clientWidth else window.innerWidth,
                if (rootContainer.clientHeight > 0) rootContainer.clientHeight else window.innerHeight
            )
    }

    override val retainedValuesStore: RetainedValuesStore = ForgetfulRetainedValuesStore
    override val rectManager: RectManager = RectManager()

    @Suppress("DEPRECATION")
    override val fontLoader: Font.ResourceLoader = object : Font.ResourceLoader {
        override fun load(font: Font): Any = font
    }

    override val fontFamilyResolver: FontFamily.Resolver = createFontFamilyResolver()
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override var showLayoutBounds: Boolean = false
    override val snapshotObserver: OwnerSnapshotObserver = OwnerSnapshotObserver { it.invoke() }
    override val modifierLocalManager: ModifierLocalManager = ModifierLocalManager(this)

    private val coroutineJob = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + coroutineJob + DomFrameClock
    private val coroutineScope = CoroutineScope(coroutineContext)

    override val dragAndDropManager: DragAndDropManager = object : DragAndDropManager {
        override val modifier: Modifier = Modifier
        override val isRequestDragAndDropTransferRequired: Boolean = false
        override fun requestDragAndDropTransfer(node: DragAndDropNode, offset: Offset) {}
        override fun registerTargetInterest(target: DragAndDropTarget) {}
        override fun isInterestedTarget(target: DragAndDropTarget): Boolean = false
    }

    override val measureIteration: Long get() = 1L

    override val viewConfiguration: ViewConfiguration = object : ViewConfiguration {
        override val longPressTimeoutMillis: Long get() = 500L
        override val doubleTapTimeoutMillis: Long get() = 300L
        override val doubleTapMinTimeMillis: Long get() = 40L
        override val touchSlop: Float get() = 18f
        override val minimumTouchTargetSize: DpSize get() = DpSize(48.dp, 48.dp)
    }

    // Maps each LayoutNode to its persistent DOM container
    private val nodeDomMap = mutableMapOf<LayoutNode, HTMLElement>()
    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(root)
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)
    private val endApplyChangesListeners = mutableListOf<() -> Unit>()

    private val recomposer = Recomposer(coroutineContext)
    private val composition = Composition(DefaultUiApplier(root), recomposer)
    private var isLayoutPassScheduled = false

    init {
        snapshotObserver.startObserving()
        root.attach(this)
        _layoutNodes[root.semanticsId] = root
        if (!isPopup) {
            rootContainer.style.position = "relative"
            rootContainer.style.setProperty("overflow", "hidden")
        }

        window.addEventListener("resize", {
            requestLayoutPass()
        })

        setupPointerEvents()
        setupKeyboardEvents()
    }

    private fun setupKeyboardEvents() {
        val onKey: (org.w3c.dom.events.Event) -> Unit = { e ->
            val ke = e as? KeyboardEvent
            if (ke != null) {
                val activeTag = document.activeElement?.tagName?.lowercase()
                if (activeTag != "input" && activeTag != "textarea") {
                    try {
                        val composeEvent = ke.toComposeEvent()
                        val consumed = focusOwner.dispatchKeyEvent(composeEvent)
                        if (consumed) {
                            ke.preventDefault()
                        }
                        Snapshot.sendApplyNotifications()
                        requestLayoutPass()
                    } catch (t: Throwable) {
                        // Prevent unattached virtual key nodes from interrupting the runtime
                    }
                }
            }
        }
        window.addEventListener("keydown", onKey)
        window.addEventListener("keyup", onKey)
    }

    private fun setupPointerEvents() {
        fun sendPointer(e: MouseEvent, eventType: PointerEventType) {
            val rect = rootContainer.getBoundingClientRect()
            val x = (e.clientX - rect.left).toFloat()
            val y = (e.clientY - rect.top).toFloat()
            val isDown = eventType == PointerEventType.Press || (eventType == PointerEventType.Move && e.buttons.toInt() > 0)
            val pointerData = PointerInputEventData(
                id = PointerId(0),
                uptime = window.performance.now().toLong(),
                positionOnScreen = Offset(x, y),
                position = Offset(x, y),
                down = isDown,
                pressure = if (isDown) 1f else 0f,
                type = PointerType.Mouse
            )
            val pointerInputEvent = PointerInputEvent(
                eventType = eventType,
                uptime = window.performance.now().toLong(),
                pointers = listOf(pointerData),
                buttons = e.composeButtons,
                nativeEvent = e,
                button = e.composeButton
            )
            pointerInputEventProcessor.process(pointerInputEvent, IdentityPositionCalculator)
            Snapshot.sendApplyNotifications()
            requestLayoutPass()
        }

        rootContainer.addEventListener("pointerdown", { e ->
            val me = e as? MouseEvent
            if (me != null) {
                var target = me.target as? HTMLElement
                while (target != null && target != rootContainer) {
                    if (target.tagName.equals("button", ignoreCase = true) ||
                        target.getAttribute("role") == "button"
                    ) {
                        DomPointerBridge.attachRipple(target, me.clientX.toDouble(), me.clientY.toDouble())
                        break
                    }
                    target = target.parentElement as? HTMLElement
                }
            }
            sendPointer(e as MouseEvent, PointerEventType.Press)
        })
        rootContainer.addEventListener("pointermove", { e -> sendPointer(e as MouseEvent, PointerEventType.Move) })
        rootContainer.addEventListener("pointerup", { e -> sendPointer(e as MouseEvent, PointerEventType.Release) })
    }

    fun setContent(content: @Composable () -> Unit) {
        GlobalSnapshotManager.ensureStarted()
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }
        composition.setContent {
            ProvideCommonCompositionLocals(
                owner = this,
                uriHandler = DomUriHandler,
                content = {
                    CompositionLocalProvider(
                        androidx.compose.ui.window.LocalActiveClipEventsTarget provides { null },
                        androidx.compose.ui.window.LocalDomRootContainer provides rootContainer,
                        LocalSystemTheme provides SystemTheme.Light,
                        content = content
                    )
                }
            )
        }
        requestLayoutPass()
    }

    fun requestLayoutPass() {
        if (isLayoutPassScheduled) return
        isLayoutPassScheduled = true
        window.requestAnimationFrame {
            isLayoutPassScheduled = false
            measureAndLayout(sendPointerUpdate = true)
        }
    }

    override fun measureAndLayout(sendPointerUpdate: Boolean) {
        val rootNode = root
        if (!rootNode.isAttached) return

        val w = if (rootContainer.clientWidth > 0 && !isPopup) rootContainer.clientWidth else window.innerWidth
        val h = if (rootContainer.clientHeight > 0 && !isPopup) rootContainer.clientHeight else window.innerHeight

        if (w > 0 && h > 0) {
            val constraints = if (isPopup) {
                Constraints(minWidth = 0, maxWidth = w, minHeight = 0, maxHeight = h)
            } else {
                Constraints.fixed(w, h)
            }
            measureAndLayoutDelegate.updateRootConstraints(constraints)
        }
        measureAndLayoutDelegate.measureAndLayout()
        syncNodeTreeToDom(rootNode)

        if (isPopup) {
            onPopupLayoutChange?.invoke(rootNode.width, rootNode.height)
        }
    }

    override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {
        measureAndLayoutDelegate.measureAndLayout(layoutNode, constraints)
        syncNodeTreeToDom(layoutNode)
    }

    override fun forceMeasureTheSubtree(layoutNode: LayoutNode, affectsLookahead: Boolean) {
        measureAndLayoutDelegate.forceMeasureTheSubtree(layoutNode, affectsLookahead)
    }

    private fun syncNodeTreeToDom(node: LayoutNode) {
        if (!node.isAttached || !node.isPlaced) return

        val config = node.semanticsConfiguration
        val semanticsNode = if (node.nodes.has(Nodes.Semantics)) {
            SemanticsNode(node, mergingEnabled = true)
        } else null
        val mergedConfig = semanticsNode?.config ?: config

        val role = mergedConfig?.getOrNull(SemanticsProperties.Role) ?: config?.getOrNull(SemanticsProperties.Role)
        val isButton = role == Role.Button
        val isCheckbox = role == Role.Checkbox
        val isClickable = isCheckbox || mergedConfig?.contains(SemanticsActions.OnClick) == true || config?.contains(SemanticsActions.OnClick) == true
        val isInput = config?.contains(SemanticsProperties.EditableText) == true ||
            config?.contains(SemanticsActions.SetText) == true

        val element = nodeDomMap.getOrPut(node) {
            val tagName = when {
                isButton -> "button"
                isInput -> "input"
                else -> "div"
            }
            val el = document.createElement(tagName) as HTMLElement
            el.setAttribute("data-luta-node", node.semanticsId.toString())
            el.style.position = "absolute"
            el.style.left = "0px"
            el.style.top = "0px"
            el.style.boxSizing = "border-box"
            if (isInput) {
                el.style.border = "none"
                el.style.outline = "none"
                el.style.background = "transparent"
                el.style.padding = "0px"
                el.style.font = "inherit"
                el.style.color = "inherit"
                el.style.zIndex = "1"
                el.style.setProperty("pointer-events", "auto")
                DomPointerBridge.bindNodeInput(el as HTMLInputElement) { newText ->
                    activeInputMethodRequest?.onEditCommand?.invoke(
                        listOf(
                            androidx.compose.ui.text.input.DeleteAllCommand(),
                            androidx.compose.ui.text.input.CommitTextCommand(newText, 1)
                        )
                    )
                    domTextInputService.onEditCommandCallback?.invoke(
                        listOf(
                            androidx.compose.ui.text.input.DeleteAllCommand(),
                            androidx.compose.ui.text.input.CommitTextCommand(newText, 1)
                        )
                    )
                    findSetTextAction(node)?.invoke(AnnotatedString(newText))
                    Snapshot.sendApplyNotifications()
                    requestLayoutPass()
                }
                (el as HTMLInputElement).onfocus = {
                    val clickAction = node.semanticsConfiguration?.getOrNull(SemanticsActions.OnClick)?.action
                        ?: mergedConfig?.getOrNull(SemanticsActions.OnClick)?.action
                    clickAction?.invoke()

                    val focusAction = node.semanticsConfiguration?.getOrNull(SemanticsActions.RequestFocus)?.action
                        ?: mergedConfig?.getOrNull(SemanticsActions.RequestFocus)?.action
                    focusAction?.invoke()
                    requestLayoutPass()
                }
            } else if (isButton) {
                el.setAttribute("type", "button")
                el.style.border = "none"
                el.style.outline = "none"
                el.style.background = "transparent"
                el.style.padding = "0px"
                el.style.cursor = "pointer"
                el.style.setProperty("overflow", "hidden")
                el.style.setProperty("pointer-events", "auto")
                DomPointerBridge.bindNodeClick(el) {
                    node.semanticsConfiguration?.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
                        ?: mergedConfig?.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
                    requestLayoutPass()
                }
            } else if (isClickable) {
                el.style.cursor = "pointer"
                el.style.setProperty("pointer-events", "auto")
                if (isCheckbox) {
                    el.setAttribute("role", "checkbox")
                    el.style.border = "none"
                    el.style.background = "transparent"
                }
                DomPointerBridge.bindNodeClick(el) {
                    node.semanticsConfiguration?.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
                        ?: mergedConfig?.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
                    (el.querySelector("input") as? HTMLInputElement)?.focus()
                    requestLayoutPass()
                }
            } else {
                el.style.setProperty("pointer-events", "none")
            }
            el
        }

        // Hierarchical scene-graph mounting: mount to parent LayoutNode DOM element
        var p = node.parent
        var parentEl: HTMLElement? = null
        while (p != null) {
            val candidate = nodeDomMap[p]
            if (candidate != null && candidate != rootContainer) {
                parentEl = candidate
                break
            }
            p = p.parent
        }
        val targetContainer = if (isPopup) rootContainer else (parentEl ?: rootContainer)
        if (element.parentElement != targetContainer) {
            targetContainer.appendChild(element)
        }

        if (isCheckbox) {
            element.setAttribute("role", "checkbox")
            val toggleState = mergedConfig?.getOrNull(SemanticsProperties.ToggleableState)
                ?: config?.getOrNull(SemanticsProperties.ToggleableState)
            val isChecked = toggleState == androidx.compose.ui.state.ToggleableState.On
            element.setAttribute("aria-checked", isChecked.toString())
            element.setAttribute("data-luta-checked", isChecked.toString())
            element.style.border = "none"
            element.style.outline = "none"
            element.style.background = "transparent"
        }

        if (isInput && element is HTMLInputElement) {
            val currentText = findEditableText(node)
            if (currentText != null && element.value != currentText && document.activeElement != element) {
                element.value = currentText
            }
        }

        val pos = node.outerCoordinator.positionInRoot()
        val basePos = if (targetContainer == rootContainer || p == null) {
            Offset.Zero
        } else {
            p.outerCoordinator.positionInRoot()
        }
        val localX = pos.x - basePos.x
        val localY = pos.y - basePos.y

        element.style.transform = "translate3d(${localX}px, ${localY}px, 0px)"
        element.style.width = "${node.width}px"
        element.style.height = "${node.height}px"

        val innerPos = node.innerCoordinator.positionInRoot() - pos
        val padLeft = innerPos.x.coerceAtLeast(0f)
        val padTop = innerPos.y.coerceAtLeast(0f)
        if (isInput && element is HTMLInputElement) {
            val hPad = if (padLeft > 0f) padLeft else 16f
            val vPad = if (padTop > 0f) padTop else 8f
            element.style.padding = "${vPad}px ${hPad}px ${vPad}px ${hPad}px"
        } else {
            element.style.padding = "0px"
        }

        // Clear previous frame SVG decorations so exited state layers (hover, focus) don't ghost
        val existingSvg = element.querySelector(":scope > svg[data-luta-svg]")
        if (existingSvg != null) {
            existingSvg.innerHTML = ""
        }

        // Execute drawing operations into the node's element
        try {
            val domCanvas = DomCanvas(element)
            val outerPos = node.outerCoordinator.position
            domCanvas.translate(-outerPos.x.toFloat(), -outerPos.y.toFloat())
            node.draw(domCanvas, null)
        } catch (t: Throwable) {
            // Draw will catch up once layout pass finalizes
        }

        if (isCheckbox) {
            element.style.border = "none"
            element.style.outline = "none"
            element.style.background = "transparent"
            element.style.setProperty("overflow", "visible")
        } else if (element.style.borderRadius.isNotEmpty()) {
            element.style.setProperty("overflow", "hidden")
        }

        // Leaf input invariant: native input handles its own text/editing; do not instantiate duplicate children
        if (isInput) {
            return
        }

        // Recurse children
        node.children.forEach { child ->
            syncNodeTreeToDom(child)
        }
    }

    private fun findEditableText(startNode: LayoutNode): String? {
        var curr: LayoutNode? = startNode
        while (curr != null) {
            val config = if (curr.nodes.has(Nodes.Semantics)) {
                SemanticsNode(curr, mergingEnabled = true).config
            } else {
                curr.semanticsConfiguration
            }
            val text = config?.getOrNull(SemanticsProperties.EditableText)?.text
            if (text != null) return text
            curr = curr.parent
        }
        fun searchChildren(n: LayoutNode): String? {
            for (child in n.children) {
                val config = if (child.nodes.has(Nodes.Semantics)) {
                    SemanticsNode(child, mergingEnabled = true).config
                } else {
                    child.semanticsConfiguration
                }
                val text = config?.getOrNull(SemanticsProperties.EditableText)?.text
                if (text != null) return text
                val found = searchChildren(child)
                if (found != null) return found
            }
            return null
        }
        return searchChildren(startNode)
    }

    private fun findSetTextAction(startNode: LayoutNode): ((AnnotatedString) -> Boolean)? {
        var curr: LayoutNode? = startNode
        while (curr != null) {
            val config = if (curr.nodes.has(Nodes.Semantics)) {
                SemanticsNode(curr, mergingEnabled = true).config
            } else {
                curr.semanticsConfiguration
            }
            config?.getOrNull(SemanticsActions.SetText)?.action?.let { return it }
            curr = curr.parent
        }
        fun searchChildren(n: LayoutNode): ((AnnotatedString) -> Boolean)? {
            for (child in n.children) {
                val config = if (child.nodes.has(Nodes.Semantics)) {
                    SemanticsNode(child, mergingEnabled = true).config
                } else {
                    child.semanticsConfiguration
                }
                config?.getOrNull(SemanticsActions.SetText)?.action?.let { return it }
                val found = searchChildren(child)
                if (found != null) return found
            }
            return null
        }
        return searchChildren(startNode)
    }

    override fun onRequestMeasure(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean,
        scheduleMeasureAndLayout: Boolean
    ) {
        if (affectsLookahead) {
            if (measureAndLayoutDelegate.requestLookaheadRemeasure(layoutNode, forceRequest) &&
                scheduleMeasureAndLayout
            ) {
                requestLayoutPass()
            }
        } else if (measureAndLayoutDelegate.requestRemeasure(layoutNode, forceRequest) &&
            scheduleMeasureAndLayout
        ) {
            requestLayoutPass()
        }
    }

    override fun onRequestRelayout(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean
    ) {
        if (affectsLookahead) {
            if (measureAndLayoutDelegate.requestLookaheadRelayout(layoutNode, forceRequest)) {
                requestLayoutPass()
            }
        } else if (measureAndLayoutDelegate.requestRelayout(layoutNode, forceRequest)) {
            requestLayoutPass()
        }
    }

    override fun requestOnPositionedCallback(layoutNode: LayoutNode) {
        measureAndLayoutDelegate.requestOnPositionedCallback(layoutNode)
    }

    override fun onPreAttach(node: LayoutNode) {
        _layoutNodes[node.semanticsId] = node
    }

    override fun onPostAttach(node: LayoutNode) {}

    override fun onDetach(node: LayoutNode) {
        _layoutNodes.remove(node.semanticsId)
        measureAndLayoutDelegate.onNodeDetached(node)
        snapshotObserver.clear(node)
        rectManager.remove(node)
        val el = nodeDomMap.remove(node)
        el?.remove()
    }

    override fun calculatePositionInWindow(localPosition: Offset): Offset = localPosition
    override fun calculateLocalPosition(positionInWindow: Offset): Offset = positionInWindow
    override fun requestAutofill(node: LayoutNode) {}

    override fun createLayer(
        drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
        invalidateParentLayer: () -> Unit,
        explicitLayer: GraphicsLayer?
    ): OwnedLayer {
        val transform = Matrix()
        val inverseTransform = Matrix()
        var currentElevation: Float = explicitLayer?.shadowElevation ?: 0f
        return object : OwnedLayer {
            override fun updateLayerProperties(scope: ReusableGraphicsLayerScope) {
                transform.reset()
                transform.scale(scope.scaleX, scope.scaleY)
                transform.rotateZ(scope.rotationZ)
                transform.translate(scope.translationX, scope.translationY)
                transform.invertTo(inverseTransform)
                currentElevation = if (scope.shadowElevation > 0f) scope.shadowElevation else (explicitLayer?.shadowElevation ?: 0f)
            }
            override fun isInLayer(position: Offset): Boolean = true
            override fun move(position: IntOffset) {}
            override fun resize(size: IntSize) {}
            override fun drawLayer(canvas: Canvas, parentLayer: GraphicsLayer?) {
                val domCanvas = canvas as? DomCanvas
                val host = domCanvas?.hostElement
                if (host != null && currentElevation > 0f) {
                    val dp = if (density.density > 0f) currentElevation / density.density else currentElevation
                    val y1 = (dp * 0.5f).toInt().coerceAtLeast(1)
                    val blur1 = (dp * 1.5f).toInt().coerceAtLeast(2)
                    val y2 = (dp * 0.8f).toInt().coerceAtLeast(1)
                    val blur2 = (dp * 2.5f).toInt().coerceAtLeast(3)
                    host.style.boxShadow = "0px ${y1}px ${blur1}px rgba(0, 0, 0, 0.2), 0px ${y2}px ${blur2}px rgba(0, 0, 0, 0.14)"
                }
                drawBlock(canvas, parentLayer)
            }
            override fun updateDisplayList() {}
            override fun invalidate() {
                invalidateParentLayer()
                requestLayoutPass()
            }
            override fun destroy() {}
            override fun mapOffset(point: Offset, inverse: Boolean): Offset = point
            override fun mapBounds(rect: MutableRect, inverse: Boolean) {}
            override fun reuseLayer(drawBlock: (Canvas, GraphicsLayer?) -> Unit, invalidateParentLayer: () -> Unit) {}
            override fun transform(matrix: Matrix) { matrix.timesAssign(transform) }
            override val underlyingMatrix: Matrix get() = transform
            override var frameRate: Float = 0f
            override var isFrameRateFromParent: Boolean = false
            override fun inverseTransform(matrix: Matrix) { matrix.timesAssign(inverseTransform) }
        }
    }

    override fun onSemanticsChange() {}
    override fun onLayoutChange(layoutNode: LayoutNode) {
        requestLayoutPass()
    }
    override fun onLayoutNodeDeactivated(layoutNode: LayoutNode) {}
    override fun onInteropViewLayoutChange(view: InteropView) {}

    override fun registerOnEndApplyChangesListener(listener: () -> Unit) {
        endApplyChangesListeners.add(listener)
    }

    override fun onEndApplyChanges() {
        val listeners = endApplyChangesListeners.toList()
        endApplyChangesListeners.clear()
        listeners.forEach { it() }
        requestLayoutPass()
    }

    override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
        measureAndLayoutDelegate.registerOnLayoutCompletedListener(listener)
    }

    private val textInputSessionMutex = SessionMutex<TextInputSession>()
    internal var activeInputMethodRequest: PlatformTextInputMethodRequest? = null

    private inner class TextInputSession(
        coroutineScope: CoroutineScope,
    ) : PlatformTextInputSessionScope, CoroutineScope by coroutineScope {
        private val innerSessionMutex = SessionMutex<Nothing?>()

        override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
            innerSessionMutex.withSessionCancellingPrevious<Nothing>(
                sessionInitializer = { null }
            ) {
                try {
                    activeInputMethodRequest = request
                    awaitCancellation()
                } finally {
                    if (activeInputMethodRequest === request) {
                        activeInputMethodRequest = null
                    }
                }
            }
        }
    }

    override suspend fun textInputSession(
        session: suspend PlatformTextInputSessionScope.() -> Nothing
    ): Nothing {
        textInputSessionMutex.withSessionCancellingPrevious<Nothing>(
            sessionInitializer = ::TextInputSession,
            session = session
        )
    }

    override fun screenToLocal(positionOnScreen: Offset): Offset = positionOnScreen
    override fun localToScreen(localPosition: Offset): Offset = localPosition

    fun dispose() {
        snapshotObserver.stopObserving()
        coroutineJob.cancel()
        nodeDomMap.values.forEach { it.remove() }
        nodeDomMap.clear()
        _layoutNodes.clear()
    }
}

internal object DomFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
        suspendCancellableCoroutine { continuation ->
            val id = window.requestAnimationFrame { time ->
                val nanos = (time * 1_000_000.0).toLong()
                val result = runCatching { onFrame(nanos) }
                continuation.resumeWith(result)
            }
            continuation.invokeOnCancellation {
                window.cancelAnimationFrame(id)
            }
        }
}

internal object DomUriHandler : UriHandler {
    override fun openUri(uri: String) {
        window.open(uri, "_blank")
    }
}

private object IdentityPositionCalculator : PositionCalculator {
    override fun screenToLocal(positionOnScreen: Offset): Offset = positionOnScreen
    override fun localToScreen(localPosition: Offset): Offset = localPosition
}
