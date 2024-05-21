package com.example.voicepilot.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject
import javax.inject.Inject

class AccessibilityDataRepository @Inject constructor(

) {
    private val _accessibilityInfo: MutableLiveData<JSONObject> = MutableLiveData()
    val accessibilityInfo: LiveData<JSONObject> = _accessibilityInfo

    fun updateAccessibilityInfo(json: JSONObject) {
        _accessibilityInfo.postValue(json)
    }
}