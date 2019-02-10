package de.rhab.wlbtimer.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class WlbUser(
        var nickname: String = ""
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
                "nickname" to nickname
        )
    }

    companion object {

        const val FBP = "users"

    }
}