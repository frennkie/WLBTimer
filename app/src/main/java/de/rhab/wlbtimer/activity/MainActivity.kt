package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Keep
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.jakewharton.threetenabp.AndroidThreeTen
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.adapter.SessionAdapter
import de.rhab.wlbtimer.databinding.ActivityMainBinding
import de.rhab.wlbtimer.fragment.SessionBottomSheetFragment
import de.rhab.wlbtimer.model.Break
import de.rhab.wlbtimer.model.Category
import de.rhab.wlbtimer.model.Session
import de.rhab.wlbtimer.model.WlbUser
import org.threeten.bp.LocalTime
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoField
import java.util.concurrent.TimeUnit


@Keep
class MainActivity : AppCompatActivity(), SessionBottomSheetFragment.BottomSheetListener {

    private lateinit var binding: ActivityMainBinding

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mDrawerLayout: DrawerLayout

    private lateinit var fabStartNew: View
    private lateinit var fabStopRunning: View

    private lateinit var mAdapter: SessionAdapter

    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView

    private val db = FirebaseFirestore.getInstance()

    private lateinit var userRef: DocumentReference

    private var mWlbUser: WlbUser? = null

    private var defBreak: Int? = 0

    private var defCategoryOff: Category? = null

    private var defCategoryWork: Category? = null

    private var mSessionRunningListener: ListenerRegistration? = null

    private var mSessionRunningStatus: Boolean = false

    private var mSessionRunningId: String? = null

    private lateinit var remoteConfig: FirebaseRemoteConfig

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

        binding = ActivityMainBinding.inflate(layoutInflater)

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

        setContentView(binding.root)
        setSupportActionBar(binding.myToolbar)

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
                R.id.nav_category_off -> {
                    Log.d(TAG, "DrawerNav: Category Off")
                    startActivity(Intent(this, CategoryOffActivity::class.java))
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
                R.id.nav_sign_out -> {
                    Log.d(TAG, "DrawerNav: Sign out")
                    // sign out
                    signOut()
                }
                R.id.nav_settings -> {
                    Log.d(TAG, "DrawerNav: Settings")
                    // open SettingsActivity
                    startActivity(Intent(applicationContext, SettingsActivity::class.java))
                }
                R.id.nav_help -> {
                    Log.d(TAG, "DrawerNav: Help")
                    // HelpActivity
                    startActivity(Intent(applicationContext, HelpActivity::class.java))
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
            var mTopTitleText = ""

            if (mAuth.currentUser!!.isAnonymous) {
                Log.d(TAG, "anon: Guest")
                mTopTitleText = "Guest"

            } else {
                mTopTitleText += if (mAuth.currentUser!!.displayName != null) {
                    if (mTopTitleText.isEmpty()) {
                        "${mAuth.currentUser!!.displayName}"
                    } else {
                        "\n${mAuth.currentUser!!.displayName}"
                    }
                } else {
                    // ToDo(frennkie) check this
                    if (mTopTitleText.isEmpty()) {
                        "NickName"
                    } else {
                        "\nNickName"
                    }
                }

                if (mAuth.currentUser!!.email != null) {
                    mTopTitleText += if (mTopTitleText.isEmpty()) {
                        "${mAuth.currentUser!!.email}"
                    } else {
                        "\n${mAuth.currentUser!!.email}"
                    }
                }
            }

            // ToDo(frennkie) for now always add the UID
            mTopTitleText += if (mTopTitleText.isEmpty()) {
                mAuth.currentUser!!.uid
            } else {
                "\n${mAuth.currentUser!!.uid}"
            }

            tvTopTitle.text = mTopTitleText

        }

        fabStartNew = findViewById(R.id.fabStartNew)
        fabStopRunning = findViewById(R.id.fabStopRunning)

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

        // Get Remote Config instance.
        val minimumFetchInvervalInSeconds: Long = if (BuildConfig.DEBUG) {
            0L
        } else {
            TimeUnit.HOURS.toSeconds(12)
        }

        remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(minimumFetchInvervalInSeconds)
                .build()
        )
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        remoteConfig.fetchAndActivate().addOnCompleteListener {
            Log.d(TAG, "addOnCompleteListener")

            val lastNumberOfEntries = getString(
                R.string.last_n_entries,
                remoteConfig.getString(MAIN_LAST_N_ENTRIES)
            )
            Log.d(TAG, "addOnCompleteListener: $lastNumberOfEntries")

            binding.tvMainHeaderLastNEntries.text = getString(
                R.string.last_n_entries,
                remoteConfig.getString(MAIN_LAST_N_ENTRIES)
            )

        }


