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
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.jakewharton.threetenabp.AndroidThreeTen
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.fragment.HomeFragment
import de.rhab.wlbtimer.fragment.SessionBottomDialogFragment
import de.rhab.wlbtimer.model.Break
import de.rhab.wlbtimer.model.Session
import de.rhab.wlbtimer.model.WlbUser


class MainActivity : AppCompatActivity(), SessionBottomDialogFragment.BottomSheetListener {

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mDrawerLayout: DrawerLayout

    private lateinit var fabStartNew: View
    private lateinit var fabStopRunning: View

    private val mDatabaseRef = FirebaseDatabase.getInstance().reference

    private var content: FrameLayout? = null

    private lateinit var mSRListener: ValueEventListener

    private var mSRRef: DatabaseReference? = null

    var mSRStatus: Boolean = false

    lateinit var mSRRunningID: String

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
                    startActivityForResult(intent, TEXT_REQUEST)
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


        content = findViewById(R.id.content)

        val fragment = HomeFragment.newInstance()

//        addFragment(fragment)

        supportFragmentManager
                .beginTransaction()
                .setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out)
                .replace(R.id.content, fragment, fragment.javaClass.simpleName)
                .commit()

        if (mSRStatus) {
            fabStartNew.visibility = View.GONE
            fabStopRunning.visibility = View.VISIBLE

        } else {
            fabStartNew.visibility = View.VISIBLE
            fabStopRunning.visibility = View.GONE

        }

        fabStartNew.setOnClickListener { _ ->
            startNewDefaultSessionDialog()
        }

        fabStopRunning.setOnClickListener { _ ->
            stopRunningSessionDialog()
        }

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
                val sessionBottomDialogFragment = SessionBottomDialogFragment.newInstance()
                sessionBottomDialogFragment.show(supportFragmentManager, "session_dialog_fragment")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun addNewSessionSingleChoice() {

        val listItems = arrayOf("Item 1", "Item 2", "Item 3")
        val mBuilder = AlertDialog.Builder(this)
        mBuilder.setTitle("Choose an item")
        mBuilder.setSingleChoiceItems(listItems, -1) { dialogInterface, i ->
            Log.i(TAG, listItems[i])
            dialogInterface.dismiss()
        }
        // Set the neutral/cancel button click listener
        mBuilder.setNeutralButton("Cancel") { dialog, _ ->
            // Do something when click the neutral button
            dialog.cancel()
        }

        val mDialog = mBuilder.create()
        mDialog.show()
    }

    /**
     * This method will show a dialog bix where user can enter new item
     * to be added
     * http://www.appsdeveloperblog.com/todo-list-app-kotlin-firebase/
     * https://www.mkyong.com/android/android-prompt-user-input-dialog-example/
     */
    private fun startNewSessionDialog() {
        val alert = AlertDialog.Builder(this)

        val choicesCategory = arrayOf("foo", "bar", "work")
        alert.setTitle("Select CategoryWork")

        val mBuilder = AlertDialog.Builder(this)
        mBuilder.setTitle("Choose an item")
        mBuilder.setSingleChoiceItems(choicesCategory, -1) { dialogInterface, i ->
            Log.i(TAG, choicesCategory[i])

            // get a new push key
            val newSessionRef = mDatabaseRef.child(Session.FBP)
                    .child(mAuth.currentUser!!.uid).push()

            val session = Session(
                    tsStart = Session.getZonedDateTimeNow().toString(),
                    //session.category = choicesCategory[i], // ToDo
                    tsEnd = null,
                    allDay = false,
                    finished = false,
                    objectId = newSessionRef.key  // additionally store push key
            )

            // prepare bulk update
            val childUpdates = HashMap<String, Any?>()

            val pathA = newSessionRef.path.toString()
            childUpdates[pathA] = session.toMap()

            val pathB = mDatabaseRef.child(WlbUser.FBP).child(mAuth.currentUser!!.uid)
                    .child(Session.FBP_SESSION_RUNNING).path.toString()
            childUpdates[pathB] = session.objectId.toString()

            // execute bulk update
            mDatabaseRef.updateChildren(childUpdates)

            dialogInterface.dismiss()
            Snackbar.make(findViewById(R.id.main_content), "Item saved with ID " + session.objectId, Snackbar.LENGTH_LONG).show()


        }
        // Set the neutral/cancel button click listener
        mBuilder.setNeutralButton("Cancel") { dialog, _ ->
            // Do something when click the neutral button
            dialog.cancel()
        }

        val mDialog = mBuilder.create()
        mDialog.show()

    }

    private fun startNewDefaultSessionDialog() {
        // get a new push key
        val newSessionRef = mDatabaseRef.child(Session.FBP)
                .child(mAuth.currentUser!!.uid).push()

        val session = Session(
                // ToDo(frennkie) check for default Category
                //session.category = CategoryWork(key?!, "Foobar")
                tsStart = Session.getZonedDateTimeNow().toString(),
                tsEnd = null,
                allDay = false,
                finished = false,
                objectId = newSessionRef.key  // additionally store push key
        )

        // prepare bulk update
        val childUpdates = HashMap<String, Any?>()

        val pathA = newSessionRef.path.toString()
        childUpdates[pathA] = session.toMap()

        val pathB = mDatabaseRef.child(WlbUser.FBP).child(mAuth.currentUser!!.uid)
                .child(Session.FBP_SESSION_RUNNING).path.toString()
        childUpdates[pathB] = session.objectId.toString()

        // execute bulk update
        mDatabaseRef.updateChildren(childUpdates)

        Snackbar.make(findViewById(R.id.main_content), "Started new session...", Snackbar.LENGTH_LONG).show()
    }


    private fun stopRunningSessionDialog() {
        if (!mSRStatus) {
            Log.w(TAG, "no session running")
        } else {

            val mRef = mDatabaseRef.child(Session.FBP)
                    .child(mAuth.currentUser!!.uid)
                    .child(mSRRunningID)

            val mSRListenerStop = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    Log.d(TAG, "monitorSessionRunning Data: " + dataSnapshot.toString())

                    val newSession = dataSnapshot.getValue(Session::class.java)
                    Log.d(TAG, "monitorSessionRunning Object: $newSession")
                    if (newSession != null) {

                        newSession.finished = true
                        newSession.tsEnd = Session.getZonedDateTimeNow().toString()

                        if (newSession.breaks == null) {
                            // ToDo calculate duration and set break according to rules
                            Log.d(TAG, "no breaks present - adding according to rules")
                            val mDefaultBreak = Break(comment = "Default Break", duration = 60 * 45)
                            newSession.breaks = mutableListOf(mDefaultBreak)
                        }


                        // prepare bulk update
                        val childUpdates = HashMap<String, Any?>()

                        val pathA = mDatabaseRef.child(Session.FBP).child(mAuth.currentUser!!.uid)
                                .child(newSession.objectId!!).path.toString()
                        childUpdates[pathA] = newSession.toMap()

                        val pathB = mDatabaseRef.child(WlbUser.FBP).child(mAuth.currentUser!!.uid)
                                .child(Session.FBP_SESSION_RUNNING).path.toString()
                        childUpdates[pathB] = null

                        // execute bulk update
                        mDatabaseRef.updateChildren(childUpdates)

                        // ToDo(frennkie) not weighted without CategoryWork factor!
                        Snackbar.make(findViewById(R.id.main_content),
                                "finished Session (${newSession.getDurationWeightedExcludingBreaks()})",
                                Snackbar.LENGTH_LONG).show()

                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "monitorSessionRunning: ${databaseError.toException()}")
                }

            }

            mRef.addListenerForSingleValueEvent(mSRListenerStop)

        }
    }

    override fun onOptionClick(text: String) {
        //change text on each item click
        Log.d(TAG, "onOptionClick: $text")
    }


    private fun monitorSessionRunning() {
        if (mAuth.currentUser == null) {
            return
        } else {
            mAuth.currentUser
            mSRListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    Log.d(TAG, "monitorSessionRunning Data: " + dataSnapshot.toString())
                    Log.d(TAG, "monitorSessionRunning Data: " + dataSnapshot.value.toString())
                    Log.d(TAG, "monitorSessionRunning Fragment: $mCurrentFragment")
                    mSRStatus = dataSnapshot.exists()
                    mSRRunningID = dataSnapshot.value.toString()

                    if (mCurrentFragment == HomeFragment::class.java.simpleName) {
                        if (mSRStatus) {
                            fabStartNew.visibility = View.GONE
                            fabStopRunning.visibility = View.VISIBLE

                        } else {
                            fabStartNew.visibility = View.VISIBLE
                            fabStopRunning.visibility = View.GONE

                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "monitorSessionRunning: ${databaseError.toException()}")
                }
            }

            mSRRef = mDatabaseRef.child(WlbUser.FBP)
                    .child(mAuth.currentUser!!.uid)
                    .child(Session.FBP_SESSION_RUNNING)
            Log.d(TAG, "added ValueEventListener to: ${mSRRef!!.path}")
            mSRRef!!.addValueEventListener(mSRListener)

        }
    }

    public override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener!!)
        if (mAuth.currentUser != null) {
            Log.d(TAG, "calling monitorSessionRunning from onStart")
            monitorSessionRunning()
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
            Log.d(TAG, "removing monitorSessionRunning from onStop")
            mSRRef?.removeEventListener(mSRListener)
        } else {
            Log.d(TAG, "NOT removing monitorSessionRunning from onStop")
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

        private const val RC_SIGN_IN = 9001

        // Unique tag for the intent reply
        const val TEXT_REQUEST = 1

    }

}