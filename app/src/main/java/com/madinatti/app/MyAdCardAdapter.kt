package com.madinatti.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class MyAdItem(
    val adId: String = "",
    val emoji: String,
    val title: String,
    val price: String,
    val status: String,
    val time: String,
    val views: Int,
    val messages: Int,
    val category: String
)

class MyAdCardAdapter(
    private var ads: MutableList<MyAdItem>,
    private val onAdClick: (MyAdItem) -> Unit,
    private val onOptionsClick: (MyAdItem, View) -> Unit
) : RecyclerView.Adapter<MyAdCardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEmoji: TextView = view.findViewById(R.id.tvAdEmoji)
        val tvTitle: TextView = view.findViewById(R.id.tvAdTitle)
        val tvPrice: TextView = view.findViewById(R.id.tvAdPrice)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val statusDot: View = view.findViewById(R.id.statusDot)
        val badgeContainer: LinearLayout = view.findViewById(R.id.badgeContainer)
        val tvTime: TextView = view.findViewById(R.id.tvAdTime)
        val tvViews: TextView = view.findViewById(R.id.tvAdViews)
        val tvMessages: TextView = view.findViewById(R.id.tvAdMessages)
        val tvCategory: TextView = view.findViewById(R.id.tvAdCategory)
        val btnOptions: TextView = view.findViewById(R.id.btnOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_ad_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ad = ads[position]

        holder.tvEmoji.text = ad.emoji
        holder.tvTitle.text = ad.title
        holder.tvPrice.text = ad.price
        holder.tvTime.text = ad.time
        holder.tvViews.text = "${ad.views} vues"
        holder.tvMessages.text = "${ad.messages} msgs"
        holder.tvCategory.text = ad.category

        // Status badge styling
        when (ad.status) {
            "active" -> {
                holder.tvStatus.text = "Active"
                holder.tvStatus.setTextColor(0xFF2ECC71.toInt())
                holder.statusDot.setBackgroundResource(R.drawable.bg_online_dot)
                holder.badgeContainer.setBackgroundResource(R.drawable.bg_status_active)
            }
            "vendue" -> {
                holder.tvStatus.text = "Vendue"
                holder.tvStatus.setTextColor(0xFF3498DB.toInt())
                holder.statusDot.setBackgroundResource(R.drawable.bg_dot_blue)
                holder.badgeContainer.setBackgroundResource(R.drawable.bg_status_sold)
            }
            "expiree" -> {
                holder.tvStatus.text = "Expirée"
                holder.tvStatus.setTextColor(0xFFE74C3C.toInt())
                holder.statusDot.setBackgroundResource(R.drawable.bg_dot_red)
                holder.badgeContainer.setBackgroundResource(R.drawable.bg_status_expired)
            }
        }

        holder.itemView.setOnClickListener { onAdClick(ad) }
        holder.btnOptions.setOnClickListener { onOptionsClick(ad, it) }
    }

    override fun getItemCount() = ads.size

    fun updateList(newList: List<MyAdItem>) {
        ads.clear()
        ads.addAll(newList)
        notifyDataSetChanged()
    }
}