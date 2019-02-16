package de.rhab.wlbtimer.adapter

import android.support.annotation.Keep
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.firebase.ui.firestore.ObservableSnapshotArray
import com.google.firebase.firestore.DocumentSnapshot
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.model.Category


@Keep
class CategoryOffAdapter(options: FirestoreRecyclerOptions<Category>) :
        FirestoreRecyclerAdapter<Category, CategoryOffAdapter.CategoryOffHolder>(options) {

    var listener: OnItemClickListener? = null

    override fun onBindViewHolder(holder: CategoryOffHolder, position: Int, model: Category) {
        if (model.default) {
            holder.textViewDefaultIcon.visibility = View.VISIBLE
        } else {
            holder.textViewDefaultIcon.visibility = View.GONE
        }
        holder.textViewTitle.text = model.title
        holder.textViewTimesUsed.text = "used: ${model.sessions.count()}"
        holder.cardLayout.setCardBackgroundColor(model.colorToInt())

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryOffHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category_off,
                parent, false)
        return CategoryOffHolder(v)
    }

    fun deleteItem(position: Int) {
        // ToDo(frennkie) deleting does not delete sub collections! Check it
        snapshots.getSnapshot(position).reference.delete()
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully deleted!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error deleting document", e) }
    }

    inner class CategoryOffHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewDefaultIcon: TextView = itemView.findViewById(R.id.tv_category_off_default_icon)
        var textViewTitle: TextView = itemView.findViewById(R.id.tv_category_off_title)
        var textViewTimesUsed: TextView = itemView.findViewById(R.id.tv_category_off_times_used)
        var cardLayout: CardView = itemView.findViewById(R.id.card_view)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener!!.onItemClick(snapshots.getSnapshot(position), position)
                }
            }
            itemView.setOnLongClickListener{
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener!!.onItemLongClick(snapshots, snapshots.getSnapshot(position), position)
                }
                true
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int)
        fun onItemLongClick(snapshots: ObservableSnapshotArray<Category>,
                            documentSnapshot: DocumentSnapshot, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "CategoryOffAdapter"
    }
}