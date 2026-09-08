package com.example.services

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoicePreset(
    val id: String,
    val titleRu: String,
    val gender: String, // "MALE" or "FEMALE"
    val pitch: Float,
    val speed: Float,
    val voiceNameHint: String,
    val description: String
)

class VoiceService private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("techmate_voice_prefs", Context.MODE_PRIVATE)

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentAssistantName = MutableStateFlow(prefs.getString("ai_name", "TechMate") ?: "TechMate")
    val currentAssistantName: StateFlow<String> = _currentAssistantName.asStateFlow()

    private val _currentGender = MutableStateFlow(prefs.getString("ai_gender", "FEMALE") ?: "FEMALE")
    val currentGender: StateFlow<String> = _currentGender.asStateFlow()

    private val _currentPitch = MutableStateFlow(prefs.getFloat("ai_pitch", 1.10f))
    val currentPitch: StateFlow<Float> = _currentPitch.asStateFlow()

    private val _currentSpeed = MutableStateFlow(prefs.getFloat("ai_speed", 1.05f))
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()

    private val ruLocale = Locale("ru", "RU")

    val availablePresets = listOf(
        VoicePreset(
            id = "female_assistant",
            titleRu = "👩 Ассистент (Алиса / Siri Style)",
            gender = "FEMALE",
            pitch = 1.15f,
            speed = 1.05f,
            voiceNameHint = "female,efc,natural",
            description = "Звонкий, приятный и живой женский голос"
        ),
        VoicePreset(
            id = "male_engineer",
            titleRu = "👨 Инженер (Джарвис / Pro Style)",
            gender = "MALE",
            pitch = 0.90f,
            speed = 1.02f,
            voiceNameHint = "male,dfz,premium",
            description = "Уверенный, глубокий мужской голос инженера-схемотехника"
        ),
        VoicePreset(
            id = "female_speed",
            titleRu = "⚡ Быстрый помощник (Женский)",
            gender = "FEMALE",
            pitch = 1.10f,
            speed = 1.20f,
            voiceNameHint = "female,neural",
            description = "Ускоренная подача информации для быстрой диагностики"
        ),
        VoicePreset(
            id = "male_narrator",
            titleRu = "🎙 Диктор лаборатории (Мужской)",
            gender = "MALE",
            pitch = 0.85f,
            speed = 0.95f,
            voiceNameHint = "male,deep",
            description = "Размеренный студийный голос для чтения пошаговых гайдов"
        )
    )

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    tts?.let { engine ->
                        engine.language = ruLocale
                        applyVoiceSettings(engine)
                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _isSpeaking.value = true
                            }

                            override fun onDone(utteranceId: String?) {
                                _isSpeaking.value = false
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                _isSpeaking.value = false
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                _isSpeaking.value = false
                            }
                        })
                    }
                } else {
                    Log.w("VoiceService", "TextToSpeech init failed with status: $status")
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceService", "Error initializing TTS: ${e.message}")
        }
    }

    private fun applyVoiceSettings(engine: TextToSpeech) {
        val isMale = _currentGender.value.equals("MALE", ignoreCase = true)
        val pitch = _currentPitch.value
        val speed = _currentSpeed.value

        engine.setPitch(pitch)
        engine.setSpeechRate(speed)

        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val matchedVoice = voices.find { voice ->
                    voice.locale.language == ruLocale.language &&
                    (if (isMale) voice.name.contains("male", ignoreCase = true) || voice.name.contains("dfz", ignoreCase = true) || voice.name.contains("x-dfz", ignoreCase = true)
                     else voice.name.contains("female", ignoreCase = true) || voice.name.contains("efc", ignoreCase = true) || voice.name.contains("x-efc", ignoreCase = true)) &&
                    (voice.name.contains("natural", ignoreCase = true) ||
                     voice.name.contains("premium", ignoreCase = true) ||
                     voice.name.contains("neural", ignoreCase = true) ||
                     voice.quality >= Voice.QUALITY_HIGH)
                } ?: voices.find { voice ->
                    voice.locale.language == ruLocale.language &&
                    (if (isMale) voice.name.contains("male", ignoreCase = true)
                     else voice.name.contains("female", ignoreCase = true))
                } ?: voices.find { it.locale.language == ruLocale.language }

                if (matchedVoice != null) {
                    engine.voice = matchedVoice
                }
            }
        } catch (e: Exception) {
            Log.w("VoiceService", "Could not set custom voice: ${e.message}")
        }
    }

    fun saveVoicePreferences(
        name: String,
        gender: String,
        pitch: Float = _currentPitch.value,
        speed: Float = _currentSpeed.value
    ) {
        val cleanName = name.trim().ifBlank { "TechMate" }
        _currentAssistantName.value = cleanName
        _currentGender.value = gender
        _currentPitch.value = pitch
        _currentSpeed.value = speed

        prefs.edit()
            .putString("ai_name", cleanName)
            .putString("ai_gender", gender)
            .putFloat("ai_pitch", pitch)
            .putFloat("ai_speed", speed)
            .apply()

        tts?.let { applyVoiceSettings(it) }
    }

    fun applyPreset(preset: VoicePreset) {
        _currentGender.value = preset.gender
        _currentPitch.value = preset.pitch
        _currentSpeed.value = preset.speed

        prefs.edit()
            .putString("ai_gender", preset.gender)
            .putFloat("ai_pitch", preset.pitch)
            .putFloat("ai_speed", preset.speed)
            .apply()

        tts?.let { applyVoiceSettings(it) }
    }

    private val openAiVoiceService = OpenAiVoiceService.getInstance(context)

    fun speak(text: String, utteranceId: String = "tts_${System.currentTimeMillis()}") {
        if (text.isBlank()) return

        if (openAiVoiceService.engineType.value == VoiceEngineType.OPENAI_WHISPER_TTS) {
            openAiVoiceService.speak(text)
            return
        }

        val cleanText = cleanMarkdownAndSpecialTags(text)

        if (!isInitialized || tts == null) {
            initTts()
        }

        tts?.let { engine ->
            applyVoiceSettings(engine)
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            _isSpeaking.value = true
            engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    fun playSampleVoice(assistantName: String, gender: String, pitch: Float, speed: Float) {
        val sampleText = if (gender.equals("MALE", ignoreCase = true)) {
            "Приветствую! Я ваш инженерный помощник $assistantName. Готов к поиску схем, тестпоинтов и диагностике материнских плат."
        } else {
            "Здравствуйте! Я ваш ИИ-ассистент $assistantName. Готова помочь с разбором устройств, поиском компонентов и ремонтом электроники."
        }

        _currentAssistantName.value = assistantName
        _currentGender.value = gender
        _currentPitch.value = pitch
        _currentSpeed.value = speed

        tts?.let { engine ->
            applyVoiceSettings(engine)
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "sample_preview")
            }
            _isSpeaking.value = true
            engine.speak(sampleText, TextToSpeech.QUEUE_FLUSH, params, "sample_preview")
        }
    }

    fun stop() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.e("VoiceService", "Error stopping TTS: ${e.message}")
        }
    }

    private fun cleanMarkdownAndSpecialTags(text: String): String {
        return text
            .replace(Regex("\\[GOOGLE_SEARCH_DATA:.*?\\]"), "")
            .replace(Regex("```[\\s\\S]*?```"), "Блок кода или таблицы")
            .replace(Regex("[#*`_~]"), "")
            .replace(Regex("http\\S+"), "")
            .trim()
    }

    companion object {
        @Volatile
        private var INSTANCE: VoiceService? = null

        fun getInstance(context: Context): VoiceService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VoiceService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
