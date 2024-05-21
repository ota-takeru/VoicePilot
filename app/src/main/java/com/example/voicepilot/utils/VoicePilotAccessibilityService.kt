package com.example.voicepilot.utils

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.voicepilot.repositories.AccessibilityDataRepository
import org.json.JSONArray
import org.json.JSONObject

class VoicePilotAccessibilityService : AccessibilityService() {
    private val dataRepository = AccessibilityDataRepository()

    override fun onServiceConnected() {
        Log.d("VoicePilotAccessibilityService", "onServiceConnected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val source = event.source ?: return
            val activeWindowId = event.windowId
            // ツリーの探索を開始
            val jsonStr = convertNodeToJson(source)
            logLargeString("VoicePilotAccessibilityService", jsonStr.toString())
            dataRepository.updateAccessibilityInfo(jsonStr)
        }
        Log.d(" ", "Event:")
    }

    private fun convertNodeToJson(node: AccessibilityNodeInfo): JSONObject {
        val jsonObject = JSONObject()

        // ノードの基本情報をJSONに追加
        jsonObject.put("text", node.text?.toString())
//        jsonObject.put("className", node.className)
        jsonObject.put("contentDescription", node.contentDescription)
        jsonObject.put("viewIdResourceName", node.viewIdResourceName)

        // 子要素があれば、それらもJSONに変換
        if (node.childCount > 0) {
            val childrenArray = JSONArray()
            for (i in 0 until node.childCount) {
                val childNode = node.getChild(i)
                if (childNode != null) {
                    childrenArray.put(convertNodeToJson(childNode))
                    childNode.recycle()
                }
            }
            jsonObject.put("children", childrenArray)
        }

        return jsonObject
    }

    private fun isNodeVisible(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val metrics = DisplayMetrics()
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        return rect.intersect(Rect(0, 0, screenWidth, screenHeight))
    }

    private fun logLargeString(tag: String, content: String) {
        if (content.length > 4000) {
            var chunkCount = content.length / 4000   // 4000文字ごとに分割
            for (i in 0..chunkCount) {
                val start = i * 4000
                var end = (i + 1) * 4000
                end = if (end > content.length) content.length else end
                Log.d(tag, content.substring(start, end))
            }
        } else {
            Log.d(tag, content)
        }
    }

    override fun onInterrupt() {
        // サービスが中断された時の処理
    }
}
