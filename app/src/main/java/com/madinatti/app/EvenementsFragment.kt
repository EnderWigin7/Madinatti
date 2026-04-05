package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class EvenementsFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_evenements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(R.id.rvEvents)
        val tvCount = view.findViewById<TextView>(R.id.tvEventCount)
        rv.layoutManager = LinearLayoutManager(requireContext())
        loadEvents(rv, tvCount)
    }

    override fun onResume() {
        super.onResume()
        val rv = view?.findViewById<RecyclerView>(R.id.rvEvents) ?: return
        val tvCount = view?.findViewById<TextView>(R.id.tvEventCount)
        loadEvents(rv, tvCount)
    }

    private fun loadEvents(rv: RecyclerView, tvCount: TextView?) {
        if (!isAdded) return

        val uid = auth.currentUser?.uid
        val city = requireContext()
            .getSharedPreferences("madinatti_prefs", 0)
            .getString("selected_city", null)

        // Remove orderBy to avoid index/type issues
        db.collection("events")
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded) return@addOnSuccessListener

                val events = mutableListOf<EventItem>()

                for (doc in result) {
                    val docCity = doc.getString("city") ?: ""
                    if (city != null &&
                        city != "Toutes les villes" &&
                        docCity.isNotEmpty() &&
                        docCity != city) continue

                    events.add(EventItem(
                        emoji = doc.getString("emoji") ?: "🎉",
                        title = doc.getString("title") ?: "",
                        date = doc.getString("date") ?: "",
                        location = doc.getString("location") ?: docCity,
                        imageEmoji = doc.getString("imageEmoji") ?: "🎉",
                        interestedCount = (doc.getLong("interestedCount") ?: 0).toInt(),
                        duration = doc.getString("duration") ?: "",
                        venue = doc.getString("venue") ?: "",
                        startTime = doc.getString("startTime") ?: "",
                        isFree = doc.getBoolean("isFree") ?: true,
                        price = doc.getString("price") ?: "",
                        description = doc.getString("description") ?: "",
                        organizer = doc.getString("organizer") ?: "",
                        organizerEmoji = doc.getString("organizerEmoji") ?: "🏛️",
                        docId = doc.id,
                        imageUrl = doc.getString("imageUrl") ?: ""
                    ))
                }
                
                events.sortBy { it.date }

                tvCount?.text = if (events.isNotEmpty())
                    "${events.size} à venir" else "0 événement"

                rv.adapter = EventCardAdapter(
                    events = events,
                    onEventClick = { event ->
                        EventDetailBottomSheet.newInstance(event)
                            .show(parentFragmentManager, "EventDetail")
                    },
                    onInterestedClick = { _, event ->
                        if (uid != null) toggleInterested(event, uid)
                    }
                )
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                android.util.Log.e("Events", "Failed: ${e.message}")
                tvCount?.text = "Erreur de chargement"
                rv.adapter = EventCardAdapter(emptyList(), {}, { _, _ -> })
            }
    }

    private fun toggleInterested(event: EventItem, uid: String) {
        if (event.docId.isEmpty()) return

        val interestedRef = db.collection("events")
            .document(event.docId)
            .collection("interested")
            .document(uid)

        val savedEventRef = db.collection("users")
            .document(uid)
            .collection("savedEvents")
            .document(event.docId)

        if (!event.isInterested) {
            interestedRef.delete()
            savedEventRef.delete()

            db.collection("events").document(event.docId)
                .update("interestedCount",
                    com.google.firebase.firestore.FieldValue.increment(-1))

        } else {
            interestedRef.set(mapOf(
                "userId" to uid,
                "timestamp" to com.google.firebase.Timestamp.now()
            ))

            savedEventRef.set(mapOf(
                "eventId" to event.docId,
                "title" to event.title,
                "date" to event.date,
                "venue" to event.venue,
                "city" to event.location,
                "emoji" to event.emoji,
                "savedAt" to com.google.firebase.Timestamp.now()
            ))

            db.collection("events").document(event.docId)
                .update("interestedCount",
                    com.google.firebase.firestore.FieldValue.increment(1))
        }
    }
}