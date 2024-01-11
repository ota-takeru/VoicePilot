package com.example.voicepilot.viewModels

import android.app.Application
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
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class MyJsonData(
    val function: String,
    val arg: String
)


@HiltViewModel
class GeminiViewModel @Inject constructor(
    private val packageManager: PackageManager,
    application: Application
) : ViewModel() {
    private val appContext: Context = application.applicationContext

    private val _responseText = MutableLiveData<String>()
    val responseText: LiveData<String> get() = _responseText


    // APIの初期化
    private val generativeModel by lazy {
        GenerativeModel(modelName = "gemini-pro", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    private val appPackageNames = mutableSetOf<String>()
    private val installedApps: List<ApplicationInfo>
        get() = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    private val functionMap = mapOf(
        "launchOtherApp" to ::launchOtherApp,
        "noAvailableFunction" to { _, _ -> Log.d("Gemini", "No available function") }
    )

    private val functions: MutableList<() -> Unit> = mutableListOf()

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
            "鍵カッコ内はユーザーからの入力です。その入力に基づいて実行する関数をjson形式で出力してください。\n 出力はfunctionとargを必ず出力してください。 \n 使える関数は以下です。$functionMap \n インストールされているアプリのパッケージ名は以下です。\n $appPackageNames \n 以下はユーザーからの入力です、\n「$input」"
        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                val rawJson = (response.text)?.trimIndent()
//                val jsonObject = rawJson?.let { Json.parseToJsonElement(it).jsonObject }
//                val formattedJson = jsonObject?.let {
//                    Json.encodeToString(JsonObject.serializer(), it)
//                }
                val jsonData = rawJson?.let { Json.decodeFromString<MyJsonData>(it) }

                try {
                    val function1 = functionMap[jsonData?.function]
                    val arg1 = jsonData?.arg
                    if (function1 != null) {
                        functions.add {
                            if (arg1 != null) {
                                function1(appContext, arg1)
                            }

                        }
                    }
                    Log.d("Gemini", "Response: ${_responseText.value}")
                } catch (e: Exception) {
                    println("Error occurred: ${e.message}")
                }
            } catch (e: Exception) {
                _responseText.postValue("Error: ${e.localizedMessage}")
            }
        }
    }

    private fun launchOtherApp(context: Context, packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }
}