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
import de.rhab.wlbtimer.model.Category
import de.rhab.wlbtimer.model.Session
import de.rhab.wlbtimer.model.WlbUser


class CategoryOffUpdateActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mCategoryRef: DocumentReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@CategoryOffUpdateActivity, SignInActivity::class.java))
            }
        }

        setContentView(R.layout.activity_category_off_update)

        val toolbar = findViewById<android.support.v7.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val mId = intent.getStringExtra("KEY")
        val mTitle = intent.getStringExtra("TITLE")
        var mColor = intent.getStringExtra("COLOR") ?: "#A89AAF"

        var mCategory: Category? = null

        val userRef = db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid)

        if (mId == null) {
            setTitle(R.string.title_activity_category_off_add)
            Log.d(TAG, "asked to add new Category")
            mCategoryRef = userRef.collection(Category.FBP).document()

        } else {
            setTitle(R.string.title_activity_category_off_update)
            Log.d(TAG, "asked to edit Category with key: $mId")
            mCategoryRef = userRef.collection(Category.FBP).document(mId)

            mCategoryRef.get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot != null) {
                            mCategory = documentSnapshot.toObject(Category::class.java)
                            Log.d(TAG, "found: $mCategory")
                        } else {
                            Log.w(TAG, "No such document")
                        }

                    }
                    .addOnFailureListener { e ->
                        Log.d(TAG, "get failed with ", e)
                    }
        }

        if (mCategory == null) {
            mCategory = Category()
        }

        val editTextTitle = findViewById<EditText>(R.id.category_off_update_title)
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

        val buttonSaveCategory = findViewById<FloatingActionButton>(R.id.button_save)
        buttonSaveCategory.setOnClickListener {_ ->

            if (mId == null) {
                Log.d(TAG, "add Category")
                Toast.makeText(this, "Adding...", Toast.LENGTH_SHORT).show()

            } else {
                Log.d(TAG, "update Category")
                Toast.makeText(this, "Updating...", Toast.LENGTH_SHORT).show()
            }

            mCategory!!.objectId = mCategoryRef.id
            mCategory!!.type = Category.TYPE_OFF
            mCategory!!.title = editTextTitle.text.toString()
            mCategory!!.color = mColor

            val batch = db.batch()
            batch.set(mCategoryRef, mCategory!!)
            // store a "last edited" reference
            batch.update(userRef, WlbUser.FBP_LAST_CATEGORY_OFF, mCategoryRef.id)

            mCategory?.sessions?.forEach { session ->
                batch.update(userRef.collection(Session.FBP).document(session),
                        Category.FBP,
                        mCategory!!.toMapNoSessions())
            }

            // additionally store copy on user document
            batch.update(db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid),
                    "default_" + Category.FBP + "_" + Category.TYPE_OFF, mCategory!!.toMapNoSessions())

            batch.commit()
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to update Category Work! Error: ", e)
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

        private const val TAG = "CategoryOffUpdateAct"
    }
}
