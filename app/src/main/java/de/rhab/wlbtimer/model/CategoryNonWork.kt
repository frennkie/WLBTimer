package de.rhab.wlbtimer.model

import android.graphics.Color
import android.util.Log
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class CategoryNonWork(
        var objectId: String = "",
        var title: String = "",
        var color: String = "lightgray"
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
                "title" to title,
                "color" to color
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
        const val FBP = "category_non_work"
        private const val TAG = "CategoryNonWork"
    }

}