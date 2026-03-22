package com.madinatti.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EventDetailBottomSheet : BottomSheetDialogFragment() {

    private var event: EventItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.GlassBottomSheetDialog)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.layout_event_detail_sheet, container, false
        )
    }

    override fun onViewCreated(
        view: View, savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.ivPrevImage)?.setOnClickListener { /* TODO */ }
        view.findViewById<View>(R.id.ivNextImage)?.setOnClickListener { /* TODO */ }

        val e = event ?: return

        // Hero
        view.findViewById<TextView>(R.id.tvHeroEmoji)
            ?.text = e.imageEmoji

        // Category
        view.findViewById<TextView>(R.id.tvCategory)
            ?.text = categoryLabel(e)

        // Title
        view.findViewById<TextView>(R.id.tvDetailTitle)
            ?.text = e.title

        // Date
        view.findViewById<TextView>(R.id.tvDetailDate)
            ?.text = e.date
        view.findViewById<TextView>(R.id.tvDetailDuration)
            ?.text = " · ${e.duration}"

        // Location
        view.findViewById<TextView>(R.id.tvDetailVenue)
            ?.text = e.venue
        view.findViewById<TextView>(R.id.tvDetailNeighborhood)
            ?.text = " · ${e.location}"

        // Time
        view.findViewById<TextView>(R.id.tvDetailTime)
            ?.text = buildString {
            append("Commence à ${e.startTime}")
            if (e.isFree) append(" · Entrée gratuite")
        }

        // Price
        view.findViewById<TextView>(R.id.tvDetailPrice)
            ?.text = if (e.isFree) "Gratuit" else e.price
        view.findViewById<TextView>(R.id.tvDetailPriceSub)
            ?.text = if (e.isFree) " · Accès libre" else ""

        // Description
        view.findViewById<TextView>(R.id.tvDetailDescription)
            ?.text = e.description

        // Organizer
        view.findViewById<TextView>(R.id.tvOrganizerEmoji)
            ?.text = e.organizerEmoji
        view.findViewById<TextView>(R.id.tvOrganizerName)
            ?.text = e.organizer

        // Map
        view.findViewById<TextView>(R.id.tvMapAddress)
            ?.text = "${e.venue}, ${e.location}"

        // Interested label
        view.findViewById<TextView>(R.id.tvInterestedLabel)
            ?.text = "${e.interestedCount} personnes sont intéressées"

        // Maps link
        view.findViewById<TextView>(R.id.tvMapsLink)
            ?.setOnClickListener {
                val uri = Uri.parse(
                    "geo:0,0?q=${Uri.encode(
                        "${e.venue}, ${e.location}"
                    )}"
                )
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }

        // Interested button
        val btnInterested = view.findViewById<LinearLayout>(
            R.id.btnInterested
        )
        val tvLabel = view.findViewById<TextView>(
            R.id.tvInterestedBtnLabel
        )
        updateInterestedBtn(tvLabel, btnInterested, e.isInterested)

        btnInterested?.setOnClickListener {
            e.isInterested = !e.isInterested
            updateInterestedBtn(tvLabel, btnInterested, e.isInterested)
            btnInterested.animate()
                .scaleX(0.95f).scaleY(0.95f).setDuration(80)
                .withEndAction {
                    btnInterested.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(120).start()
                }.start()
        }

        // Bookmark
        var bookmarked = false
        view.findViewById<FrameLayout>(R.id.btnBookmark)
            ?.setOnClickListener { v ->
                bookmarked = !bookmarked
                view.findViewById<TextView>(
                    R.id.tvBookmarkIcon
                )?.text = if (bookmarked) "✓" else "🔖"
                v.animate()
                    .scaleX(0.9f).scaleY(0.9f).setDuration(80)
                    .withEndAction {
                        v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(120).start()
                    }.start()
            }
    }

    private fun updateInterestedBtn(
        tv: TextView?,
        btn: LinearLayout?,
        interested: Boolean
    ) {
        if (interested) {
            tv?.text = getString(R.string.event_interested_done)
            tv?.setTextColor(
                android.graphics.Color.parseColor("#2ECC71")
            )
            btn?.setBackgroundResource(
                R.drawable.bg_button_outline_green
            )
        } else {
            tv?.text = getString(R.string.event_interested_btn)
            tv?.setTextColor(
                android.graphics.Color.parseColor("#0D1F17")
            )
            btn?.setBackgroundResource(R.drawable.bg_button_green)
        }
    }

    private fun categoryLabel(e: EventItem): String {
        return when (e.emoji) {
            "🎭" -> getString(R.string.event_cat_culture)
            "🏃" -> getString(R.string.event_cat_sport)
            "🎨" -> getString(R.string.event_cat_art)
            "🎵" -> getString(R.string.event_cat_music)
            "🍽️" -> getString(R.string.event_cat_food)
            "📚" -> getString(R.string.event_cat_books)
            else -> "🎪 Événement"
        }
    }

    companion object {
        fun newInstance(
            event: EventItem
        ): EventDetailBottomSheet {
            return EventDetailBottomSheet().apply {
                this.event = event
            }
        }
    }
}