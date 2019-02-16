package de.rhab.wlbtimer.model

import android.graphics.Color
import android.support.annotation.Keep
import android.util.Log
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties


@Keep
@IgnoreExtraProperties
data class Category (
        var objectId: String = "",
        var type: String = Category.TYPE_WORK,
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
                "type" to type,
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
                "type" to type,
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

        const val TYPE_WORK = "work"

        const val TYPE_OFF = "off"

        const val FBP = "category"

        private const val TAG = "Category"

    }
}