package de.rhab.wlbtimer.activity

import android.app.ProgressDialog.show
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.widget.Toast
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.adapter.SessionAdapter
import de.rhab.wlbtimer.fragment.SessionBottomSheetFragment
import de.rhab.wlbtimer.model.Session
import de.rhab.wlbtimer.model.WlbUser


class SessionActivity : AppCompatActivity() {

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mAdapter: SessionAdapter

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@SessionActivity, SignInActivity::class.java))
            }
        }

        setContentView(R.layout.activity_session)

        val toolbar = findViewById<android.support.v7.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
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

        val recyclerView = findViewById<RecyclerView>(R.id.session_recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = mAdapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                val mBuilder = AlertDialog.Builder(recyclerView.context)
                mBuilder.setTitle("Delete entry?")
                mBuilder.setMessage("Are you sure you want to delete this entry? This can not be undone!")
                mBuilder.setNeutralButton(R.string.session_cancel_delete) { _, _ ->
                    Log.d(TAG, "canceled swipe delete")
                    mAdapter.notifyItemChanged(viewHolder.adapterPosition)
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