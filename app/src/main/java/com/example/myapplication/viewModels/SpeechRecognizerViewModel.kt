package com.example.myapplication.viewModels

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.myapplication.MainActivity
import java.util.Locale

class SpeechRecognizerViewModel : ViewModel() {
    private var speechRecognizer: SpeechRecognizer? = null
    var speechText = mutableStateOf("")
    var resultText = mutableStateOf("")

    fun initializeSpeechRecognizer(mainActivity: MainActivity) {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(mainActivity).apply {
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
                        // Handle different errors here
//                        restartListening()

                    }

                    override fun onResults(results: Bundle?) {
                        val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        resultText = data?.getOrNull(0) ?: ""
                        Log.d("Speech", "Recognized Text: $resultText")
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partialText =
                            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        speechText += partialText?.getOrNull(0) ?: ""
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

    fun startListening() {
        Log.d("Speech", "Listening...")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                5000
            )
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000)
        }
        speechRecognizer?.startListening(intent)
    }

    fun restartListening() {
        speechRecognizer?.stopListening()
        // 必要に応じて適当な遅延を設ける
        Handler(Looper.getMainLooper()).postDelayed({
            startListening()
        }, 1000) // 例えば1秒後に再開
    }

    fun stopListening() {
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

