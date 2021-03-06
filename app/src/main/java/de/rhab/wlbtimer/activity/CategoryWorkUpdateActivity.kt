package de.rhab.wlbtimer.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.larswerkman.holocolorpicker.ColorPicker
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.databinding.ActivityCategoryWorkUpdateBinding
import de.rhab.wlbtimer.model.Category
import de.rhab.wlbtimer.model.Session
import de.rhab.wlbtimer.model.WlbUser


@Keep
class CategoryWorkUpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryWorkUpdateBinding

    private val db = FirebaseFirestore.getInstance()

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mCategoryWorkRef: DocumentReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCategoryWorkUpdateBinding.inflate(layoutInflater)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@CategoryWorkUpdateActivity, SignInActivity::class.java))
            }
        }

        setContentView(binding.root)
        setSupportActionBar(binding.myToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val mId = intent.getStringExtra("ID")
        val mTitle = intent.getStringExtra("TITLE")
        val mFactor = intent.getStringExtra("FACTOR")
        var mColor = intent.getStringExtra("COLOR") ?: "#A89AAF"


        var mCategory: Category? = null

        val userRef = db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid)

        if (mId == null) {
            setTitle(R.string.title_activity_category_work_add)
            Log.d(TAG, "asked to add new Category")
            mCategoryWorkRef = userRef.collection(Category.FBP).document()

        } else {
            setTitle(R.string.title_activity_category_work_update)
            Log.d(TAG, "asked to edit Category with key: $mId")
            mCategoryWorkRef = userRef.collection(Category.FBP).document(mId)

            mCategoryWorkRef.get()
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
                Log.d(TAG, "add Category")
                Toast.makeText(this, "Adding...", Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "update Category")
                Toast.makeText(this, "Updating...", Toast.LENGTH_SHORT).show()
            }

            val mNewFactor = if (editTextFactor.text.toString().isEmpty()) {
                1.0
            } else {
                editTextFactor.text.toString().toDouble()
            }

            mCategory!!.objectId = mCategoryWorkRef.id
            mCategory!!.title = editTextTitle.text.toString()
            mCategory!!.color = mColor
            mCategory!!.factor = mNewFactor

            val batch = db.batch()
            batch.set(mCategoryWorkRef, mCategory!!)
            // store a "last edited" reference
            batch.update(userRef, WlbUser.FBP_LAST_CATEGORY_WORK, mCategoryWorkRef.id)

            mCategory?.sessions?.forEach { session ->
                batch.update(userRef.collection(Session.FBP).document(session),
                        Category.FBP,
                        mCategory!!.toMapNoSessions())
            }

            // additionally store copy on user document
            batch.update(db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid),
                    "default_" + Category.FBP + "_" + Category.TYPE_WORK, mCategory!!.toMapNoSessions())

            batch.commit()
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to update Category Off! Error: ", e)
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
