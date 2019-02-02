package de.rhab.wlbtimer.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

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

        private const val TAG = "Note"
    }

}