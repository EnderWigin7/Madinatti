package com.madinatti.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EventDetailBottomSheet : BottomSheetDialogFragment() {

    private var event: EventItem? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.GlassBottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_event_detail_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val e = event ?: return
        val uid = auth.currentUser?.uid


        val ivHero = view.findViewById<ImageView>(R.id.ivEventDetailImage)
        val tvHeroEmoji = view.findViewById<TextView>(R.id.tvHeroEmoji)

        if (e.imageUrl.isNotEmpty() && ivHero != null) {
            ivHero.visibility = View.VISIBLE
            tvHeroEmoji?.visibility = View.GONE
            Glide.with(requireContext())
                .load(e.imageUrl)
                .centerCrop()
                .into(ivHero)
        } else {
            ivHero?.visibility = View.GONE
            tvHeroEmoji?.visibility = View.VISIBLE
            tvHeroEmoji?.text = e.imageEmoji
        }



        view.findViewById<TextView>(R.id.tvCategory)?.text = categoryLabel(e)
        view.findViewById<TextView>(R.id.tvDetailTitle)?.text = e.title
        view.findViewById<TextView>(R.id.tvDetailDate)?.text = e.date
        view.findViewById<TextView>(R.id.tvDetailDuration)?.text = " · ${e.duration}"
        view.findViewById<TextView>(R.id.tvDetailVenue)?.text = e.venue
        view.findViewById<TextView>(R.id.tvDetailNeighborhood)?.text = " · ${e.location}"
        view.findViewById<TextView>(R.id.tvDetailTime)?.text = buildString {
            append("Commence à ${e.startTime}")
            if (e.isFree) append(" · Entrée gratuite")
        }
        view.findViewById<TextView>(R.id.tvDetailPrice)?.text =
            if (e.isFree) "Gratuit" else e.price
        view.findViewById<TextView>(R.id.tvDetailPriceSub)?.text =
            if (e.isFree) " · Accès libre" else ""
        view.findViewById<TextView>(R.id.tvDetailDescription)?.text = e.description
        view.findViewById<TextView>(R.id.tvOrganizerEmoji)?.text = e.organizerEmoji
        view.findViewById<TextView>(R.id.tvOrganizerName)?.text = e.organizer
        view.findViewById<TextView>(R.id.tvMapAddress)?.text = "${e.venue}, ${e.location}"

        // ── Real interested count from Firestore ──
        val tvInterestedLabel = view.findViewById<TextView>(R.id.tvInterestedLabel)
        loadRealInterestedCount(e.docId, tvInterestedLabel)

        // ── Maps link ──
        view.findViewById<TextView>(R.id.tvMapsLink)?.setOnClickListener {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode("${e.venue}, ${e.location}")}")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        // ── Interested button — check real state + update ──
        val btnInterested = view.findViewById<LinearLayout>(R.id.btnInterested)
        val tvLabel = view.findViewById<TextView>(R.id.tvInterestedBtnLabel)

        // Check if current user is already interested
        if (uid != null && e.docId.isNotEmpty()) {
            db.collection("events").document(e.docId)
                .collection("interested").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    if (!isAdded) return@addOnSuccessListener
                    e.isInterested = doc.exists()
                    updateInterestedBtn(tvLabel, btnInterested, e.isInterested)
                }
        }

        updateInterestedBtn(tvLabel, btnInterested, e.isInterested)

        btnInterested?.setOnClickListener {
            if (uid == null) {
                android.widget.Toast.makeText(requireContext(),
                    "Connectez-vous d'abord", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (e.docId.isEmpty()) return@setOnClickListener

            e.isInterested = !e.isInterested
            updateInterestedBtn(tvLabel, btnInterested, e.isInterested)

            btnInterested.animate()
                .scaleX(0.95f).scaleY(0.95f).setDuration(80)
                .withEndAction {
                    btnInterested.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()

            // Persist to Firebase
            val ref = db.collection("events").document(e.docId)
                .collection("interested").document(uid)

            if (e.isInterested) {
                ref.set(mapOf(
                    "userId" to uid,
                    "timestamp" to com.google.firebase.Timestamp.now()
                ))
                db.collection("events").document(e.docId)
                    .update("interestedCount",
                        com.google.firebase.firestore.FieldValue.increment(1))
                e.interestedCount += 1
            } else {
                ref.delete()
                db.collection("events").document(e.docId)
                    .update("interestedCount",
                        com.google.firebase.firestore.FieldValue.increment(-1))
                e.interestedCount = maxOf(0, e.interestedCount - 1)
            }

            // Update count label with new value
            tvInterestedLabel?.text =
                "${e.interestedCount} personnes sont intéressées"
        }

        // ── Bookmark ──
        var bookmarked = false
        val tvBookmarkIcon = view.findViewById<TextView>(R.id.tvBookmarkIcon)

        if (uid != null && e.docId.isNotEmpty()) {
            db.collection("users").document(uid)
                .collection("savedEvents").document(e.docId)
                .get().addOnSuccessListener { doc ->
                    if (!isAdded) return@addOnSuccessListener
                    bookmarked = doc.exists()
                    tvBookmarkIcon?.text = if (bookmarked) "✓" else "🔖"
                }
        }

        view.findViewById<FrameLayout>(R.id.btnBookmark)?.setOnClickListener { v ->
            if (uid == null) {
                android.widget.Toast.makeText(requireContext(),
                    "Connectez-vous d'abord", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            bookmarked = !bookmarked
            tvBookmarkIcon?.text = if (bookmarked) "✓" else "🔖"
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
                .withEndAction {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }.start()

            if (uid != null && e.docId.isNotEmpty()) {
                val ref = db.collection("users").document(uid)
                    .collection("savedEvents").document(e.docId)
                if (bookmarked) {
                    ref.set(mapOf(
                        "eventId" to e.docId, "title" to e.title,
                        "savedAt" to com.google.firebase.Timestamp.now()
                    ))
                    android.widget.Toast.makeText(requireContext(),
                        "🔖 Événement sauvegardé!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    ref.delete()
                    android.widget.Toast.makeText(requireContext(),
                        "Retiré des favoris", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ── Prev/Next image nav (for future multi-image support) ──
        view.findViewById<View>(R.id.ivPrevImage)?.visibility = View.GONE
        view.findViewById<View>(R.id.ivNextImage)?.visibility = View.GONE
    }

    private fun loadRealInterestedCount(docId: String, tvLabel: TextView?) {
        if (docId.isEmpty() || tvLabel == null) return

        // First show the cached count
        tvLabel.text = "${event?.interestedCount ?: 0} personnes sont intéressées"

        // Then fetch the real count from subcollection
        db.collection("events").document(docId)
            .collection("interested")
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded) return@addOnSuccessListener
                val realCount = result.size()
                // Update the event's count too for sync
                event?.interestedCount = realCount
                tvLabel.text = "$realCount personnes sont intéressées"
            }
    }

    private fun updateInterestedBtn(
        tv: TextView?, btn: LinearLayout?, interested: Boolean
    ) {
        if (interested) {
            tv?.text = getString(R.string.event_interested_done)
            tv?.setTextColor(android.graphics.Color.parseColor("#2ECC71"))
            btn?.setBackgroundResource(R.drawable.bg_button_outline_green)
        } else {
            tv?.text = getString(R.string.event_interested_btn)
            tv?.setTextColor(android.graphics.Color.parseColor("#0D1F17"))
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
        fun newInstance(event: EventItem): EventDetailBottomSheet {
            return EventDetailBottomSheet().apply { this.event = event }
        }
    }
}