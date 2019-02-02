package de.rhab.wlbtimer

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import de.rhab.wlbtimer.model.WlbUser


// https://stackoverflow.com/questions/37753991/com-google-firebase-database-databaseexception-calls-to-setpersistenceenabled
class FirebaseHandler : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        Log.i(TAG, "enabled offline persistence")
    }

    companion object {

        private const val TAG = "FirebaseHandler"

        fun lastOnlineTracking(user: FirebaseUser) {

            // since I can connect from multiple devices, we store each connection instance separately
            // any time that connectionsRef's value is null (i.e. has no children) I am offline
            val database = FirebaseDatabase.getInstance()
            val myConnectionsRef = database.reference
                    .child(WlbUser.FBP)
                    .child(user.uid)
                    .child("connections")

            // stores the timestamp of my last disconnect (the last time I was seen online)
            val lastOnlineRef = database.reference
                    .child(WlbUser.FBP)
                    .child(user.uid)
                    .child("lastOnline")

            val connectedRef = database.getReference(".info/connected")
            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java)!!
                    if (connected) {
                        val con = myConnectionsRef.push()

                        // when this device disconnects, remove it
                        con.onDisconnect().removeValue()

                        // when I disconnect, update the last time I was seen online
                        lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP)

                        // add this device to my connections list
                        // this value could contain info about the device or a timestamp too
                        con.setValue(java.lang.Boolean.TRUE)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    System.err.println("Listener was cancelled at .info/connected")
                }
            })
        }
    }
}