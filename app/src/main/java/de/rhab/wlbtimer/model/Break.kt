package de.rhab.wlbtimer.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties


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

    @Exclude
    fun applyBreakRules(duration: Long): Long {
        return when {
            duration < 0 -> -1
            duration <= (60 * 60 * 2.5).toInt() -> 0
            duration in ((60 * 60 * 2.5).toInt() + 1)..(60 * 60 * 5.0).toInt()  -> 60 * 15
            duration in ((60 * 60 * 5.0).toInt() + 1)..(60 * 60 * 7.5).toInt()  -> 60 * 30
            duration in ((60 * 60 * 7.5).toInt() + 1)..(60 * 60 * 10.0).toInt() -> 60 * 45
            duration > 60 * 60 * 10.0 -> 60 * 60
            else -> -1
        }
    }

    companion object {

        const val FBP = "breaks"

    }
}