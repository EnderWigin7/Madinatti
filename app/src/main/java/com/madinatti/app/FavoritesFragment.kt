package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FavoritesFragment : Fragment() {

    private var isFirstLoad = true

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
            view.findViewById(R.id.favTopBar)
        ) { _, insets ->
            val h = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.statusBars()
            ).top
            view.findViewById<View>(R.id.statusBarSpacer)
                .apply { layoutParams.height = h; requestLayout() }
            insets
        }

        view.findViewById<View>(R.id.btnBack)?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        isFirstLoad = true
        loadFavorites(view)
        loadSavedEvents(view)
    }

    private fun loadFavorites(view: View) {
        val uid = auth.currentUser?.uid ?: return
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyFavorites)
        val rv = view.findViewById<RecyclerView>(R.id.rvFavorites)

        db.collection("users").document(uid)
            .collection("favorites")
            .orderBy("savedAt",
                com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded) return@addOnSuccessListener

                if (result.isEmpty) {
                    // Don't show empty yet — events might still load
                    rv?.visibility = View.GONE
                    checkEmpty(view)
                    return@addOnSuccessListener
                }

                tvEmpty?.visibility = View.GONE
                rv?.visibility = View.VISIBLE

                val ads = mutableListOf<AdItem>()

                for (doc in result) {
                    val imageUrls = doc.get("imageUrls") as? List<*>
                    val firstImage = imageUrls?.firstOrNull()?.toString() ?: ""
                    val adId = doc.getString("adId") ?: doc.id

                    ads.add(AdItem(
                        emoji = doc.getString("emoji") ?: "📦",
                        title = doc.getString("title") ?: "",
                        location = "📍 ${doc.getString("city") ?: ""}",
                        price = doc.getString("price") ?: "",
                        time = "",
                        imageUrl = firstImage,
                        bookmarked = true,
                        adId = adId
                    ))
                }

                val adapter = AdCardAdapter(
                    ads = ads,
                    onAdClick = { ad ->
                        if (ad.adId.isNotEmpty()) {
                            db.collection("ads").document(ad.adId).get()
                                .addOnSuccessListener { doc ->
                                    if (!isAdded) return@addOnSuccessListener
                                    if (doc == null || !doc.exists()) {
                                        AdDetailBottomSheet.newInstance(
                                            title = ad.title, price = ad.price,
                                            description = "",
                                            city = ad.location.replace("📍 ", ""),
                                            category = "", userName = "",
                                            emoji = ad.emoji, adId = ad.adId,
                                            imageUrls = if (ad.imageUrl.isNotEmpty())
                                                listOf(ad.imageUrl) else emptyList()
                                        ).show(parentFragmentManager, "AdDetail")
                                        return@addOnSuccessListener
                                    }
                                    val imgList = (doc.get("imageUrls") as? List<*>)
                                        ?.mapNotNull { it?.toString() } ?: emptyList()
                                    AdDetailBottomSheet.newInstance(
                                        title = doc.getString("title") ?: ad.title,
                                        price = "${doc.getDouble("price")?.toInt() ?: 0} MAD",
                                        description = doc.getString("description") ?: "",
                                        city = doc.getString("city") ?: "",
                                        category = doc.getString("category") ?: "",
                                        userName = doc.getString("userName") ?: "",
                                        emoji = doc.getString("emoji") ?: ad.emoji,
                                        userId = doc.getString("userId") ?: "",
                                        adId = doc.id, imageUrls = imgList
                                    ).show(parentFragmentManager, "AdDetail")
                                }
                        }
                    },
                    onBookmarkClick = { pos, ad ->
                        if (ad.adId.isNotEmpty()) {
                            db.collection("users").document(uid)
                                .collection("favorites").document(ad.adId)
                                .delete()
                                .addOnSuccessListener {
                                    if (!isAdded) return@addOnSuccessListener
                                    ads.removeAt(pos)
                                    rv?.adapter?.notifyItemRemoved(pos)
                                    rv?.adapter?.notifyItemRangeChanged(pos, ads.size)
                                    if (ads.isEmpty()) {
                                        rv?.visibility = View.GONE
                                        checkEmpty(requireView())
                                    }
                                }
                        }
                    }
                )

                rv?.layoutManager = GridLayoutManager(requireContext(), 2)
                rv?.adapter = adapter

                if (rv?.itemDecorationCount == 0) {
                    val sp = (4 * resources.displayMetrics.density).toInt()
                    rv?.addItemDecoration(object : RecyclerView.ItemDecoration() {
                        override fun getItemOffsets(
                            outRect: android.graphics.Rect, view: View,
                            parent: RecyclerView, state: RecyclerView.State
                        ) {
                            if (parent.getChildAdapterPosition(view) % 2 == 0)
                                outRect.right = sp else outRect.left = sp
                            outRect.bottom = sp * 2
                        }
                    })
                }
            }
    }

    private fun loadSavedEvents(view: View) {
        val uid = auth.currentUser?.uid ?: return
        val eventsContainer = view.findViewById<LinearLayout>(
            R.id.savedEventsContainer) ?: return

        eventsContainer.removeAllViews()

        db.collection("users").document(uid)
            .collection("savedEvents")
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded) return@addOnSuccessListener

                eventsContainer.removeAllViews()

                if (result.isEmpty) {
                    checkEmpty(view)
                    return@addOnSuccessListener
                }

                val dp = resources.displayMetrics.density

                eventsContainer.addView(TextView(requireContext()).apply {
                    text = "🎉 Événements sauvegardés"
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 14f
                    typeface = resources.getFont(R.font.poppins_semibold)
                    setPadding((16 * dp).toInt(), (16 * dp).toInt(), 0, (8 * dp).toInt())
                })

                for (doc in result) {
                    val eventId = doc.getString("eventId") ?: doc.id
                    val title = doc.getString("title") ?: "Événement"

                    val row = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding((16 * dp).toInt(), (12 * dp).toInt(),
                            (16 * dp).toInt(), (12 * dp).toInt())
                        isClickable = true; isFocusable = true
                        val ta = context.obtainStyledAttributes(
                            intArrayOf(android.R.attr.selectableItemBackground))
                        foreground = ta.getDrawable(0); ta.recycle()
                    }

                    // Emoji thumbnail
                    row.addView(TextView(requireContext()).apply {
                        text = "🎉"; textSize = 28f
                        gravity = android.view.Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            (56 * dp).toInt(), (56 * dp).toInt())
                        setBackgroundResource(R.drawable.bg_photo_add)
                    })

                    // Text column
                    val col = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        ).apply { marginStart = (12 * dp).toInt() }
                    }
                    col.addView(TextView(requireContext()).apply {
                        text = title
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 13f
                        typeface = resources.getFont(R.font.poppins_semibold)
                        maxLines = 1
                    })
                    col.addView(TextView(requireContext()).apply {
                        text = "Événement sauvegardé"
                        setTextColor(android.graphics.Color.parseColor("#7FA68A"))
                        textSize = 10f
                        typeface = resources.getFont(R.font.poppins_regular)
                    })
                    row.addView(col)

                    row.addView(TextView(requireContext()).apply {
                        text = "✕"; textSize = 16f
                        setTextColor(android.graphics.Color.parseColor("#7FA68A"))
                        gravity = android.view.Gravity.CENTER
                        setPadding((8 * dp).toInt(), 0, 0, 0)
                        setOnClickListener {
                            val uid2 = auth.currentUser?.uid ?: return@setOnClickListener

                            db.collection("users").document(uid2)
                                .collection("savedEvents").document(eventId)
                                .delete()

                            db.collection("events").document(eventId)
                                .collection("interested").document(uid2)
                                .delete()

                            db.collection("events").document(eventId)
                                .update(
                                    "interestedCount",
                                    com.google.firebase.firestore.FieldValue.increment(-1)
                                )

                            if (isAdded) loadSavedEvents(requireView())
                        }
                    })

                    row.setOnClickListener {
                        db.collection("events").document(eventId).get()
                            .addOnSuccessListener { eDoc ->
                                if (!isAdded || eDoc == null || !eDoc.exists())
                                    return@addOnSuccessListener
                                val event = EventItem(
                                    emoji = eDoc.getString("emoji") ?: "🎉",
                                    title = eDoc.getString("title") ?: "",
                                    date = eDoc.getString("date") ?: "",
                                    location = eDoc.getString("location") ?: "",
                                    imageEmoji = eDoc.getString("imageEmoji") ?: "🎉",
                                    interestedCount = (eDoc.getLong("interestedCount")
                                        ?: 0).toInt(),
                                    duration = eDoc.getString("duration") ?: "",
                                    venue = eDoc.getString("venue") ?: "",
                                    startTime = eDoc.getString("startTime") ?: "",
                                    isFree = eDoc.getBoolean("isFree") ?: true,
                                    price = eDoc.getString("price") ?: "",
                                    description = eDoc.getString("description") ?: "",
                                    organizer = eDoc.getString("organizer") ?: "",
                                    organizerEmoji = eDoc.getString("organizerEmoji")
                                        ?: "🏛️",
                                    docId = eDoc.id,
                                    imageUrl = eDoc.getString("imageUrl") ?: ""
                                )
                                EventDetailBottomSheet.newInstance(event)
                                    .show(parentFragmentManager, "EventDetail")
                            }
                    }

                    eventsContainer.addView(row)

                    // Divider
                    eventsContainer.addView(View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1
                        ).apply {
                            marginStart = (16 * dp).toInt()
                            marginEnd = (16 * dp).toInt()
                        }
                        setBackgroundColor(
                            android.graphics.Color.parseColor("#1E3D2A"))
                    })
                }

                view.findViewById<TextView>(R.id.tvEmptyFavorites)
                    ?.visibility = View.GONE
            }
    }

    private fun checkEmpty(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.rvFavorites)
        val events = view.findViewById<LinearLayout>(R.id.savedEventsContainer)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyFavorites)

        val adsEmpty = rv?.visibility == View.GONE
                || (rv?.adapter?.itemCount ?: 0) == 0
        val eventsEmpty = (events?.childCount ?: 0) == 0

        tvEmpty?.visibility = if (adsEmpty && eventsEmpty) View.VISIBLE
        else View.GONE
    }


    override fun onResume() {
        super.onResume()
        if (isFirstLoad) {
            isFirstLoad = false
            return
        }
        view?.let {
            loadFavorites(it)
            loadSavedEvents(it)
        }
    }
}