        // ToDo(frennkie) hm...
        if (mAuth.currentUser != null) {
            setUpTop()
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

                val mBuilder = AlertDialog.Builder(this)

                mBuilder.setTitle("Choose type for new entry")

                val dialogLayout = layoutInflater.inflate(R.layout.alert_dialog_main_add, null)
                val llOff = dialogLayout.findViewById<LinearLayout>(R.id.linear_layout_off)
                val llWork = dialogLayout.findViewById<LinearLayout>(R.id.linear_layout_work)
                mBuilder.setView(dialogLayout)

                mBuilder.setNegativeButton(android.R.string.cancel) { _, _ -> }

                val mDialog = mBuilder.create()

                llOff.setOnClickListener {
                    addNewSessionOff()
                    mDialog.dismiss()

                }

                llWork.setOnClickListener {
                    addNewSessionWork()
                    mDialog.dismiss()
                }

                mDialog.show()

                true

            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpTop() {

        val docRefTenDays = db.collection(WlbUser.FBP)
            .document(mAuth.currentUser!!.uid)
            .collection(Session.FBP)
            .orderBy("tsStart", Query.Direction.DESCENDING)
            .limit(10)

        docRefTenDays.addSnapshotListener { documents, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)

                val topLeft = findViewById<TextView>(R.id.tv_main_header_left)
                topLeft.text = getString(R.string.last_10_entries, "N/A")

                return@addSnapshotListener
            }

            var tenDays: Long = 0
            for (document in documents!!) {
                Log.d(TAG, "${document.id} => ${document.data}")

                val session = document.toObject(Session::class.java)
                Log.d(TAG, "${document.id} => ${session.getDurationLong()}")

                if (session.category?.type.equals(Category.TYPE_WORK)) {
                    tenDays += session.getDurationExcludingBreaksLong()
                }

            }

            val tenDaysStr = String.format(
                "%02d:%02d",
                TimeUnit.SECONDS.toHours(tenDays),
                TimeUnit.SECONDS.toMinutes(tenDays) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.SECONDS.toHours(
                        tenDays
                    )
                ),
            )

            Log.d(TAG, "10 Day sum: $tenDaysStr")
            binding.tvMainHeaderLeft.text = getString(R.string.last_10_entries, tenDaysStr)

        }

        val docRefWork = db.collection(WlbUser.FBP)
            .document(mAuth.currentUser!!.uid)
            .collection(Session.FBP)
