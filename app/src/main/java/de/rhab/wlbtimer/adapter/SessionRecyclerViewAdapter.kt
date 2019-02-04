package de.rhab.wlbtimer.adapter

import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.fragment.HomeFragment
import de.rhab.wlbtimer.fragment.SessionBottomSheetFragment
import de.rhab.wlbtimer.model.Session


/**
 * [RecyclerView.Adapter] that can display a [Session] and makes a call to the
 * specified [HomeFragment.OnListFragmentInteractionListener].
 */
class SessionRecyclerViewAdapter(
        private val mValues: List<Session>,
        private val mListener: HomeFragment.OnListFragmentInteractionListener?
) : RecyclerView.Adapter<SessionRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_session_fragment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.mDateView.text = mValues[position].getDateStart()

        // ToDo(frennkie) somehow add color here

        holder.mTsStartView.text = mValues[position].getTimeStart()

        if (mValues[position].finished) {
            val mTimeZonedStart = Session.fromDefaultStr(mValues[position].tsStart!!)!!
            val mTimeZonedEnd = Session.fromDefaultStr(mValues[position].tsEnd!!)!!

            val mDiff = mTimeZonedEnd.dayOfYear - mTimeZonedStart.dayOfYear
            val mOffsetString: String = when {  // add " (-1)", " (+2)" or nothing to the end
                mDiff < 0 -> {
                    " ($mDiff)"
                }
                mDiff > 0 -> {
                    " (+$mDiff)"
                }
                else -> {
                    ""
                }
            }
            holder.mTsEndView.text = mValues[position].getTimeEnd() + mOffsetString

        } else {
            holder.mTsEndView.text = "..."
        }

        if (mValues[position].category != null) {
            holder.mTsDurationView.text = mValues[position].getDurationWeightedExcludingBreaks(mValues[position].category!!.factor)
        } else {
            holder.mTsDurationView.text = mValues[position].getDurationWeightedExcludingBreaks(1.0)
        }

        // Make sure that FAB does not overlay last items of RecyclerView. Add margin to bottom.
        // position 0 is **last** item due to app:reverseLayout="true" (in .xml)
        if (position == 0) {
            // set bottom margin to 72dp.
            setBottomMargin(holder.itemView, (72 * Resources.getSystem().displayMetrics.density).toInt())
        } else {
            // reset bottom margin back to zero. (your value may be different)
            setBottomMargin(holder.itemView, 0)
        }

        holder.mView.setOnClickListener { v ->
            val activity = v.context as AppCompatActivity
            val sessionBottomDialogFragment = SessionBottomSheetFragment.newInstance()

            val bundle = Bundle()
            bundle.putString(SessionBottomSheetFragment.ARG_SESSION_ID, holder.mItem!!.objectId)
            sessionBottomDialogFragment.arguments = bundle

            sessionBottomDialogFragment.show(activity.supportFragmentManager, "session_dialog_fragment")

            mListener?.onListFragmentInteraction(holder.mItem!!)

        }

        // ToDo(frennkie) disabled for now
//        holder.mView.setOnLongClickListener { v ->
//            mListener?.onListFragmentInteraction(holder.mItem!!)
//
//            val activity = v.context as AppCompatActivity
//            val fabStartView: View = activity.findViewById(R.id.fabStartNew)
//
//            Snackbar.make(fabStartView,
//                    "Thanks for long pressing: " + holder.mItem!!.tsStart!!,
//                    Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
//
//            true
//        }

    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    private fun setBottomMargin(view: View, bottomMargin: Int) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, bottomMargin)
            view.requestLayout()
        }
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mDateView: TextView = mView.findViewById<View>(R.id.fragment_session_date) as TextView
        val mIcon: TextView = mView.findViewById<View>(R.id.fragment_session_icon) as TextView
        val mTsStartView: TextView = mView.findViewById<View>(R.id.fragment_session_ts_start) as TextView
        val mTsEndView: TextView = mView.findViewById<View>(R.id.fragment_session_ts_end) as TextView
        val mTsDurationView: TextView = mView.findViewById<View>(R.id.fragment_session_duration) as TextView
        var mItem: Session? = null

        override fun toString(): String {
            return super.toString() + " '" + mTsStartView.text
        }
    }

    companion object {
        private const val TAG = "SessionRecViewAdapter"  // Log Tag
    }
}
