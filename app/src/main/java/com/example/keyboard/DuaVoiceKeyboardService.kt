package com.example.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Real Android InputMethodService ("Dua Fatima Voice & AI Keyboard")
 * Allows the user to type by voice (Urdu, Hindi, English), insert AI prompts,
 * add/remove text, and control input in ANY Android application.
 */
class DuaVoiceKeyboardService : InputMethodService() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var statusTextView: TextView? = null
    private var micButton: Button? = null
    private var isListening = false
    private var isUppercase = false
    private var currentLocale = "en-IN" // Supports English, Urdu, Hindi & Roman Urdu

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#140408"))
            setPadding(dp(6), dp(8), dp(6), dp(12))
        }

        // 1. Top Voice & AI Smart Action Bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(6))
        }

        micButton = createPillButton("🎤 Speak (UR/HI/EN)", "#FF1744", "#FFFFFF").apply {
            setOnClickListener { toggleVoiceDictation() }
        }
        topBar.addView(micButton, LinearLayout.LayoutParams(0, dp(38), 1.3f).apply { rightMargin = dp(6) })

        val langBtn = createPillButton("🌐 EN/UR/HI", "#2B0912", "#FF8A80").apply {
            setOnClickListener {
                currentLocale = when (currentLocale) {
                    "en-IN" -> "ur-PK"
                    "ur-PK" -> "hi-IN"
                    else -> "en-IN"
                }
                text = when (currentLocale) {
                    "ur-PK" -> "🌐 اردو"
                    "hi-IN" -> "🌐 हिन्दी"
                    else -> "🌐 EN/UR"
                }
                statusTextView?.text = "Dua Fatima Voice Keyboard • Locale: $currentLocale"
            }
        }
        topBar.addView(langBtn, LinearLayout.LayoutParams(0, dp(38), 0.9f).apply { rightMargin = dp(6) })

        val clearAllBtn = createPillButton("🗑 Clear All", "#2B0912", "#FF5252").apply {
            setOnClickListener { clearEntireInputField() }
        }
        topBar.addView(clearAllBtn, LinearLayout.LayoutParams(0, dp(38), 0.9f))

        root.addView(topBar)

        // 2. Quick AI Prompt & Clipboard Strip
        val scrollStrip = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val chipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(2), dp(2), dp(2), dp(6))
        }

        val quickChips = listOf(
            "✨ 10 Horror Stories Prompt" to "Write 10 bone-chilling horror stories with twist endings in Urdu/Hindi.",
            "✨ YouTube Script Prompt" to "Write a viral YouTube video script with hook, scenes, and image prompts.",
            "✨ Assalam-o-Alaikum" to "Assalam-o-Alaikum! Kaise hain aap? ",
            "📋 Paste Clipboard" to "__PASTE__",
            "⌨ Switch Keyboard" to "__SWITCH_IME__"
        )

        for ((label, payload) in quickChips) {
            val chip = createPillButton(label, "#240B13", "#FFD9DF").apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setOnClickListener {
                    when (payload) {
                        "__PASTE__" -> pasteFromClipboard()
                        "__SWITCH_IME__" -> {
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showInputMethodPicker()
                        }
                        else -> currentInputConnection?.commitText(payload, 1)
                    }
                }
            }
            chipRow.addView(
                chip,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(32)
                ).apply { rightMargin = dp(6) }
            )
        }
        scrollStrip.addView(chipRow)
        root.addView(scrollStrip)

        // Status subtitle line
        statusTextView = TextView(this).apply {
            text = "Dua Fatima AI Keyboard • Tap 🎤 to speak or tap keys to type"
            setTextColor(Color.parseColor("#D9A7B2"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(4))
        }
        root.addView(statusTextView)

        // 3. Number Row
        root.addView(createKeyRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")))

        // 4. QWERTY Rows
        root.addView(createKeyRow(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")))
        root.addView(createKeyRow(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")))

        // 5. Shift + Z..M + Backspace (Remove)
        val row3 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(2))
        }
        val shiftBtn = createKeyButton("⇧", "#380C18").apply {
            setOnClickListener {
                isUppercase = !isUppercase
            }
        }
        row3.addView(shiftBtn, LinearLayout.LayoutParams(0, dp(42), 1.3f).apply { setMargins(dp(2), 0, dp(2), 0) })

        for (ch in listOf("z", "x", "c", "v", "b", "n", "m")) {
            val btn = createKeyButton(ch, "#240B13").apply {
                setOnClickListener {
                    val out = if (isUppercase) ch.uppercase() else ch
                    currentInputConnection?.commitText(out, 1)
                }
            }
            row3.addView(btn, LinearLayout.LayoutParams(0, dp(42), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }

        val backspaceBtn = createKeyButton("⌫", "#420A17").apply {
            setOnClickListener {
                val ic = currentInputConnection ?: return@setOnClickListener
                val selected = ic.getSelectedText(0)
                if (!selected.isNullOrEmpty()) {
                    ic.commitText("", 1)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
        }
        row3.addView(backspaceBtn, LinearLayout.LayoutParams(0, dp(42), 1.4f).apply { setMargins(dp(2), 0, dp(2), 0) })
        root.addView(row3)

        // 6. Bottom Space & Enter/Send Row
        val row4 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(3), 0, dp(2))
        }

        val commaBtn = createKeyButton(",", "#240B13").apply {
            setOnClickListener { currentInputConnection?.commitText(",", 1) }
        }
        row4.addView(commaBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })

        val spaceBtn = createKeyButton("DUA FATIMA SPACE", "#2B0912").apply {
            setOnClickListener { currentInputConnection?.commitText(" ", 1) }
        }
        row4.addView(spaceBtn, LinearLayout.LayoutParams(0, dp(44), 4.5f).apply { setMargins(dp(2), 0, dp(2), 0) })

        val dotBtn = createKeyButton(".", "#240B13").apply {
            setOnClickListener { currentInputConnection?.commitText(".", 1) }
        }
        row4.addView(dotBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })

        val enterBtn = createKeyButton("Send ↵", "#FF1744").apply {
            setOnClickListener {
                val ic = currentInputConnection ?: return@setOnClickListener
                val handled = ic.performEditorAction(EditorInfo.IME_ACTION_SEND) ||
                    ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH) ||
                    ic.performEditorAction(EditorInfo.IME_ACTION_GO) ||
                    ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
                if (!handled) {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
            }
        }
        row4.addView(enterBtn, LinearLayout.LayoutParams(0, dp(44), 1.8f).apply { setMargins(dp(2), 0, dp(2), 0) })
        root.addView(row4)

        return root
    }

    private fun createKeyRow(keys: List<String>): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(2))
        }
        for (k in keys) {
            val btn = createKeyButton(k, "#240B13").apply {
                setOnClickListener {
                    val out = if (isUppercase) k.uppercase() else k
                    currentInputConnection?.commitText(out, 1)
                }
            }
            row.addView(btn, LinearLayout.LayoutParams(0, dp(42), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        return row
    }

    private fun createKeyButton(label: String, bgHex: String): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 0)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(8).toFloat()
                setColor(Color.parseColor(bgHex))
                setStroke(dp(1), Color.parseColor("#5C192B"))
            }
        }
    }

    private fun createPillButton(label: String, bgHex: String, textHex: String): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.parseColor(textHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), 0, dp(10), 0)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(18).toFloat()
                setColor(Color.parseColor(bgHex))
                setStroke(dp(1), Color.parseColor("#FF1744"))
            }
        }
    }

    private fun clearEntireInputField() {
        val ic = currentInputConnection ?: return
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        if (extracted != null && extracted.text != null) {
            val len = extracted.text.length
            ic.setSelection(0, len)
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(500, 500)
        }
        statusTextView?.text = "Cleared input field text"
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (text.isNotBlank()) {
            currentInputConnection?.commitText(text, 1)
            statusTextView?.text = "Pasted from clipboard"
        }
    }

    private fun toggleVoiceDictation() {
        if (isListening) {
            stopListening()
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusTextView?.text = "Speech recognition unavailable on this device"
            return
        }
        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        micButton?.text = "🔴 Listening..."
                        statusTextView?.text = "Speak now in Urdu, Hindi, or English..."
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        micButton?.text = "🎤 Speak (UR/HI/EN)"
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        micButton?.text = "🎤 Speak (UR/HI/EN)"
                        statusTextView?.text = "Tap 🎤 to speak again"
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        micButton?.text = "🎤 Speak (UR/HI/EN)"
                        val spoken = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (spoken.isNotBlank()) {
                            currentInputConnection?.commitText("$spoken ", 1)
                            statusTextView?.text = "Typed: \"$spoken\""
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (partial.isNotBlank()) {
                            statusTextView?.text = "Hearing: $partial..."
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLocale)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            statusTextView?.text = "Check Microphone permission in Dua Fatima app"
        }
    }

    private fun stopListening() {
        try {
            isListening = false
            micButton?.text = "🎤 Speak (UR/HI/EN)"
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
