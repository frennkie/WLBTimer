package de.rhab.wlbtimer.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.model.Break
import de.rhab.wlbtimer.model.CategoryWork
import de.rhab.wlbtimer.model.Session
import de.rhab.wlbtimer.model.WlbUser
import org.threeten.bp.ZonedDateTime


class SessionBottomDialogFragment : BottomSheetDialogFragment() {

    private val mAuth = FirebaseAuth.getInstance()

    private val mDatabaseRef = FirebaseDatabase.getInstance().reference

    private lateinit var mCategoryWorkRef: DatabaseReference
    private lateinit var mSessionRef: DatabaseReference

    private lateinit var mCategoryWorkListener: ValueEventListener
    private lateinit var mSessionListener: ValueEventListener

    private var mCategoryWorkList = HashMap<String, CategoryWork>()

    private lateinit var numberList: ListPopupWindow
    private var listCategoryWork = ArrayList<Map<String, String>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_bottom_sheet_session, container, false)

        val tvStartDate = rootView.findViewById<TextView>(R.id.tv_btn_start_date)
        val tvStartTime = rootView.findViewById<TextView>(R.id.tv_btn_start_time)
        val tvEndDate = rootView.findViewById<TextView>(R.id.tv_btn_end_date)
        val tvEndTime = rootView.findViewById<TextView>(R.id.tv_btn_end_time)
        val tvBreaks = rootView.findViewById<TextView>(R.id.tv_btn_breaks)
        val tvDuration = rootView.findViewById<TextView>(R.id.tv_btn_duration)
        val tvCategory = rootView.findViewById<TextView>(R.id.tv_btn_category)
        val tvRemoveSession = rootView.findViewById<TextView>(R.id.tv_btn_remove_session)

        // Variable to hold existing or new Session Object
        var mSession: Session? = null

        // get passed in Session ID
        val sessionId = arguments?.getString(ARG_SESSION_ID)

        if (sessionId != null) {
            Log.d(TAG, "using existing session")
            mSessionRef = mDatabaseRef.child(Session.FBP).child(mAuth.currentUser!!.uid).child(sessionId)

        } else {
            Log.i(TAG, "adding new session")

            mSession = Session(allDay = false, finished = true)
            // We first make a push so that a new item is made with a unique ID
            mSessionRef = mDatabaseRef.child(Session.FBP).child(mAuth.currentUser!!.uid).push()
            mSession.objectId = mSessionRef.key
            mSession.tsStart = Session.getZonedDateTimeNow().minusHours(8).toString()
            mSession.tsEnd = Session.getZonedDateTimeNow().toString()

            // ToDo(frennkie) how to get full details from CategoryWork here?! :-/
//            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this.context)
//            val defaultCategoryWork = sharedPref.getString("default_category_work", null)
//            if (defaultCategoryWork != null) {
//                Log.d(TAG, "setting default category work from prefs")
//                mSession.category = CategoryWork(defaultCategoryWork)
//            }

            /* ToDO(frennkie) makes no sense.. would at least need to check for length of session
            as a 60min session won't have a 45min break.. Also this is most likely not the
            right place for this logic
            */
            val mDefaultBreak = Break(comment = "Default Break", duration = 60 * 45)
            if (mSession.breaks != null) {
                Log.d(TAG, "breaks present - adding default")
                mSession.breaks!!.add(mDefaultBreak)
            } else {
                Log.d(TAG, "no break yet - adding default")
                mSession.breaks = mutableListOf(mDefaultBreak)
            }

            // then, we use the reference to set the value on that ID
            mSessionRef.setValue(mSession.toMap())

        }

        mCategoryWorkRef = mDatabaseRef.child(WlbUser.FBP).child(mAuth.currentUser!!.uid).child(CategoryWork.FBP)
        mCategoryWorkListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "Number of category_work entries: ${dataSnapshot.childrenCount}")

                listCategoryWork = ArrayList()

                dataSnapshot.children.forEach { child ->
                    // Extract Message object from the DataSnapshot
                    Log.d(TAG, child.toString())
                    val mCategoryWork = child.getValue(CategoryWork::class.java)
                    if (mCategoryWork != null) {
                        mCategoryWorkList[mCategoryWork.objectId] = mCategoryWork

                        val tran = LinkedHashMap<String, String>()
                        tran["title"] = mCategoryWork.title
                        tran["objectId"] = mCategoryWork.objectId
                        listCategoryWork.add(tran)
                    }

                }

                Log.d(TAG, "listCategoryWork: $listCategoryWork")

            }

            override fun onCancelled(error: DatabaseError) {
                // Could not successfully listen for data, log the error
                Log.e(TAG, "category_work:onCancelled: ${error.message}")
            }
        }

        mSessionListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "foobar1: ${mSession?.toMap()}")
                Log.d(TAG, "foobar2: $dataSnapshot")

                mSession = dataSnapshot.getValue(Session::class.java)

                dataSnapshot.children.forEach { child ->
                    // Extract Message object from the DataSnapshot
                    Log.d(TAG, child.toString())
                    if (child.key == CategoryWork.FBP_SHORT) {
                        Log.d(TAG, "found cat work")

                        mSession!!.category = child.getValue(CategoryWork::class.java)
                    }
                }

                // ToDo foobar3 already lost an existing category!!
                Log.d(TAG, "foobar3: ${mSession?.toMap()}")
                if (mSession != null) {

                    if (mSession!!.tsStart != null) {
                        tvStartDate.text = mSession!!.getDateStart()
                        tvStartTime.text = mSession!!.getTimeZonedStart()
                    } else {
                        tvStartDate.text = "..."
                        tvStartTime.text = "..."
                    }

                    if (mSession!!.tsEnd != null) {
                        tvEndDate.text = mSession!!.getDateEnd()
                        tvEndTime.text = mSession!!.getTimeZonedEnd()
                    } else {
                        tvEndDate.text = "..."
                        tvEndTime.text = "..."
                    }

                    tvBreaks.text = mSession!!.getTotalBreakTime()

                    val mCategoryWork = mSession!!.category
                    Log.d(TAG, "result $mCategoryWork")
                    if (mCategoryWork != null) {
                        tvCategory.text = mCategoryWork.title
                        tvDuration.text = mSession!!.getDurationWeightedExcludingBreaks(mFactor = mCategoryWork.factor)
                    } else {
                        tvCategory.text = "N/A"
                        tvDuration.text = mSession!!.getDurationWeightedExcludingBreaks()
                    }

                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(TAG, "SessionBottomDialogFragment Cancel: ${databaseError.toException()}")
            }
        }

        //handle clicks
        tvStartDate.setOnClickListener {
            mBottomSheetListener!!.onOptionClick("tvStartDate clicked...")
            Log.d(TAG, "tvStartDate clicked...")

            val startDateTime: ZonedDateTime = Session.fromDefaultStr(mSession!!.tsStart!!)!!

            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                val newStartDateTime: ZonedDateTime = startDateTime.withYear(year).withMonth(month + 1).withDayOfMonth(day)
                // update db
                mSession!!.tsStart = newStartDateTime.toString()
                mSessionRef.setValue(mSession!!.toMap())
                // update TextView on Bottom Dialog
                tvStartDate.text = Session.toDateStr(newStartDateTime)
            }

            DatePickerDialog(context!!, dateSetListener, startDateTime.year, startDateTime.monthValue - 1, startDateTime.dayOfMonth).show()

        }

        tvStartTime.setOnClickListener {
            mBottomSheetListener!!.onOptionClick("tvStartTime clicked...")
            Log.d(TAG, "tvStartTime clicked...")

            val startDateTime: ZonedDateTime = Session.fromDefaultStr(mSession!!.tsStart!!)!!

            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                val newStartDateTime: ZonedDateTime = startDateTime.withHour(hour).withMinute(minute).withSecond(0)
                // update db
                mSession!!.tsStart = newStartDateTime.toString()
                mSessionRef.setValue(mSession!!.toMap())
                // update TextView on Bottom Dialog
                tvStartTime.text = Session.toTimeZonedStr(newStartDateTime)
            }

            TimePickerDialog(context, timeSetListener, startDateTime.hour, startDateTime.minute, true).show()
        }

        tvEndDate.setOnClickListener {
            mBottomSheetListener!!.onOptionClick("tvEndDate clicked...")
            Log.d(TAG, "tvEndDate clicked...")

            // only allow editing if session is finished
            if (mSession!!.finished!!) {
                val endDateTime: ZonedDateTime = Session.fromDefaultStr(mSession!!.tsEnd!!)!!

                val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                    val newEndDateTime: ZonedDateTime = endDateTime.withYear(year).withMonth(month + 1).withDayOfMonth(day)
                    // update db
                    mSession!!.tsEnd = newEndDateTime.toString()
                    mSessionRef.setValue(mSession!!.toMap())
                    // update TextView on Bottom Dialog
                    tvEndDate.text = Session.toDateStr(newEndDateTime)
                }

                DatePickerDialog(context!!, dateSetListener, endDateTime.year, endDateTime.monthValue - 1, endDateTime.dayOfMonth).show()
            }

        }

        tvEndTime.setOnClickListener {
            mBottomSheetListener!!.onOptionClick("tvEndTime clicked...")
            Log.d(TAG, "tvEndTime clicked...")

            // only allow editing if session is finished
            if (mSession!!.finished!!) {
                val endDateTime: ZonedDateTime = Session.fromDefaultStr(mSession!!.tsEnd!!)!!

                val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                    val newEndDateTime: ZonedDateTime = endDateTime.withHour(hour).withMinute(minute).withSecond(0)
                    // update db
                    mSession!!.tsEnd = newEndDateTime.toString()
                    mSessionRef.setValue(mSession!!.toMap())
                    // update TextView on Bottom Dialog
                    tvEndTime.text = Session.toTimeZonedStr(newEndDateTime)
                }

                TimePickerDialog(context, timeSetListener, endDateTime.hour, endDateTime.minute, true).show()
            }
        }

        tvBreaks.setOnClickListener {
            mBottomSheetListener!!.onOptionClick("tvBreaks clicked...")
            Log.d(TAG, "tvBreaks clicked...")

            var mBreak: Break? = null

            if (mSession!!.breaks == null)
                mBreak = Break()
            else {
                if (mSession!!.breaks?.count() == 1) {
                    mBreak = mSession!!.breaks!![0]
                } else {
                    Log.w(TAG, "more than one break found.. not yet implemented")
                }
            }

            if (mBreak != null) {
                val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                    // update db
                    mBreak.duration = hour * 60 * 60 + minute * 60
                    mSessionRef.child("breaks").child("0").setValue(mBreak.toMap())
                    // update TextView on Bottom Dialog
                    Log.d(TAG, "Selected: %01d:%02d".format(hour, minute))
                    tvBreaks.text = "%01d:%02d".format(hour, minute)
                }
                val mDurationMinutes: Int = mBreak.duration / 60
                TimePickerDialog(context, timeSetListener, 0, mDurationMinutes, true).show()
            } else {
                Toast.makeText(this.context, "not implemented", Toast.LENGTH_SHORT).show()
            }

        }

        tvCategory.setOnClickListener {
            mBottomSheetListener!!.onOptionClick("tvCategory clicked...")
            Log.d(TAG, "tvCategory clicked...")

            numberList = ListPopupWindow(context!!)
            numberList.anchorView = tvCategory

            val adapter = SimpleAdapter(context, listCategoryWork,
                    android.R.layout.simple_spinner_dropdown_item,
                    arrayOf("title"),
                    intArrayOf(android.R.id.text1))
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            numberList.setAdapter(adapter)


            numberList.setOnItemClickListener { _, _, position, _ ->
                Log.d(TAG, "listCW: $listCategoryWork; p: $position; listCW[p]:${listCategoryWork[position]}")
                val map = listCategoryWork[position]
                val mCategoryWorkObjectId = map["objectId"]!!
                val mCategoryWork = mCategoryWorkList[mCategoryWorkObjectId]!!
                tvCategory.text = mCategoryWork.title


                // prepare bulk update
                val childUpdates = HashMap<String, Any?>()

                val pathA = mSessionRef.child(CategoryWork.FBP_SHORT).path.toString()
                childUpdates[pathA] = mCategoryWork.toMapNoSessions()

                val pathB = mDatabaseRef.child(WlbUser.FBP).child(mAuth.currentUser!!.uid)
                        .child(CategoryWork.FBP)
                        .child(mCategoryWorkObjectId)
                        .child(Session.FBP)
                        .child(mSession!!.objectId!!).path.toString()
                childUpdates[pathB] = true
                Log.d(TAG, "pathB: $pathB - set to true")

                // check if entry has an old category
                if (mSession?.category != null) {
                    // if a different category was selected remove Session entry from old category
                    if (mSession?.category?.objectId != mCategoryWorkObjectId) {
                        val pathC = mDatabaseRef.child(WlbUser.FBP).child(mAuth.currentUser!!.uid)
                                .child(CategoryWork.FBP)
                                .child(mSession?.category?.objectId!!)
                                .child(Session.FBP)
                                .child(mSession!!.objectId!!).path.toString()
                        childUpdates[pathC] = null
                        Log.d(TAG, "pathC: $pathC - set to null")
                    }
                }

                // execute bulk update
                mDatabaseRef.updateChildren(childUpdates)
                Log.d(TAG, "updateChildren done")

                numberList.dismiss()
            }
            numberList.show()
        }

        tvRemoveSession.setOnClickListener {
            mBottomSheetListener!!.onOptionClick("tvRemoveSession clicked...")
            Log.d(TAG, "tvRemoveSession clicked...")

            val mBuilder = AlertDialog.Builder(context!!)
            mBuilder.setTitle("Delete this entry?")

            if (mSession!!.finished!!) {
                mBuilder.setMessage("Are you sure you want to delete the entry started: \n" +
                        "${mSession!!.getDateStart()} ${mSession!!.getTimeStart()}\n")

                mBuilder.setCancelable(false)
                mBuilder.setPositiveButton("Delete") { _, _ ->
                    mDatabaseRef.child(Session.FBP).child(mAuth.currentUser!!.uid)
                            .child(mSession!!.objectId!!).removeValue()
                    dismiss()
                }

            } else {
                mBuilder.setMessage("Are you sure you want to discard new entry")
                mBuilder.setCancelable(false)
                mBuilder.setPositiveButton("Discard") { _, _ ->

                    // prepare bulk update
                    val childUpdates = HashMap<String, Any?>()

                    val pathA = mDatabaseRef.child(Session.FBP).child(mAuth.currentUser!!.uid)
                            .child(mSession!!.objectId!!).path.toString()
                    childUpdates[pathA] = null

                    val pathB = mDatabaseRef.child(WlbUser.FBP).child(mAuth.currentUser!!.uid)
                            .child(Session.FBP_SESSION_RUNNING).path.toString()
                    childUpdates[pathB] = null

                    // execute bulk update
                    mDatabaseRef.updateChildren(childUpdates)

                    dismiss()
                }

            }

            mBuilder.setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(this.context, "Cancelled.", Toast.LENGTH_LONG).show()
            }

            val mDialog = mBuilder.create()
            mDialog.show()

        }

        return rootView
    }

    interface BottomSheetListener {
        fun onOptionClick(text: String)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        try {
            mBottomSheetListener = context as BottomSheetListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(context!!.toString())
        }
    }

    override fun onStart() {
        super.onStart()
        mCategoryWorkRef.addValueEventListener(mCategoryWorkListener)
        mSessionRef.addValueEventListener(mSessionListener)
    }

    override fun onStop() {
        super.onStop()
        mCategoryWorkRef.removeEventListener(mCategoryWorkListener)
        mSessionRef.removeEventListener(mSessionListener)
    }

    companion object {

        private const val TAG = "SessionBottomDialogFrag"

        const val ARG_SESSION_ID = "SESSION_ID"

        private var mBottomSheetListener: BottomSheetListener? = null

        fun newInstance(): SessionBottomDialogFragment {
            val fragmentSessionBottomDialog = SessionBottomDialogFragment()
            val args = Bundle()
            fragmentSessionBottomDialog.arguments = args
            return fragmentSessionBottomDialog
        }

    }
}


