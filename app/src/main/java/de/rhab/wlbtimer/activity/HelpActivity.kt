package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.rhab.wlbtimer.BuildConfig
import de.rhab.wlbtimer.R


class HelpActivity : AppCompatActivity() {

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@HelpActivity, SignInActivity::class.java))
            }
        }

        supportFragmentManager.beginTransaction().replace(android.R.id.content, HelpFragment()).commit()

    }

    public override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener!!)
    }

    public override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener!!)
        }
    }


    class HelpFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(p0: Bundle?, p1: String?) {
            Log.d(TAG, "onCreatePreferences")
            addPreferencesFromResource(R.xml.preferences_help)
            val prefHelpVersion = findPreference("prefHelpVersion")
            prefHelpVersion.summary = BuildConfig.VERSION_NAME
        }

        companion object {

            private const val TAG = "HelpFragment"
        }

    }

    companion object {

        private const val TAG = "HelpActivity"
    }
}
