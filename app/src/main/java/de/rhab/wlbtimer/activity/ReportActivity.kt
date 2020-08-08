package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.rhab.wlbtimer.BuildConfig
import de.rhab.wlbtimer.R


@Keep
class ReportActivity : AppCompatActivity() {

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@ReportActivity, SignInActivity::class.java))
            }
        }

        setContentView(R.layout.activity_report)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val strtext = intent.getStringExtra(INTENT_EXTRA_MESSAGE)

        val greetingTextView = findViewById<View>(R.id.greetingTextView) as TextView
        val curText = greetingTextView.text.toString()

        val sharedPref = this.applicationContext.getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}_prefs_${mAuth.currentUser!!.uid}", 0
        )

        val displayName = sharedPref.getString("example_text", "foobar")

        greetingTextView.text = "$curText - $displayName - $strtext"

        // Add user id
        val userTextView = findViewById<View>(R.id.userTextView) as TextView
        userTextView.text = mAuth.currentUser!!.uid

        val mMetadataPublicRef = db.collection("metadata").document("public")

        // show backend version and update button
        val backendVerTextView = findViewById<TextView>(R.id.backendVerTextView)
        val backendVerBtn = findViewById<Button>(R.id.backendVerBtn)
        backendVerBtn.setOnClickListener {

            mMetadataPublicRef.get()
                    .addOnFailureListener { e ->
                        Log.d(TAG, "get failed with ", e)
                    }
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot != null) {
                            if (documentSnapshot.contains("version")) {
                                val mVersion = documentSnapshot.get("version").toString()
                                backendVerTextView.text = mVersion
                                Log.d(TAG, "Value is: $mVersion")
                            }
                            if (documentSnapshot.contains("version")) {
                                val mMinClientVer = documentSnapshot.get("min_supported_client_version").toString()
                                Log.d(TAG, "Min Supported Client Version is: $mMinClientVer")
                            }

                        }
                    }
        }

        val tvClientVersion = findViewById<TextView>(R.id.tv_client_version)
        val versionName = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE
        tvClientVersion.text = "Version: $versionName (ID: $versionCode)"

        val crashBtn = findViewById<View>(R.id.crashBtn) as Button
        crashBtn.setOnClickListener {
            if ("-DEV" in BuildConfig.VERSION_NAME) {
                Log.e(TAG, "Crashing allowed on DEV build!")
                throw RuntimeException("This is a crash (DEV build)")
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Crashing has been disabled.. ;-)", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
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

        private const val TAG = "ReportActivity"

        const val INTENT_EXTRA_MESSAGE = "MY_MESSAGE"

    }
}