package com.madinatti.app

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.madinatti.app.databinding.FragmentHomeBinding
import com.bumptech.glide.Glide

class HomeFragment : Fragment() {

    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val discoverRefreshRunnable = object : Runnable {
        override fun run() {
            if (_binding != null && isAdded) {
                allNearbyPlaces.clear()
                lastDiscoverCity = ""
                lastDiscoverFilter = ""
                loadDiscoverPlaces(selectedChip)
            }
            refreshHandler.postDelayed(this, 5 * 60 * 1000L) // 5 minutes
        }
    }

    private var lastDiscoverCity = ""
    private var lastDiscoverFilter = ""

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var selectedChip = "tout"
    private val db = FirebaseFirestore.getInstance()

    data class NearbyPlace(
        val name: String, val type: String, val emoji: String,
        val lat: Double, val lon: Double
    )

    private val cityCoords = mapOf(
        "Agadir" to Pair(30.4278, -9.5981),
        "Al Hoceima" to Pair(35.2517, -3.9372),
        "Azrou" to Pair(33.4341, -5.2228),
        "Beni Mellal" to Pair(32.3373, -6.3498),
        "Berrechid" to Pair(33.2654, -7.5806),
        "Casablanca" to Pair(33.5731, -7.5898),
        "Chefchaouen" to Pair(35.1688, -5.2636),
        "El Jadida" to Pair(33.2316, -8.5007),
        "Errachidia" to Pair(31.9314, -4.4343),
        "Essaouira" to Pair(31.5085, -9.7595),
        "Fes" to Pair(34.0331, -5.0003),
        "Fès" to Pair(34.0331, -5.0003),
        "Guelmim" to Pair(28.9833, -10.0566),
        "Ifrane" to Pair(33.5228, -5.1109),
        "Kenitra" to Pair(34.2610, -6.5802),
        "Kénitra" to Pair(34.2610, -6.5802),
        "Khemisset" to Pair(33.8242, -6.0664),
        "Khouribga" to Pair(32.8811, -6.9063),
        "Laayoune" to Pair(27.1536, -13.2033),
        "Larache" to Pair(35.1932, -6.1561),
        "Marrakech" to Pair(31.6295, -7.9811),
        "Meknes" to Pair(33.8935, -5.5473),
        "Meknès" to Pair(33.8935, -5.5473),
        "Mohammedia" to Pair(33.6861, -7.3833),
        "Nador" to Pair(35.1681, -2.9335),
        "Ouarzazate" to Pair(30.9189, -6.8936),
        "Oujda" to Pair(34.6814, -1.9086),
        "Rabat" to Pair(34.0209, -6.8416),
        "Safi" to Pair(32.2994, -9.2372),
        "Sale" to Pair(34.0531, -6.7986),
        "Salé" to Pair(34.0531, -6.7986),
        "Settat" to Pair(33.0011, -7.6166),
        "Sidi Kacem" to Pair(34.2260, -5.7076),
        "Tanger" to Pair(35.7595, -5.8340),
        "Taza" to Pair(34.2133, -4.0103),
        "Temara" to Pair(33.9275, -6.9078),
        "Tetouan" to Pair(35.5785, -5.3684),
        "Tétouan" to Pair(35.5785, -5.3684),
        "Tiznit" to Pair(29.6974, -9.7316),
        "Toutes les villes" to Pair(33.5731, -7.5898)
    )

    private var allNearbyPlaces = mutableListOf<NearbyPlace>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("madinatti_prefs", 0)
        if (prefs.getString("selected_city", null) == null) {
            prefs.edit().putString("selected_city", "Casablanca").apply()
        }

        updateShortcutTexts()

        TopBarHelper.onCityChanged = {
            if (isAdded && _binding != null) {
                loadRecentAds()
                updateShortcutTexts()
                loadDiscoverPlaces()
            }
        }

        TopBarHelper.setup(
            topBarBinding = binding.topBarInclude,
            showBackButton = false,
            fragmentManager = parentFragmentManager
        )

        binding.topBarInclude.ivNotifications.setOnClickListener {
            NotificationsBottomSheet.newInstance()
                .show(parentFragmentManager, "notifications")
        }

