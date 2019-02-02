package de.rhab.wlbtimer.adapter

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.model.CategoryWork


class CategoryWorkAdapter(options: FirebaseRecyclerOptions<CategoryWork>) : FirebaseRecyclerAdapter<CategoryWork, CategoryWorkAdapter.CategoryWorkHolder>(options) {

    var listener: OnItemClickListener? = null

    override fun onBindViewHolder(holder: CategoryWorkHolder, position: Int, model: CategoryWork) {
        holder.textViewTitle.text = model.title
        holder.textViewFactor.text = model.factor.toString()
        holder.textViewTimesUsed.text = "used: ${model.sessions.count()}"
        holder.cardLayout.setCardBackgroundColor(model.colorToInt())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryWorkHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category_work,
                parent, false)
        return CategoryWorkHolder(v)
    }

    fun deleteItem(position: Int) {
        Log.d(TAG, "going to delete CategoryWork with title: ${snapshots[position].title}")
        snapshots.getSnapshot(position).ref.removeValue { databaseError, databaseReference ->
            if (databaseError != null) {
                Log.e(TAG, "failed to delete pos: $position at ref: $databaseReference " +
                        "with error: $databaseError!")
            }
        }
    }

    inner class CategoryWorkHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewTitle: TextView = itemView.findViewById(R.id.tv_category_work_title)
        var textViewFactor: TextView = itemView.findViewById(R.id.tv_category_work_factor)
        var textViewTimesUsed: TextView = itemView.findViewById(R.id.tv_category_work_times_used)
        var cardLayout: CardView = itemView.findViewById(R.id.card_view)

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
        fun onItemClick(dataSnapshot: DataSnapshot, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "CatWorkAdapter"
    }
}