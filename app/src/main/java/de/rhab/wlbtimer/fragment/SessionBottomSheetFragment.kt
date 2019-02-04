package de.rhab.wlbtimer.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListPopupWindow
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.model.Break
import de.rhab.wlbtimer.model.CategoryWork
import de.rhab.wlbtimer.model.Session
import de.rhab.wlbtimer.model.WlbUser
import org.threeten.bp.ZonedDateTime


class SessionBottomSheetFragment : BottomSheetDialogFragment() {

    private val mAuth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()

    private lateinit var mCategoryWorkColRef: CollectionReference

    private lateinit var mSessionRef: DocumentReference

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

        val tvCategoryIcon = rootView.findViewById<TextView>(R.id.tv_category_icon)
        val mTvCategoryIconBackground = tvCategoryIcon.background as GradientDrawable

        val tvRemoveSession = rootView.findViewById<TextView>(R.id.tv_btn_remove_session)

        // Variable to hold existing or new Session Object
        var mSession: Session? = null


        val userRef = db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid)
        // get passed in Session ID
        val mId = arguments?.getString(ARG_SESSION_ID)

        if (mId == null) {
            Log.i(TAG, "adding new session")
            mSessionRef = userRef.collection(Session.FBP).document()

            mSession = Session()
            mSession.objectId = mSessionRef.id
            mSession.allDay = false
            mSession.finished = true
            mSession.tsStart = Session.getZonedDateTimeNow().minusHours(8).toString()
            mSession.tsEnd = Session.getZonedDateTimeNow().toString()

            /* ToDO(frennkie) makes no sense.. would at least need to check for length of session
                as a 60min session won't have a 45min break.. Also this is most likely not the
                right place for this logic

                how to get full details from CategoryWork here?! :-/
                val sharedPref = PreferenceManager.getDefaultSharedPreferences(this.context)
                val defaultCategoryWork = sharedPref.getString("default_category_work", null)
                if (defaultCategoryWork != null) {
                    Log.d(TAG, "setting default category work from prefs")
                    mSession.category = CategoryWork(defaultCategoryWork)
                }
            */

            val mDefaultBreak = Break(
                    comment = "Default Break",
                    duration = 60 * 45
            )

            if (mSession.breaks != null) {
                Log.d(TAG, "breaks present - adding default")
                mSession.breaks!!.add(mDefaultBreak)
            } else {
                Log.d(TAG, "no break yet - adding default")
                mSession.breaks = mutableListOf(mDefaultBreak)
            }

            // write this to Firestore
            mSessionRef.set(mSession.toMap())

        } else {
            Log.d(TAG, "using existing session for mId: $mId")
            mSessionRef = userRef.collection(Session.FBP).document(mId)
        }

        Log.d(TAG, "mSessionRef is now: ${mSessionRef.path}")


        listCategoryWork = ArrayList()

        // get Category Work Object
        mCategoryWorkColRef = userRef.collection(CategoryWork.FBP)
        mCategoryWorkColRef.get()
                .addOnSuccessListener { result ->
                    Log.d(TAG, "Number of category_work entries: ${result.count()}")

                    for (document in result) {
                        Log.d(TAG, document.id + " => " + document.data)
                        val mCategoryWork = document.toObject(CategoryWork::class.java)

                        mCategoryWorkList[mCategoryWork.objectId] = mCategoryWork

                        val tran = LinkedHashMap<String, String>()
                        tran["title"] = mCategoryWork.title
                        tran["objectId"] = mCategoryWork.objectId
                        listCategoryWork.add(tran)

                    }

                    Log.d(TAG, "listCategoryWork: $listCategoryWork")

                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "get failed with ", e)
                }

        // get Session Object
        mSessionRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot != null) {
                        mSession = documentSnapshot.toObject(Session::class.java)
                        Log.d(TAG, "found: $mSession")
                    } else {
                        Log.w(TAG, "No such document")
                    }

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

                           tvCategoryIcon.text = mCategoryWork.title.substring(0, 1)
                            val mColor = Color.parseColor(mCategoryWork.color)
                            mTvCategoryIconBackground.setColor(mColor)

                            tvDuration.text = mSession!!.getDurationWeightedExcludingBreaks(mFactor = mCategoryWork.factor)
                        } else {
                            tvCategory.text = "N/A"

                            tvCategoryIcon.text = "-"
                            val mColor = Color.parseColor("#666666")
                            mTvCategoryIconBackground.setColor(mColor)

                            tvDuration.text = mSession!!.getDurationWeightedExcludingBreaks()
                        }

                    }

                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "get failed with ", e)
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
                mSessionRef.set(mSession!!.toMap())
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
                mSessionRef.set(mSession!!.toMap())
                // update TextView on Bottom Dialog
                tvStartTime.text = Session.toTimeZonedStr(newStartDateTime)
            }

            TimePickerDialog(context, timeSetListener, startDateTime.hour, startDateTime.minute, true).show()
        }

        tvEndDate.setOnClickListener {
            mBottomSheetListener!!.onOptionClick("tvEndDate clicked...")
            Log.d(TAG, "tvEndDate clicked...")

            // only allow editing if session is finished
            if (mSession!!.finished) {
                val endDateTime: ZonedDateTime = Session.fromDefaultStr(mSession!!.tsEnd!!)!!

                val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                    val newEndDateTime: ZonedDateTime = endDateTime.withYear(year).withMonth(month + 1).withDayOfMonth(day)
                    // update db
                    mSession!!.tsEnd = newEndDateTime.toString()
                    mSessionRef.set(mSession!!.toMap())
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
            if (mSession!!.finished) {
                val endDateTime: ZonedDateTime = Session.fromDefaultStr(mSession!!.tsEnd!!)!!

                val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                    val newEndDateTime: ZonedDateTime = endDateTime.withHour(hour).withMinute(minute).withSecond(0)
                    // update db
                    mSession!!.tsEnd = newEndDateTime.toString()
                    mSessionRef.set(mSession!!.toMap())
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
                    Log.d(TAG, "Selected: %01d:%02d".format(hour, minute))

                    mBreak.duration = hour * 60 * 60 + minute * 60
                    // update db
                    mSession!!.breaks = mutableListOf(mBreak)
                    mSessionRef.set(mSession!!.toMap())

                    // update TextView on Bottom Dialog
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


                tvCategoryIcon.text = mCategoryWork.title.substring(0, 1)
                val mColor = Color.parseColor(mCategoryWork.color)
                mTvCategoryIconBackground.setColor(mColor)


                // store possible old CategoryWork objectId
                var mOldCatWorkObjectId: String? = null
                if (mSession!!.category?.objectId != null) mOldCatWorkObjectId = mSession!!.category!!.objectId

                val batch = db.batch()

                mSession!!.category = mCategoryWork
                batch.update(mSessionRef, CategoryWork.FBP_SHORT, mSession!!.category?.toMapNoSessions())

                // store an array on the CategoryWork instance of every session where the instance is used
                val mCatWorkRef = userRef.collection(CategoryWork.FBP).document(mCategoryWorkObjectId)
                batch.update(mCatWorkRef, Session.FBP, FieldValue.arrayUnion(mSession!!.objectId))

                // check if entry has an old category
                if (mOldCatWorkObjectId != null) {
                    // if a different category was selected remove Session entry from old category
                    if (mOldCatWorkObjectId != mCategoryWorkObjectId) {
                        batch.update(userRef.collection(CategoryWork.FBP).document(mOldCatWorkObjectId),
                                Session.FBP, FieldValue.arrayRemove(mSession!!.objectId))
                        Log.d(TAG, "removed from array on Category Work")
                    }
                }

                // execute bulk update
                batch.commit()
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to start new session! Error: ", e)
                            // this catches the error.. may be do something with this?! UI does reflect the
                            // intended change until refresh!
                        }
                        .addOnSuccessListener { _ ->
                            Log.d(TAG, "updateChildren done")
                        }

                numberList.dismiss()
            }
            numberList.show()
        }

        tvRemoveSession.setOnClickListener {
            mBottomSheetListener!!.onOptionClick("tvRemoveSession clicked...")
            Log.d(TAG, "tvRemoveSession clicked...")

            val mBuilder = AlertDialog.Builder(context!!)
            mBuilder.setTitle("Delete this entry?")

            if (mSession!!.finished) {
                mBuilder.setMessage("Are you sure you want to delete the entry started: \n" +
                        "${mSession!!.getDateStart()} ${mSession!!.getTimeStart()}\n")

                mBuilder.setCancelable(false)
                mBuilder.setPositiveButton("Delete") { _, _ ->
                    userRef.collection(Session.FBP).document(mSession!!.objectId!!).delete()
                    dismiss()
                }

            } else {
                mBuilder.setMessage("Are you sure you want to discard new entry")
                mBuilder.setCancelable(false)
                mBuilder.setPositiveButton("Discard") { _, _ ->

                    val batch = db.batch()

                    batch.update(userRef, Session.FBP_SESSION_RUNNING, FieldValue.delete())
                    batch.delete(userRef.collection(Session.FBP).document(mSession!!.objectId!!))

                    // execute bulk update
                    batch.commit()
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Failed to start new session! Error: ", e)
                                // this catches the error.. may be do something with this?! UI does reflect the
                                // intended change until refresh!
                            }
                            .addOnSuccessListener { _ ->
                                Log.d(TAG, "updateChildren done")
                            }

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

    }

    override fun onStop() {
        super.onStop()
    }

    companion object {

        private const val TAG = "SessionBottomDialogFrag"

        const val ARG_SESSION_ID = "SESSION_ID"

        private var mBottomSheetListener: BottomSheetListener? = null

        fun newInstance(): SessionBottomSheetFragment {
            val fragmentSessionBottomDialog = SessionBottomSheetFragment()
            val args = Bundle()
            fragmentSessionBottomDialog.arguments = args
            return fragmentSessionBottomDialog
        }

    }
}


