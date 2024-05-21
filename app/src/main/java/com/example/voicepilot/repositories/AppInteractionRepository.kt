package com.example.voicepilot.repositories

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import javax.inject.Inject

class AppInteractionRepository @Inject constructor(
    private val packageManager: PackageManager,
    private val application: Application,
) {
    fun launchOtherApp(packageName: String): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                application.startActivity(it)
                Log.d("Gemini", "Response: Succeed start activity")
                true

            } ?: run {
                Log.e("Gemini", "Error: Launch intent not found for package $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e("Gemini", "Error: ${e.localizedMessage}")
            false
        }
    }
}