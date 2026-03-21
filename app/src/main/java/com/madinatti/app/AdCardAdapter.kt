package com.madinatti.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class AdItem(
    val emoji: String,
    val title: String,
    val location: String,
    val price: String,
    val time: String,
    var bookmarked: Boolean = false
)

class AdCardAdapter(
    private val ads: List<AdItem>,
    private val onAdClick: (AdItem) -> Unit,
    private val onBookmarkClick: (Int, AdItem) -> Unit
) : RecyclerView.Adapter<AdCardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.tvAdEmoji)
        val tvTitle: TextView = view.findViewById(R.id.tvAdTitle)
        val tvLocation: TextView = view.findViewById(R.id.tvAdLocation)
        val tvPrice: TextView = view.findViewById(R.id.tvAdPrice)
        val tvTime: TextView = view.findViewById(R.id.tvAdTime)
        val ivBookmark: ImageView = view.findViewById(R.id.ivBookmark)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ad_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ad = ads[position]
        holder.tvEmoji.text = ad.emoji
        holder.tvTitle.text = ad.title
        holder.tvLocation.text = ad.location
        holder.tvPrice.text = ad.price
        holder.tvTime.text = ad.time
        holder.ivBookmark.setImageResource(
            if (ad.bookmarked) R.drawable.ic_bookmark_filled
            else R.drawable.ic_bookmark_outline
        )

        holder.itemView.setOnClickListener { onAdClick(ad) }

        holder.ivBookmark.setOnClickListener {
            ad.bookmarked = !ad.bookmarked
            holder.ivBookmark.setImageResource(
                if (ad.bookmarked) R.drawable.ic_bookmark_filled
                else R.drawable.ic_bookmark_outline
            )
            holder.ivBookmark.animate()
                .scaleX(1.3f).scaleY(1.3f).setDuration(100)
                .withEndAction {
                    holder.ivBookmark.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(100).start()
                }.start()
            onBookmarkClick(position, ad)
        }
    }

    override fun getItemCount() = ads.size
}