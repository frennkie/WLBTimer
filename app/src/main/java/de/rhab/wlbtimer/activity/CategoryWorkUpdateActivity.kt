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
import de.rhab.wlbtimer.model.CategoryWork
import de.rhab.wlbtimer.model.Session
import de.rhab.wlbtimer.model.WlbUser


class CategoryWorkUpdateActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mCategoryWorkRef: DocumentReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@CategoryWorkUpdateActivity, SignInActivity::class.java))
            }
        }

        setContentView(R.layout.activity_category_work_update)

        val toolbar = findViewById<android.support.v7.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val mId = intent.getStringExtra("ID")
        val mTitle = intent.getStringExtra("TITLE")
        val mFactor = intent.getStringExtra("FACTOR")
        var mColor = intent.getStringExtra("COLOR") ?: "#A89AAF"


        var mCategoryWork: CategoryWork? = null

        val userRef = db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid)

        if (mId == null) {
            setTitle(R.string.title_activity_category_work_add)
            Log.d(TAG, "asked to add new CategoryWork")
            mCategoryWorkRef = userRef.collection(CategoryWork.FBP).document()

        } else {
            setTitle(R.string.title_activity_category_work_update)
            Log.d(TAG, "asked to edit CategoryWork with key: $mId")
            mCategoryWorkRef = userRef.collection(CategoryWork.FBP).document(mId)

            mCategoryWorkRef.get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot != null) {
                            mCategoryWork = documentSnapshot.toObject(CategoryWork::class.java)
                            Log.d(TAG, "found: $mCategoryWork")
                        } else {
                            Log.w(TAG, "No such document")
                        }

                    }
                    .addOnFailureListener { e ->
                        Log.d(TAG, "get failed with ", e)
                    }
        }

        if (mCategoryWork == null) {
            mCategoryWork = CategoryWork()
        }

        val editTextTitle = findViewById<EditText>(R.id.category_work_update_title)
        editTextTitle.setText(mTitle)
        editTextTitle.requestFocus()
        editTextTitle.selectAll()

        val editTextFactor = findViewById<EditText>(R.id.category_work_update_factor)
        editTextFactor.setText(mFactor)

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

        val buttonSaveCategoryWork = findViewById<FloatingActionButton>(R.id.button_save)
        buttonSaveCategoryWork.setOnClickListener { _ ->

            if (mId == null) {
                Log.d(TAG, "add CategoryWork")
                Toast.makeText(this, "Adding...", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "update CategoryWork")
                Toast.makeText(this, "Updating...", Toast.LENGTH_SHORT).show()
            }

            val mNewFactor = if (editTextFactor.text.toString().isEmpty()) {
                1.0
            } else {
                editTextFactor.text.toString().toDouble()
            }

            mCategoryWork!!.objectId = mCategoryWorkRef.id
            mCategoryWork!!.title = editTextTitle.text.toString()
            mCategoryWork!!.color = mColor
            mCategoryWork!!.factor = mNewFactor

            val batch = db.batch()
            batch.set(mCategoryWorkRef, mCategoryWork!!)
            batch.update(userRef, "CategoryWork-last", mCategoryWorkRef.id)

            mCategoryWork?.sessions?.forEach { session ->
                batch.update(userRef.collection(Session.FBP).document(session),
                        CategoryWork.FBP_SHORT,
                        mCategoryWork!!.toMapNoSessions())
            }

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

        private const val TAG = "CatWorkUpdateAct"
    }
}
