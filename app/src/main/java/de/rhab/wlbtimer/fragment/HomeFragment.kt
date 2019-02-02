package de.rhab.wlbtimer.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.jakewharton.threetenabp.AndroidThreeTen
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.adapter.SessionRecyclerViewAdapter
import de.rhab.wlbtimer.model.Session
import android.support.v7.widget.LinearLayoutManager


class HomeFragment : Fragment() {

    private var mListener: OnListFragmentInteractionListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private var databaseReference: DatabaseReference? = null
    private var mSessionsRef: DatabaseReference? = null

    private var mSessionListener: ValueEventListener? = null

    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    //    private var mLayoutManager: RecyclerView.LayoutManager? = null

    private val mSessions: MutableList<Session> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        // Java8 310 Backport https://github.com/JakeWharton/ThreeTenABP
        AndroidThreeTen.init(context)

        mRecyclerView = rootView.findViewById(R.id.list_recycler_view)

        // this works.. but can also be done in RecyclerView XML section
        val mLayoutManager = LinearLayoutManager(this.activity)
        mLayoutManager.reverseLayout = true
        mLayoutManager.stackFromEnd = true
        mRecyclerView!!.layoutManager = mLayoutManager


        // this adds divider lines.. not so cool
//        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
//        mRecyclerView!!.addItemDecoration(itemDecoration)

        return rootView
    }


    private fun basicListen() {
        // Get a reference to the Firebase Database
        databaseReference = FirebaseDatabase.getInstance().reference
        mSessionsRef = databaseReference!!.child(Session.FBP).child(mAuth.currentUser!!.uid)
        mSessionListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // New data at this path. This method will be called after every change in the
                // data at this path or a subpath.

                mSessions.clear()

                // Get the data as Session objects
                Log.d(TAG, "Number of sessions: " + dataSnapshot.childrenCount)

                for (child in dataSnapshot.children) {
                    // Extract a Message object from the DataSnapshot
                    Log.d(TAG, child.key)
                    val session = child.getValue<Session>(Session::class.java)
                    if (session != null) {
                        mSessions.add(session)
                    }

                    // Use the Session
                    // [START_EXCLUDE]
                    Log.d(TAG, "session start:" + session!!.tsStart + "session end:" + session.tsEnd)
                    // [END_EXCLUDE]
                }

                mAdapter = SessionRecyclerViewAdapter(mSessions, mListener)
                mRecyclerView?.adapter = mAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                // Could not successfully listen for data, log the error
                Log.e(TAG, "sessions:onCancelled:" + error.message)
            }
        }
        // order by Start
        mSessionsRef!!.orderByChild("tsStartForward").addValueEventListener(mSessionListener!!)
    }

    private fun cleanBasicListener() {
        // Clean up value listener
        mSessionsRef!!.removeEventListener(mSessionListener!!)
    }


    override fun onStart() {
        super.onStart()
        Log.d(TAG, "Home:onStart")

        if (mAuth.currentUser != null) {
            Log.d(TAG, "Home:onStart:is_signed_in:")
            basicListen()
        } else {
            Log.d(TAG, "Home:onStart:is_signed_out")
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "Home::onStop")

        if (mAuth.currentUser != null) {
            Log.d(TAG, "Home:onStop:is_signed_in:")
            cleanBasicListener()
        } else {
            Log.d(TAG, "Home:onStop:is_signed_out")
        }

    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html) for
     * more information.
     */
    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: Session)
    }

    companion object {

        private const val TAG = "HomeFragment"  // Log Tag

        fun newInstance(): HomeFragment {
            val fragmentHome = HomeFragment()
            val args = Bundle()
            fragmentHome.arguments = args
            return fragmentHome
        }
    }
}