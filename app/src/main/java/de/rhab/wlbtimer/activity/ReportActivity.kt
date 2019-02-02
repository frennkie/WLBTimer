package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import com.google.firebase.auth.FirebaseAuth
import de.rhab.wlbtimer.model.CategoryWork
import io.fabric.sdk.android.Fabric
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.firebase.database.*
import de.rhab.wlbtimer.BuildConfig
import de.rhab.wlbtimer.FirebaseHandler
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.model.Session


class ReportActivity : AppCompatActivity() {

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private val mDatabaseRef = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@ReportActivity, SignInActivity::class.java))
            }
        }

        setContentView(R.layout.activity_report)

        val toolbar = findViewById<android.support.v7.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        // ToDo(frennkie) Adds tracking - check this
        if (mAuth.currentUser != null) {
            FirebaseHandler.lastOnlineTracking(mAuth.currentUser!!)
        }

        val strtext = intent.getStringExtra(INTENT_EXTRA_MESSAGE)

        val greetingTextView = findViewById<View>(R.id.greetingTextView) as TextView
        val curText = greetingTextView.text.toString()

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this.applicationContext)
        val displayName = sharedPref.getString("example_text", "foobar")

        greetingTextView.text = "$curText - $displayName - $strtext"

        // Add user id
        val userTextView = findViewById<View>(R.id.userTextView) as TextView
        userTextView.text = mAuth.currentUser!!.uid

        // show backend version and update button
        val backendVerTextView = findViewById<TextView>(R.id.backendVerTextView)
        val backendVerBtn = findViewById<Button>(R.id.backendVerBtn)
        backendVerBtn.setOnClickListener {
            // Write a message to the mDatabaseRef
            val myInBtnRef = mDatabaseRef.child("info").child("version")

            // Read from the mDatabaseRef - ToDo(frennkie) I think I should remove this in onStop()
            myInBtnRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    val value = dataSnapshot.getValue(String::class.java)
                    backendVerTextView.text = value
                    Log.d(TAG, "Value is: " + value!!)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Log.w(TAG, "Failed to read value.", error.toException())
                }
            })
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