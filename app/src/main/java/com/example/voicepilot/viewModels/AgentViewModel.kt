package com.example.voicepilot.viewModels

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicepilot.repositories.AccessibilityDataRepository
import com.example.voicepilot.repositories.AiResponseRepository
import com.example.voicepilot.repositories.AppInteractionRepository
import com.example.voicepilot.repositories.AppPackageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import javax.inject.Inject

@Serializable
data class MyJsonData(
    val function: String,
    val packageName: String,
)

val json = Json {
    ignoreUnknownKeys = true
}


@HiltViewModel
class AgentViewModel @Inject constructor(
    application: Application,
    appPackageRepository: AppPackageRepository,
    appInteractionRepository: AppInteractionRepository,
    private val aiResponseRepository: AiResponseRepository,
    private val accessibilityDataRepository: AccessibilityDataRepository,
) : ViewModel() {


    private val _responseText = MutableLiveData<String>()
    val responseText: LiveData<String> get() = _responseText
    val isLoading = MutableLiveData(false)

    val accessibilityInfo: LiveData<JSONObject> =
        accessibilityDataRepository.accessibilityInfo


    // インストールされているアプリのパッケージ名
    private val appPackageNames: LiveData<Set<String>> = appPackageRepository.appPackageNames

    // 実装した関数名と関数のマップ
    private val functionMap = mapOf(
        "launchOtherApp" to LaunchOtherAppCommand(appInteractionRepository),
        "noAvailableFunction" to NoAvailableFunctionCommand(_responseText),
    )

    // 実行する関数のリスト
    private val functions: MutableList<() -> Unit> = mutableListOf()


    fun generateContentFromModel(input: String) {
        isLoading.postValue(true)
        viewModelScope.launch {
            try {
                _responseText.postValue("Response: Generating content...")
                val jsonData =
                    aiResponseRepository.selectFunction(functionMap, appPackageNames, input)
                _responseText.postValue("Response: $jsonData")
                try {
//                    _responseText.postValue("now executing...")
                    val function1 = functionMap[jsonData?.function]
                    val packageName = jsonData?.packageName

                    if (function1 != null) {
                        functions.add {
                            if (packageName != null) {
                                function1.execute(packageName)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Error occurred: ${e.message}")
                }
                functions.forEach { it() }
                functions.clear()
            } catch (e: Exception) {
                _responseText.postValue("Error: ${e.localizedMessage}")
            }
        }
        isLoading.postValue(false)
    }

    suspend fun selectFunctionByScreen(input: String) {
        aiResponseRepository.selectFunctionByScreen(input)
    }
}

interface Command {
    fun execute(vararg args: Any)
}

class LaunchOtherAppCommand(
    private val repository: AppInteractionRepository,
) : Command {
    override fun execute(vararg args: Any) {
        val packageName = args[0] as String
        repository.launchOtherApp(packageName)
    }
}

class NoAvailableFunctionCommand(
    private val responseText: MutableLiveData<String>,
) : Command {
    override fun execute(vararg args: Any) {
        responseText.postValue("Error: No available function")
    }
}
