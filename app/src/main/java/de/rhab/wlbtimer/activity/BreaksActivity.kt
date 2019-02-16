package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.support.annotation.Keep
import android.support.v7.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import de.rhab.wlbtimer.R
import kotlinx.android.synthetic.main.activity_breaks.*


@Keep
class BreaksActivity : AppCompatActivity() {

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@BreaksActivity, SignInActivity::class.java))
            }
        }

        setContentView(R.layout.activity_breaks)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
}
