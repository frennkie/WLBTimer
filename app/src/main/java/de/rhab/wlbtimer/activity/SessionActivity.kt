package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.adapter.SessionAdapter
import de.rhab.wlbtimer.databinding.ActivitySessionBinding
import de.rhab.wlbtimer.model.Session
import de.rhab.wlbtimer.model.WlbUser


@Keep
class SessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionBinding

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mAdapter: SessionAdapter

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySessionBinding.inflate(layoutInflater)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@SessionActivity, SignInActivity::class.java))
            }
        }

        setContentView(binding.root)
        setSupportActionBar(binding.myToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setUpRecyclerView()

    }

    private fun setUpRecyclerView() {
        val query = db.collection(WlbUser.FBP)
            .document(mAuth.currentUser!!.uid)
            .collection(Session.FBP)
            .orderBy("tsStart", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Session>()
            .setQuery(query, Session::class.java)
            .build()

        mAdapter = SessionAdapter(options)

        val recyclerView =
            findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.session_recycler_view)
        recyclerView.setHasFixedSize(true)  // ToDo(frennkie) fixed!? yes or no
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

                val mBuilder = AlertDialog.Builder(recyclerView.context)
                mBuilder.setTitle("Delete entry?")
                mBuilder.setMessage("Are you sure you want to delete this entry? This can not be undone!")
                mBuilder.setNegativeButton(R.string.session_cancel_delete) { _, _ ->
                    Log.d(TAG, "canceled swipe delete")
                    mAdapter.notifyItemChanged(viewHolder.adapterPosition)  // ToDo(frennkie) this is "costly"
                }
                mBuilder.setPositiveButton(R.string.session_confirm_delete) { _, _ ->
                    Log.d(TAG, "deleted by swipe")
                    mAdapter.deleteItem(viewHolder.adapterPosition)
                }
                mBuilder.setCancelable(false)  // user has two press one of the two buttons

                val mDialog = mBuilder.create()
                mDialog.show()

            }
        }).attachToRecyclerView(recyclerView)

        mAdapter.setOnItemClickListener(object : SessionAdapter.OnItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
                val session = documentSnapshot.toObject(Session::class.java)
                val id = documentSnapshot.id

                Snackbar.make(findViewById(R.id.session_recycler_view),
                        "ToDo: Start Session Detail/Update Activity on $id ${session?.objectId}",
                        Snackbar.LENGTH_LONG).show()

            }
        })

    }

    public override fun onStart() {
        super.onStart()
        mAdapter.startListening()
        mAuth.addAuthStateListener(mAuthListener!!)
    }

    public override fun onStop() {
        super.onStop()
        mAdapter.stopListening()
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener!!)
        }
    }

    companion object {

        private const val TAG = "SessionActivity"

    }

}