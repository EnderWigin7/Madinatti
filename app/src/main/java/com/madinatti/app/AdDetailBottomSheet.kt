package com.madinatti.app

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdDetailBottomSheet : BottomSheetDialogFragment() {

    private var isSaved = false
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var imageUrls = listOf<String>()

    // Real-time listener for expiry - updates if ad is edited while open
    private var expiryListener:
            com.google.firebase.firestore.ListenerRegistration? = null

    companion object {
        // Track viewed ads per session - prevents double counting
        private val viewedAdIds = mutableSetOf<String>()

        fun newInstance(
            title: String, price: String, description: String,
            city: String, category: String, userName: String,
            emoji: String, userId: String = "", phone: String = "",
            adId: String = "", imageUrls: List<String> = emptyList()
        ): AdDetailBottomSheet {
            return AdDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("title", title)
                    putString("price", price)
                    putString("description", description)
                    putString("city", city)
                    putString("category", category)
                    putString("userName", userName)
                    putString("emoji", emoji)
                    putString("userId", userId)
                    putString("phone", phone)
                    putString("adId", adId)
                    putStringArrayList("imageUrls", ArrayList(imageUrls))
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_ad_detail_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title        = arguments?.getString("title")       ?: "Annonce"
        val price        = arguments?.getString("price")       ?: ""
        val description  = arguments?.getString("description") ?: ""
        val city         = arguments?.getString("city")        ?: ""
        val category     = arguments?.getString("category")    ?: ""
        val userName     = arguments?.getString("userName")    ?: "Vendeur"
        val emoji        = arguments?.getString("emoji")       ?: "📦"
        val userId       = arguments?.getString("userId")      ?: ""
        val phone        = arguments?.getString("phone")       ?: ""
        val adId         = arguments?.getString("adId")        ?: ""
        val imageUrlsArg = arguments?.getStringArrayList("imageUrls")
            ?: arrayListOf()
        imageUrls = imageUrlsArg

        view.findViewById<TextView>(R.id.tvAdTitle)?.text = title
        view.findViewById<TextView>(R.id.tvAdPrice)?.text = price
        view.findViewById<TextView>(R.id.tvAdDescription)?.text =
            description.ifEmpty { "Pas de description" }
        view.findViewById<TextView>(R.id.tvAdLocation)?.text = "📍 $city"
        view.findViewById<TextView>(R.id.tvSellerName)?.text = userName

        // ── Image carousel ──
        val ivMainImage = view.findViewById<ImageView>(R.id.ivMainImage)
        val tvEmoji     = view.findViewById<TextView>(R.id.tvImageEmoji)
        val tvCount     = view.findViewById<TextView>(R.id.tvImageCount)
        val ivPrev      = view.findViewById<View>(R.id.ivPrevImage)
        val ivNext      = view.findViewById<View>(R.id.ivNextImage)
        var currentIndex = 0

        fun showImage(index: Int) {
            if (imageUrls.isNotEmpty() && index < imageUrls.size) {
                val url = imageUrls[index]
                if (url.isNotEmpty()) {
                    ivMainImage?.visibility = View.VISIBLE
                    tvEmoji?.visibility = View.GONE
                    Glide.with(requireContext())
                        .load(url)
                        .placeholder(R.color.surface)
                        .error(R.color.surface)
                        .centerCrop()
                        .into(ivMainImage!!)
                    tvCount?.text = "${index + 1}/${imageUrls.size} 📷"
                } else {
                    ivMainImage?.visibility = View.GONE
                    tvEmoji?.visibility = View.VISIBLE
                    tvEmoji?.text = emoji
                    tvCount?.text = ""
                }
                ivPrev?.visibility =
                    if (imageUrls.size > 1) View.VISIBLE else View.GONE
                ivNext?.visibility =
                    if (imageUrls.size > 1) View.VISIBLE else View.GONE
            } else {
                ivMainImage?.visibility = View.GONE
                tvEmoji?.visibility = View.VISIBLE
                tvEmoji?.text = emoji
                tvCount?.text = ""
                ivPrev?.visibility = View.GONE
                ivNext?.visibility = View.GONE
            }
        }
        showImage(0)

        ivNext?.setOnClickListener {
            if (imageUrls.size > 1) {
                currentIndex = (currentIndex + 1) % imageUrls.size
                showImage(currentIndex)
            }
        }
        ivPrev?.setOnClickListener {
            if (imageUrls.size > 1) {
                currentIndex = (currentIndex - 1 + imageUrls.size) % imageUrls.size
                showImage(currentIndex)
            }
        }

        // ── Expiry (real-time listener) ──
        loadAdExpiry(adId, view)

        // ── Bookmark ──
        val ivSave = view.findViewById<ImageView>(R.id.ivSaveAd)
        val currentUid = auth.currentUser?.uid

        if (currentUid != null && adId.isNotEmpty()) {
            db.collection("users").document(currentUid)
                .collection("favorites").document(adId)
                .get().addOnSuccessListener { doc ->
                    if (!isAdded) return@addOnSuccessListener
                    isSaved = doc.exists()
                    ivSave?.setImageResource(
                        if (isSaved) R.drawable.ic_bookmark_filled
                        else R.drawable.ic_bookmark_outline
                    )
                }
        }

        ivSave?.isClickable = true
        ivSave?.setOnClickListener {
            if (currentUid == null) {
                Toast.makeText(requireContext(),
                    "Connectez-vous pour sauvegarder",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (adId.isEmpty()) return@setOnClickListener

            isSaved = !isSaved
            ivSave.setImageResource(
                if (isSaved) R.drawable.ic_bookmark_filled
                else R.drawable.ic_bookmark_outline
            )
            ivSave.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100)
                .withEndAction {
                    ivSave.animate().scaleX(1f).scaleY(1f)
                        .setDuration(100).start()
                }.start()

            val favRef = db.collection("users").document(currentUid)
                .collection("favorites").document(adId)

            if (isSaved) {
                favRef.set(mapOf(
                    "adId" to adId, "title" to title,
                    "price" to price, "city" to city,
                    "emoji" to emoji, "userName" to userName,
                    "imageUrls" to imageUrls,
                    "savedAt" to com.google.firebase.Timestamp.now()
                ))
                Toast.makeText(requireContext(),
                    "🔖 Annonce sauvegardée!", Toast.LENGTH_SHORT).show()
            } else {
                favRef.delete()
                Toast.makeText(requireContext(),
                    "Retiré des favoris", Toast.LENGTH_SHORT).show()
            }
        }

        // ── Seller info ──
        loadSellerRating(userId, view)

        if (userId.isNotEmpty()) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (!isAdded) return@addOnSuccessListener
                    val joinYear = doc.getTimestamp("createdAt")?.toDate()
                        ?.let {
                            java.text.SimpleDateFormat("yyyy",
                                java.util.Locale.getDefault()).format(it)
                        } ?: ""
                    view.findViewById<TextView>(R.id.tvSellerJoined)?.text =
                        if (joinYear.isNotEmpty()) "Membre depuis $joinYear"
                        else "Nouveau membre"

                    val avatarUrl = doc.getString("avatarUrl") ?: ""
                    val sellerInitial =
                        view.findViewById<TextView>(R.id.tvSellerInitial)
                    val sellerAvatar =
                        view.findViewById<ImageView>(R.id.ivSellerAvatar)

                    if (avatarUrl.isNotEmpty() && sellerAvatar != null) {
                        sellerAvatar.visibility = View.VISIBLE
                        sellerInitial?.visibility = View.GONE
                        Glide.with(requireContext())
                            .load(avatarUrl).circleCrop()
                            .placeholder(R.drawable.bg_avatar)
                            .into(sellerAvatar)
                    } else {
                        sellerInitial?.text =
                            (doc.getString("name") ?: userName)
                                .firstOrNull()?.toString()?.uppercase() ?: "?"
                    }

                    val realName = doc.getString("name")
                    if (!realName.isNullOrEmpty()) {
                        view.findViewById<TextView>(R.id.tvSellerName)
                            ?.text = realName
                    }
                }
        }

        // ── Message ──
        view.findViewById<View>(R.id.btnMessage)?.setOnClickListener {
            val uid = auth.currentUser?.uid
            when {
                uid == null -> Toast.makeText(requireContext(),
                    "Connectez-vous d'abord", Toast.LENGTH_SHORT).show()
                userId == uid -> Toast.makeText(requireContext(),
                    "C'est votre propre annonce", Toast.LENGTH_SHORT).show()
                userId.isEmpty() -> Toast.makeText(requireContext(),
                    "Vendeur introuvable", Toast.LENGTH_SHORT).show()
                else -> openChat(uid, userId, userName, adId, title)
            }
        }

        // ── Call ──
        view.findViewById<View>(R.id.btnCall)?.setOnClickListener {
            if (phone.isNotEmpty()) {
                startActivity(Intent(Intent.ACTION_DIAL,
                    Uri.parse("tel:$phone")))
            } else if (userId.isNotEmpty()) {
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { doc ->
                        val sellerPhone = doc.getString("phone") ?: ""
                        if (sellerPhone.isNotEmpty()) {
                            startActivity(Intent(Intent.ACTION_DIAL,
                                Uri.parse("tel:$sellerPhone")))
                        } else {
                            Toast.makeText(requireContext(),
                                "📞 Numéro non disponible",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        // ── Report ──
        view.findViewById<View>(R.id.btnReport)?.setOnClickListener {
            if (adId.isNotEmpty() && currentUid != null) {
                db.collection("reports").add(mapOf(
                    "adId" to adId,
                    "reportedBy" to currentUid,
                    "reason" to "Signalement utilisateur",
                    "createdAt" to com.google.firebase.Timestamp.now()
                ))
            }
            Toast.makeText(requireContext(),
                "✅ Signalement envoyé", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────
    // EXPIRY - real-time listener so it updates
    // immediately if ad is edited while sheet is open
    // ─────────────────────────────────────────────
    private fun loadAdExpiry(adId: String, view: View) {
        if (adId.isEmpty()) {
            view.findViewById<TextView>(R.id.tvAdExpiry)?.visibility = View.GONE
            return
        }

        val tvExpiry    = view.findViewById<TextView>(R.id.tvAdExpiry) ?: return
        val tvSoldBadge = view.findViewById<TextView>(R.id.tvSoldBadge)

        expiryListener?.remove()
        expiryListener = db.collection("ads").document(adId)
            .addSnapshotListener { doc, error ->
                if (error != null || !isAdded) return@addSnapshotListener
                if (doc == null || !doc.exists()) {
                    tvExpiry.visibility = View.GONE
                    return@addSnapshotListener
                }

                // ── Sold check ──
                val status = doc.getString("status") ?: "active"
                if (status == "vendue") {
                    tvSoldBadge?.visibility = View.VISIBLE
                    tvSoldBadge?.text = "✅ VENDUE"
                    tvExpiry.text = "Cette annonce a été vendue"
                    tvExpiry.setTextColor(
                        android.graphics.Color.parseColor("#7FA68A"))
                    tvExpiry.visibility = View.VISIBLE
                    return@addSnapshotListener
                }
                tvSoldBadge?.visibility = View.GONE

                // ── Get timestamps and duration ──
                val expiresAt = doc.getTimestamp("expiresAt")
                val createdAt = doc.getTimestamp("createdAt")
                val duration: Long? = doc.get("duration")?.let {
                    when (it) {
                        is Long   -> it
                        is Double -> it.toLong()
                        is Int    -> it.toLong()
                        is String -> it.toLongOrNull()
                        else      -> null
                    }
                }

                // ── Calculate expiry ms ──
                // Priority: expiresAt field > createdAt + duration
                val expiryMs: Long? = when {
                    expiresAt != null ->
                        expiresAt.toDate().time
                    createdAt != null && duration != null && duration > 0 ->
                        createdAt.toDate().time +
                                (duration * 24L * 60L * 60L * 1000L)
                    else -> null
                }

                android.util.Log.d("AdExpiry", """
                    adId=$adId
                    expiresAt=${expiresAt?.toDate()}
                    createdAt=${createdAt?.toDate()}
                    duration=$duration days
                    expiryMs=$expiryMs
                    diffDays=${expiryMs?.let {
                    (it - System.currentTimeMillis()) /
                            (1000L * 60L * 60L * 24L)
                }}
                """.trimIndent())

                if (expiryMs == null) {
                    tvExpiry.visibility = View.GONE
                    countView(adId, doc)
                    return@addSnapshotListener
                }

                // ── Display ──
                val nowMs     = System.currentTimeMillis()
                val diffMs    = expiryMs - nowMs
                val diffDays  = diffMs / (1000L * 60L * 60L * 24L)
                val diffHours = (diffMs / (1000L * 60L * 60L)) % 24L

                when {
                    diffMs <= 0 -> {
                        tvExpiry.text = "⏰ Annonce expirée"
                        tvExpiry.setTextColor(
                            android.graphics.Color.parseColor("#E74C3C"))
                    }
                    diffDays >= 30 -> {
                        val months = diffDays / 30
                        tvExpiry.text = "⏰ Expire dans $months mois"
                        tvExpiry.setTextColor(
                            android.graphics.Color.parseColor("#7FA68A"))
                    }
                    diffDays > 1 -> {
                        tvExpiry.text = "⏰ Expire dans $diffDays jours"
                        tvExpiry.setTextColor(when {
                            diffDays <= 3 ->
                                android.graphics.Color.parseColor("#E67E22")
                            diffDays <= 7 ->
                                android.graphics.Color.parseColor("#F1C40F")
                            else ->
                                android.graphics.Color.parseColor("#7FA68A")
                        })
                    }
                    diffDays == 1L -> {
                        tvExpiry.text = "⏰ Expire demain"
                        tvExpiry.setTextColor(
                            android.graphics.Color.parseColor("#E67E22"))
                    }
                    diffHours > 0 -> {
                        tvExpiry.text = "⏰ Expire dans ${diffHours}h"
                        tvExpiry.setTextColor(
                            android.graphics.Color.parseColor("#E74C3C"))
                    }
                    else -> {
                        tvExpiry.text = "⏰ Expire très bientôt"
                        tvExpiry.setTextColor(
                            android.graphics.Color.parseColor("#E74C3C"))
                    }
                }
                tvExpiry.visibility = View.VISIBLE

                // ── Count view ──
                countView(adId, doc)
            }
    }

    private fun countView(
        adId: String,
        doc: com.google.firebase.firestore.DocumentSnapshot
    ) {
        val currentUid = auth.currentUser?.uid ?: return
        val adOwnerId  = doc.getString("userId") ?: ""
        if (currentUid != adOwnerId && !viewedAdIds.contains(adId)) {
            viewedAdIds.add(adId)
            db.collection("ads").document(adId)
                .update("views",
                    com.google.firebase.firestore.FieldValue.increment(1))
                .addOnFailureListener { e ->
                    android.util.Log.e("AdExpiry",
                        "View count failed: ${e.message}")
                }
        }
    }

    private fun loadSellerRating(userId: String, view: View) {
        if (userId.isEmpty()) return
        val tvRating = view.findViewById<TextView>(R.id.tvSellerRating) ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                if (!isAdded) return@addOnSuccessListener
                val createdAt = userDoc.getTimestamp("createdAt")
                val twoWeeksAgo =
                    System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000L)
                val isNewSeller = createdAt != null &&
                        createdAt.toDate().time > twoWeeksAgo

                db.collection("ratings")
                    .whereEqualTo("sellerId", userId).get()
                    .addOnSuccessListener { result ->
                        if (!isAdded) return@addOnSuccessListener
                        val ratings = result.documents
                            .mapNotNull { it.getDouble("rating") }
                        tvRating.text = when {
                            ratings.isEmpty() && isNewSeller -> "Nouveau vendeur"
                            ratings.isEmpty() -> "Pas encore d'avis"
                            else -> {
                                val avg = ratings.average()
                                String.format("%.1f/10 ⭐ (%d avis)",
                                    avg, ratings.size)
                            }
                        }
                    }
                    .addOnFailureListener {
                        if (isAdded) tvRating.text =
                            if (isNewSeller) "Nouveau vendeur" else "–"
                    }
            }
    }

    private fun openChat(
        currentUid: String, sellerId: String,
        sellerName: String, adId: String, adTitle: String
    ) {
        val chatId = if (currentUid < sellerId)
            "${currentUid}_${sellerId}"
        else "${sellerId}_${currentUid}"

        val chatRef = db.collection("chats").document(chatId)
        chatRef.get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                if (!doc.exists()) {
                    chatRef.set(mapOf(
                        "participants" to listOf(currentUid, sellerId),
                        "participantNames" to mapOf(
                            currentUid to
                                    (auth.currentUser?.displayName ?: "Moi"),
                            sellerId to sellerName
                        ),
                        "adId"            to adId,
                        "adTitle"         to adTitle,
                        "lastMessage"     to "",
                        "lastMessageTime" to
                                com.google.firebase.Timestamp.now(),
                        "createdAt"       to
                                com.google.firebase.Timestamp.now(),
                        "unread_$sellerId"    to 0,
                        "unread_$currentUid"  to 0
                    )).addOnSuccessListener {
                        navigateToChat(chatId, sellerId, sellerName, adTitle)
                    }.addOnFailureListener { e ->
                        if (isAdded) Toast.makeText(requireContext(),
                            "❌ ${e.localizedMessage}",
                            Toast.LENGTH_LONG).show()
                    }
                } else {
                    navigateToChat(chatId, sellerId, sellerName, adTitle)
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(),
                    "❌ ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToChat(
        chatId: String, sellerId: String,
        sellerName: String, adTitle: String
    ) {
        if (!isAdded) return
        val bundle = Bundle().apply {
            putString("chatId", chatId)
            putString("otherUserId", sellerId)
            putString("otherUserName", sellerName)
            putString("adTitle", adTitle)
        }
        val mainActivity = activity as? MainActivity
            ?: run { dismiss(); return }
        dismiss()
        mainActivity.window.decorView.postDelayed({
            try {
                val navHost = mainActivity.supportFragmentManager
                    .findFragmentById(R.id.navHostFragment)
                        as? androidx.navigation.fragment.NavHostFragment
                navHost?.navController?.navigate(R.id.dmFragment, bundle)
            } catch (e: Exception) {
                try {
                    mainActivity.binding.bottomNav
                        .selectedItemId = R.id.messagesFragment
                    Toast.makeText(mainActivity,
                        "💬 Chat créé! Ouvrez '$sellerName' dans Messages.",
                        Toast.LENGTH_LONG).show()
                } catch (_: Exception) {}
            }
        }, 300)
    }

    override fun onDestroyView() {
        expiryListener?.remove()
        super.onDestroyView()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { di ->
            val bs = (di as BottomSheetDialog).findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet)
            bs?.let {
                it.setBackgroundResource(R.drawable.bg_bottom_sheet_glass)
                val behavior = BottomSheetBehavior.from(it)
                behavior.peekHeight =
                    (resources.displayMetrics.heightPixels * 0.85).toInt()
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        dialog.window?.setDimAmount(0.3f)
        return dialog
    }

    override fun getTheme(): Int = R.style.GlassBottomSheetDialog
}