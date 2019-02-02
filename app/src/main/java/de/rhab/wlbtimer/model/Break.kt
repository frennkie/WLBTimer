package de.rhab.wlbtimer.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Break(
        var duration: Int = 0,
        var comment: String = ""
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
                "duration" to duration,
                "comment" to comment
        )
    }

    companion object {
        const val FBP = "breaks"
    }
}