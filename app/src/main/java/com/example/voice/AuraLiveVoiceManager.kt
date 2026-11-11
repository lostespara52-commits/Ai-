package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class AuraLiveVoiceManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onFinalTranscript: (String) -> Unit,
    private val onPartialTranscript: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _continuousLiveMode = MutableStateFlow(false)
    val continuousLiveMode: StateFlow<Boolean> = _continuousLiveMode.asStateFlow()

    // Default to Urdu/Hindi ("ur-PK") so voice recognition & TTS speak natural Urdu/Hindi out of the box
    private val _selectedLocaleTag = MutableStateFlow("ur-PK")
    val selectedLocaleTag: StateFlow<String> = _selectedLocaleTag.asStateFlow()

    private val _voiceAmplitude = MutableStateFlow(0f)
    val voiceAmplitude: StateFlow<Float> = _voiceAmplitude.asStateFlow()

    private var restartJob: Job? = null
    private var isCurrentlyListening = false
    private var isCurrentlySpeaking = false
    private var lastPartialTranscript = ""

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    configureNaturalUrduHindiVoice()
                    tts?.setSpeechRate(1.02f)
                    tts?.setPitch(1.04f)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isCurrentlySpeaking = true
                            onSpeakingStateChanged(true)
                        }

                        override fun onDone(utteranceId: String?) {
                            isCurrentlySpeaking = false
                            onSpeakingStateChanged(false)
                            if (_continuousLiveMode.value) {
                                scheduleAutoListen(180L)
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            isCurrentlySpeaking = false
                            onSpeakingStateChanged(false)
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS initialization error", e)
        }
    }

    /**
     * Selects the best available Urdu or Hindi voice on the Android device so Dua Fatima
     * never sounds like an English robot reading Roman Urdu words.
     */
    private fun configureNaturalUrduHindiVoice(): Locale {
        val engine = tts ?: return Locale.forLanguageTag("hi-IN")
        val hiLocale = Locale.forLanguageTag("hi-IN")
        val urPkLocale = Locale.forLanguageTag("ur-PK")
        val urInLocale = Locale.forLanguageTag("ur-IN")

        // Prefer hi-IN for phonetic Devanagari/Urdu-Hindi pronunciation because hi-IN is pre-installed
        // on virtually all Android devices and pronounces phonetic Urdu/Hindi words naturally.
        val hiStatus = engine.isLanguageAvailable(hiLocale)
        if (hiStatus >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = hiLocale
            return hiLocale
        }
        val urStatus = engine.isLanguageAvailable(urPkLocale)
        if (urStatus >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = urPkLocale
            return urPkLocale
        }
        val urInStatus = engine.isLanguageAvailable(urInLocale)
        if (urInStatus >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = urInLocale
            return urInLocale
        }
        engine.language = hiLocale
        return hiLocale
    }

    fun setVoiceLanguage(localeTag: String) {
        _selectedLocaleTag.value = localeTag
        if (isTtsReady) {
            configureNaturalUrduHindiVoice()
        }
    }

    fun setContinuousLiveMode(enabled: Boolean) {
        _continuousLiveMode.value = enabled
        if (enabled) {
            startListening()
        } else {
            stopListening()
            stopSpeaking()
        }
    }

    fun startListening() {
        scope.launch(Dispatchers.Main) {
            if (isCurrentlySpeaking) {
                stopSpeaking()
            }

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w(TAG, "Speech recognition not available on this device.")
                onListeningStateChanged(false)
                return@launch
            }

            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                    speechRecognizer?.setRecognitionListener(createRecognitionListener())
                }

                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    // Use selected locale but also allow multilingual Urdu/Hindi/English recognition
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, _selectedLocaleTag.value)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, _selectedLocaleTag.value)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 650L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 480L)
                }

                lastPartialTranscript = ""
                isCurrentlyListening = true
                onListeningStateChanged(true)
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                isCurrentlyListening = false
                onListeningStateChanged(false)
            }
        }
    }

    fun stopListening() {
        restartJob?.cancel()
        scope.launch(Dispatchers.Main) {
            try {
                isCurrentlyListening = false
                _voiceAmplitude.value = 0f
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                onListeningStateChanged(false)
            } catch (_: Exception) {
            }
        }
    }

    fun speakResponse(text: String) {
        if (!isTtsReady || text.isBlank()) return
        val cleanText = text
            .replace(Regex("[*#_`~>•✦⚙ℹ]"), " ")
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            .take(700)

        // Check if text already contains Urdu script
        val hasUrduScript = cleanText.any { it in '\u0600'..'\u06FF' }
        val hasHindiScript = cleanText.any { it in '\u0900'..'\u097F' }

        val spokenText: String
        if (hasUrduScript) {
            val urLocale = Locale.forLanguageTag("ur-PK")
            if ((tts?.isLanguageAvailable(urLocale) ?: -1) >= TextToSpeech.LANG_AVAILABLE) {
                tts?.language = urLocale
                spokenText = cleanText
            } else {
                tts?.language = Locale.forLanguageTag("hi-IN")
                spokenText = cleanText
            }
        } else if (hasHindiScript) {
            tts?.language = Locale.forLanguageTag("hi-IN")
            spokenText = cleanText
        } else {
            // Convert Roman Urdu / Hinglish to natural Devanagari Hindi-Urdu phonetics
            // so Android's hi-IN TTS engine pronounces it with 100% authentic Urdu/Hindi accent instead of English!
            tts?.language = Locale.forLanguageTag("hi-IN")
            spokenText = convertRomanUrduToNaturalHindiUrduPhonetics(cleanText)
        }

        val utteranceId = "dua_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    /**
     * Converts Roman Urdu / Hinglish words into Devanagari Hindi-Urdu phonetics so that
     * Android's pre-installed hi-IN TTS voice speaks with a 100% natural Pakistani/Indian Urdu-Hindi accent
     * instead of reading Roman words ("main", "hun", "karo") with an English accent.
     */
    private fun convertRomanUrduToNaturalHindiUrduPhonetics(input: String): String {
        var s = input
        // Phrase-level replacements first
        val phraseMap = listOf(
            Regex("(?i)assalam[-\\s]*o[-\\s]*alaikum") to "अस्सलामु अलैकुम",
            Regex("(?i)assalamu\\s*alaikum") to "अस्सलामु अलैकुम",
            Regex("(?i)walaikum\\s*assalam") to "वालेकुम अस्सलाम",
            Regex("(?i)alhamdulillah") to "अलहम्दुलिल्लाह",
            Regex("(?i)inshallah") to "इंशाअल्लाह",
            Regex("(?i)dua\\s*fatima") to "दुआ फ़ातिमा",
            Regex("(?i)chatgpt") to "चैट जी पी टी",
            Regex("(?i)whatsapp") to "व्हाट्सएप",
            Regex("(?i)youtube") to "यूट्यूब",
            Regex("(?i)instagram") to "इंस्टाग्राम",
            Regex("(?i)chrome") to "क्रोम",
            Regex("(?i)google\\s*maps") to "गूगल मैप्स",
            Regex("(?i)flashlight") to "फ़्लैशलाइट"
        )
        for ((regex, rep) in phraseMap) {
            s = s.replace(regex, rep)
        }

        val wordMap = mapOf(
            "main" to "मैं",
            "mein" to "में",
            "hun" to "हूँ",
            "hoon" to "हूँ",
            "hai" to "है",
            "hain" to "हैं",
            "ho" to "हो",
            "aap" to "आप",
            "aapka" to "आपका",
            "aapki" to "आपकी",
            "aapke" to "आपके",
            "apni" to "अपनी",
            "apna" to "अपना",
            "mera" to "मेरा",
            "meri" to "मेरी",
            "mere" to "मेरे",
            "hum" to "हम",
            "tum" to "तुम",
            "kya" to "क्या",
            "kaisi" to "कैसी",
            "kaise" to "कैसे",
            "kahan" to "कहाँ",
            "kab" to "कब",
            "kyun" to "क्यों",
            "kaun" to "कौन",
            "kaunsa" to "कौनसा",
            "abhi" to "अभी",
            "aaj" to "आज",
            "kal" to "कल",
            "subah" to "सुबह",
            "shaam" to "शाम",
            "raat" to "रात",
            "waqt" to "वक़्त",
            "tarikh" to "तारीख़",
            "ji" to "जी",
            "haan" to "हाँ",
            "nahi" to "नहीं",
            "nahin" to "नहीं",
            "bilkul" to "बिल्कुल",
            "foran" to "फ़ौरन",
            "theek" to "ठीक",
            "achha" to "अच्छा",
            "bohat" to "बहुत",
            "bahut" to "बहुत",
            "shukriya" to "शुक्रिया",
            "madad" to "मदद",
            "hazir" to "हाज़िर",
            "tayyar" to "तैयार",
            "hukam" to "हुक्म",
            "karein" to "करें",
            "karo" to "करो",
            "kar" to "कर",
            "rahi" to "रही",
            "raha" to "रहा",
            "rahe" to "रहे",
            "diya" to "दिया",
            "di" to "दी",
            "liya" to "लिया",
            "khol" to "खोल",
            "kholo" to "खोलो",
            "khola" to "खोला",
            "band" to "बंद",
            "chala" to "चला",
            "chalao" to "चलाओ",
            "likh" to "लिख",
            "likho" to "लिखो",
            "likha" to "लिखा",
            "padho" to "पढ़ो",
            "dekho" to "देखो",
            "sun" to "सुन",
            "suno" to "सुनो",
            "bolo" to "बोलो",
            "bol" to "बोल",
            "batayein" to "बताएँ",
            "batao" to "बताओ",
            "bata" to "बता",
            "jawab" to "जवाब",
            "sawal" to "सवाल",
            "awaaz" to "आवाज़",
            "awaz" to "आवाज़",
            "saaf" to "साफ़",
            "aa" to "आ",
            "jao" to "जाओ",
            "gaye" to "गए",
            "upar" to "ऊपर",
            "neeche" to "नीचे",
            "peeche" to "पीछे",
            "aage" to "आगे",
            "par" to "पर",
            "pe" to "पे",
            "se" to "से",
            "ko" to "को",
            "ka" to "का",
            "ki" to "की",
            "ke" to "के",
            "aur" to "और",
            "ya" to "या",
            "bhi" to "भी",
            "hi" to "ही",
            "to" to "तो",
            "jo" to "जो",
            "woh" to "वह",
            "yeh" to "यह",
            "is" to "इस",
            "us" to "उस",
            "iske" to "इसके",
            "andar" to "अंदर",
            "bahar" to "बाहर",
            "sara" to "सारा",
            "sare" to "सारे",
            "har" to "हर",
            "koi" to "कोई",
            "kisi" to "किसी",
            "kaam" to "काम",
            "namaste" to "नमस्ते",
            "salam" to "सलाम",
            "mila" to "मिला",
            "milane" to "मिलाने",
            "bhej" to "भेज",
            "bhejo" to "भेजो",
            "mita" to "मिटा",
            "hata" to "हटा",
            "dabao" to "दबाओ",
            "dabayein" to "दबाएँ",
            "rasta" to "रास्ता",
            "jagah" to "जगह"
        )

        return s.split(" ").joinToString(" ") { token ->
            val leadingPunct = token.takeWhile { !it.isLetterOrDigit() }
            val trailingPunct = token.takeLastWhile { !it.isLetterOrDigit() }
            val core = token.removePrefix(leadingPunct).removeSuffix(trailingPunct)
            val mapped = wordMap[core.lowercase()]
            if (mapped != null) {
                "$leadingPunct$mapped$trailingPunct"
            } else {
                token
            }
        }
    }

    fun stopSpeaking() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
            isCurrentlySpeaking = false
            onSpeakingStateChanged(false)
        } catch (_: Exception) {
        }
    }

    fun forceStopAll() {
        _continuousLiveMode.value = false
        restartJob?.cancel()
        stopListening()
        stopSpeaking()
    }

    fun shutdown() {
        forceStopAll()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {
        }
    }

    private fun scheduleAutoListen(delayMs: Long) {
        restartJob?.cancel()
        restartJob = scope.launch(Dispatchers.Main) {
            delay(delayMs)
            if (_continuousLiveMode.value && !isCurrentlySpeaking && !isCurrentlyListening) {
                startListening()
            }
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isCurrentlyListening = true
            onListeningStateChanged(true)
        }

        override fun onBeginningOfSpeech() {
            if (isCurrentlySpeaking) {
                stopSpeaking()
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
            _voiceAmplitude.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isCurrentlyListening = false
            _voiceAmplitude.value = 0f
            onListeningStateChanged(false)
        }

        override fun onError(error: Int) {
            isCurrentlyListening = false
            _voiceAmplitude.value = 0f
            onListeningStateChanged(false)
            val fallback = lastPartialTranscript.trim()
            lastPartialTranscript = ""
            if (fallback.isNotBlank()) {
                onFinalTranscript(fallback)
            } else if (_continuousLiveMode.value && !isCurrentlySpeaking) {
                scheduleAutoListen(550L)
            }
        }

        override fun onResults(results: Bundle?) {
            isCurrentlyListening = false
            _voiceAmplitude.value = 0f
            onListeningStateChanged(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val transcript = matches?.firstOrNull()?.trim().orEmpty().ifBlank { lastPartialTranscript.trim() }
            lastPartialTranscript = ""
            if (transcript.isNotBlank()) {
                onFinalTranscript(transcript)
            } else if (_continuousLiveMode.value) {
                scheduleAutoListen(350L)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (partial.isNotBlank()) {
                lastPartialTranscript = partial
                onPartialTranscript(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    companion object {
        private const val TAG = "AuraLiveVoice"
    }
}
