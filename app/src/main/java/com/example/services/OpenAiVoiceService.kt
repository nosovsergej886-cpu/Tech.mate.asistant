package com.example.services

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.config.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class VoiceEngineType(val titleRu: String) {
    OPENAI_WHISPER_TTS("OpenAI Whisper (STT) + OpenAI TTS"),
    ANDROID_NATIVE("Встроенный синтезатор Android")
}

data class OpenAiVoiceOption(
    val id: String,
    val nameRu: String,
    val gender: String, // "MALE" or "FEMALE"
    val description: String
)

class OpenAiVoiceService private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("techmate_openai_voice_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Preferences StateFlows
    private val _engineType = MutableStateFlow(
        try {
            VoiceEngineType.valueOf(prefs.getString("voice_engine_type", VoiceEngineType.OPENAI_WHISPER_TTS.name)!!)
        } catch (e: Exception) {
            VoiceEngineType.OPENAI_WHISPER_TTS
        }
    )
    val engineType: StateFlow<VoiceEngineType> = _engineType.asStateFlow()

    private val _apiEndpoint = MutableStateFlow(
        prefs.getString("openai_audio_endpoint", "https://api.aitunnel.ru/v1") ?: "https://api.aitunnel.ru/v1"
    )
    val apiEndpoint: StateFlow<String> = _apiEndpoint.asStateFlow()

    private val _customApiKey = MutableStateFlow(
        prefs.getString("openai_audio_api_key", "") ?: ""
    )
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _selectedVoice = MutableStateFlow(
        prefs.getString("openai_tts_voice", "nova") ?: "nova"
    )
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private val _ttsSpeed = MutableStateFlow(
        prefs.getFloat("openai_tts_speed", 1.08f)
    )
    val ttsSpeed: StateFlow<Float> = _ttsSpeed.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioRecordFile: File? = null

    // Native Android TTS Fallback
    private var nativeTts: TextToSpeech? = null
    private var isNativeTtsReady = false

    val openAiVoices = listOf(
        OpenAiVoiceOption("nova", "Nova (Женский)", "FEMALE", "Теплый, живой и естественный женский голос"),
        OpenAiVoiceOption("shimmer", "Shimmer (Женский)", "FEMALE", "Чистый, выразительный женский голос"),
        OpenAiVoiceOption("echo", "Echo (Мужской)", "MALE", "Уверенный, четкий голос мастера"),
        OpenAiVoiceOption("onyx", "Onyx (Мужской)", "MALE", "Глубокий, авторитетный мужской тембр инженера"),
        OpenAiVoiceOption("alloy", "Alloy (Универсальный)", "FEMALE", "Нейтральный, сбалансированный студийный голос"),
        OpenAiVoiceOption("fable", "Fable (Английский акцент)", "MALE", "Британский благородный тон")
    )

    init {
        initNativeTts()
    }

    private fun initNativeTts() {
        try {
            nativeTts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isNativeTtsReady = true
                    nativeTts?.language = Locale("ru", "RU")
                    nativeTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
                        override fun onDone(utteranceId: String?) { _isSpeaking.value = false }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) { _isSpeaking.value = false }
                        override fun onError(utteranceId: String?, errorCode: Int) { _isSpeaking.value = false }
                    })
                }
            }
        } catch (e: Exception) {
            Log.w("OpenAiVoiceService", "Native TTS init error: ${e.message}")
        }
    }

    fun getEffectiveApiKey(): String {
        val custom = _customApiKey.value.trim()
        if (custom.isNotBlank() && !custom.startsWith("sk-aitunnel")) {
            return custom
        }
        return ApiConfig.API_KEY.trim()
    }

    fun getEffectiveBaseUrl(): String {
        val custom = _apiEndpoint.value.trim().removeSuffix("/")
        val key = getEffectiveApiKey()
        if (key.startsWith("sk-aitunnel") && (custom.contains("api.openai.com") || custom.isBlank())) {
            return "https://api.aitunnel.ru/v1"
        }
        if (custom.isNotBlank()) {
            return custom
        }
        return "https://api.aitunnel.ru/v1"
    }

    fun updateSettings(
        engine: VoiceEngineType,
        endpoint: String = _apiEndpoint.value,
        apiKey: String = _customApiKey.value,
        voice: String,
        speed: Float
    ) {
        val cleanEndpoint = endpoint.trim().removeSuffix("/")
        val cleanApiKey = apiKey.trim()
        val cleanVoice = voice.trim()

        _engineType.value = engine
        _apiEndpoint.value = cleanEndpoint.ifBlank { "https://api.aitunnel.ru/v1" }
        _customApiKey.value = cleanApiKey
        _selectedVoice.value = cleanVoice.ifBlank { "nova" }
        _ttsSpeed.value = speed

        prefs.edit()
            .putString("voice_engine_type", engine.name)
            .putString("openai_audio_endpoint", _apiEndpoint.value)
            .putString("openai_audio_api_key", cleanApiKey)
            .putString("openai_tts_voice", _selectedVoice.value)
            .putFloat("openai_tts_speed", speed)
            .apply()
    }

    // ==========================================
    // 1. OPENAI TTS (TEXT TO SPEECH)
    // ==========================================
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (text.isBlank()) return
        stop()

        val cleanText = cleanTextForSpeech(text)
        if (cleanText.isBlank()) return

        if (_engineType.value == VoiceEngineType.ANDROID_NATIVE) {
            speakNativeTts(cleanText)
            return
        }

        scope.launch {
            try {
                _isSpeaking.value = true
                val apiKey = getEffectiveApiKey()
                var baseUrl = getEffectiveBaseUrl()
                if (!baseUrl.contains("/v1") && !baseUrl.endsWith("/audio/speech")) {
                    baseUrl = "$baseUrl/v1"
                }
                val url = if (baseUrl.endsWith("/audio/speech")) baseUrl else "$baseUrl/audio/speech"

                val jsonPayload = JSONObject().apply {
                    put("model", "tts-1")
                    put("input", cleanText.take(4096))
                    put("voice", _selectedVoice.value)
                    put("response_format", "mp3")
                    put("speed", _ttsSpeed.value.toDouble())
                }

                val requestBuilder = Request.Builder()
                    .url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .addHeader("Content-Type", "application/json")

                if (apiKey.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                }

                val response = httpClient.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful || response.body == null) {
                    val errBody = response.body?.string() ?: ""
                    Log.w("OpenAiVoiceService", "OpenAI TTS failed (${response.code}): $errBody, falling back to Native TTS")
                    withContext(Dispatchers.Main) {
                        speakNativeTts(cleanText)
                    }
                    return@launch
                }

                val audioBytes = response.body!!.bytes()
                val tempFile = File.createTempFile("tts_openai_", ".mp3", context.cacheDir)
                FileOutputStream(tempFile).use { it.write(audioBytes) }

                withContext(Dispatchers.Main) {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(tempFile.absolutePath)
                        setOnCompletionListener {
                            _isSpeaking.value = false
                            tempFile.delete()
                            onComplete?.invoke()
                        }
                        setOnErrorListener { _, _, _ ->
                            _isSpeaking.value = false
                            tempFile.delete()
                            speakNativeTts(cleanText)
                            true
                        }
                        prepare()
                        start()
                    }
                }
            } catch (e: Exception) {
                Log.e("OpenAiVoiceService", "TTS Exception: ${e.message}, falling back to Native")
                withContext(Dispatchers.Main) {
                    speakNativeTts(cleanText)
                }
            }
        }
    }

    private fun speakNativeTts(text: String) {
        if (!isNativeTtsReady || nativeTts == null) {
            initNativeTts()
        }
        nativeTts?.let { engine ->
            val isMale = _selectedVoice.value in listOf("echo", "onyx", "fable")
            engine.setPitch(if (isMale) 0.90f else 1.15f)
            engine.setSpeechRate(_ttsSpeed.value)
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "native_${System.currentTimeMillis()}")
            }
            _isSpeaking.value = true
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "native_${System.currentTimeMillis()}")
        }
    }

    fun playSampleVoice(voiceId: String, onDone: (() -> Unit)? = null) {
        val sampleText = when (voiceId) {
            "echo", "onyx" -> "Инженерная система готова. Готов к поиску схем, тестпоинтов и диагностике материнских плат."
            else -> "Здравствуйте! Я ваш голосовой ассистент. Помогу с разбором устройств и ремонтом электроники."
        }
        _selectedVoice.value = voiceId
        speak(sampleText, onDone)
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
            nativeTts?.stop()
            _isSpeaking.value = false
        } catch (e: Exception) {
            Log.w("OpenAiVoiceService", "Stop error: ${e.message}")
        }
    }

    // ==========================================
    // 2. OPENAI WHISPER (STT: SPEECH TO TEXT)
    // ==========================================
    fun startAudioRecording(): Boolean {
        return try {
            stop()
            val tempAudio = File.createTempFile("whisper_rec_", ".m4a", context.cacheDir)
            audioRecordFile = tempAudio

            mediaRecorder = MediaRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(tempAudio.absolutePath)
                prepare()
                start()
            }
            _isRecording.value = true
            true
        } catch (e: Exception) {
            Log.e("OpenAiVoiceService", "Start audio recording error: ${e.message}")
            _isRecording.value = false
            false
        }
    }

    fun cancelRecording() {
        _isRecording.value = false
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            audioRecordFile?.delete()
            audioRecordFile = null
        } catch (e: Exception) {
            Log.w("OpenAiVoiceService", "Cancel recording error: ${e.message}")
        }
    }

    fun stopAndTranscribe(
        promptHint: String = "Диагностика смартфонов, тестпоинт, EDL, BROM, замыкание, прозвонка",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        _isRecording.value = false
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            Log.w("OpenAiVoiceService", "MediaRecorder stop error: ${e.message}")
        }

        val file = audioRecordFile
        if (file == null || !file.exists() || file.length() < 100) {
            onError("Аудиозапись слишком короткая")
            return
        }

        scope.launch {
            _isTranscribing.value = true
            try {
                val apiKey = getEffectiveApiKey()
                var baseUrl = getEffectiveBaseUrl()
                if (!baseUrl.contains("/v1") && !baseUrl.endsWith("/audio/transcriptions")) {
                    baseUrl = "$baseUrl/v1"
                }
                val url = if (baseUrl.endsWith("/audio/transcriptions")) baseUrl else "$baseUrl/audio/transcriptions"

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-1")
                    .addFormDataPart("language", "ru")
                    .addFormDataPart("prompt", promptHint)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                    )
                    .build()

                val requestBuilder = Request.Builder()
                    .url(url)
                    .post(requestBody)

                if (apiKey.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                }

                val response = httpClient.newCall(requestBuilder.build()).execute()
                val resBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(resBody)
                    val text = json.optString("text", "").trim()
                    withContext(Dispatchers.Main) {
                        _isTranscribing.value = false
                        file.delete()
                        if (text.isNotBlank()) {
                            onSuccess(text)
                        } else {
                            onError("Не удалось распознать речь")
                        }
                    }
                } else {
                    Log.w("OpenAiVoiceService", "Whisper STT failed (${response.code}): $resBody")
                    withContext(Dispatchers.Main) {
                        _isTranscribing.value = false
                        file.delete()
                        onError("Ошибка Whisper STT: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("OpenAiVoiceService", "Whisper STT exception: ${e.message}")
                withContext(Dispatchers.Main) {
                    _isTranscribing.value = false
                    file.delete()
                    onError("Ошибка сети при отправке на Whisper: ${e.message}")
                }
            }
        }
    }

    private fun cleanTextForSpeech(text: String): String {
        var processed = text
            .replace(Regex("```[\\s\\S]*?```"), "Схема на экране.")
            .replace(Regex("`[^`]*`"), "")
            .replace(Regex("!\\[.*?\\]\\(.*?\\)"), "")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\[GOOGLE_SEARCH_DATA:.*?\\]"), "")
            .replace(Regex("[*#_~]"), "")
            .replace("[VOICE]", "")
            .replace("🎙️ [Голосовое сообщение]", "")
            .replace(Regex("http\\S+"), "")
            .replace("⚠️", " Обрати внимание: ")
            .replace("⚡", " По питанию: ")
            .replace("💡", " Кстати, лайфхак: ")
            .replace("🔬", " На микроскопе: ")
            .replace("🚨", " Внимание, критический сбой: ")

        // Make list reading conversational rather than robotic
        processed = processed.replace(Regex("(?m)^\\s*1\\.\\s*"), "Так, спервааа... ")
            .replace(Regex("(?m)^\\s*2\\.\\s*"), " Дальше, смотри... ")
            .replace(Regex("(?m)^\\s*3\\.\\s*"), " Ну а тут, может помочь следующее... ")
            .replace(Regex("(?m)^\\s*4\\.\\s*"), " А ещё, между делом... ")
            .replace(Regex("(?m)^\\s*5\\.\\s*"), " И напоследок... ")
            .replace(Regex("(?m)^\\s*[-•*]\\s*"), " — ")

        // Phonetic and colloquial adaptations for master speech
        processed = processed
            .replace(Regex("(?i)\\bКЗ\\b"), "короткое замыкание")
            .replace(Regex("(?i)\\bАКБ\\b"), "аккумулятор")
            .replace(Regex("(?i)\\bEDL\\b"), "Е Д Л")
            .replace(Regex("(?i)\\bBROM\\b"), "Бром")
            .replace(Regex("(?i)\\bVBUS\\b"), "Вэ-бас")
            .replace(Regex("(?i)\\bVBAT\\b"), "Вэ-бат")
            .replace(Regex("(?i)\\bPMIC\\b"), "контроллер питания")
            .replace(Regex("(?i)\\b(примечание|важно):"), "Смотри, тут важный момент:")
            .replace(Regex("(?i)\\b(если не помогло|дополнительно)"), "ну а тут, может поможет и это, но спервааа надо проверить главное")

        // Smooth out punctuation and line breaks
        processed = processed
            .replace(Regex("\\n+"), ". ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

        return processed
    }

    companion object {
        @Volatile
        private var INSTANCE: OpenAiVoiceService? = null

        fun getInstance(context: Context): OpenAiVoiceService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OpenAiVoiceService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
