package com.madinatti.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class EventItem(
    val emoji: String,
    val title: String,
    val date: String,
    val location: String,
    val imageEmoji: String,
    val interestedCount: Int,

    val duration: String = "",
    val venue: String = "",
    val startTime: String = "18:00",
    val isFree: Boolean = true,
    val price: String = "Gratuit",
    val description: String = "",
    val organizer: String = "",
    val organizerEmoji: String = "🏛️",
    var isInterested: Boolean = false
)

class EventCardAdapter(
    private val events: List<EventItem>,
    private val onEventClick: (EventItem) -> Unit,
    private val onInterestedClick: (Int, EventItem) -> Unit
) : RecyclerView.Adapter<EventCardAdapter.ViewHolder>() {

    inner class ViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.tvEventEmoji)
        val tvTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val tvDate: TextView = view.findViewById(R.id.tvEventDate)
        val tvLocation: TextView =
            view.findViewById(R.id.tvEventLocation)
        val tvImageEmoji: TextView =
            view.findViewById(R.id.tvEventImageEmoji)
        val tvInterested: TextView =
            view.findViewById(R.id.tvEventInterested)
        val btnInterested: TextView =
            view.findViewById(R.id.btnInterested)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder, position: Int
    ) {
        val event = events[position]

        holder.tvEmoji.text = event.emoji
        holder.tvTitle.text = event.title
        holder.tvDate.text = "📅 ${event.date}"
        holder.tvLocation.text = "📍 ${event.location}"
        holder.tvImageEmoji.text = event.imageEmoji
        holder.tvInterested.text =
            "${event.interestedCount} intéressés"


        holder.btnInterested.visibility = View.VISIBLE
        updateInterestedButton(holder, event)

        holder.btnInterested.setOnClickListener {
            event.isInterested = !event.isInterested
            if (event.isInterested) {

                holder.btnInterested.animate()
                    .scaleX(0.9f).scaleY(0.9f)
                    .setDuration(100)
                    .withEndAction {
                        holder.btnInterested.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(100).start()
                    }.start()
            }
            updateInterestedButton(holder, event)
            onInterestedClick(position, event)
        }

        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }

    private fun updateInterestedButton(
        holder: ViewHolder, event: EventItem
    ) {
        if (event.isInterested) {
            holder.btnInterested.text = "✓ Intéressé"
            holder.btnInterested.setBackgroundResource(
                R.drawable.bg_interested_count
            )
            holder.btnInterested.setTextColor(
                android.graphics.Color.parseColor("#2ECC71")
            )
        } else {
            holder.btnInterested.text = "Je suis intéressé!"
            holder.btnInterested.setBackgroundResource(
                R.drawable.bg_interested_btn
            )
            holder.btnInterested.setTextColor(
                android.graphics.Color.WHITE
            )
        }
    }

    override fun getItemCount() = events.size
}