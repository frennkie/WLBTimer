package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.jakewharton.threetenabp.AndroidThreeTen
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.adapter.SessionAdapter
import de.rhab.wlbtimer.fragment.SessionBottomSheetFragment
import de.rhab.wlbtimer.model.Break
import de.rhab.wlbtimer.model.Session
import de.rhab.wlbtimer.model.WlbUser


class MainActivity : AppCompatActivity(), SessionBottomSheetFragment.BottomSheetListener {

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mDrawerLayout: DrawerLayout

    private lateinit var fabStartNew: View
    private lateinit var fabStopRunning: View

    private lateinit var mAdapter: SessionAdapter

    private val db = FirebaseFirestore.getInstance()

    private lateinit var userRef: DocumentReference

    private var mSessionRunningListener: ListenerRegistration? = null

    private var mSessionRunningStatus: Boolean = false

    var mSessionRunningId: String? = null


    private var content: FrameLayout? = null

    var mCurrentFragment: String? = "HomeFragment"

//    private fun addFragment(fragment: Fragment) {
//        supportFragmentManager
//                .beginTransaction()
//                .setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out)
//                .replace(R.id.content, fragment, fragment.javaClass.simpleName)
//                .addToBackStack(fragment.javaClass.simpleName)
//                .commit()
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                Log.d(TAG, "onAuthStateChanged:signed_out")
                startActivity(Intent(this@MainActivity, SignInActivity::class.java))
            } else {
                Log.d(TAG, "onAuthStateChanged:signed_in:" + auth.currentUser!!.uid)
            }
        }
        Log.d(TAG, "Passed auth check")

        // Java8 310 Backport https://github.com/JakeWharton/ThreeTenABP
        AndroidThreeTen.init(this)

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<android.support.v7.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionbar: ActionBar? = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
        }

        mDrawerLayout = findViewById(R.id.drawer_layout)

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // set item as selected to persist highlight
            // ToDo(frennkie) launchMode="singleTop" no longer restarts .. therefore
            // highlight makes no sense when coming back
            menuItem.isChecked = false
            // close drawer when item is tapped
            mDrawerLayout.closeDrawers()

            // Add code here to update the UI based on the item selected
            // For example, swap UI fragments here

            when (menuItem.itemId) {
                R.id.nav_home -> {
                    Log.d(TAG, "DrawerNav: Go Home")
                }

                R.id.nav_sessions -> {
                    Log.d(TAG, "DrawerNav: Sessions")
                    startActivity(Intent(this, SessionActivity::class.java))
                }
                R.id.nav_break -> {
                    Log.d(TAG, "DrawerNav: Break Rules")
                    startActivity(Intent(this, BreaksActivity::class.java))
                }
                R.id.nav_category_work -> {
                    Log.d(TAG, "DrawerNav: Category Work")
                    startActivity(Intent(this, CategoryWorkActivity::class.java))
                }
                R.id.nav_category_non_work -> {
                    Log.d(TAG, "DrawerNav: Category Non Work")
                    startActivity(Intent(this, CategoryNonWorkActivity::class.java))
                }
                R.id.nav_category_note -> {
                    Log.d(TAG, "DrawerNav: Note")
                    startActivity(Intent(this, NoteActivity::class.java))
                }
                R.id.nav_report -> {
                    Log.d(TAG, "DrawerNav: Nav Report")
                    val intent = Intent(this, ReportActivity::class.java)
                    val message = "From Drawer"

                    intent.putExtra(ReportActivity.INTENT_EXTRA_MESSAGE, message)
                    startActivityForResult(intent, TEXT_REQUEST_REPORT_ACTIVITY_TEST)
                }
                R.id.nav_settings -> {
                    Log.d(TAG, "DrawerNav: Settings")
                    // open SettingsActivity
                    startActivity(Intent(applicationContext, SettingsActivity::class.java))
                }
                R.id.nav_sign_out -> {
                    Log.d(TAG, "DrawerNav: Sign out")
                    // sign out
                    signOut()
                }
                else -> {
                    Log.d(TAG, "DrawerNav: Not Found")
                }
            }
            false
        }

        val headerView = navigationView.getHeaderView(0)
        val tvTopTitle = headerView.findViewById<TextView>(R.id.tv_drawer_top_title)

        if (mAuth.currentUser != null) {
            if (mAuth.currentUser!!.isAnonymous) {
                Log.d(TAG, "anon: Guest")
                tvTopTitle.text = "Guest"
            } else {
                Log.d(TAG, "anon: User: ${mAuth.currentUser!!.uid}")
                Log.d(TAG, "anon: UserMail: ${mAuth.currentUser!!.email}")
                if (mAuth.currentUser!!.email != null) {
                    tvTopTitle.text = "${mAuth.currentUser!!.email}\n${mAuth.currentUser!!.uid}"
                } else {
                    tvTopTitle.text = mAuth.currentUser!!.uid
                }
            }
        }

        fabStartNew = findViewById(R.id.fabStartNew)
        fabStopRunning = findViewById(R.id.fabStopRunning)

