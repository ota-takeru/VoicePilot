package com.example.voicepilot.repositories

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject

class AppPackageRepository @Inject constructor(
    private val packageManager: PackageManager,
) {
    private val _appPackageNames = MutableLiveData<Set<String>>()
    val appPackageNames: LiveData<Set<String>> = _appPackageNames

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                !(appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
                        appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
            }
            .map { it.packageName }
            .toSet()
        _appPackageNames.postValue(installedApps)
    }
}
