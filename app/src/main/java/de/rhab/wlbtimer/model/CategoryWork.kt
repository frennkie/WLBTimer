package de.rhab.wlbtimer.model

import android.graphics.Color
import android.util.Log
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties


@IgnoreExtraProperties
data class CategoryWork (
        var objectId: String = "",
        var title: String = "",
        var color: String = "darkgray",
        var factor: Double = 1.0,
        var sessions: ArrayList<String> = ArrayList(),  // session is intentionally not included in toMap() -> ToDo check
        var default: Boolean = false
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
                "objectId" to objectId,
                "title" to title,
                "color" to color,
                "factor" to factor,
                "sessions" to sessions,
                "default" to default
        )
    }

    @Exclude
    fun toMapNoSessions(): Map<String, Any?> {
        return mapOf(
                "objectId" to objectId,
                "title" to title,
                "color" to color,
                "factor" to factor,
                "default" to default
        )
    }

    @Exclude
    fun colorToInt(): Int {
        return try {
            Color.parseColor(color)
        }
        catch (e: java.lang.IllegalArgumentException) {
            Log.w(TAG, "Caught Unknown color")
            Color.parseColor("lightgray")
        }
    }

    companion object {
        const val FBP = "category_work"
        const val FBP_SHORT = "category"  // ToDo(frennkie) hm..
        private const val TAG = "CategoryWork"
    }
}