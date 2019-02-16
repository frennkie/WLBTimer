package de.rhab.wlbtimer.model

import android.support.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties


@Keep
@IgnoreExtraProperties
data class Note(
        val title: String = "",
        val description: String = "",
        val priority: Int = 0
) {

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
                "title" to title,
                "description" to description,
                "priority" to priority
        )
    }

    companion object {

        const val FBP = "notes"

        private const val TAG = "Note"
    }

}