package com.example.myapplication

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.utils.NotificationHelper
import com.example.myapplication.viewModels.SpeechRecognizerViewModel
import androidx.activity.compose.setContent as setContent1

class MainActivity : ComponentActivity() {
    private val notificationHelper = NotificationHelper(this)
    private val viewModel: SpeechRecognizerViewModel by viewModels()

    companion object {
        private const val PERMISSION_REQUEST_POST_NOTIFICATIONS =
            101 // Request code for post notifications
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationHelper.createNotificationChannel()
        handleNotificationsPermission()
        handleAudioPermission()

        viewModel.initializeSpeechRecognizer(this)

        setContent1 {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Greeting(speechText)
                        Greeting(resultText)
                        StartRecordingButton(
                            onStart = { startListening() },
                            onStop = { stopListening() }
                        )
                    }
                }
            }
        }
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
                initializeSpeechRecognizer()
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
    Text(text = "Hello $name!", modifier = modifier)
}

@Composable
fun StartRecordingButton(
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,

    ) {
    var isRecording by remember { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        Button(
            onClick = {
                isRecording = !isRecording
                if (isRecording) {
                    onStart()
                } else {
                    onStop()
                }
            },
        ) {
            Text(text = if (isRecording) "Stop Recording" else "Start Recording")
        }
    }
}