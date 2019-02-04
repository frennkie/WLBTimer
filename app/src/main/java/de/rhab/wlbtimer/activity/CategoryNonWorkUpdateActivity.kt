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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.larswerkman.holocolorpicker.ColorPicker
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.model.CategoryNonWork
import de.rhab.wlbtimer.model.CategoryWork
import de.rhab.wlbtimer.model.WlbUser


class CategoryNonWorkUpdateActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mCategoryNonWorkRef: DocumentReference

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

        var mId = intent.getStringExtra("KEY")
        val mTitle = intent.getStringExtra("TITLE")
        var mColor = intent.getStringExtra("COLOR") ?: "#A89AAF"

        var mCategoryNonWork: CategoryNonWork? = null

        val userRef = db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid)

        if (mId == null) {
            setTitle(R.string.title_activity_category_non_work_add)
            Log.d(TAG, "asked to add new CategoryNonWork")
            mCategoryNonWorkRef = userRef.collection(CategoryNonWork.FBP).document()

        } else {
            setTitle(R.string.title_activity_category_non_work_update)
            Log.d(TAG, "asked to edit CategoryNonWork with key: $mId")
            mCategoryNonWorkRef = userRef.collection(CategoryWork.FBP).document(mId)

            mCategoryNonWorkRef.get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot != null) {
                            mCategoryNonWork = documentSnapshot.toObject(CategoryNonWork::class.java)
                            Log.d(TAG, "found: $mCategoryNonWork")
                        } else {
                            Log.w(TAG, "No such document")
                        }

                    }
                    .addOnFailureListener { e ->
                        Log.d(TAG, "get failed with ", e)
                    }
        }

        if (mCategoryNonWork == null) {
            mCategoryNonWork = CategoryNonWork()
        }

        val editTextTitle = findViewById<EditText>(R.id.category_non_work_update_title)
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
        buttonSaveCategoryNonWork.setOnClickListener {_ ->

            if (mId == null) {
                Log.d(TAG, "add CategoryNonWork")
                Toast.makeText(this, "Adding...", Toast.LENGTH_SHORT).show()

            } else {
                Log.d(TAG, "update CategoryNonWork")
                Toast.makeText(this, "Updating...", Toast.LENGTH_SHORT).show()
            }

            mCategoryNonWork!!.objectId = mCategoryNonWorkRef.id
            mCategoryNonWork!!.title = editTextTitle.text.toString()
            mCategoryNonWork!!.color = mColor

            val batch = db.batch()
            batch.set(mCategoryNonWorkRef, mCategoryNonWork!!)
            batch.update(userRef, "CategoryNonWork-last", mCategoryNonWorkRef.id)

            batch.commit()
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to start new session! Error: ", e)
                        // this catches the error.. may be do something with this?! UI does reflect the
                        // intended change until refresh!
                    }
                    .addOnSuccessListener { _ ->
                        Log.d(TAG, "success!")
                    }

            // finish activity after save and return to list
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
