package de.rhab.wlbtimer.fragment
//
//import android.os.Bundle
//import android.support.v4.app.Fragment
//import android.support.v7.widget.RecyclerView
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import com.google.firebase.auth.FirebaseAuth
//import com.jakewharton.threetenabp.AndroidThreeTen
//import de.rhab.wlbtimer.model.Session
//import android.support.v7.widget.LinearLayoutManager
//import com.google.firebase.firestore.*
//import de.rhab.wlbtimer.R
//import de.rhab.wlbtimer.model.WlbUser
//
//
//class HomeFragment : Fragment() {
//
//    private val mAuth = FirebaseAuth.getInstance()
//
//    private val db = FirebaseFirestore.getInstance()
//
//    private var mSessionListener: ListenerRegistration? = null
//
//    private var mRecyclerView: RecyclerView? = null
//    private var mAdapter: RecyclerView.Adapter<*>? = null
//    //    private var mLayoutManager: RecyclerView.LayoutManager? = null
//
//    private val mSessions: MutableList<Session> = mutableListOf()
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        val rootView = inflater.inflate(R.layout.fragment_home, container, false)
//
//        // Java8 310 Backport https://github.com/JakeWharton/ThreeTenABP
//        AndroidThreeTen.init(context)
//
//        mRecyclerView = rootView.findViewById(R.id.list_recycler_view)
//
//        // this works.. but can also be done in RecyclerView XML section
//        val mLayoutManager = LinearLayoutManager(this.activity)
//        mLayoutManager.reverseLayout = true
//        mLayoutManager.stackFromEnd = true
//        mRecyclerView!!.layoutManager = mLayoutManager
//
//
//        // this adds divider lines.. not so cool
////        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
////        mRecyclerView!!.addItemDecoration(itemDecoration)
//
//        return rootView
//    }
//
//    private fun basicListen() {
//        mSessionListener = db.collection(WlbUser.FBP)
//                .document(mAuth.currentUser!!.uid)
//                .collection(Session.FBP)
//                .addSnapshotListener(EventListener<QuerySnapshot> { value, e ->
//                    if (e != null) {
//                        Log.w(TAG, "monitorSessionRunning Listen failed.", e)
//                        return@EventListener
//                    }
//
//                    mSessions.clear()
//
//                    for (doc in value!!) {
//                        mSessions.add(doc.toObject(Session::class.java))
//                    }
//                })
//    }
//
//    private fun cleanBasicListener() {
//        // Clean up value listener
//        mSessionListener?.remove()
//    }
//
//
//    override fun onStart() {
//        super.onStart()
//        Log.d(TAG, "Home:onStart")
//
//        if (mAuth.currentUser != null) {
//            Log.d(TAG, "Home:onStart:is_signed_in:")
//            basicListen()
//        } else {
//            Log.d(TAG, "Home:onStart:is_signed_out")
//        }
//    }
//
//    override fun onStop() {
//        super.onStop()
//        Log.d(TAG, "Home::onStop")
//
//        if (mAuth.currentUser != null) {
//            Log.d(TAG, "Home:onStop:is_signed_in:")
//            cleanBasicListener()
//
//        }
//    }
//
//
//    /**
//     * This interface must be implemented by activities that contain this
//     * fragment to allow an interaction in this fragment to be communicated
//     * to the activity and potentially other fragments contained in that
//     * activity.
//     *
//     *
//     * See the Android Training lesson [Communicating with Other Fragments]
//     * (http://developer.android.com/training/basics/fragments/communicating.html) for
//     * more information.
//     */
//    interface OnListFragmentInteractionListener {
//        fun onListFragmentInteraction(item: Session)
//    }
//
//    companion object {
//
//        private const val TAG = "HomeFragment"  // Log Tag
//
//        fun newInstance(): HomeFragment {
//            val fragmentHome = HomeFragment()
//            val args = Bundle()
//            fragmentHome.arguments = args
//            return fragmentHome
//        }
//    }
//}