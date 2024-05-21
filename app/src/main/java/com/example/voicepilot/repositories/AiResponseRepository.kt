package com.example.voicepilot.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.voicepilot.BuildConfig
import com.example.voicepilot.viewModels.Command
import com.example.voicepilot.viewModels.MyJsonData
import com.example.voicepilot.viewModels.json
import com.google.ai.client.generativeai.GenerativeModel
import java.lang.reflect.Modifier
import javax.inject.Inject

class AiResponseRepository @Inject constructor(
    private val accessibilityDataRepository: AccessibilityDataRepository
) {
    companion object {
        private const val TAG = "AiResponseRepository"
    }

    private val generativeModel by lazy {
        GenerativeModel(modelName = "gemini-pro", apiKey = BuildConfig.GEMINI_API_KEY)
    }
    private val myJsonData = MyJsonData::class.java.declaredFields
        .filter { field -> Modifier.isPrivate(field.modifiers) && !field.isSynthetic }
        .joinToString { "${it.name}: ${it.type}" }

    suspend fun selectFunction(
        functionMap: Map<String, Command>,
        appPackageNames: LiveData<Set<String>>,
        input: String
    ): MyJsonData? {
        val prompt = """
            Based on the user's input, please output the function to execute in JSON format.
            The output should be based on the following class:
            $myJsonData
            Available functions are:
            $functionMap
            Installed app package names (you must use one of these)
            $appPackageNames
            User input:
            "$input"
        """.trimIndent()

        Log.d("Speech", "Prompt: $input")
        val response = generativeModel.generateContent(prompt)
        val rawJson = (response.text)?.trimIndent()
        val jsonInput = rawJson?.removeSurrounding("```")?.trim()?.removePrefix("json")?.trim()

        Log.d("Speech", "Response: $jsonInput")
        return try {
            jsonInput?.let { json.decodeFromString<MyJsonData>(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON", e)
            null
        }
    }

    suspend fun selectFunctionByScreen(input: String): MyJsonData? {
        val prompt = """入力に基づいて、タップするべき要素を画面をjson形式にしたものから選択してください。出力はjsonで行なってください。
            |画面の情報
            |${accessibilityDataRepository.accessibilityInfo.value}
            |ユーザーの入力
            |$input
        """.trimMargin()

        val response = generativeModel.generateContent(prompt)
        val rawJson = (response.text)?.trimIndent()
        val jsonInput = rawJson?.removeSurrounding("```")?.trim()?.removePrefix("json")?.trim()

        return try {
            jsonInput?.let { json.decodeFromString<MyJsonData>(it) }
        } catch (e: Exception) {
            Log.d(TAG, "Error parsing JSON", e)
            null
        }

    }

}