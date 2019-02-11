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
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.model.Break
import de.rhab.wlbtimer.model.Category
import de.rhab.wlbtimer.model.Session
import de.rhab.wlbtimer.model.WlbUser
import org.threeten.bp.ZonedDateTime


class SessionBottomSheetFragment : BottomSheetDialogFragment() {

    private val mAuth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()

    private lateinit var mCategoryColRef: CollectionReference

    private lateinit var mSessionRef: DocumentReference

    private var mType = Category.TYPE_WORK

    private lateinit var numberList: ListPopupWindow

    private var mCategoryList = HashMap<String, Category>()

    private var mArrayListCategory = ArrayList<Map<String, String>>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_bottom_sheet_session, container, false)

        val llStart = rootView.findViewById<LinearLayout>(R.id.linear_layout_start)
        val tvStartSymbol = rootView.findViewById<TextView>(R.id.tv_btn_start_symbol)
        val tvStartSymbolAllDay = rootView.findViewById<TextView>(R.id.tv_btn_start_symbol_all_day)
        val tvStartDate = rootView.findViewById<TextView>(R.id.tv_btn_start_date)
        val tvStartTime = rootView.findViewById<TextView>(R.id.tv_btn_start_time)

        val llEnd = rootView.findViewById<LinearLayout>(R.id.linear_layout_end)
        val tvEndDate = rootView.findViewById<TextView>(R.id.tv_btn_end_date)
        val tvEndTime = rootView.findViewById<TextView>(R.id.tv_btn_end_time)

        val llBreaks = rootView.findViewById<LinearLayout>(R.id.linear_layout_breaks)
        val tvBreaks = rootView.findViewById<TextView>(R.id.tv_btn_breaks)

        val llNote = rootView.findViewById<LinearLayout>(R.id.linear_layout_note)
        val tvNote = rootView.findViewById<TextView>(R.id.tv_btn_note)

        val llDuration = rootView.findViewById<LinearLayout>(R.id.linear_layout_duration)
        val tvDuration = rootView.findViewById<TextView>(R.id.tv_btn_duration)
        val tvCategory = rootView.findViewById<TextView>(R.id.tv_btn_category)

        val llCategory = rootView.findViewById<LinearLayout>(R.id.linear_layout_category)
        val tvCategoryIcon = rootView.findViewById<TextView>(R.id.tv_category_icon)
        val mTvCategoryIconBackground = tvCategoryIcon.background as GradientDrawable

        val tvRemoveSession = rootView.findViewById<TextView>(R.id.tv_btn_remove_session)

        // first hide all fields until data is loaded (and clear which should be shown)
        // Start has two different Symbols (depending on AllDay or not)
        llStart.visibility = View.GONE
        tvStartSymbol.visibility = View.GONE
        tvStartSymbolAllDay.visibility = View.GONE
        tvStartDate.visibility = View.GONE
        tvStartTime.visibility = View.GONE
        llEnd.visibility = View.GONE
        llBreaks.visibility = View.GONE
        llDuration.visibility = View.GONE
        llNote.visibility = View.VISIBLE
        llCategory.visibility = View.GONE

        // Variable to hold existing or new Session Object
        var mSession: Session? = null


        val userRef = db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid)
        mCategoryColRef = userRef.collection(Category.FBP)

        // get passed in Session ID
        val mId = arguments?.getString(ARG_SESSION_ID)

        if (arguments?.getString(ARG_SESSION_TYPE) != null) {
            mType = arguments?.getString(ARG_SESSION_TYPE)!!
        }

        // get user Document (get default values and other settings)
        userRef.get()
                .addOnFailureListener { e ->
                    Log.d(TAG, "get failed with ", e)
                }
                .addOnSuccessListener { documentSnapshot ->
                    val mWlbUser = documentSnapshot.toObject(WlbUser::class.java)!!
                    val defBreak = mWlbUser.default_break ?: 60 * 45
                    val defCategoryOff = mWlbUser.default_category_off
                    val defCategoryWork = mWlbUser.default_category_work

                    if (mId == null) {
                        Log.i(TAG, "adding new session (Type: $mType)")

                        mSessionRef = userRef.collection(Session.FBP).document()

                        mSession = Session()
                        mSession!!.objectId = mSessionRef.id

                        mSession!!.allDay = when (mType) {
                            Category.TYPE_OFF -> {
                                mSession!!.category = defCategoryOff
                                true
                            }
                            Category.TYPE_WORK -> {
                                mSession!!.category = defCategoryWork
                                false
                            }
                            else -> false
                        }

                        mSession!!.finished = true
                        mSession!!.tsStart = Session.getZonedDateTimeNow().minusHours(8).toString()
                        mSession!!.tsEnd = Session.getZonedDateTimeNow().toString()
                        mSession!!.note = null

                        /* ToDO(frennkie) makes no sense.. working on this in onComplete block below
                            how to get full details from Category here?! :-/
                            val sharedPref = PreferenceManager.getDefaultSharedPreferences(this.context)
                            val defaultCategoryWork = sharedPref.getString("default_category_work", null)
                            if (defaultCategoryWork != null) {
                                Log.d(TAG, "setting default category work from prefs")
                                mSession.category = Category(defaultCategoryWork)
                            }
                        */

                        val mDefaultBreak = Break(
                                comment = "Default Break",
                                duration = defBreak
                        )

                        if (mSession!!.breaks != null) {
                            Log.d(TAG, "breaks present - adding default")
                            mSession!!.breaks!!.add(mDefaultBreak)
                        } else {
                            Log.d(TAG, "no break yet - adding default")
                            mSession!!.breaks = mutableListOf(mDefaultBreak)
                        }

                        // write this to Firestore
                        mSessionRef.set(mSession!!.toMap())

                    } else {
                        Log.d(TAG, "using existing session for mId: $mId (Type: $mType)")
                        mSessionRef = userRef.collection(Session.FBP).document(mId)
                    }

                    Log.d(TAG, "mSessionRef is now: ${mSessionRef.path}")
                }


        // get Categories (either Work or Off type)
        mArrayListCategory = ArrayList()
        mCategoryColRef
                .whereEqualTo("type", mType)
                .get()
                .addOnFailureListener { e ->
                    Log.d(TAG, "get failed with ", e)
                }
                .addOnSuccessListener { docSnapshotCategory ->
                    Log.d(TAG, "Number of category ($mType) entries: ${docSnapshotCategory.count()}")

                    for (document in docSnapshotCategory) {
                        Log.d(TAG, document.id + " => " + document.data)
                        val mCategory = document.toObject(Category::class.java)

                        mCategoryList[mCategory.objectId] = mCategory

                        val tran = LinkedHashMap<String, String>()
                        tran["title"] = mCategory.title
                        tran["objectId"] = mCategory.objectId
                        mArrayListCategory.add(tran)

                    }

                    Log.d(TAG, "listCategory ($mType): $mArrayListCategory")


                    // get Session Object - this has to wait for Categories to be loaded
                    mSessionRef.get()
                            .addOnFailureListener { e ->
                                Log.d(TAG, "get failed with ", e)
                            }
                            .addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot != null) {
                                    mSession = documentSnapshot.toObject(Session::class.java)
                                    Log.d(TAG, "found: $mSession")
                                } else {
                                    Log.w(TAG, "No such document")
                                }

                                if (mSession != null) {

                                    if (mSession!!.allDay) {
                                        llStart.visibility = View.VISIBLE
                                        tvStartSymbol.visibility = View.GONE
                                        tvStartSymbolAllDay.visibility = View.VISIBLE
                                        tvStartDate.visibility = View.VISIBLE
                                        tvStartTime.visibility = View.GONE

                                        llEnd.visibility = View.GONE
                                        llBreaks.visibility = View.GONE
                                        llDuration.visibility = View.GONE
                                        llNote.visibility = View.VISIBLE
                                        llCategory.visibility = View.VISIBLE
                                    } else {
                                        llStart.visibility = View.VISIBLE
                                        tvStartSymbol.visibility = View.VISIBLE
                                        tvStartSymbolAllDay.visibility = View.GONE
                                        tvStartDate.visibility = View.VISIBLE
                                        tvStartTime.visibility = View.VISIBLE

                                        llEnd.visibility = View.VISIBLE
                                        llBreaks.visibility = View.VISIBLE
                                        llDuration.visibility = View.VISIBLE
                                        llNote.visibility = View.VISIBLE
                                        llCategory.visibility = View.VISIBLE
                                    }

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

                                    if (mSession!!.note != null) {
                                        tvNote.text = mSession!!.note
                                    } else {
                                        tvNote.text = "N/A"  // ToDo(frennkie) special format?!
                                    }

                                    val mCategoryWork = mSession!!.category
                                    Log.d(TAG, "result $mCategoryWork")
                                    if (mCategoryWork != null) {
                                        tvCategory.text = mCategoryWork.title

                                        tvCategoryIcon.text = mCategoryWork.title.substring(0, 1)
                                        val mColor = Color.parseColor(mCategoryWork.color)
                                        mTvCategoryIconBackground.setColor(mColor)

                                        tvDuration.text = mSession!!
                                                .getDurationWeightedExcludingBreaks(mFactor = mCategoryWork.factor)
                                    } else {

                                        if (mCategoryList.isEmpty()) {
                                            // User has no Category defined for this Tpe
                                            Log.w(TAG, "User has no Categories defined for type: $mType")
                                            tvCategory.text = "N/A"

                                            tvCategoryIcon.text = "-"
                                            val mColor = Color.parseColor("#666666")
                                            mTvCategoryIconBackground.setColor(mColor)

                                            tvDuration.text = mSession!!.getDurationWeightedExcludingBreaks()
                                        } else {
                                            for (it in mCategoryList.values) {
                                                if (it.default) {
                                                    // set on instance
                                                    mSession!!.category = it

                                                    // upload toFirestore
                                                    val batch = db.batch()
                                                    batch.update(mSessionRef, Category.FBP, mSession!!.category?.toMapNoSessions())

                                                    // ToDo(frennkie) also store on Session?!
                                                    batch.commit()
                                                            .addOnFailureListener { e ->
                                                                Log.w(TAG, "Failed to set/update Category! Error: ", e)
                                                            }
                                                            .addOnSuccessListener { _ ->
                                                                Log.d(TAG, "Category set/updated")
                                                            }

                                                    // display
                                                    tvCategory.text = it.title

                                                    tvCategoryIcon.text = it.title.substring(0, 1)
                                                    val mColor = Color.parseColor(it.color)
                                                    mTvCategoryIconBackground.setColor(mColor)

                                                    tvDuration.text = mSession!!
                                                            .getDurationWeightedExcludingBreaks(mFactor = it.factor)
                                                    break
                                                }
                                            }

                                        }

                                    }

                                }

                            }  // End of mSessionRef.get()

                }  // End of mCategoryColRef.get()

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

        tvNote.setOnClickListener {
            mBottomSheetListener!!.onOptionClick("tvNote clicked...")
            Log.d(TAG, "tvNote clicked...")

            val mBuilder = AlertDialog.Builder(context!!)
            val mNoteInflater = this.layoutInflater
            val dialogView = mNoteInflater.inflate(R.layout.custom_dialog, null)
            mBuilder.setView(dialogView)

            val etNote = dialogView.findViewById(R.id.edit1) as EditText
            etNote.setText(mSession!!.note, TextView.BufferType.EDITABLE)

            mBuilder.setTitle("Update Note")
            mBuilder.setNeutralButton("Cancel") { _, _ -> }
            mBuilder.setNegativeButton("Clear Note") { _, _ ->
                tvNote.text = "N/A"
                mSessionRef.update("note", null)
            }
            mBuilder.setPositiveButton("Save") { _, _ ->
                tvNote.text = etNote.text.toString()
                mSessionRef.update("note", etNote.text.toString())
            }

            val mDialog = mBuilder.create()
            mDialog.show()

        }

        tvCategory.setOnClickListener {
            mBottomSheetListener!!.onOptionClick("tvCategory clicked...")
            Log.d(TAG, "tvCategory clicked...")

            numberList = ListPopupWindow(context!!)
            numberList.anchorView = tvCategory

            val adapter = SimpleAdapter(context, mArrayListCategory,
                    android.R.layout.simple_spinner_dropdown_item,
                    arrayOf("title"),
                    intArrayOf(android.R.id.text1))
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            numberList.setAdapter(adapter)


            // DRY attempt
//                listCategory = listCategoryWork
//                mCategoryList = mCategoryWorkList


            numberList.setOnItemClickListener { _, _, position, _ ->
                Log.d(TAG, "listCW: $mArrayListCategory; p: $position; listCW[p]:${mArrayListCategory[position]}")
                val map = mArrayListCategory[position]
                val mCategoryObjectId = map["objectId"]!!
                val mCategory = mCategoryList[mCategoryObjectId]!!
                tvCategory.text = mCategory.title


                tvCategoryIcon.text = mCategory.title.substring(0, 1)
                val mColor = Color.parseColor(mCategory.color)
                mTvCategoryIconBackground.setColor(mColor)


                // store possible old Category objectId
                var mOldCategoryObjectId: String? = null
                if (mSession!!.category?.objectId != null) mOldCategoryObjectId = mSession!!.category!!.objectId

                val batch = db.batch()

                mSession!!.category = mCategory
                batch.update(mSessionRef, Category.FBP, mSession!!.category?.toMapNoSessions())

                // store an array on the Category instance of every session where the instance is used
                val mCategoryRef = userRef.collection(Category.FBP).document(mCategoryObjectId)
                batch.update(mCategoryRef, Session.FBP, FieldValue.arrayUnion(mSession!!.objectId))

                // check if entry has an old category
                if (mOldCategoryObjectId != null) {
                    // if a different category was selected remove Session entry from old category
                    if (mOldCategoryObjectId != mCategoryObjectId) {
                        batch.update(userRef.collection(Category.FBP).document(mOldCategoryObjectId),
                                Session.FBP, FieldValue.arrayRemove(mSession!!.objectId))
                        Log.d(TAG, "removed from array on Category")
                    }
                }

                // execute bulk update
                batch.commit()
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to set/update Category! Error: ", e)
                            // this catches the error.. may be do something with this?! UI does reflect the
                            // intended change until refresh!
                        }
                        .addOnSuccessListener { _ ->
                            Log.d(TAG, "Category set/updated")
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

        const val ARG_SESSION_TYPE = "SESSION_TYPE"

        private var mBottomSheetListener: BottomSheetListener? = null

        fun newInstance(): SessionBottomSheetFragment {
            val fragmentSessionBottomDialog = SessionBottomSheetFragment()
            val args = Bundle()
            fragmentSessionBottomDialog.arguments = args
            return fragmentSessionBottomDialog
        }

    }
}


