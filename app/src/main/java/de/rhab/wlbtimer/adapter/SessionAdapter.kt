package de.rhab.wlbtimer.adapter


import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.support.annotation.Keep
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.model.Session
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.IsoFields
import android.support.v7.widget.CardView


@Keep
class SessionAdapter(options: FirestoreRecyclerOptions<Session>)
    : FirestoreRecyclerAdapter<Session, SessionAdapter.SessionHolder>(options) {

    var listener: OnItemClickListener? = null

    private lateinit var ctx: Context

    override fun onBindViewHolder(holder: SessionHolder, position: Int, model: Session) {

        if (model.allDay) {
            onBindViewHolderSessionAllDay(holder, position, model)

        } else {
            onBindViewHolderSessionTimed(holder, position, model)

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_session,
                parent, false)
        ctx = v.context
        return SessionHolder(v)
    }

    private fun onBindViewHolderSessionAllDay(holder: SessionHolder, position: Int, model: Session) {

        holder.mTsEndView.visibility = View.GONE
        holder.mTsDurationView.visibility = View.GONE

        val tsStartZonedDateTime = ZonedDateTime.parse(model.tsStart!!)!!
        val weekOfYear = tsStartZonedDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

        if (weekOfYear.rem(2) == 0) {
            holder.mCard.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.session_off_card_week_even))
        } else {
            holder.mCard.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.session_off_card_week_odd))
        }

        val mIconBackground = holder.mIcon.background as GradientDrawable

        if (model.category != null) {
            holder.mIcon.text = model.category!!.title.substring(0, 1)
            mIconBackground.setColor(Color.parseColor(model.category!!.color))

        } else {
            holder.mIcon.text = "-"
            mIconBackground.setColor(Color.parseColor("#666666"))
        }

        holder.mDateView.text = model.getDateStartWithWeekday()

        if (model.note != null) {
            holder.mTsStartView.text = model.note
        } else {
            holder.mTsStartView.text = ""
        }

    }

    private fun onBindViewHolderSessionTimed(holder: SessionHolder, position: Int, model: Session) {

        holder.mTsEndView.visibility = View.VISIBLE
        holder.mTsDurationView.visibility = View.VISIBLE

        val tsStartZonedDateTime = ZonedDateTime.parse(model.tsStart!!)!!
        val weekOfYear = tsStartZonedDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

        if (weekOfYear.rem(2) == 0) {
            holder.mCard.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.session_work_card_week_even))
        } else {
            holder.mCard.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.session_work_card_week_odd))
        }

        val mIconBackground = holder.mIcon.background as GradientDrawable

        holder.mDateView.text = model.getDateStartWithWeekday()

        holder.mTsStartView.text = model.getTimeStart()

        if (model.finished) {
            val mTimeZonedStart = Session.fromDefaultStr(model.tsStart!!)!!
            val mTimeZonedEnd = Session.fromDefaultStr(model.tsEnd!!)!!

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
            holder.mTsEndView.text = model.getTimeEnd() + mOffsetString

        } else {
            holder.mTsEndView.text = "..."
        }

        if (model.category != null) {
            holder.mTsDurationView.text = model.getDurationWeightedExcludingBreaks()
            holder.mIcon.text = model.category!!.title.substring(0, 1)

            val mColor = Color.parseColor(model.category!!.color)
            mIconBackground.setColor(mColor)

        } else {
            holder.mTsDurationView.text = model.getDurationWeightedExcludingBreaks()

            holder.mIcon.text = "-"
            mIconBackground.setColor(Color.parseColor("#666666"))

        }

    }

    fun deleteItem(position: Int) {
        // ToDo(frennkie) deleting does not delete sub collections! Check it
        snapshots.getSnapshot(position).reference.delete()
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully deleted!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error deleting document", e) }
    }

    inner class SessionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val mCard: CardView = itemView.findViewById<View>(R.id.card_session) as CardView
        val mDateView: TextView = itemView.findViewById<View>(R.id.tv_session_date) as TextView
        val mIcon: TextView = itemView.findViewById<View>(R.id.tv_session_icon) as TextView
        val mTsStartView: TextView = itemView.findViewById<View>(R.id.tv_session_ts_start) as TextView
        val mTsEndView: TextView = itemView.findViewById<View>(R.id.tv_session_ts_end) as TextView
        val mTsDurationView: TextView = itemView.findViewById<View>(R.id.tv_session_duration) as TextView

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener!!.onItemClick(snapshots.getSnapshot(position), position)
                }
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "SessionAdapter"
    }
}