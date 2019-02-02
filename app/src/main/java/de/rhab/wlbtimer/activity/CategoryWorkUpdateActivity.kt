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
import com.google.firebase.database.*
import com.larswerkman.holocolorpicker.ColorPicker
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.model.CategoryWork
import de.rhab.wlbtimer.model.Session
import de.rhab.wlbtimer.model.WlbUser


class CategoryWorkUpdateActivity : AppCompatActivity() {

    private val mDatabaseRef = FirebaseDatabase.getInstance().reference

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mCategoryWorkRef: DatabaseReference
    private lateinit var mCategoryWorkListener: ValueEventListener
    private var mCategoryWorkList = HashMap<String, CategoryWork>()

    private lateinit var mColor: String

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

        var mKey = intent.getStringExtra("KEY")
        val mPathStr = intent.getStringExtra("PATH")
        val mTitle = intent.getStringExtra("TITLE")
        val mFactor = intent.getStringExtra("FACTOR")
        mColor = intent.getStringExtra("COLOR") ?: "#A89AAF"

        Log.d(TAG, "asked to edit CategoryWork with key: $mKey and path: $mPathStr")
        Log.d(TAG, "asked to edit CategoryWork with factor: $mFactor")

        if (mKey == null) {
            setTitle(R.string.title_activity_category_work_add)
        } else {
            setTitle(R.string.title_activity_category_work_update)
        }




        mCategoryWorkRef = mDatabaseRef.child(WlbUser.FBP).child(mAuth.currentUser!!.uid).child(CategoryWork.FBP)
        mCategoryWorkListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach { child ->
                    val mCategoryWork = child.getValue(CategoryWork::class.java)
                    if (mCategoryWork != null) {
                        mCategoryWorkList[mCategoryWork.objectId] = mCategoryWork
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Could not successfully listen for data, log the error
                Log.e(TAG, "category_work:onCancelled: ${error.message}")
            }
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
        buttonSaveCategoryWork.setOnClickListener {

            Log.d(TAG, "foo")

            if (mKey == null) {
                Log.d(TAG, "add CategoryWork")
                Toast.makeText(this, "Adding...", Toast.LENGTH_SHORT).show()

                mKey = mDatabaseRef.child(WlbUser.FBP)
                    .child(mAuth.currentUser!!.uid)
                    .child(CategoryWork.FBP).push().key
                if (mKey == null) {
                    Log.w(TAG, "Couldn't get push key")
                }

            } else {
                Log.d(TAG, "update CategoryWork")
                Toast.makeText(this, "Updating...", Toast.LENGTH_SHORT).show()
            }

            val mNewFactor = if (editTextFactor.text.toString().isEmpty()) {
                1.0
            } else {
                editTextFactor.text.toString().toDouble()
            }

            val childUpdates = HashMap<String, Any>()

            // ToDo(frennkie) writing a new object here will delete the sessions data
            val mCategoryWork: CategoryWork
            if (mCategoryWorkList[mKey] != null) {
                // update to existing CategoryWork
                mCategoryWork = mCategoryWorkList[mKey]!!
                mCategoryWork.title = editTextTitle.text.toString()
                mCategoryWork.color = mColor
                mCategoryWork.factor = mNewFactor

                // also push changes to all sessions
                // ToDo - first check whether that session exists..?! to avoid ghost sessions?!
                mCategoryWork.sessions.forEach {
                    val pathX = mDatabaseRef.child(Session.FBP).child(mAuth.currentUser!!.uid)
                            .child(it.key).child(CategoryWork.FBP_SHORT).path.toString()
                    childUpdates[pathX] = mCategoryWork.toMapNoSessions()
                }

            } else {
                // new CategoryWork
                mCategoryWork = CategoryWork(
                        objectId = mKey,
                        title = editTextTitle.text.toString(),
                        color = mColor,
                        factor = mNewFactor
                )
            }


            val pathA = mDatabaseRef.child(WlbUser.FBP).child(mAuth.currentUser!!.uid)
                    .child(CategoryWork.FBP).child(mKey).path.toString()
            childUpdates[pathA] = mCategoryWork.toMap()

            mDatabaseRef.updateChildren(childUpdates)

            finish()

        }

    }

    public override fun onStart() {
        super.onStart()
        mCategoryWorkRef.addValueEventListener(mCategoryWorkListener)
        mAuth.addAuthStateListener(mAuthListener!!)
    }

    public override fun onStop() {
        super.onStop()
        mCategoryWorkRef.removeEventListener(mCategoryWorkListener)
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener!!)
        }
    }

    companion object {

        private const val TAG = "CatWorkUpdateAct"
    }
}