        binding.root.findViewById<LinearLayout>(R.id.shortcutMarketplace)?.setOnClickListener {
            (requireActivity() as MainActivity).navigateToVilleTab("marketplace")
        }
        binding.root.findViewById<LinearLayout>(R.id.shortcutPrieres)?.setOnClickListener {
            (requireActivity() as MainActivity).navigateToVilleTab("prieres")
        }
        binding.root.findViewById<LinearLayout>(R.id.shortcutMeteo)?.setOnClickListener {
            (requireActivity() as MainActivity).navigateToVilleTab("meteo")
        }
        binding.root.findViewById<LinearLayout>(R.id.shortcutEvenements)?.setOnClickListener {
            (requireActivity() as MainActivity).navigateToVilleTab("evenements")
        }

        setupChips()
        startWeatherPulse()
        loadRecentAds()
        refreshHandler.postDelayed(discoverRefreshRunnable, 5 * 60 * 1000L)

        val etHomeSearch = binding.root.findViewById<android.widget.EditText>(R.id.etHomeSearch)
        etHomeSearch?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filterHomeContent(s?.toString()?.trim() ?: "")
            }
        })

        binding.tvVoirToutAnnonces.setOnClickListener {
            (requireActivity() as MainActivity).navigateToVilleTab("marketplace")
        }

        binding.root.findViewById<TextView>(R.id.tvVoirToutDiscover)?.setOnClickListener {
            showFullDiscoverList()
        }
    }

    // ══════════════════════════════════════════
    //  NORMALIZE HELPER
    // ══════════════════════════════════════════

    private fun norm(t: String): String =
        java.text.Normalizer.normalize(t.lowercase(), java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

    // ══════════════════════════════════════════
    //  SEARCH / FILTER
    // ══════════════════════════════════════════

    private fun filterHomeContent(query: String) {
        if (_binding == null || !isAdded) return

        if (query.isEmpty()) {
            loadRecentAds()
            binding.annoncesHeader.visibility = View.VISIBLE
            binding.adsGrid.visibility = View.VISIBLE
            binding.root.findViewById<View>(R.id.discoverHeader)?.visibility = View.VISIBLE
            binding.root.findViewById<View>(R.id.discoverScrollView)?.visibility = View.VISIBLE
            loadDiscoverPlaces(selectedChip)
            updateExploreSection(selectedChip)
            return
        }

        val q = norm(query)
        val city = requireContext().getSharedPreferences("madinatti_prefs", 0)
            .getString("selected_city", null)

        // Filter ads
        db.collection("ads").get().addOnSuccessListener { result ->
            if (_binding == null || !isAdded) return@addOnSuccessListener

            val cards = listOf(
                Triple(R.id.adTitle1, R.id.adPrice1, R.id.adCard1),
                Triple(R.id.adTitle2, R.id.adPrice2, R.id.adCard2),
                Triple(R.id.adTitle3, R.id.adPrice3, R.id.adCard3),
                Triple(R.id.adTitle4, R.id.adPrice4, R.id.adCard4)
            )
            cards.forEach { binding.root.findViewById<View>(it.third)?.visibility = View.GONE }

            val filtered = result.documents.filter {
                (it.getString("status") ?: "") == "active"
            }.filter {
                city == null || city == "Toutes les villes" || it.getString("city") == city
            }.filter {
                norm(it.getString("title") ?: "").contains(q) ||
                        norm(it.getString("category") ?: "").contains(q) ||
                        norm(it.getString("description") ?: "").contains(q)
            }.sortedByDescending { it.getTimestamp("createdAt")?.toDate()?.time ?: 0 }

            filtered.take(4).forEachIndexed { i, doc ->
                if (i >= cards.size) return@forEachIndexed
                val (titleId, priceId, cardId) = cards[i]
                val card = binding.root.findViewById<View>(cardId) ?: return@forEachIndexed
                binding.root.findViewById<TextView>(titleId)?.text = doc.getString("title") ?: ""
                val p = doc.getDouble("price")?.toInt() ?: 0
                binding.root.findViewById<TextView>(priceId)?.text = "$p MAD"
                card.visibility = View.VISIBLE

                val imageViewIds = listOf(R.id.adImage1, R.id.adImage2, R.id.adImage3, R.id.adImage4)
                val imgView = binding.root.findViewById<ImageView>(imageViewIds[i])
                val firstImage = (doc.get("imageUrls") as? List<*>)?.firstOrNull()?.toString() ?: ""
                if (firstImage.isNotEmpty() && imgView != null) {
                    Glide.with(this).load(firstImage).placeholder(R.color.surface).centerCrop().into(imgView)
                }

                card.setOnClickListener {
                    val imgList = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    AdDetailBottomSheet.newInstance(
                        title = doc.getString("title") ?: "", price = "$p MAD",
                        description = doc.getString("description") ?: "",
                        city = doc.getString("city") ?: "", category = doc.getString("category") ?: "",
                        userName = doc.getString("userName") ?: "", emoji = doc.getString("emoji") ?: "📦",
                        userId = doc.getString("userId") ?: "", adId = doc.id, imageUrls = imgList
                    ).show(parentFragmentManager, "AdDetail")
                }
            }

            binding.annoncesHeader.visibility = if (filtered.isNotEmpty()) View.VISIBLE else View.GONE
            binding.adsGrid.visibility = if (filtered.isNotEmpty()) View.VISIBLE else View.GONE
        }

        // Filter discover places
        val discoverHeader = binding.root.findViewById<View>(R.id.discoverHeader)
        val discoverScroll = binding.root.findViewById<View>(R.id.discoverScrollView)
        if (allNearbyPlaces.isNotEmpty()) {
            val filteredPlaces = allNearbyPlaces.filter {
                norm(it.name).contains(q) || norm(it.type).contains(q)
            }
            if (filteredPlaces.isNotEmpty()) {
                populateDiscoverCards(filteredPlaces)
                discoverHeader?.visibility = View.VISIBLE
                discoverScroll?.visibility = View.VISIBLE
            } else {
                discoverHeader?.visibility = View.GONE
                discoverScroll?.visibility = View.GONE
            }
        } else {
            discoverHeader?.visibility = View.GONE
            discoverScroll?.visibility = View.GONE
        }
    }


    private fun loadDiscoverPlaces(filter: String = "tout") {
        val city = requireContext().getSharedPreferences("madinatti_prefs", 0)
            .getString("selected_city", "Casablanca") ?: "Casablanca"

        // ── If city changed, clear the cache so we fetch fresh data ──
        if (city != lastDiscoverCity) {
            allNearbyPlaces.clear()
            lastDiscoverCity = city
            lastDiscoverFilter = ""
        }

        // ── If we already have cached data for this city ──
        if (allNearbyPlaces.isNotEmpty()) {
            val display = if (filter == "tout" || filter == "annonces") {
                allNearbyPlaces.shuffled().take(6)
            } else {
                allNearbyPlaces.filter { it.type == filter }
            }
            populateDiscoverCards(display)
            lastDiscoverFilter = filter
            return
        }

        lastDiscoverFilter = filter

        val coords = if (city == "Toutes les villes") {
            cityCoords.values.toList().random()
        } else {
            cityCoords[city] ?: run {
                // Fallback: try to find a partial match (handles accent differences)
                val normalized = norm(city)
                cityCoords.entries.firstOrNull { norm(it.key) == normalized }?.value
                    ?: Pair(33.57, -7.59)
            }
        }

        val tvLoading = binding.root.findViewById<TextView>(R.id.tvDiscoverLoading)
        val container = binding.root.findViewById<LinearLayout>(R.id.discoverContainer)

        tvLoading?.visibility = View.VISIBLE
        container?.removeAllViews()

        Thread {
            try {
                val typeMap = linkedMapOf(
                    "restaurant" to "amenity=restaurant",
                    "pharmacie" to "amenity=pharmacy",
                    "mosquee" to "amenity=place_of_worship",
                    "cafe" to "amenity=cafe",
                    "hotel" to "tourism=hotel",
                    "hammam" to "amenity=public_bath"
                )

                val queries = if (filter == "tout" || filter == "annonces") {
                    typeMap.entries.map { it.key to it.value }
                } else {
                    val q = typeMap[filter] ?: return@Thread
                    listOf(filter to q)
                }

                val lat = coords.first; val lon = coords.second
                val places = mutableListOf<NearbyPlace>()

                for ((type, osmTag) in queries) {
                    val emoji = when (type) {
                        "restaurant" -> "🍽️"; "pharmacie" -> "💊"; "mosquee" -> "🕌"
                        "cafe" -> "☕"; "hotel" -> "🏨"; "hammam" -> "🛁"; else -> "📍"
                    }
                    try {
                        val limit = if (filter == "tout") 5 else 25
                        val query = "[out:json][timeout:15];node[$osmTag](around:5000,$lat,$lon);out $limit;"
                        val overpassUrl = "https://overpass-api.de/api/interpreter?data=" +
                                java.net.URLEncoder.encode(query, "UTF-8")
                        val conn = java.net.URL(overpassUrl).openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 15000
                        conn.readTimeout = 15000
                        val response = conn.inputStream.bufferedReader().readText()
                        val json = org.json.JSONObject(response)
                        val elements = json.getJSONArray("elements")

                        for (i in 0 until elements.length()) {
                            val el = elements.getJSONObject(i)
                            val tags = el.optJSONObject("tags") ?: continue
                            val name = tags.optString("name", "").ifEmpty {
                                tags.optString("name:fr", "").ifEmpty {
                                    tags.optString("name:ar", "")
                                }
                            }
                            if (name.isEmpty()) continue
                            places.add(NearbyPlace(name, type, emoji, el.getDouble("lat"), el.getDouble("lon")))
                        }
                    } catch (_: Exception) {}
                    Thread.sleep(200)
                }

                allNearbyPlaces.clear()
                allNearbyPlaces.addAll(places)

                activity?.runOnUiThread {
                    if (_binding == null || !isAdded) return@runOnUiThread
                    tvLoading?.visibility = View.GONE
                    val display = if (filter == "tout" || filter == "annonces") {
                        places.shuffled().take(6)
                    } else {
                        places.filter { it.type == filter }
                    }
                    populateDiscoverCards(display)
                }
            } catch (_: Exception) {
                activity?.runOnUiThread {
                    if (_binding == null || !isAdded) return@runOnUiThread
                    tvLoading?.visibility = View.GONE
                    populateDiscoverCards(emptyList())
                }
            }
        }.start()
    }

    private fun populateDiscoverCards(places: List<NearbyPlace>) {
        val container = binding.root.findViewById<LinearLayout>(R.id.discoverContainer) ?: return
        container.removeAllViews()
        val dp = resources.displayMetrics.density

        if (places.isEmpty()) {
            container.addView(TextView(requireContext()).apply {
                text = "Aucun lieu trouvé à proximité"
                setTextColor(android.graphics.Color.parseColor("#7FA68A"))
                textSize = 12f; gravity = android.view.Gravity.CENTER
                setPadding(0, (24 * dp).toInt(), 0, (24 * dp).toInt())
            })
            return
        }

        places.forEach { place ->
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams((140 * dp).toInt(), (130 * dp).toInt()).apply {
                    marginEnd = (8 * dp).toInt()
                }
                setBackgroundResource(R.drawable.bg_restaurant_card)
                isClickable = true; isFocusable = true; elevation = 2 * dp
                setOnClickListener {
                    startActivity(android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=${place.lat},${place.lon}")
                    ))
                }
            }

            val header = android.widget.FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (65 * dp).toInt())
                setBackgroundColor(android.graphics.Color.parseColor("#1E3D2A"))
            }
            header.addView(TextView(requireContext()).apply {
                text = place.emoji; textSize = 32f
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply { gravity = android.view.Gravity.CENTER }
            })
            card.addView(header)

            card.addView(TextView(requireContext()).apply {
                text = place.name; setTextColor(android.graphics.Color.WHITE)
                textSize = 10f; typeface = resources.getFont(R.font.poppins_semibold)
                maxLines = 1; setPadding((10 * dp).toInt(), (6 * dp).toInt(), (10 * dp).toInt(), 0)
            })

            val typeName = when (place.type) {
                "restaurant" -> "Restaurant"; "pharmacie" -> "Pharmacie"
                "mosquee" -> "Mosquée"; "cafe" -> "Café"
                "hotel" -> "Hôtel"; "hammam" -> "Hammam"; else -> place.type
            }
            card.addView(TextView(requireContext()).apply {
                text = typeName; setTextColor(android.graphics.Color.parseColor("#7FA68A"))
                textSize = 9f; typeface = resources.getFont(R.font.poppins_regular)
                setPadding((10 * dp).toInt(), (2 * dp).toInt(), 0, 0)
            })

            card.addView(TextView(requireContext()).apply {
                text = "📍 Voir sur Maps"
                setTextColor(android.graphics.Color.parseColor("#2ECC71"))
                textSize = 8f; typeface = resources.getFont(R.font.poppins_regular)
                setPadding((10 * dp).toInt(), (3 * dp).toInt(), 0, 0)
            })

            container.addView(card)
        }
    }

    private fun showFullDiscoverList() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(
            requireContext(), R.style.GlassBottomSheetDialog
        )
        val dp = resources.displayMetrics.density
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (16 * dp).toInt(), 0, (32 * dp).toInt())
            setBackgroundResource(R.drawable.bg_bottom_sheet_glass)
        }
        root.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams((40 * dp).toInt(), (4 * dp).toInt()).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL; bottomMargin = (16 * dp).toInt()
            }
            setBackgroundResource(R.drawable.bg_drag_handle)
        })
        root.addView(TextView(requireContext()).apply {
            text = "🌍 Tous les lieux"; setTextColor(android.graphics.Color.WHITE)
            textSize = 18f; typeface = resources.getFont(R.font.poppins_bold)
            setPadding((24 * dp).toInt(), 0, (24 * dp).toInt(), (12 * dp).toInt())
        })

        val scroll = android.widget.ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.7).toInt()
            )
        }
        val list = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        allNearbyPlaces.forEach { place ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding((20 * dp).toInt(), (12 * dp).toInt(), (20 * dp).toInt(), (12 * dp).toInt())
                isClickable = true; isFocusable = true
                val ta = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                foreground = ta.getDrawable(0); ta.recycle()
                setOnClickListener {
                    dialog.dismiss()
                    startActivity(android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=${place.lat},${place.lon}")
                    ))
                }
            }
            row.addView(TextView(requireContext()).apply {
                text = place.emoji; textSize = 24f
                layoutParams = LinearLayout.LayoutParams((40 * dp).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            val col = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (12 * dp).toInt()
                }
            }
            col.addView(TextView(requireContext()).apply {
                text = place.name; setTextColor(android.graphics.Color.WHITE)
                textSize = 13f; typeface = resources.getFont(R.font.poppins_semibold)
            })
            val typeName = when (place.type) {
                "restaurant" -> "Restaurant"; "pharmacie" -> "Pharmacie"
                "mosquee" -> "Mosquée"; "cafe" -> "Café"
                "hotel" -> "Hôtel"; "hammam" -> "Hammam"; else -> place.type
            }
            col.addView(TextView(requireContext()).apply {
                text = "$typeName · 📍 Voir sur Maps"
                setTextColor(android.graphics.Color.parseColor("#7FA68A"))
                textSize = 11f; typeface = resources.getFont(R.font.poppins_regular)
            })
            row.addView(col)
            list.addView(row)
        }

        scroll.addView(list); root.addView(scroll)
        dialog.setContentView(root); dialog.show()
    }

    // ══════════════════════════════════════════
    //  RECENT ADS
    // ══════════════════════════════════════════

    private fun loadRecentAds() {
        val city = requireContext().getSharedPreferences("madinatti_prefs", 0)
            .getString("selected_city", null)

        db.collection("ads").get().addOnSuccessListener { result ->
            if (_binding == null || !isAdded) return@addOnSuccessListener

            val cards = listOf(
                Triple(R.id.adTitle1, R.id.adPrice1, R.id.adCard1),
                Triple(R.id.adTitle2, R.id.adPrice2, R.id.adCard2),
                Triple(R.id.adTitle3, R.id.adPrice3, R.id.adCard3),
                Triple(R.id.adTitle4, R.id.adPrice4, R.id.adCard4)
            )
            cards.forEach { binding.root.findViewById<View>(it.third)?.visibility = View.GONE }

            val activeAds = result.documents.filter {
                (it.getString("status") ?: "") == "active"
            }.filter {
                city == null || city == "Toutes les villes" || it.getString("city") == city
            }.sortedByDescending { it.getTimestamp("createdAt")?.toDate()?.time ?: 0 }

            activeAds.take(4).forEachIndexed { i, doc ->
                if (i >= cards.size) return@forEachIndexed
                val (titleId, priceId, cardId) = cards[i]
                val card = binding.root.findViewById<View>(cardId) ?: return@forEachIndexed
                binding.root.findViewById<TextView>(titleId)?.text = doc.getString("title") ?: ""
                val p = doc.getDouble("price")?.toInt() ?: 0
                binding.root.findViewById<TextView>(priceId)?.text = "$p MAD"
                card.visibility = View.VISIBLE

                val imageViewIds = listOf(R.id.adImage1, R.id.adImage2, R.id.adImage3, R.id.adImage4)
                val imgView = binding.root.findViewById<ImageView>(imageViewIds[i])
                val firstImage = (doc.get("imageUrls") as? List<*>)?.firstOrNull()?.toString() ?: ""
                if (firstImage.isNotEmpty() && imgView != null) {
                    Glide.with(this).load(firstImage).placeholder(R.color.surface).centerCrop().into(imgView)
                }

                val cardLayout = card as? LinearLayout
                if (cardLayout != null && cardLayout.childCount > 3) {
                    (cardLayout.getChildAt(3) as? TextView)?.text = doc.getString("userName") ?: ""
                }

                card.setOnClickListener {
                    val imgList = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    AdDetailBottomSheet.newInstance(
                        title = doc.getString("title") ?: "", price = "$p MAD",
                        description = doc.getString("description") ?: "",
                        city = doc.getString("city") ?: "", category = doc.getString("category") ?: "",
                        userName = doc.getString("userName") ?: "", emoji = doc.getString("emoji") ?: "📦",
                        userId = doc.getString("userId") ?: "", adId = doc.id, imageUrls = imgList
                    ).show(parentFragmentManager, "AdDetail")
                }
            }

            if (activeAds.isEmpty()) binding.adsGrid.visibility = View.GONE
            else binding.adsGrid.visibility = View.VISIBLE
        }
    }

    // ══════════════════════════════════════════
    //  SHORTCUT TEXTS
    // ══════════════════════════════════════════

    private fun updateShortcutTexts() {
        val city = requireContext().getSharedPreferences("madinatti_prefs", 0)
            .getString("selected_city", "Casablanca") ?: "Casablanca"

        db.collection("ads").get().addOnSuccessListener { result ->
            if (_binding == null || !isAdded) return@addOnSuccessListener
            val count = result.documents.count {
                it.getString("status") == "active" &&
                        (city == "Toutes les villes" || it.getString("city") == city)
            }
            binding.root.findViewById<TextView>(R.id.tvShortcutMarket)?.text = "$count annonces"
        }

        fetchWeatherSubtext(city)
        fetchNextPrayerSubtext(city)

        db.collection("events").get().addOnSuccessListener { result ->
            if (_binding == null || !isAdded) return@addOnSuccessListener
            val count = result.size()
            binding.root.findViewById<TextView>(R.id.tvShortcutEvents)
                ?.text = if (count > 0) "$count événements" else "Voir événements"
        }
    }

    private fun fetchWeatherSubtext(city: String) {
        val coords = cityCoords[city] ?: Pair(33.57, -7.59)
        Thread {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=${coords.first}&longitude=${coords.second}&current=temperature_2m,weather_code"
                val response = java.net.URL(url).readText()
                val json = org.json.JSONObject(response)
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toInt()
                val code = current.getInt("weather_code")
                val desc = when (code) {
                    0 -> "Ensoleillé"; 1, 2 -> "Nuageux"; 3 -> "Couvert"
                    45, 48 -> "Brouillard"; 51, 53, 55, 61, 63, 65 -> "Pluvieux"
                    71, 73, 75 -> "Neigeux"; 80, 81, 82 -> "Averses"
                    95, 96, 99 -> "Orageux"; else -> "Variable"
                }
                activity?.runOnUiThread {
                    if (_binding == null || !isAdded) return@runOnUiThread
                    binding.root.findViewById<TextView>(R.id.tvShortcutWeather)?.text = "$temp°C · $desc"
                }
            } catch (_: Exception) {
                activity?.runOnUiThread {
                    if (_binding == null || !isAdded) return@runOnUiThread
                    binding.root.findViewById<TextView>(R.id.tvShortcutWeather)?.text = "Voir météo"
                }
            }
        }.start()
    }

    private fun fetchNextPrayerSubtext(city: String) {
        val coords = cityCoords[city] ?: Pair(33.57, -7.59)
        Thread {
            try {
                val today = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                val url = "https://api.aladhan.com/v1/timings/$today?latitude=${coords.first}&longitude=${coords.second}&method=21"
                val response = java.net.URL(url).readText()
                val timings = org.json.JSONObject(response).getJSONObject("data").getJSONObject("timings")
                val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
                val now = java.util.Calendar.getInstance()
                var nextName = ""; var nextTime = ""
                for (p in prayers) {
                    val t = timings.getString(p).replace(Regex("\\s*\\(.*\\)"), "").trim()
                    val parts = t.split(":"); if (parts.size < 2) continue
                    val cal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, parts[0].trim().toInt())
                        set(java.util.Calendar.MINUTE, parts[1].trim().toInt())
                    }
                    if (cal.after(now)) { nextName = p; nextTime = t; break }
                }
                activity?.runOnUiThread {
                    if (_binding == null || !isAdded) return@runOnUiThread
                    binding.root.findViewById<TextView>(R.id.tvShortcutPrayer)
                        ?.text = if (nextName.isNotEmpty()) "$nextName · $nextTime" else "Isha terminé"
                }
            } catch (_: Exception) {
                activity?.runOnUiThread {
                    if (_binding == null || !isAdded) return@runOnUiThread
                    binding.root.findViewById<TextView>(R.id.tvShortcutPrayer)?.text = "Voir horaires"
                }
            }
        }.start()
    }

    // ══════════════════════════════════════════
    //  CHIPS
    // ══════════════════════════════════════════

    private fun setupChips() {
        val chipMap = linkedMapOf(
            binding.chipTout to "tout", binding.chipMarketplace to "annonces",
            binding.chipPharmacie to "pharmacie", binding.chipRestaurant to "restaurant",
            binding.chipHotel to "hotel", binding.chipCafe to "cafe",
            binding.chipHammam to "hammam", binding.chipMosquee to "mosquee"
        )
        chipMap.forEach { (chipView, tag) ->
            chipView.setOnClickListener {
                selectedChip = tag
                updateChipStyles(chipMap)
                updateExploreSection(tag)
                triggerRipple(chipView)
            }
        }
        updateChipStyles(chipMap)
        updateExploreSection("tout")
    }

    private fun updateChipStyles(chipMap: Map<LinearLayout, String>) {
        chipMap.forEach { (chip, tag) ->
            val isSelected = tag == selectedChip
            chip.setBackgroundResource(if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip)
            val label = chip.getChildAt(if (chip.childCount > 1) 1 else 0)
            if (label is TextView) {
                label.setTextColor(
                    if (isSelected) android.graphics.Color.parseColor("#0D1F17")
                    else android.graphics.Color.parseColor("#7FA68A")
                )
            }
        }
    }

    private fun updateExploreSection(filter: String) {
        val showAll = filter == "tout"
        val showAds = showAll || filter == "annonces"
        val showDiscover = showAll || filter in listOf("restaurant", "pharmacie", "mosquee", "cafe", "hotel", "hammam")

        binding.adsGrid.visibility = if (showAds) View.VISIBLE else View.GONE
        binding.annoncesHeader.visibility = if (showAds) View.VISIBLE else View.GONE

        val discoverHeader = binding.root.findViewById<View>(R.id.discoverHeader)
        val discoverScroll = binding.root.findViewById<View>(R.id.discoverScrollView)
        discoverHeader?.visibility = if (showDiscover) View.VISIBLE else View.GONE
        discoverScroll?.visibility = if (showDiscover) View.VISIBLE else View.GONE

        if (showDiscover && filter != "annonces") {
            loadDiscoverPlaces(filter)
        }
    }

    private fun triggerRipple(view: View) {
        (requireActivity() as? MainActivity)?.binding?.particleView?.triggerRippleFromView(view)
    }

    private fun startWeatherPulse() {
        binding.root.findViewById<TextView>(R.id.tvShortcutWeather)?.let {
            ObjectAnimator.ofFloat(it, "alpha", 1f, 0.4f, 1f).apply {
                duration = 2000; repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }.start()
        }
    }

    override fun onDestroyView() {
        refreshHandler.removeCallbacks(discoverRefreshRunnable)
        super.onDestroyView()
        _binding = null
    }
}