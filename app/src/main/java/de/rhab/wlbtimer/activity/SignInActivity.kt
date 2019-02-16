package de.rhab.wlbtimer.activity


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.annotation.Keep
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import de.rhab.wlbtimer.BuildConfig
import java.util.*
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import de.rhab.wlbtimer.model.WlbUser


@Keep
class SignInActivity : AppCompatActivity() {

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                Log.d(TAG, "onAuthStateChanged: status=signed_in:" + mAuth.currentUser!!.uid)

                Log.d(TAG, "store successful sign-in to Firestore")
                storeSuccessfulSignIn(mAuth.currentUser!!.uid)

                val intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)
                finish() // call this to finish the current activity

            } else {
                Log.d(TAG, "onAuthStateChanged: status=signed_out")

                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setIsSmartLockEnabled(!BuildConfig.DEBUG /* credentials */, true /* hints */)
                                .setAvailableProviders(Arrays.asList<AuthUI.IdpConfig>(
                                        AuthUI.IdpConfig.GoogleBuilder().build(),
                                        AuthUI.IdpConfig.EmailBuilder().build(),
                                        AuthUI.IdpConfig.AnonymousBuilder().build()))
                                .build(),
                        RC_SIGN_IN)

            }
        }
    }

    // ToDo(frennkie) this is never called!
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "Snack: onActivityResult called!")

        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            // Successfully signed in
            if (resultCode == Activity.RESULT_OK) {

                Log.d(TAG, "Snack: sign_in_ok - starting MainActivity")
                val intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)
                finishAffinity()

            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    //showSnackbar(R.string.sign_in_cancelled)
                    Log.e(TAG, "Snack: sign_in_cancelled")
                    return
                }

                if (response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    //showSnackbar(R.string.no_internet_connection)
                    Log.e(TAG, "Snack: no_internet_connection")
                    return
                }

                //showSnackbar(R.string.unknown_error)
                Log.e(TAG, "Snack: unknown_error")
                Log.e(TAG, "Sign-in error: ", response.error)
            }
        }
    }

    private fun storeSuccessfulSignIn(uid: String) {
        val userRef = db.collection(WlbUser.FBP).document(uid)
        val data = HashMap<String, Any>()
        data[WlbUser.FBP_LAST_SIGN_IN] = FieldValue.serverTimestamp()
        userRef.update(data)
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

    companion object {

        private const val TAG = "SignInActivity"

        private const val RC_SIGN_IN = 9001

    }

}