//
//        content = findViewById(R.id.content)
//
//        val fragment = HomeFragment.newInstance()

//        addFragment(fragment)
//
//        supportFragmentManager
//                .beginTransaction()
//                .setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out)
//                .replace(R.id.content, fragment, fragment.javaClass.simpleName)
//                .commit()

        if (mSessionRunningStatus) {
            fabStartNew.visibility = View.GONE
            fabStopRunning.visibility = View.VISIBLE

        } else {
            fabStartNew.visibility = View.VISIBLE
            fabStopRunning.visibility = View.GONE

        }

        fabStartNew.setOnClickListener { _ ->
            startNewSession()
        }

        fabStopRunning.setOnClickListener { _ ->
            stopRunningSession()
        }

        // ToDo(frennkie) hm...
        if (mAuth.currentUser != null) {
            userRef = db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid)

            setUpRecyclerView()

        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_top, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mDrawerLayout.openDrawer(GravityCompat.START)
                true
            }

            R.id.action_add -> {
                val sessionBottomDialogFragment = SessionBottomSheetFragment.newInstance()
                sessionBottomDialogFragment.show(supportFragmentManager, "session_dialog_fragment")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
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
        Log.d(TAG, "Adapter: $mAdapter")

        val recyclerView = findViewById<RecyclerView>(R.id.session_main_recycler_view)
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
                AlertDialog.Builder(viewHolder.itemView.context)
                        .setTitle("Disabled")
                        .setMessage("Deleting Session by swipe is currently disabled")
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                mAdapter.notifyItemChanged(viewHolder.adapterPosition)
                // mAdapter.deleteItem(viewHolder.adapterPosition)
            }
        }).attachToRecyclerView(recyclerView)

        mAdapter.setOnItemClickListener(object : SessionAdapter.OnItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
                val session = documentSnapshot.toObject(Session::class.java)
                val sessionBottomDialogFragment = SessionBottomSheetFragment.newInstance()
                val bundle = Bundle()

                bundle.putString(SessionBottomSheetFragment.ARG_SESSION_ID, session!!.objectId)
                sessionBottomDialogFragment.arguments = bundle
                sessionBottomDialogFragment.show(supportFragmentManager, "session_dialog_fragment")

            }
        })

    }

