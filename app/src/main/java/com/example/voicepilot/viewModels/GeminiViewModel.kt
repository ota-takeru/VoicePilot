package com.example.voicepilot.viewModels

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicepilot.BuildConfig
import com.example.voicepilot.MainActivity
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeminiViewModel @Inject constructor(
    private val packageManager: PackageManager
) : ViewModel() {
    private val _responseText = MutableLiveData<String>()
    val responseText: LiveData<String> get() = _responseText


    // APIの初期化
    private val generativeModel by lazy {
        GenerativeModel(modelName = "gemini-pro", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    private val appPackageNames = mutableSetOf<String>()
    private val installedApps: List<ApplicationInfo>
        get() = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    private val functionMap = mapOf<String, (Map<String, Any>) -> Unit>(
        "launchOtherApp" to { packageName ->
            launchOtherApp(MainActivity, packageName)
        },
        "noAvailableFunction" to { _ ->
            println("No available function")
        },
    )

    init {
        logInstalledApps()
    }

    private fun logInstalledApps() {
        appPackageNames.addAll(installedApps.map { it.packageName })
        Log.d("Gemini", "Installed apps: $appPackageNames")

    }

    fun generateContentFromModel(input: String?) {
        if (input.isNullOrEmpty()) {
            _responseText.postValue("Error: Input is null or empty")
            return
        }
        val prompt =
            "鍵カッコ内はユーザーからの入力です。その入力に基づいて実行する関数をjson形式で出力してください。\n 使える関数は以下です。$functionMap \n 出力はfunctionと引数が必要ならばargを出力してください。\n 以下はユーザーからの入力です、\n「$input」"
        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                _responseText.postValue(response.text)
            } catch (e: Exception) {
                _responseText.postValue("Error: ${e.localizedMessage}")
            }
        }
        try {
//            functionMap[_responseText.function]?.invoke(_responseText.arg)
//                ?: println("Function not found for the input: ")
            Log.d("Gemini", "Response: ${_responseText.value}")
        } catch (e: Exception) {
            println("Error occurred: ${e.message}")
        }
    }

    private fun launchOtherApp(context: Context, packageName: Map<String, Any>) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName.toString())
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }
}