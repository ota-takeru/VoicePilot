package com.example.voicepilot.viewModels

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject


@HiltViewModel
class SpeechRecognizerViewModel @Inject constructor(application: Application) :
    AndroidViewModel(application) {
    private var speechRecognizer: SpeechRecognizer? = null

    val speechText = MutableLiveData("")
    val resultText = MutableLiveData("")

    private val resultTextBuilder = StringBuilder()

    val isListening = MutableLiveData(false)

    fun onStartRecording() {
        resultTextBuilder.setLength(0)
        speechText.postValue("")
        startListening()
    }

    fun onStopRecording() {
        stopListening()
    }

    fun initializeSpeechRecognizer() {
        val context = getApplication<Application>().applicationContext

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("Speech", "Ready for speech")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("Speech", "Speech beginning")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Maybe update a UI element with the volume level
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // This can be used for more advanced speech recognition tasks
                    }

                    override fun onEndOfSpeech() {
                        Log.d("Speech", "Speech ended")
//                        restartListening()
                    }

                    override fun onError(error: Int) {
                        Log.d("Speech", "Error recognized: $error")
                        if (isListening.value!! && isRecoverableError(error)) {
                            restartListening()
                        }

                    }

                    private fun isRecoverableError(error: Int): Boolean {
                        return error != SpeechRecognizer.ERROR_CLIENT &&
                                error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                    }

                    override fun onResults(results: Bundle?) {
                        if (isListening.value!!) {
                            restartListening()
                        }

                        val data =
                            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!data.isNullOrEmpty()) {
                            val mostConfidentResult = data[0]
                            // Append this result to the StringBuilder
                            resultTextBuilder.append(mostConfidentResult)
                                .append(" ") // Append the text

                            // Update the MutableLiveData with the new text
                            resultText.postValue(resultTextBuilder.toString())
                        }
                        Log.d("Speech", "Recognized Text: ${speechText.value}")
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partialText =
                            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?: listOf()
                        speechText.value = partialText.getOrNull(0) ?: ""
                        Log.d("Speech", "Partial Recognized Text: $speechText")
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {
                        TODO("Not yet implemented")
                    }
                    // Implement other abstract methods here as needed
                })
            }
        }
//        startListening()
    }

    private fun startListening() {
        Log.d("Speech", "Listening...")
        isListening.value = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        speechRecognizer?.startListening(intent)
    }

    private fun restartListening() {
        Handler(Looper.getMainLooper()).postDelayed({
            startListening()
        }, 1000) // 例えば1秒後に再開
    }

    private fun stopListening() {
        isListening.value = false
        Log.d("Speech", "Stopped listening")

        speechRecognizer?.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

}

