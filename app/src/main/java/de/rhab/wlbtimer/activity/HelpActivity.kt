package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.support.annotation.Keep
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.rhab.wlbtimer.BuildConfig
import de.rhab.wlbtimer.R


@Keep
class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, HelpFragment()).commit()
    }

    class HelpFragment : PreferenceFragmentCompat() {

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

            addPreferencesFromResource(R.xml.preferences_help)

            var mUrl: String? = null
            val prefHelpHomepage = findPreference("prefHelpHomepage")

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
                    } else {
                        prefHelpHomepage.isVisible = false
                    }

                }

            val prefHelpVersion = findPreference("prefHelpVersion")
            prefHelpVersion.summary = BuildConfig.VERSION_NAME
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

            private const val TAG = "HelpFragment"
        }

    }

}
