package de.rhab.wlbtimer.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import de.rhab.wlbtimer.R
import de.rhab.wlbtimer.adapter.NoteAdapter
import de.rhab.wlbtimer.databinding.ActivityNoteBinding
import de.rhab.wlbtimer.model.Note
import de.rhab.wlbtimer.model.WlbUser


@Keep
class NoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteBinding

    private var mAuthListener: FirebaseAuth.AuthStateListener? = null

    private val mAuth = FirebaseAuth.getInstance()

    private lateinit var mAdapter: NoteAdapter

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNoteBinding.inflate(layoutInflater)

        // check user authentication - don't forget onStart() and onStop()
        mAuthListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser == null) {
                startActivity(Intent(this@NoteActivity, SignInActivity::class.java))
            }
        }

        setContentView(binding.root)
        setSupportActionBar(binding.myToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val buttonAddNote = findViewById<FloatingActionButton>(R.id.button_add_note)
        buttonAddNote.setOnClickListener {
            addNote()
            Log.d(TAG, "added Note")
            // startActivity(Intent(this@NoteActivity, NewNoteActivity::class.java))
        }

        setUpRecyclerView()

    }

    private fun setUpRecyclerView() {
        val query = db.collection(WlbUser.FBP)
                .document(mAuth.currentUser!!.uid)
            .collection(Note.FBP)
            .orderBy("priority", Query.Direction.DESCENDING)
        // .whereArrayContains("tags", "ccc")

        val options = FirestoreRecyclerOptions.Builder<Note>()
            .setQuery(query, Note::class.java)
            .build()

        mAdapter = NoteAdapter(options)

        val recyclerView =
            findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.note_recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this)
        recyclerView.adapter = mAdapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                direction: Int
            ) {
                mAdapter.deleteItem(viewHolder.adapterPosition)
            }
        }).attachToRecyclerView(recyclerView)

        mAdapter.setOnItemClickListener(object : NoteAdapter.OnItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot, position: Int) {
                //val note = documentSnapshot.toObject(Note::class.java)
                val id = documentSnapshot.id
                //val path = documentSnapshot.reference
                Toast.makeText(
                    this@NoteActivity,
                    "Position: $position ID: $id", Toast.LENGTH_SHORT
                ).show()
            }
        })

    }

    public override fun onStart() {
        super.onStart()
        mAdapter.startListening()
        mAuth.addAuthStateListener(mAuthListener!!)
    }

    public override fun onStop() {
        super.onStop()
        mAdapter.stopListening()
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener!!)
        }
    }

    private fun addNote() {
        Toast.makeText(this, "Posting...", Toast.LENGTH_SHORT).show()
        writeNewNote("Title123", "Description123", 123)

    }

    private fun writeNewNote(title: String, description: String, priority: Int) {
        // Create new Note at /users/$userid/notes/$noteid and
        // post key at /users/$userid/note-last

        val userRef = db.collection(WlbUser.FBP).document(mAuth.currentUser!!.uid)

        val batch = db.batch()

        val note = Note(title, description, priority)
        val noteRef = userRef.collection(Note.FBP).document()
        batch.set(noteRef, note)

        batch.update(userRef, "note-last", noteRef.id)

        batch.commit().addOnSuccessListener { _ -> Log.d(TAG, "Successfully batch committed something") }
                .addOnFailureListener { e -> Log.w(TAG, "Error running batch commit", e) }

    }

    companion object {

        private const val TAG = "NoteActivity"
    }
}
