package de.rhab.wlbtimer.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.Keep
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.rhab.wlbtimer.BuildConfig
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.activity.SignInActivity


@Keep
class HelpPrefFragment : PreferenceFragmentCompat() {

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(activity, SignInActivity::class.java))
            }
        }

        Log.d(TAG, "onCreatePreferences")

        addPreferencesFromResource(R.xml.pref_help)

        var mUrl: String?
        val prefHelpHomepage = findPreference(getString(R.string.pref_help_homepage_key))

        val metaPublicRef = db.collection("metadata").document("public")
        metaPublicRef.get()
            .addOnFailureListener { e ->
                Log.d(TAG, "get failed with ", e)

                prefHelpHomepage.isVisible = false

            }
            .addOnSuccessListener { documentSnapshot ->
                mUrl = documentSnapshot.get("url").toString()
                Log.d(TAG, "mUrl: $mUrl")

                if (mUrl != null) {
                    prefHelpHomepage.summary = mUrl
                    prefHelpHomepage.intent.data = Uri.parse(mUrl)
                } else {
                    prefHelpHomepage.isVisible = false
                }

            }

        val prefHelpVersion = findPreference(getString(R.string.pref_help_version_key))
        prefHelpVersion.summary = BuildConfig.VERSION_NAME
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            getString(R.string.pref_help_feedback_key) -> {
//                    startActivity(Intent(activity, ChangePasswordActivity::class.java))
                Snackbar.make(view!!, "Sorry - not yet implemented!", Snackbar.LENGTH_LONG).show()
                true
            }
            else -> {
                super.onPreferenceTreeClick(preference)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener!!)
    }

    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener!!)
        }
    }

    companion object {

        private const val TAG = "HelpPrefFragment"
    }

}