package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.adapter.CategoryNonWorkAdapter
import de.rhab.wlbtimer.model.CategoryNonWork
import de.rhab.wlbtimer.model.WlbUser

class CategoryNonWorkActivity : AppCompatActivity() {

    private lateinit var mAdapter: CategoryNonWorkAdapter

    private val db = FirebaseFirestore.getInstance()

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@CategoryNonWorkActivity, SignInActivity::class.java))
            }
        }

        setContentView(R.layout.activity_category_non_work)

        val toolbar = findViewById<android.support.v7.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val buttonAddCategoryNonWork = findViewById<FloatingActionButton>(R.id.button_add_category_non_work)
        buttonAddCategoryNonWork.setOnClickListener {
             startActivity(Intent(this@CategoryNonWorkActivity, CategoryNonWorkUpdateActivity::class.java))
        }

        setUpRecyclerView()

    }

    private fun setUpRecyclerView() {
        // sort Categories alphabetically by title
        val query = db.collection(WlbUser.FBP)
                .document(mAuth.currentUser!!.uid)
                .collection(CategoryNonWork.FBP)
                .orderBy("title", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<CategoryNonWork>()
                .setQuery(query, CategoryNonWork::class.java)
                .build()

        mAdapter = CategoryNonWorkAdapter(options)

        val recyclerView = findViewById<RecyclerView>(R.id.category_non_work_recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = mAdapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                mAdapter.deleteItem(viewHolder.adapterPosition)
            }
        }).attachToRecyclerView(recyclerView)

        mAdapter.setOnItemClickListener(object : CategoryNonWorkAdapter.OnItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
                val categoryNonWork = documentSnapshot.toObject(CategoryNonWork::class.java)!!
                val id = documentSnapshot.id
                val color = categoryNonWork.color
                val title = categoryNonWork.title

                // only serializable data can be sent to intent via putExtra
                val intent = Intent(this@CategoryNonWorkActivity, CategoryNonWorkUpdateActivity::class.java)
                intent.putExtra("KEY", id)
                intent.putExtra("COLOR", color)
                intent.putExtra("TITLE", title)
                startActivity(intent)
            }
        })

    }

    public override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener!!)
        mAdapter.startListening()
    }

    public override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener!!)
        }
        mAdapter.stopListening()
    }

    companion object {

        private const val TAG = "CatNonWorkActivity"
    }
}
