package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenElementNode(
    val index: Int,
    val text: String,
    val contentDescription: String,
    val className: String,
    val viewId: String,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val centerX: Float,
    val centerY: Float,
    val bounds: String
) {
    val label: String
        get() = text.ifBlank { contentDescription }.ifBlank { viewId }.ifBlank { className }
}

data class ScreenSnapshot(
    val activePackage: String = "None",
    val visibleTextSummary: String = "",
    val interactiveElements: List<ScreenElementNode> = emptyList(),
    val lastActionLog: String = "Idle",
    val timestamp: Long = System.currentTimeMillis()
)

data class PendingAppAutomationTask(
    val targetAppHint: String,
    val textToType: String,
    val buttonToTapAfter: String = "Send",
    val createdAt: Long = System.currentTimeMillis(),
    var attemptsRemaining: Int = 5
)

class AuraAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        Log.d(TAG, "Dua Fatima Screen Control Service connected.")
        captureScreenSnapshot("Screen Control Engine Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString().orEmpty()
        if (pkg.isNotBlank() && pkg != packageName) {
            captureScreenSnapshot("Window active: $pkg")
            checkAndRunPendingAutomation(pkg)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AuraAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        _isConnected.value = false
    }

    private fun checkAndRunPendingAutomation(activePkg: String) {
        val task = pendingTask ?: return
        if (System.currentTimeMillis() - task.createdAt > 30_000L || task.attemptsRemaining <= 0) {
            pendingTask = null
            return
        }
        // Ignore system launcher or our own app package
        if (activePkg == packageName || activePkg.contains("launcher", ignoreCase = true) || activePkg.contains("systemui", ignoreCase = true)) {
            return
        }

        // Schedule multi-stage retry loop so even if Chrome / ChatGPT / YouTube takes a few seconds to render its input box,
        // Dua Fatima focuses the input, types/pastes the prompt, and taps Send/Search automatically!
        pendingTask = null
        runAutomationAttempt(task, attemptNumber = 1)
    }

    private fun runAutomationAttempt(task: PendingAppAutomationTask, attemptNumber: Int) {
        val delayMs = if (attemptNumber == 1) 1100L else 1400L
        mainHandler.postDelayed({
            if (task.textToType.isNotBlank()) {
                val typed = typeTextIntoFocusedOrFirstInput(task.textToType)
                if (typed) {
                    if (task.buttonToTapAfter.isNotBlank()) {
                        mainHandler.postDelayed({
                            val sent = tapElementByText(task.buttonToTapAfter) ||
                                tapElementByText("Send") ||
                                tapElementByText("Search") ||
                                tapElementByText("Submit") ||
                                tapElementByText("Go")
                            if (sent) {
                                captureScreenSnapshot("Typed '${task.textToType.take(30)}' & tapped Send")
                            }
                        }, 650L)
                    }
                } else if (attemptNumber < 5) {
                    // Try tapping Search / Message / Ask box to reveal the editable input, then retry
                    tapElementByText("Ask anything") ||
                        tapElementByText("Message") ||
                        tapElementByText("Search") ||
                        tapElementByText("Type a message")
                    runAutomationAttempt(task, attemptNumber + 1)
                }
            }
        }, delayMs)
    }

    fun captureScreenSnapshot(actionNote: String = "Screen Scanned"): ScreenSnapshot {
        val elements = mutableListOf<ScreenElementNode>()
        val textSnippets = mutableListOf<String>()
        var pkg = "Unknown"

        val rootNodes = mutableListOf<AccessibilityNodeInfo>()
        rootInActiveWindow?.let {
            pkg = it.packageName?.toString() ?: "Unknown"
            rootNodes.add(it)
        }
        try {
            windows?.forEach { win ->
                win.root?.let { r ->
                    if (!rootNodes.contains(r)) rootNodes.add(r)
                }
            }
        } catch (_: Exception) {
        }

        if (rootNodes.isEmpty()) {
            val empty = _screenSnapshot.value.copy(lastActionLog = "$actionNote (Waiting for active window)")
            _screenSnapshot.value = empty
            return empty
        }

        for (root in rootNodes) {
            traverseNode(root, elements, textSnippets, 0)
        }

        val snapshot = ScreenSnapshot(
            activePackage = pkg,
            visibleTextSummary = textSnippets.distinct().joinToString(" • ").take(1600),
            interactiveElements = elements.take(80),
            lastActionLog = actionNote,
            timestamp = System.currentTimeMillis()
        )
        _screenSnapshot.value = snapshot
        return snapshot
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo?,
        elements: MutableList<ScreenElementNode>,
        texts: MutableList<String>,
        depth: Int
    ) {
        if (node == null || depth > 25) return

        val nodeText = node.text?.toString()?.trim().orEmpty()
        val contentDesc = node.contentDescription?.toString()?.trim().orEmpty()
        val className = node.className?.toString()?.substringAfterLast('.') ?: "View"
        val viewId = node.viewIdResourceName?.substringAfterLast('/') ?: ""

        if (nodeText.isNotEmpty()) texts.add(nodeText)
        else if (contentDesc.isNotEmpty()) texts.add(contentDesc)

        val looksEditable = node.isEditable ||
            className.contains("EditText", ignoreCase = true) ||
            className.contains("AutoComplete", ignoreCase = true)

        if (node.isClickable || looksEditable || node.isScrollable || nodeText.isNotEmpty() || contentDesc.isNotEmpty()) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (!rect.isEmpty && rect.width() > 4 && rect.height() > 4) {
                elements.add(
                    ScreenElementNode(
                        index = elements.size + 1,
                        text = nodeText,
                        contentDescription = contentDesc,
                        className = className,
                        viewId = viewId,
                        isClickable = node.isClickable,
                        isEditable = looksEditable,
                        isScrollable = node.isScrollable,
                        centerX = rect.exactCenterX(),
                        centerY = rect.exactCenterY(),
                        bounds = "[${rect.left},${rect.top} - ${rect.right},${rect.bottom}]"
                    )
                )
            }
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), elements, texts, depth + 1)
        }
    }

    /**
     * Taps a UI element matching the target label, content description, or view ID.
     */
    fun tapElementByText(target: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val matched = findMatchingNode(root, target)
        if (matched != null) {
            var clickableNode: AccessibilityNodeInfo? = matched
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            val clicked = clickableNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ?: matched.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clicked) {
                captureScreenSnapshot("Tapped '$target'")
                return true
            }
            val rect = Rect()
            matched.getBoundsInScreen(rect)
            if (!rect.isEmpty) {
                return tapCoordinates(rect.exactCenterX(), rect.exactCenterY(), "Gesture-tapped '$target'")
            }
        }
        captureScreenSnapshot("Could not locate '$target' on screen")
        return false
    }

    /**
     * Taps a specific element from the live inspected screen elements list by its 1-based index.
     */
    fun tapElementByNode(element: ScreenElementNode): Boolean {
        if (element.text.isNotBlank() && tapElementByText(element.text)) return true
        if (element.contentDescription.isNotBlank() && tapElementByText(element.contentDescription)) return true
        return tapCoordinates(element.centerX, element.centerY, "Tapped #${element.index} (${element.text.ifBlank { element.className }})")
    }

    /**
     * Types or appends text into the focused or first editable input field on screen across all active windows.
     * Also copies the text to Clipboard and uses ACTION_PASTE fallback for WebViews/ChatGPT/browsers.
     */
    fun typeTextIntoFocusedOrFirstInput(
        textToType: String,
        targetFieldHint: String = "",
        append: Boolean = false
    ): Boolean {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Dua Fatima Input", textToType))

        val roots = mutableListOf<AccessibilityNodeInfo>()
        rootInActiveWindow?.let { roots.add(it) }
        try {
            windows?.forEach { w -> w.root?.let { r -> if (!roots.contains(r)) roots.add(r) } }
        } catch (_: Exception) {
        }
        if (roots.isEmpty()) return false

        var targetNode: AccessibilityNodeInfo? = null
        for (root in roots) {
            targetNode = if (targetFieldHint.isNotBlank()) {
                findEditableNodeByHint(root, targetFieldHint) ?: findFirstEditableNode(root)
            } else {
                root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { isNodeEditableLike(it) }
                    ?: findFirstEditableNode(root)
                    ?: findWebComposerNode(root)
            }
            if (targetNode != null) break
        }

        if (targetNode != null) {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            val finalText = if (append) {
                val existing = targetNode.text?.toString().orEmpty()
                if (existing.isNotBlank()) "$existing $textToType" else textToType
            } else {
                textToType
            }

            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    finalText
                )
            }
            val setOk = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            val pasteOk = if (!setOk) {
                targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            } else {
                true
            }
            captureScreenSnapshot("Typed on screen: '${finalText.take(40)}'")
            return setOk || pasteOk
        }
        captureScreenSnapshot("Copied '${textToType.take(30)}' to clipboard (tap input box to paste)")
        return false
    }

    /**
     * Pastes current clipboard text directly into the focused or first editable input field on screen.
     */
    fun pasteClipboardIntoScreenInput(customText: String = ""): Boolean {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (customText.isNotBlank()) {
            clipboard?.setPrimaryClip(ClipData.newPlainText("Dua Fatima Paste", customText))
        }
        val clipText = customText.ifBlank {
            clipboard?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        }
        if (clipText.isNotBlank()) {
            return typeTextIntoFocusedOrFirstInput(clipText, append = true)
        }
        val root = rootInActiveWindow ?: return false
        val targetNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findFirstEditableNode(root)
            ?: findWebComposerNode(root)
        if (targetNode != null) {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val ok = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            captureScreenSnapshot("Pasted clipboard text into screen input")
            return ok
        }
        return false
    }

    /**
     * Removes / clears text from the currently focused or first editable input field on screen.
     */
    fun clearTextFromScreenInput(): Boolean {
        val root = rootInActiveWindow ?: return false
        val targetNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { isNodeEditableLike(it) }
            ?: findFirstEditableNode(root)
            ?: findWebComposerNode(root)

        if (targetNode != null) {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    ""
                )
            }
            val ok = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            captureScreenSnapshot("Cleared / removed text from screen input box")
            return ok
        }
        captureScreenSnapshot("No editable input field found to clear")
        return false
    }

    fun scrollScreen(forward: Boolean = true): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findFirstScrollableNode(root)
        if (scrollable != null) {
            val action = if (forward) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            val ok = scrollable.performAction(action)
            captureScreenSnapshot(if (forward) "Scrolled Down" else "Scrolled Up")
            return ok
        }
        // Fallback swipe gesture
        val dm = resources.displayMetrics
        val centerX = dm.widthPixels / 2f
        val startY = if (forward) dm.heightPixels * 0.72f else dm.heightPixels * 0.28f
        val endY = if (forward) dm.heightPixels * 0.28f else dm.heightPixels * 0.72f
        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 260L))
            .build()
        val dispatched = dispatchGesture(gesture, null, null)
        captureScreenSnapshot(if (forward) "Swiped Down" else "Swiped Up")
        return dispatched
    }

    fun tapCoordinates(x: Float, y: Float, label: String = "Tapped (${x.toInt()}, ${y.toInt()})"): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 90L))
            .build()
        val dispatched = dispatchGesture(gesture, null, null)
        captureScreenSnapshot(label)
        return dispatched
    }

    fun performNavigateHome(): Boolean {
        val ok = performGlobalAction(GLOBAL_ACTION_HOME)
        captureScreenSnapshot("Navigated Home")
        return ok
    }

    fun performNavigateBack(): Boolean {
        val ok = performGlobalAction(GLOBAL_ACTION_BACK)
        captureScreenSnapshot("Navigated Back")
        return ok
    }

    fun performOpenRecents(): Boolean {
        val ok = performGlobalAction(GLOBAL_ACTION_RECENTS)
        captureScreenSnapshot("Opened Recent Apps")
        return ok
    }

    fun performOpenNotifications(): Boolean {
        val ok = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        captureScreenSnapshot("Opened Notification Shade")
        return ok
    }

    private fun isNodeEditableLike(node: AccessibilityNodeInfo): Boolean {
        val cls = node.className?.toString().orEmpty()
        return node.isEditable ||
            cls.contains("EditText", ignoreCase = true) ||
            cls.contains("AutoComplete", ignoreCase = true)
    }

    private fun findMatchingNode(node: AccessibilityNodeInfo?, query: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val q = query.lowercase().trim()
        val text = node.text?.toString()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        val id = node.viewIdResourceName?.lowercase().orEmpty()
        if (text.contains(q) || desc.contains(q) || id.contains(q)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val childMatch = findMatchingNode(node.getChild(i), query)
            if (childMatch != null) return childMatch
        }
        return null
    }

    private fun findEditableNodeByHint(node: AccessibilityNodeInfo?, hint: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val q = hint.lowercase()
        val text = node.text?.toString()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        if (isNodeEditableLike(node) && (text.contains(q) || desc.contains(q))) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findEditableNodeByHint(node.getChild(i), hint)
            if (found != null) return found
        }
        return null
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (isNodeEditableLike(node)) return node
        for (i in 0 until node.childCount) {
            val found = findFirstEditableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun findWebComposerNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val text = node.text?.toString()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        val hint = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            node.hintText?.toString()?.lowercase().orEmpty()
        } else {
            ""
        }
        val combined = "$text $desc $hint"
        if (combined.contains("ask anything") ||
            combined.contains("message") ||
            combined.contains("prompt") ||
            combined.contains("search") ||
            combined.contains("type a") ||
            combined.contains("write")
        ) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findWebComposerNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun findFirstScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = findFirstScrollableNode(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    companion object {
        private const val TAG = "DuaScreenControl"

        @Volatile
        var instance: AuraAccessibilityService? = null
            private set

        @Volatile
        var pendingTask: PendingAppAutomationTask? = null

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _screenSnapshot = MutableStateFlow(ScreenSnapshot())
        val screenSnapshot: StateFlow<ScreenSnapshot> = _screenSnapshot.asStateFlow()
    }
}
