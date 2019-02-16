package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.support.annotation.Keep
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.model.WlbUser


@Keep
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {

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

            addPreferencesFromResource(R.xml.preferences_settings)

            val prefSettingsAccount = findPreference("prefSettingsAccount")
            val prefSettingsAccountLast = findPreference("prefSettingsAccountLast")
            val prefSettingsAccountNickname = findPreference("prefSettingsAccountNickname")

            if (mAuth.currentUser != null) {
                prefSettingsAccount.summary = mAuth.currentUser!!.email
            } else {
                prefSettingsAccount.summary = "test@example.com"
            }

            val userRef = db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid)

            userRef.get()
                .addOnFailureListener { e ->
                    Log.d(TAG, "get failed with ", e)
                }
                .addOnSuccessListener { documentSnapshot ->
                    val mWlbUser = documentSnapshot.toObject(WlbUser::class.java)!!
                    if (mWlbUser.last_sign_in != null) {
                        prefSettingsAccountLast.summary = mWlbUser.last_sign_in!!.toDate().toString()
                    }

                    if (mWlbUser.nickname != null) {
                        prefSettingsAccountNickname.summary = mWlbUser.nickname
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

            private const val TAG = "SettingsFragment"
        }

    }

}