//    private fun addNewSessionSingleChoice() {
//
//        val listItems = arrayOf("Item 1", "Item 2", "Item 3")
//        val mBuilder = AlertDialog.Builder(this)
//        mBuilder.setTitle("Choose an item")
//        mBuilder.setSingleChoiceItems(listItems, -1) { dialogInterface, i ->
//            Log.i(TAG, listItems[i])
//            dialogInterface.dismiss()
//        }
//        // Set the neutral/cancel button click listener
//        mBuilder.setNeutralButton("Cancel") { dialog, _ ->
//            // Do something when click the neutral button
//            dialog.cancel()
//        }
//
//        val mDialog = mBuilder.create()
//        mDialog.show()
//    }

    private fun startNewSession() {
        // get a new "push key" for document
        val mSessionRef = userRef.collection(Session.FBP).document()

        val mSession = Session(
                // ToDo(frennkie) check for default Category
                //session.category = CategoryWork(key?!, "Foobar")
                tsStart = Session.getZonedDateTimeNow().toString(),
                tsEnd = null,
                allDay = false,
                finished = false,
                objectId = mSessionRef.id  // additionally store push key
        )

        val batch = db.batch()

        // store session-running
        val mSessionRunning = HashMap<String, String>()
        mSessionRunning[Session.FBP_SESSION_RUNNING] = mSessionRef.id
        batch.set(userRef, mSessionRunning)

        batch.set(mSessionRef, mSession.toMap())  // ToDo(frennkie) check

        batch.commit()
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to start new session! Error: ", e)
                    // this catches the error.. may be do something with this?! UI does reflect the
                    // intended change until refresh!
                }
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully started new Session")

                    val mSnackbar = Snackbar.make(findViewById(R.id.main_content),
                            "started new Session...", Snackbar.LENGTH_LONG)
                    mSnackbar.setAction("VIEW") { _ ->
                        val sessionBottomDialogFragment = SessionBottomSheetFragment.newInstance()
                        val bundle = Bundle()
                        bundle.putString(SessionBottomSheetFragment.ARG_SESSION_ID, mSession.objectId)
                        sessionBottomDialogFragment.arguments = bundle
                        sessionBottomDialogFragment.show(supportFragmentManager, "session_dialog_fragment")
                    }
                    mSnackbar.show()

                }

    }

    private fun stopRunningSession() {
        Log.d(TAG, "stopRunningSession called")

        if (!mSessionRunningStatus) {
            Log.w(TAG, "no session running")
            return
        }

        if (mSessionRunningId == null) {
            Log.w(TAG, "no session running id")
            return
        }

        Log.d(TAG, "current session running: $mSessionRunningId")

        // get Session Object
        userRef.collection(Session.FBP)
                .document(mSessionRunningId!!)
                .get()
                .addOnFailureListener { e ->
                    Log.d(TAG, "get failed with ", e)
                }
                .addOnSuccessListener { documentSnapshot ->

                    if (documentSnapshot != null) {
                        val mSession = documentSnapshot.toObject(Session::class.java)!!
                        Log.w(TAG, "found: $mSession")

                        val batch = db.batch()

                        // remove session-running
                        batch.update(userRef, Session.FBP_SESSION_RUNNING, FieldValue.delete())

                        mSession.objectId = mSessionRunningId
                        mSession.finished = true
                        mSession.tsEnd = Session.getZonedDateTimeNow().toString()

                        if (mSession.breaks == null) {
                            val mDefaultBreak = Break(comment = "Default Break")
                            val mBreakDuration = mDefaultBreak.applyBreakRules(mSession.getDurationLong())
                            Log.d(TAG, "no breaks present - adding according to rules (length: $mBreakDuration)")
                            mDefaultBreak.duration = mBreakDuration.toInt()
                            mSession.breaks = mutableListOf(mDefaultBreak)
                        }

                        val mSessionRef = userRef.collection(Session.FBP).document(mSessionRunningId!!)
                        batch.set(mSessionRef, mSession.toMap())

                        batch.commit()
                                .addOnFailureListener {
                                    Log.w(TAG, "error!")
                                    // this catches the error.. may be do something with this?! UI does reflect the
                                    // intended change until refresh!
                                }
                                .addOnSuccessListener {
                                    Log.d(TAG, "success!")
                                }

                        Snackbar.make(findViewById(R.id.main_content),
                                "finished Session (${mSession.getDurationWeightedExcludingBreaks()})",
                                Snackbar.LENGTH_LONG).show()

                    }
                }

    }

    private fun setUiSessionRunning(mId: String) {
        mSessionRunningId = mId
        mSessionRunningStatus = true
        fabStartNew.visibility = View.GONE
        fabStopRunning.visibility = View.VISIBLE
    }

    private fun unsetUiSessionRunning() {
        mSessionRunningId = null
        mSessionRunningStatus = false
        fabStartNew.visibility = View.VISIBLE
        fabStopRunning.visibility = View.GONE
    }

    private fun monitorSessionRunning() {
        mSessionRunningListener = db.collection(WlbUser.FBP)
                .document(mAuth.currentUser!!.uid)
                .addSnapshotListener(EventListener<DocumentSnapshot> { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "monitorSessionRunning Listen failed.", e)
                        return@EventListener
                    }

                    if (snapshot != null
                            && snapshot.exists()
                            && snapshot.contains(Session.FBP_SESSION_RUNNING)) {

                        snapshot.data?.get(Session.FBP_SESSION_RUNNING).toString()
                        Log.d(TAG, "monitorSessionRunning found running session")
                        setUiSessionRunning(snapshot.data?.get(Session.FBP_SESSION_RUNNING).toString())

                    } else {
                        unsetUiSessionRunning()
                    }
                })

    }

    override fun onOptionClick(text: String) {
        //change text on each item click
        Log.d(TAG, "onOptionClick: $text")
    }

    public override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener!!)
        if (mAuth.currentUser != null) {
            Log.d(TAG, "setup mSessionRunningListener and mAdapter from onStart")
            monitorSessionRunning()
            mAdapter.startListening()
        } else {
            Log.d(TAG, "NOT starting monitorSessionRunning from onStart")
        }
    }

    public override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener!!)
        }
        if (mAuth.currentUser != null) {
            Log.d(TAG, "removing mSessionRunningListener and mAdapter from onStop")
            mSessionRunningListener?.remove()
            mAdapter.stopListening()
        }
    }

    private fun signOut() {
        AuthUI.getInstance().signOut(this)
                .addOnCompleteListener {
                    Log.i(TAG, "Signed out")
                    startActivity(Intent(this@MainActivity, SignInActivity::class.java))
                    finish()
                }
    }

    companion object {

        private const val TAG = "MainActivity"

        // Unique tag for the intent reply
        const val TEXT_REQUEST_REPORT_ACTIVITY_TEST = 1

    }

}