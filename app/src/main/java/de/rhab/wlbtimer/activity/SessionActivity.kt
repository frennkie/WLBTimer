package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
//                .orderBy("tsStartForward")

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
                AlertDialog.Builder(recyclerView.context)
                        .setTitle("Disabled")
                        .setMessage("Deleting Session by swipe is currently disabled")
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                // ToDo(frennkie) this (also) thinks it has removed the entry)
                // mAdapter.deleteItem(viewHolder.adapterPosition)
            }
        }).attachToRecyclerView(recyclerView)

        mAdapter.setOnItemClickListener(object : SessionAdapter.OnItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
                val session = documentSnapshot.toObject(Session::class.java)
                val id = documentSnapshot.id

                val sessionBottomDialogFragment = SessionBottomSheetFragment.newInstance()
                val bundle = Bundle()

                bundle.putString(SessionBottomSheetFragment.ARG_SESSION_ID, session!!.objectId)
                sessionBottomDialogFragment.arguments = bundle
                sessionBottomDialogFragment.show(supportFragmentManager, "session_dialog_fragment")

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