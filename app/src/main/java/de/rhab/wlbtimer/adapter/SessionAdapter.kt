package de.rhab.wlbtimer.adapter


import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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


class SessionAdapter(options: FirestoreRecyclerOptions<Session>)
    : FirestoreRecyclerAdapter<Session, SessionAdapter.SessionHolder>(options) {

    var listener: OnItemClickListener? = null

    override fun onBindViewHolder(holder: SessionHolder, position: Int, model: Session) {
        holder.mDateView.text = model.getDateStartWithWeekday()

        val mIconBackground = holder.mIcon.background as GradientDrawable
//        val mRandom = Random()
//        val color = Color.argb(255, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256))
//        mIconBackground.setColor(color)

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
            holder.mTsDurationView.text = model.getDurationWeightedExcludingBreaks(model.category!!.factor)
            holder.mIcon.text = model.category!!.title.substring(0, 1)

            val mColor = Color.parseColor(model.category!!.color)
            mIconBackground.setColor(mColor)

        } else {
            holder.mTsDurationView.text = model.getDurationWeightedExcludingBreaks(1.0)

            holder.mIcon.text = "-"
            mIconBackground.setColor(Color.parseColor("#666666"))

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_session,
                parent, false)
        return SessionHolder(v)
    }

    fun deleteItem(position: Int) {
        // ToDo(frennkie) deleting does not delete sub collections! Check it
        snapshots.getSnapshot(position).reference.delete()
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully deleted!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error deleting document", e) }
    }

    inner class SessionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

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