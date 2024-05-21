package com.example.voicepilot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.voicepilot.ui.theme.MyApplicationTheme
import com.example.voicepilot.utils.NotificationHelper
import com.example.voicepilot.viewModels.AgentViewModel
import com.example.voicepilot.viewModels.AppInteractionViewModel
import com.example.voicepilot.viewModels.SpeechRecognizerViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.compose.setContent as setContent1

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val notificationHelper = NotificationHelper(this)

    private val appInteractionViewModel: AppInteractionViewModel by viewModels()
    private val agentViewModel: AgentViewModel by viewModels()
    private val speechRecognizerViewModel: SpeechRecognizerViewModel by viewModels()


    companion object {
        private const val PERMISSION_REQUEST_POST_NOTIFICATIONS =
            101 // Request code for post notifications
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")

        notificationHelper.createNotificationChannel()
        handleNotificationsPermission()
        handleAudioPermission()

        speechRecognizerViewModel.initializeSpeechRecognizer()

        agentViewModel.accessibilityInfo.observe(this) {
            Log.d("MainActivity", "accessibilityInfo: $it")
            val test = "未読のトークを教えて"
//            lifecycleScope.launch {
//                val result = agentViewModel.selectFunctionByScreen(test)
//                Log.d("Speech", "input: $test, Result: $result")
//            }

        }


        setContent1 {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        val speechText by speechRecognizerViewModel.speechText.observeAsState()
                        val resultText by speechRecognizerViewModel.resultText.observeAsState()
                        val isListening by speechRecognizerViewModel.isListening.observeAsState()
                        val isLoading by agentViewModel.isLoading.observeAsState()
                        val responseText by agentViewModel.responseText.observeAsState()

//                        speechText?.let { Greeting(it) }
                        resultText?.let { Greeting(it) }
                        StartRecordingButton(
                            isListening = isListening ?: false,
                            isLoading = isLoading ?: false,
                            onStart = { speechRecognizerViewModel.onStartRecording() },
                            onStop = { speechRecognizerViewModel.onStopRecording() },
                            speechRecognizerViewModel = speechRecognizerViewModel,
                            agentViewModel = agentViewModel,
                            mainActivity = this@MainActivity
                        )
                        if (isLoading == true) {
                            CircularProgressIndicator()
                        }
                        responseText?.let { Greeting(it) }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy")
    }

    override fun onResume() {
        super.onResume()
        // 必要な初期化やリスナーの設定を確認
        Log.d("MainActivity", "onResume: Initializing components")
    }

    private fun handleNotificationsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_POST_NOTIFICATIONS
                )
            }
        } else {
            notificationHelper.createNotification()
            Log.d("MainActivity", "Permission granted1")
        }
    }

    private fun handleAudioPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) -> {
                speechRecognizerViewModel.initializeSpeechRecognizer()
            }

            else -> ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_RECORD_AUDIO
            )
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_POST_NOTIFICATIONS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    notificationHelper.createNotification()
                    Log.d("MainActivity", "Permission granted2")
                }
                return
            }

            else -> {
                // Ignore all other requests.
            }
        }
    }


}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Log.d("MainActivity", "Greeting: $name")
    Text(text = "Hello $name", modifier = modifier)
}

@Composable
fun StartRecordingButton(
    isListening: Boolean,
    isLoading: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    speechRecognizerViewModel: SpeechRecognizerViewModel,
    agentViewModel: AgentViewModel,
    mainActivity: MainActivity,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Button(
            onClick = {
                if (!isListening) {
                    onStart()
                } else {
                    onStop()
                    speechRecognizerViewModel.recordingStopped.observe(mainActivity) { event ->
                        event.getContentIfNotHandled()?.let { recordedText ->
                            agentViewModel.generateContentFromModel(recordedText)
                        }
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text(text = if (isListening) "Stop Recording" else "Start Recording")
        }
    }
}