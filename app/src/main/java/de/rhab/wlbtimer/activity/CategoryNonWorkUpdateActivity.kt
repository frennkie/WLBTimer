package de.rhab.wlbtimer.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.larswerkman.holocolorpicker.ColorPicker
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.model.CategoryNonWork
import de.rhab.wlbtimer.model.WlbUser


class CategoryNonWorkUpdateActivity : AppCompatActivity() {

    private val mDatabaseRef = FirebaseDatabase.getInstance().reference

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mColor: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@CategoryNonWorkUpdateActivity, SignInActivity::class.java))
            }
        }

        setContentView(R.layout.activity_category_non_work_update)

        val toolbar = findViewById<android.support.v7.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var mKey = intent.getStringExtra("KEY")
        val mPathStr = intent.getStringExtra("PATH")
        val mTitle = intent.getStringExtra("TITLE")
        mColor = intent.getStringExtra("COLOR") ?: "#A89AAF"

        Log.d(TAG, "asked to edit CategoryNonWork with key: $mKey and path: $mPathStr")

        if (mKey == null) {
            setTitle(R.string.title_activity_category_non_work_add)
        } else {
            setTitle(R.string.title_activity_category_non_work_update)
        }

        val editTextTitle = findViewById<EditText>(R.id.update_title)
        editTextTitle.setText(mTitle)
        editTextTitle.requestFocus()
        editTextTitle.selectAll()

        val mColorPicker = findViewById<ColorPicker>(R.id.picker)
        val mSetColorButton = findViewById<Button>(R.id.btn_set_color)
        val mColorText = findViewById<TextView>(R.id.tv_show_color)

        mColorText.text = mColor

        Log.d(TAG, "selected (either default or from db) Color: $mColor")
        mColorPicker.color = Color.parseColor(mColor)
        mColorPicker.oldCenterColor = Color.parseColor(mColor)

        mSetColorButton.setOnClickListener {
            mColorText.setTextColor(mColorPicker.color)
            mColorPicker.oldCenterColor = mColorPicker.color

            mColor = String.format("#%06X", 0xFFFFFF and mColorPicker.color)
            mColorText.text = mColor
            Log.d(TAG, "new/updated Color: $mColor")
        }

        val buttonSaveCategoryNonWork = findViewById<FloatingActionButton>(R.id.button_save)
        buttonSaveCategoryNonWork.setOnClickListener {

            Log.d(TAG, "foo")

            if (mKey == null) {
                Log.d(TAG, "add CategoryNonWork")
                Toast.makeText(this, "Adding...", Toast.LENGTH_SHORT).show()

                mKey = mDatabaseRef.child(WlbUser.FBP)
                    .child(mAuth.currentUser!!.uid)
                    .child(CategoryNonWork.FBP).push().key
                if (mKey == null) {
                    Log.w(TAG, "Couldn't get push key")
                }

            } else {
                Log.d(TAG, "update CategoryNonWork")
                Toast.makeText(this, "Updating...", Toast.LENGTH_SHORT).show()
            }


            val childUpdates = HashMap<String, Any>()

            val mCategoryNonWork  = CategoryNonWork(editTextTitle.text.toString(), mColor)
            val pathA = mDatabaseRef.child(WlbUser.FBP).child(mAuth.currentUser!!.uid)
                    .child(CategoryNonWork.FBP).child(mKey).path.toString()
            childUpdates[pathA] = mCategoryNonWork.toMap()

            mDatabaseRef.updateChildren(childUpdates)

            finish()

        }
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

        private const val TAG = "CatNonWorkUpdateAct"
    }
}
