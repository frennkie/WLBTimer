package de.rhab.wlbtimer.adapter

import android.support.v7.widget.CardView
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
import de.rhab.wlbtimer.model.CategoryNonWork


class CategoryNonWorkAdapter(options: FirestoreRecyclerOptions<CategoryNonWork>) :
        FirestoreRecyclerAdapter<CategoryNonWork, CategoryNonWorkAdapter.CategoryNonWorkHolder>(options) {

    var listener: OnItemClickListener? = null

    override fun onBindViewHolder(holder: CategoryNonWorkHolder, position: Int, model: CategoryNonWork) {
        holder.textViewTitle.text = model.title
        holder.cardLayout.setCardBackgroundColor(model.colorToInt())

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryNonWorkHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category_non_work,
                parent, false)
        return CategoryNonWorkHolder(v)
    }

    fun deleteItem(position: Int) {
        // ToDo(frennkie) deleting does not delete sub collections! Check it
        snapshots.getSnapshot(position).reference.delete()
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully deleted!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error deleting document", e) }
    }

    inner class CategoryNonWorkHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewTitle: TextView = itemView.findViewById(R.id.tv_category_non_work_title)
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
        fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "CatNonWorkAdapter"
    }
}