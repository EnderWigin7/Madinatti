package com.madinatti.app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FirebaseFirestore

object PresenceManager {

    private val auth = FirebaseAuth.getInstance()
    private val rtdb = FirebaseDatabase.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun startTracking() {
        val uid = auth.currentUser?.uid ?: return

        val userStatusRTDB = rtdb.getReference("status/$uid")
        val connectedRef = rtdb.getReference(".info/connected")

        // What to write when OFFLINE - this happens automatically
        // even if app crashes or loses internet!
        val offlineStatus = mapOf(
            "isOnline" to false,
            "lastSeen" to ServerValue.TIMESTAMP
        )

        val onlineStatus = mapOf(
            "isOnline" to true,
            "lastSeen" to ServerValue.TIMESTAMP
        )

        connectedRef.addValueEventListener(object :
            com.google.firebase.database.ValueEventListener {

            override fun onDataChange(
                snapshot: com.google.firebase.database.DataSnapshot
            ) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (!connected) return

                // When disconnected (for ANY reason), set offline automatically
                userStatusRTDB.onDisconnect().setValue(offlineStatus)

                // Set online now
                userStatusRTDB.setValue(onlineStatus)

                // Also sync to Firestore so your existing
                // DmFragment listener still works
                userStatusRTDB.addValueEventListener(object :
                    com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(
                        snap: com.google.firebase.database.DataSnapshot
                    ) {
                        val isOnline = snap.child("isOnline")
                            .getValue(Boolean::class.java) ?: false
                        val lastSeenMs = snap.child("lastSeen")
                            .getValue(Long::class.java) ?: 0L

                        // Sync RTDB status → Firestore
                        val firestoreUid = auth.currentUser?.uid ?: return
                        firestore.collection("users").document(firestoreUid)
                            .update(mapOf(
                                "isOnline" to isOnline,
                                "lastSeen" to com.google.firebase.Timestamp(
                                    lastSeenMs / 1000, 0
                                )
                            ))
                    }
                    override fun onCancelled(
                        error: com.google.firebase.database.DatabaseError) {}
                })
            }
            override fun onCancelled(
                error: com.google.firebase.database.DatabaseError) {}
        })
    }

    fun stopTracking() {
        val uid = auth.currentUser?.uid ?: return
        rtdb.getReference("status/$uid").setValue(
            mapOf(
                "isOnline" to false,
                "lastSeen" to ServerValue.TIMESTAMP
            )
        )
    }
}