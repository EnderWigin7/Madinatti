package com.madinatti.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.madinatti.app.databinding.FragmentMarketplaceBinding
import androidx.recyclerview.widget.RecyclerView

class MarketplaceFragment : Fragment() {

    private var _binding: FragmentMarketplaceBinding? = null
    private val binding get() = _binding!!
    private var selectedCategory = "tout"
    private val db = FirebaseFirestore.getInstance()
    private var allAds = mutableListOf<AdItem>()
    private lateinit var adapter: AdCardAdapter
    // Store raw Firestore docs for detail view
    private var adDocs = mutableListOf<Map<String, Any?>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMarketplaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryChips()
        setupAdsGrid()
        loadAds()

        val etSearch = view.findViewById<EditText>(R.id.etMarketSearch)
        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                loadAds(selectedCategory, s?.toString()?.trim() ?: "")
            }
        })

        binding.tvSort.setOnClickListener {
            val options = arrayOf("Plus récent", "Prix ↑", "Prix ↓")
            android.app.AlertDialog.Builder(requireContext())
                .setItems(options) { _, w ->
                    when (w) {
                        1 -> allAds.sortBy { it.price.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0 }
                        2 -> allAds.sortByDescending { it.price.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0 }
                    }
                    adapter.notifyDataSetChanged()
                    binding.tvSort.text = options[w]
                }.show()
        }
    }

    private fun setupAdsGrid() {
        adapter = AdCardAdapter(allAds,
            onAdClick = { ad ->
                val idx = allAds.indexOf(ad)
                val doc = if (idx in adDocs.indices) adDocs[idx] else null
                val imgs = (doc?.get("imageUrls") as? List<*>)
                    ?.mapNotNull { it?.toString() } ?: emptyList()
                val docId = doc?.get("__docId") as? String
                    ?: doc?.get("id") as? String ?: ad.adId

                AdDetailBottomSheet.newInstance(
                    title       = ad.title,
                    price       = ad.price,
                    description = doc?.get("description") as? String ?: "",
                    city        = doc?.get("city")        as? String ?: "",
                    category    = doc?.get("category")    as? String ?: "",
                    userName    = doc?.get("userName")     as? String ?: "",
                    emoji       = ad.emoji,
                    userId      = doc?.get("userId")       as? String ?: "",
                    adId        = docId,
                    imageUrls   = imgs
                ).show(parentFragmentManager, "AdDetail")
            },
            onBookmarkClick = { pos, ad ->
            }
        )
        binding.rvAds.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvAds.adapter = adapter
        val sp = (4 * resources.displayMetrics.density).toInt()
        binding.rvAds.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: android.graphics.Rect, view: View,
                parent: RecyclerView, state: RecyclerView.State
            ) {
                if (parent.getChildAdapterPosition(view) % 2 == 0) outRect.right = sp
                else outRect.left = sp
                outRect.bottom = sp * 2
            }
        })
    }

    private fun loadAds(category: String = "tout", search: String = "") {
        val city = requireContext().getSharedPreferences("madinatti_prefs", 0)
            .getString("selected_city", null)

        db.collection("ads").get().addOnSuccessListener { result ->
            if (_binding == null || !isAdded) return@addOnSuccessListener
            allAds.clear(); adDocs.clear()

            val filtered = result.documents.filter { doc ->
                val status = doc.getString("status") ?: ""
                if (status != "active") return@filter false
                val docCity = doc.getString("city") ?: ""
                val docCat = doc.getString("category") ?: ""
                val docTitle = doc.getString("title") ?: ""

                if (city != null && city != "Toutes les villes" && docCity != city) return@filter false
                if (category != "tout") {
                    val map = mapOf(
                        "maison" to "Maison", "electronique" to "Électronique",
                        "voitures" to "Véhicules", "vetements" to "Vêtements",
                        "immobilier" to "Immobilier", "artisanat" to "Artisanat"
                    )
                    if (docCat != map[category]) return@filter false
                }
                if (search.isNotEmpty() && !norm(docTitle).contains(norm(search))) return@filter false
                true
            }.sortedByDescending {
                it.getTimestamp("createdAt")?.toDate()?.time ?: 0
            }

            for (doc in filtered) {
                val price = doc.getDouble("price")?.toInt() ?: 0
                val imageUrls = doc.get("imageUrls") as? List<*>
                val firstImage = imageUrls?.firstOrNull()?.toString() ?: ""
                val docId = doc.id

                allAds.add(AdItem(
                    emoji = doc.getString("emoji") ?: "📦",
                    title = doc.getString("title") ?: "",
                    location = "📍 ${doc.getString("city") ?: ""}",
                    price = "$price MAD",
                    time = timeAgo(doc.getTimestamp("createdAt")),
                    imageUrl = firstImage,
                    adId = docId
                ))

                val data = (doc.data ?: emptyMap()).toMutableMap()
                data["__docId"] = docId
                adDocs.add(data)
            }

            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("favorites").get()
                    .addOnSuccessListener { favResult ->
                        if (_binding == null || !isAdded) return@addOnSuccessListener
                        val savedIds = favResult.documents.map { it.id }.toSet()
                        allAds.forEachIndexed { i, ad ->
                            ad.bookmarked = savedIds.contains(ad.adId)
                        }
                        adapter.notifyDataSetChanged()
                    }
            }

            adapter.notifyDataSetChanged()
            binding.tvAdCount.text = "${allAds.size} annonce${if (allAds.size != 1) "s" else ""}"
        }
    }

    private fun norm(t: String) = java.text.Normalizer.normalize(t.lowercase(), java.text.Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

    private fun timeAgo(ts: com.google.firebase.Timestamp?): String {
        if (ts == null) return ""; val d = System.currentTimeMillis() - ts.toDate().time; val m = d/60000; val h = m/60; val dy = h/24
        return when { m<1->"Maintenant"; m<60->"Il y a ${m}min"; h<24->"Il y a ${h}h"; dy<7->"Il y a ${dy}j"; else->"Il y a ${dy/7} sem" }
    }

    private fun setupCategoryChips() {
        val chips = linkedMapOf(binding.catTout to "tout", binding.catMaison to "maison", binding.catElectronique to "electronique",
            binding.catVoitures to "voitures", binding.catVetements to "vetements", binding.catImmobilier to "immobilier", binding.catArtisanat to "artisanat")
        chips.forEach { (chip, tag) -> chip.setOnClickListener {
            selectedCategory = tag
            chips.forEach { (c, t) ->
                c.setBackgroundResource(if (t == tag) R.drawable.bg_chip_selected else R.drawable.bg_chip)
                val l = c.getChildAt(if (c.childCount > 1) 1 else 0)
                if (l is TextView) l.setTextColor(if (t == tag) 0xFF0D1F17.toInt() else 0xFF7FA68A.toInt())
            }
            loadAds(tag)
        }}
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}