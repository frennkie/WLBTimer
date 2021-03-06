package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.adapter.CategoryOffAdapter
import de.rhab.wlbtimer.databinding.ActivityCategoryOffBinding
import de.rhab.wlbtimer.model.Category
import de.rhab.wlbtimer.model.WlbUser


@Keep
class CategoryOffActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryOffBinding

    private lateinit var mAdapter: CategoryOffAdapter

    private val db = FirebaseFirestore.getInstance()

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCategoryOffBinding.inflate(layoutInflater)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@CategoryOffActivity, SignInActivity::class.java))
            }
        }

        setContentView(binding.root)
        setSupportActionBar(binding.myToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val buttonAddCategoryOff = findViewById<FloatingActionButton>(R.id.button_add_category_off)
        buttonAddCategoryOff.setOnClickListener {
            startActivity(Intent(this@CategoryOffActivity, CategoryOffUpdateActivity::class.java))
        }

        setUpRecyclerView()

    }

    private fun setUpRecyclerView() {
        // sort Categories alphabetically by title
        val query = db.collection(WlbUser.FBP)
            .document(mAuth.currentUser!!.uid)
            .collection(Category.FBP)
            .whereEqualTo("type", Category.TYPE_OFF)
            .orderBy("title", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<Category>()
            .setQuery(query, Category::class.java)
            .build()

        mAdapter = CategoryOffAdapter(options)

        val recyclerView =
            findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.category_off_recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.adapter = mAdapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                direction: Int
            ) {
                mAdapter.deleteItem(viewHolder.adapterPosition)
            }
        }).attachToRecyclerView(recyclerView)

        mAdapter.setOnItemClickListener(object : CategoryOffAdapter.OnItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
                val categoryOff = documentSnapshot.toObject(Category::class.java)!!
                val id = documentSnapshot.id
                val color = categoryOff.color
                val title = categoryOff.title

                // only serializable data can be sent to intent via putExtra
                val intent = Intent(this@CategoryOffActivity, CategoryOffUpdateActivity::class.java)
                intent.putExtra("KEY", id)
                intent.putExtra("COLOR", color)
                intent.putExtra("TITLE", title)
                startActivity(intent)
            }

            override fun onItemLongClick(snapshots: ObservableSnapshotArray<Category>,
                                         documentSnapshot: DocumentSnapshot, position: Int) {
                val category = documentSnapshot.toObject(Category::class.java) ?: return

                val batch = db.batch()

                // set default to false for all entries
                for (i in 0 until snapshots.count()) {
                    batch.update(snapshots.getSnapshot(i).reference, "default", false)
                }

                // set default to true on the selected entry
                batch.update(documentSnapshot.reference, "default", true)
                // additionally store copy on user document
                batch.update(db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid),
                        "default_" + Category.FBP + "_" + Category.TYPE_OFF, category.toMapNoSessions())
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

        private const val TAG = "CategoryOffActivity"
    }
}
