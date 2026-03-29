package com.madinatti.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class EventItem(
    val emoji: String = "",
    val title: String = "",
    val date: String = "",
    val location: String = "",
    val imageEmoji: String = "",
    var interestedCount: Int = 0,
    val duration: String = "",
    val venue: String = "",
    val startTime: String = "",
    val isFree: Boolean = true,
    val price: String = "",
    val description: String = "",
    val organizer: String = "",
    val organizerEmoji: String = "",
    var isInterested: Boolean = false,
    val docId: String = "",
    val imageUrl: String = ""
)

class EventCardAdapter(
    private var events: List<EventItem>,
    private val onEventClick: (EventItem) -> Unit,
    private val onInterestedClick: (Int, EventItem) -> Unit
) : RecyclerView.Adapter<EventCardAdapter.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.tvEventEmoji)
        val tvTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val tvDate: TextView = view.findViewById(R.id.tvEventDate)
        val tvLocation: TextView = view.findViewById(R.id.tvEventLocation)
        val tvImageEmoji: TextView = view.findViewById(R.id.tvEventImageEmoji)
        val ivEventImage: ImageView = view.findViewById(R.id.ivEventImage)
        val tvInterested: TextView = view.findViewById(R.id.tvEventInterested)
        val btnInterested: TextView = view.findViewById(R.id.btnInterested)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_event_card, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]

        holder.tvEmoji.text = event.emoji
        holder.tvTitle.text = event.title
        holder.tvDate.text = "📅 ${event.date}"
        holder.tvLocation.text = "📍 ${event.location}"

        // Image or emoji
        if (event.imageUrl.isNotEmpty()) {
            holder.ivEventImage.visibility = View.VISIBLE
            holder.tvImageEmoji.visibility = View.GONE
            Glide.with(holder.itemView.context)
                .load(event.imageUrl).centerCrop()
                .into(holder.ivEventImage)
        } else {
            holder.ivEventImage.visibility = View.GONE
            holder.tvImageEmoji.visibility = View.VISIBLE
            holder.tvImageEmoji.text = event.imageEmoji
        }

        // ── Real interested count from subcollection ──
        if (event.docId.isNotEmpty()) {
            db.collection("events").document(event.docId)
                .collection("interested").get()
                .addOnSuccessListener { result ->
                    val realCount = result.size()
                    event.interestedCount = realCount
                    holder.tvInterested.text = "$realCount intéressés"
                }

            // Check if current user is interested
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                db.collection("events").document(event.docId)
                    .collection("interested").document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        event.isInterested = doc.exists()
                        updateInterestedButton(holder, event)
                    }
            }
        }

        holder.tvInterested.text = "${event.interestedCount} intéressés"
        holder.btnInterested.visibility = View.VISIBLE
        updateInterestedButton(holder, event)

        holder.btnInterested.setOnClickListener {
            event.isInterested = !event.isInterested
            if (event.isInterested) event.interestedCount += 1
            else event.interestedCount = maxOf(0, event.interestedCount - 1)

            holder.tvInterested.text = "${event.interestedCount} intéressés"
            updateInterestedButton(holder, event)

            holder.btnInterested.animate()
                .scaleX(0.9f).scaleY(0.9f).setDuration(100)
                .withEndAction {
                    holder.btnInterested.animate()
                        .scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()

            onInterestedClick(position, event)
        }

        holder.itemView.setOnClickListener { onEventClick(event) }
    }

    private fun updateInterestedButton(holder: ViewHolder, event: EventItem) {
        if (event.isInterested) {
            holder.btnInterested.text = "✓ Intéressé"
            holder.btnInterested.setBackgroundResource(R.drawable.bg_interested_count)
            holder.btnInterested.setTextColor(
                android.graphics.Color.parseColor("#2ECC71"))
        } else {
            holder.btnInterested.text = "Je suis intéressé!"
            holder.btnInterested.setBackgroundResource(R.drawable.bg_interested_btn)
            holder.btnInterested.setTextColor(android.graphics.Color.WHITE)
        }
    }

    override fun getItemCount() = events.size
}