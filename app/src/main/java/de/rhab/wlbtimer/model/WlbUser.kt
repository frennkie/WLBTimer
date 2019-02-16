package de.rhab.wlbtimer.model

import android.support.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties


@Keep
@IgnoreExtraProperties
data class WlbUser(
    var nickname: String? = "",
    val default_break: Int? = 60 * 45,
    var default_category_work: Category? = null,
    var default_category_off: Category? = null,
    var last_category_off: String? = null,
    var last_category_work: String? = null,
    var last_sign_in: Timestamp? = null,
    var session_running: String? = null

) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
                "nickname" to nickname,
                FBP_DEFAULT_BREAK to default_break,
                FBP_DEFAULT_CATEGORY_WORK to default_category_work,
                FBP_DEFAULT_CATEGORY_OFF to default_category_off,
                FBP_LAST_CATEGORY_OFF to last_category_off,
                FBP_LAST_CATEGORY_WORK to last_category_work,
                FBP_LAST_SIGN_IN to last_sign_in,
                FBP_SESSION_RUNNING to session_running
        )
    }

    companion object {

        const val FBP = "users"

        const val FBP_DEFAULT_BREAK = "default_break"
        const val FBP_DEFAULT_CATEGORY_OFF = "default_category_off"
        const val FBP_DEFAULT_CATEGORY_WORK = "default_category_work"

        const val FBP_LAST_CATEGORY_OFF = "last_category_off"
        const val FBP_LAST_CATEGORY_WORK = "last_category_work"
        const val FBP_LAST_SIGN_IN = "last_sign_in"

        const val FBP_SESSION_RUNNING  = "session_running"

    }
}