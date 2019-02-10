package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.adapter.CategoryWorkAdapter
import de.rhab.wlbtimer.model.Category
import de.rhab.wlbtimer.model.WlbUser

class CategoryWorkActivity : AppCompatActivity() {

    private lateinit var mAdapter: CategoryWorkAdapter

    private val db = FirebaseFirestore.getInstance()

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@CategoryWorkActivity, SignInActivity::class.java))
            }
        }

        setContentView(R.layout.activity_category_work)

        val toolbar = findViewById<android.support.v7.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val buttonAddCategoryWork = findViewById<FloatingActionButton>(R.id.button_add_category_work)
        buttonAddCategoryWork.setOnClickListener {
             startActivity(Intent(this@CategoryWorkActivity, CategoryWorkUpdateActivity::class.java))
        }

        setUpRecyclerView()

    }

    private fun setUpRecyclerView() {
        // sort Categories alphabetically by title
        val query = db.collection(WlbUser.FBP)
                .document(mAuth.currentUser!!.uid)
                .collection(Category.FBP)
                .whereEqualTo("type", Category.TYPE_WORK)
                .orderBy("title", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<Category>()
                .setQuery(query, Category::class.java)
                .build()

        mAdapter = CategoryWorkAdapter(options)

        val recyclerView = findViewById<RecyclerView>(R.id.category_work_recycler_view)
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

        mAdapter.setOnItemClickListener(object : CategoryWorkAdapter.OnItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
                val categoryWork = documentSnapshot.toObject(Category::class.java)!!
                val id = documentSnapshot.id
                val color = categoryWork.color
                val title = categoryWork.title
                val factor = categoryWork.factor

                // only serializable data can be sent to intent via putExtra
                val intent = Intent(this@CategoryWorkActivity, CategoryWorkUpdateActivity::class.java)
                intent.putExtra("ID", id)
                intent.putExtra("COLOR", color)
                intent.putExtra("TITLE", title)
                intent.putExtra("FACTOR", factor.toString())
                startActivity(intent)
            }
            override fun onItemLongClick(snapshots: ObservableSnapshotArray<Category>,
                                         documentSnapshot: DocumentSnapshot, position: Int) {
                val batch = db.batch()

                for (i in 0 until snapshots.count()) {
                    batch.update(snapshots.getSnapshot(i).reference, "default", false)
                }

                batch.update(documentSnapshot.reference, "default", true)
                batch.commit()
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

        private const val TAG = "CatWorkActivity"
    }
}