//            .whereEqualTo("category.type", Category.TYPE_WORK)
            .orderBy("tsStart", Query.Direction.DESCENDING)
            .limit(25)

        docRefWork.addSnapshotListener { documents, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)

                val topLeft = findViewById<TextView>(R.id.tv_main_header_left)
                topLeft.text = getString(R.string.last_10_entries, "N/A")

                return@addSnapshotListener
            }

            val startOfWeek =
                ZonedDateTime.now().with(ChronoField.DAY_OF_WEEK, 1).with(LocalTime.MIN)
                    .toEpochSecond()
            val endOfWeek =
                ZonedDateTime.now().with(ChronoField.DAY_OF_WEEK, 7).with(LocalTime.MAX)
                    .toEpochSecond()

            var workWeek: Long = 0
            for (document in documents!!) {
                Log.d(TAG, "${document.id} => ${document.data}")

                val session = document.toObject(Session::class.java)

                if (session.category?.type.equals(Category.TYPE_WORK)) {

                    if (session.tsStartForward in startOfWeek..endOfWeek) {
                        workWeek += session.getDurationExcludingBreaksLong()
                    }

                }
            }

            val workWeekStr = String.format(
                "%02d:%02d",
                TimeUnit.SECONDS.toHours(workWeek),
                TimeUnit.SECONDS.toMinutes(workWeek) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.SECONDS.toHours(
                        workWeek
                    )
                ),
            )
            Log.d(TAG, "Work Week sum: $workWeekStr")

            val topRight = findViewById<TextView>(R.id.tv_main_header_right)
            topRight.text = getString(R.string.work_week, workWeekStr)

        }


    }

    private fun setUpRecyclerView() {
        // make sure that there is an identified user (Guest or sign-in)
        if (mAuth.currentUser == null) {
            return
        }

        val query = db.collection(WlbUser.FBP)
            .document(mAuth.currentUser!!.uid)
            .collection(Session.FBP)
                .orderBy("tsStart", Query.Direction.DESCENDING)
                .limit(remoteConfig.getLong(MAIN_LAST_N_ENTRIES))

        val options = FirestoreRecyclerOptions.Builder<Session>()
            .setQuery(query, Session::class.java)
            .build()

        mAdapter = SessionAdapter(options)
        Log.d(TAG, "Adapter: $mAdapter")

        recyclerView = findViewById(R.id.session_main_recycler_view)
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
                AlertDialog.Builder(viewHolder.itemView.context)
                    .setTitle("Disabled")
                    .setMessage("Deleting Session by swipe is currently disabled")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                mAdapter.notifyItemChanged(viewHolder.adapterPosition)   // ToDo(frennkie) this is "costly"
                // mAdapter.deleteItem(viewHolder.adapterPosition)
            }
        }).attachToRecyclerView(recyclerView)

        mAdapter.setOnItemClickListener(object : SessionAdapter.OnItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
                val session = documentSnapshot.toObject(Session::class.java) ?: return

                val bundle = Bundle()
                if (session.allDay) {
                    bundle.putString(SessionBottomSheetFragment.ARG_SESSION_TYPE, Category.TYPE_OFF)
                } else {
                    bundle.putString(
                        SessionBottomSheetFragment.ARG_SESSION_TYPE,
                        Category.TYPE_WORK
                    )
                }
                bundle.putString(SessionBottomSheetFragment.ARG_SESSION_ID, session.objectId)

                val sessionBottomDialogFragment = SessionBottomSheetFragment.newInstance()
                sessionBottomDialogFragment.arguments = bundle
                sessionBottomDialogFragment.show(supportFragmentManager, "session_dialog_fragment")

            }
        })

    }

    private fun addNewSessionOff() {
        Log.d(TAG, "adding new session (Type: ${Category.TYPE_OFF})")

        // get a new "push key" for document
        val mSessionRef = userRef.collection(Session.FBP).document()

        val mSession = Session(
            // ToDo(frennkie) check for default Category
            //session.category = Category(key?!, "Foobar")  <- from User!
            tsStart = Session.getZonedDateTimeNow().toString(),
            tsEnd = Session.getZonedDateTimeNow().toString(),
            allDay = true,
            note = null,
            finished = true,
            breaks = null,
            objectId = mSessionRef.id  // additionally store push key
        )

        Log.d(TAG, "mSession: $mSession")

        val batch = db.batch()
        batch.set(mSessionRef, mSession.toMap())  // ToDo(frennkie) check

        val defCategoryOff = mWlbUser!!.default_category_off
        if (defCategoryOff != null) {
            Log.d(TAG, "defCatO: $defCategoryOff")
            batch.update(mSessionRef, Category.FBP, defCategoryOff.toMapNoSessions())
            // also append new session to list on Category
            batch.update(
                userRef.collection(Category.FBP).document(defCategoryOff.objectId),
                Session.FBP,
                FieldValue.arrayUnion(mSessionRef.id)
            )
        }

        batch.commit()
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to add new (Off) session! Error: ", e)

            }
            .addOnSuccessListener {
                Log.d(TAG, "Successfully added new Session (Type: ${Category.TYPE_OFF})")

                val mSnackbar = Snackbar.make(
                    findViewById(R.id.main_content),
                    "Added new Day Off!", Snackbar.LENGTH_LONG
                )
                mSnackbar.setAction("VIEW") {
                    val sessionBottomDialogFragment = SessionBottomSheetFragment.newInstance()
                    val bundle = Bundle()
                    bundle.putString(SessionBottomSheetFragment.ARG_SESSION_ID, mSession.objectId)
                    bundle.putString(SessionBottomSheetFragment.ARG_SESSION_TYPE, Category.TYPE_OFF)
                    sessionBottomDialogFragment.arguments = bundle
                    sessionBottomDialogFragment.show(
                        supportFragmentManager,
                        "session_dialog_fragment"
                    )
                }
                mSnackbar.show()

                recyclerView.layoutManager?.scrollToPosition(0)  // Scroll to Top

            }

    }

    private fun addNewSessionWork() {
        Log.d(TAG, "adding new session (Type: ${Category.TYPE_WORK})")

        // get a new "push key" for document
        val mSessionRef = userRef.collection(Session.FBP).document()

        val mSession = Session(
            // ToDo(frennkie) check for default Category
            //session.category = Category(key?!, "Foobar")  <- from User!
            tsStart = Session.getZonedDateTimeNow().minusHours(8).toString(),
            tsEnd = Session.getZonedDateTimeNow().toString(),
            allDay = false,
            note = null,
            finished = true,
            breaks = mutableListOf(
                Break(comment = "Default Break", duration = defBreak!!)
            ),
            objectId = mSessionRef.id  // additionally store push key
        )

        Log.d(TAG, "mSession: $mSession")

        val batch = db.batch()
        batch.set(mSessionRef, mSession.toMap())  // ToDo(frennkie) check

        val defCategoryWork = mWlbUser!!.default_category_work
        if (defCategoryWork != null) {
            Log.d(TAG, "defCatW: $defCategoryWork")
            batch.update(mSessionRef, Category.FBP, defCategoryWork.toMapNoSessions())
            // also append new session to list on Category
            batch.update(
                userRef.collection(Category.FBP).document(defCategoryWork.objectId),
                Session.FBP,
                FieldValue.arrayUnion(mSessionRef.id)
            )
        }

        batch.commit()
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to add new session! Error: ", e)

                }
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully added new Session (Type: ${Category.TYPE_WORK})")

                    val mSnackbar = Snackbar.make(
                        findViewById(R.id.main_content),
                        "Added new session!", Snackbar.LENGTH_LONG
                    )
                    mSnackbar.setAction("VIEW") { _ ->
                        val sessionBottomDialogFragment = SessionBottomSheetFragment.newInstance()
                        val bundle = Bundle()
                        bundle.putString(
                            SessionBottomSheetFragment.ARG_SESSION_ID,
                            mSession.objectId
                        )
                        bundle.putString(
                            SessionBottomSheetFragment.ARG_SESSION_TYPE,
                            Category.TYPE_WORK
                        )
                        sessionBottomDialogFragment.arguments = bundle
                        sessionBottomDialogFragment.show(
                            supportFragmentManager,
                            "session_dialog_fragment"
                        )
                    }
                    mSnackbar.show()

                    recyclerView.layoutManager?.scrollToPosition(0)  // Scroll to Top

                }
    }

    private fun startNewSession() {
        // get a new "push key" for document
        val mSessionRef = userRef.collection(Session.FBP).document()

        val mSession = Session(
            // ToDo(frennkie) check for default Category
            //session.category = Category(key?!, "Foobar")
            tsStart = Session.getZonedDateTimeNow().toString(),
            tsEnd = null,
            allDay = false,
            finished = false,
            objectId = mSessionRef.id  // additionally store push key
        )

        Log.d(TAG, "mSession: $mSession")

        val batch = db.batch()

        // store session-running
        batch.update(userRef, Session.FBP_SESSION_RUNNING, mSessionRef.id)

        batch.set(mSessionRef, mSession.toMap())  // ToDo(frennkie) check


        val defCategoryWork = mWlbUser!!.default_category_work
        if (defCategoryWork != null) {
            Log.d(TAG, "defCatW: $defCategoryWork")
            batch.update(mSessionRef, Category.FBP, defCategoryWork.toMapNoSessions())
            // also append new session to list on Category
            batch.update(
                userRef.collection(Category.FBP).document(defCategoryWork.objectId),
                Session.FBP,
                FieldValue.arrayUnion(mSessionRef.id)
            )
        }

        batch.commit()
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to start new session! Error: ", e)
                    // this catches the error.. may be do something with this?! UI does reflect the
                    // intended change until refresh!
                }
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully started new Session")

                    val mSnackbar = Snackbar.make(
                        findViewById(R.id.main_content),
                        "started new Session...", Snackbar.LENGTH_LONG
                    )
                    mSnackbar.setAction("VIEW") { _ ->
                        val sessionBottomDialogFragment = SessionBottomSheetFragment.newInstance()
                        val bundle = Bundle()
                        bundle.putString(
                            SessionBottomSheetFragment.ARG_SESSION_ID,
                            mSession.objectId
                        )
                        bundle.putString(
                            SessionBottomSheetFragment.ARG_SESSION_TYPE,
                            Category.TYPE_WORK
                        )
                        sessionBottomDialogFragment.arguments = bundle
                        sessionBottomDialogFragment.show(
                            supportFragmentManager,
                            "session_dialog_fragment"
                        )
                    }

                    mSnackbar.show()

                    recyclerView.layoutManager?.scrollToPosition(0)  // Scroll to Top
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
                            val mBreakDuration =
                                mDefaultBreak.applyBreakRules(mSession.getDurationLong())
                            Log.d(
                                TAG,
                                "no breaks present - adding according to rules (length: $mBreakDuration)"
                            )
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

                                    val mSnackbar = Snackbar.make(
                                        findViewById(R.id.main_content),
                                        "finished Session (${mSession.getDurationWeightedExcludingBreaks()})",
                                        Snackbar.LENGTH_LONG
                                    )
                                    mSnackbar.setAction("VIEW") { _ ->
                                        val sessionBottomDialogFragment =
                                            SessionBottomSheetFragment.newInstance()
                                        val bundle = Bundle()
                                        bundle.putString(
                                            SessionBottomSheetFragment.ARG_SESSION_ID,
                                            mSession.objectId
                                        )
                                        bundle.putString(
                                            SessionBottomSheetFragment.ARG_SESSION_TYPE,
                                            Category.TYPE_WORK
                                        )
                                        sessionBottomDialogFragment.arguments = bundle
                                        sessionBottomDialogFragment.show(
                                            supportFragmentManager,
                                            "session_dialog_fragment"
                                        )
                                    }
                                    mSnackbar.show()

                                    recyclerView.layoutManager?.scrollToPosition(0)  // Scroll to Top

                                }
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

                    if (snapshot != null && snapshot.exists()) {

                        mWlbUser = snapshot.toObject(WlbUser::class.java)!!
                        Log.d(TAG, "mWlbUser: $mWlbUser")

                        defBreak = mWlbUser!!.default_break ?: 60 * 45
                        defCategoryOff = mWlbUser!!.default_category_off
                        defCategoryWork = mWlbUser!!.default_category_work

                        if (snapshot.contains(Session.FBP_SESSION_RUNNING)) {

                            snapshot.data?.get(Session.FBP_SESSION_RUNNING).toString()
                            Log.d(TAG, "monitorSessionRunning found running session")
                            setUiSessionRunning(
                                snapshot.data?.get(Session.FBP_SESSION_RUNNING).toString()
                            )

                        } else {
                            unsetUiSessionRunning()
                        }

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
            userRef = db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid)
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

        // Remote Config
        private const val MAIN_LAST_N_ENTRIES = "main_last_n_entries"

        // Unique tag for the intent reply
        const val TEXT_REQUEST_REPORT_ACTIVITY_TEST = 1

    }

}