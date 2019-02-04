package de.rhab.wlbtimer.adapter

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
import de.rhab.wlbtimer.model.Note


class NoteAdapter(options: FirestoreRecyclerOptions<Note>) : FirestoreRecyclerAdapter<Note, NoteAdapter.NoteHolder>(options) {

    var listener: OnItemClickListener? = null

    override fun onBindViewHolder(holder: NoteHolder, position: Int, model: Note) {
        holder.textViewTitle.text = model.title
        holder.textViewDescription.text = model.description
        holder.textViewPriority.text = model.priority.toString()

        // Make sure that FAB does not overlay last items of RecyclerView. Add margin to bottom.
        // ToDO this does not work cleanly!
//        if (position + 1 == itemCount) {
//            // set bottom margin to 72 or 76dp (depends on list item layout)
//            setBottomMargin(holder.itemView, (76 * Resources.getSystem().displayMetrics.density).toInt())
//        } else {
//            // reset bottom margin back to zero. (your value may be different)
//            setBottomMargin(holder.itemView, 0)
//        }
    }

//    private fun setBottomMargin(view: View, bottomMargin: Int) {
//        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
//            val params = view.layoutParams as ViewGroup.MarginLayoutParams
//            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, bottomMargin)
//            view.requestLayout()
//        }
//    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note,
                parent, false)
        return NoteHolder(v)
    }

    fun deleteItem(position: Int) {
        // ToDo(frennkie) deleting does not delete sub collections! Check it
        snapshots.getSnapshot(position).reference.delete()
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully deleted!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error deleting document", e) }
    }

    inner class NoteHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textViewTitle: TextView = itemView.findViewById(R.id.tv_note_title)
        var textViewDescription: TextView = itemView.findViewById(R.id.tv_note_description)
        var textViewPriority: TextView = itemView.findViewById(R.id.tv_note_priority)

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
        private const val TAG = "NoteAdapter"
    }
}