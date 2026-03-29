package com.madinatti.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class AdItem(
    val emoji: String,
    val title: String,
    val location: String,
    val price: String,
    val time: String,
    val imageUrl: String = "",
    var bookmarked: Boolean = false,
    val adId: String = ""
)

class AdCardAdapter(
    private val ads: List<AdItem>,
    private val onAdClick: (AdItem) -> Unit,
    private val onBookmarkClick: (Int, AdItem) -> Unit
) : RecyclerView.Adapter<AdCardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView     = view.findViewById(R.id.tvAdEmoji)
        val tvTitle: TextView     = view.findViewById(R.id.tvAdTitle)
        val tvLocation: TextView  = view.findViewById(R.id.tvAdLocation)
        val tvPrice: TextView     = view.findViewById(R.id.tvAdPrice)
        val tvTime: TextView      = view.findViewById(R.id.tvAdTime)
        val ivBookmark: ImageView = view.findViewById(R.id.ivBookmark)
        val ivAdImage: ImageView  = view.findViewById(R.id.ivAdImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ad_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ad = ads[position]
        holder.tvTitle.text    = ad.title
        holder.tvLocation.text = ad.location
        holder.tvPrice.text    = ad.price
        holder.tvTime.text     = ad.time

        if (ad.imageUrl.isNotEmpty()) {
            holder.ivAdImage.visibility = View.VISIBLE
            holder.tvEmoji.visibility   = View.GONE
            Glide.with(holder.itemView.context)
                .load(ad.imageUrl)
                .placeholder(R.color.surface)
                .error(R.color.surface)
                .centerCrop()
                .into(holder.ivAdImage)
        } else {
            Glide.with(holder.itemView.context).clear(holder.ivAdImage)
            holder.ivAdImage.visibility = View.GONE
            holder.tvEmoji.visibility   = View.VISIBLE
            holder.tvEmoji.text         = ad.emoji
        }

        holder.ivBookmark.setImageResource(
            if (ad.bookmarked) R.drawable.ic_bookmark_filled
            else R.drawable.ic_bookmark_outline
        )

        holder.itemView.setOnClickListener { onAdClick(ad) }

        holder.ivBookmark.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null || ad.adId.isEmpty()) {
                Toast.makeText(holder.itemView.context,
                    "Connectez-vous pour sauvegarder", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            ad.bookmarked = !ad.bookmarked
            holder.ivBookmark.setImageResource(
                if (ad.bookmarked) R.drawable.ic_bookmark_filled
                else R.drawable.ic_bookmark_outline
            )
            holder.ivBookmark.animate()
                .scaleX(1.3f).scaleY(1.3f).setDuration(100)
                .withEndAction {
                    holder.ivBookmark.animate()
                        .scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()

            val favRef = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("favorites").document(ad.adId)

            if (ad.bookmarked) {
                favRef.set(mapOf(
                    "adId" to ad.adId,
                    "title" to ad.title,
                    "price" to ad.price,
                    "savedAt" to com.google.firebase.Timestamp.now()
                ))
            } else {
                favRef.delete()
            }

            onBookmarkClick(position, ad)
        }
    }

    override fun getItemCount() = ads.size
}