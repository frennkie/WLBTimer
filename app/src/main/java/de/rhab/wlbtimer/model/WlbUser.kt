package de.rhab.wlbtimer.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class WlbUser(
        var nickname: String? = "",
        val default_break: Int? = 60 * 45,
        var default_category_work: Category? = null,
        var default_category_off: Category? = null,
        var last_category_off: String? = null,
        var last_category_work: String? = null,
        var session_running: String? = null

) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
                "nickname" to nickname,
                "default_break" to default_break,
                "default_category_work" to default_category_work,
                "default_category_off" to default_category_off,
                FBP_LAST_CATEGORY_OFF to last_category_off,
                FBP_LAST_CATEGORY_WORK to last_category_work,
                "session_running" to session_running
        )
    }

    companion object {

        const val FBP = "users"

        const val FBP_LAST_CATEGORY_OFF = "last_category_off"

        const val FBP_LAST_CATEGORY_WORK = "last_category_work"

    }
}