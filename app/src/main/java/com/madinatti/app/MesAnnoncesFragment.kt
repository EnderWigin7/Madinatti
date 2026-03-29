    package com.madinatti.app

    import android.os.Bundle
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.PopupMenu
    import android.widget.TextView
    import android.widget.Toast
    import androidx.fragment.app.Fragment
    import androidx.navigation.fragment.findNavController
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.firebase.firestore.FieldValue
    import android.widget.ImageView

    class MesAnnoncesFragment : Fragment() {

        private var adsListener: com.google.firebase.firestore.ListenerRegistration? = null

        private lateinit var rvMyAds: RecyclerView
        private lateinit var adapter: MyAdCardAdapter
        private var allAds = mutableListOf<MyAdItem>()
        private var currentFilter = "all"

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return inflater.inflate(R.layout.fragment_mes_annonces, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val statusBarSpacer = view.findViewById<View>(R.id.statusBarSpacer)
            statusBarSpacer.layoutParams.height = getStatusBarHeight()

            view.findViewById<ImageView>(R.id.ivBack).setOnClickListener {
                findNavController().navigateUp()
            }




            rvMyAds = view.findViewById(R.id.rvMyAds)
            rvMyAds.layoutManager = LinearLayoutManager(requireContext())


            loadMyAds()

            adapter = MyAdCardAdapter(
                allAds.toMutableList(),
                onAdClick = { ad ->
                    val db = FirebaseFirestore.getInstance()
                    db.collection("ads").document(ad.adId).get().addOnSuccessListener { doc ->
                        if (doc == null || !doc.exists()) return@addOnSuccessListener
                        val imgList = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        val p = doc.getDouble("price")?.toInt() ?: 0
                        AdDetailBottomSheet.newInstance(
                            title = doc.getString("title") ?: "",
                            price = "$p MAD",
                            description = doc.getString("description") ?: "",
                            city = doc.getString("city") ?: "",
                            category = doc.getString("category") ?: "",
                            userName = doc.getString("userName") ?: "",
                            emoji = doc.getString("emoji") ?: "📦",
                            userId = doc.getString("userId") ?: "",
                            adId = doc.id,
                            imageUrls = imgList
                        ).show(parentFragmentManager, "AdDetail")
                    }
                },
                onOptionsClick = { ad, anchorView ->
                    showOptionsMenu(ad, anchorView)
                }
            )
            rvMyAds.adapter = adapter


            updateStats(view)


            setupChips(view)
        }

        private fun loadMyAds() {
            val user = FirebaseAuth.getInstance().currentUser ?: return
            val db = FirebaseFirestore.getInstance()

            adsListener = db.collection("ads")
                .whereEqualTo("userId", user.uid)
                .orderBy("createdAt",
                    com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { result, error ->
                    if (error != null || result == null) return@addSnapshotListener
                    if (!isAdded) return@addSnapshotListener

                    allAds.clear()
                    for (doc in result) {
                        allAds.add(MyAdItem(
                            adId     = doc.id,
                            emoji    = doc.getString("emoji")    ?: "📦",
                            title    = doc.getString("title")    ?: "",
                            price    = "${doc.getDouble("price")?.toInt() ?: 0} DH",
                            status   = doc.getString("status")   ?: "active",
                            time     = getTimeAgo(doc.getTimestamp("createdAt")),
                            views    = (doc.getLong("views")     ?: 0).toInt(),
                            messages = 0,
                            category = doc.getString("category") ?: "—"
                        ))
                    }

                    if (view != null && isAdded) {
                        adapter.updateList(filterAds())
                        updateStats(requireView())
                    }
                }
        }

        private fun filterAds(): List<MyAdItem> {
            return when (currentFilter) {
                "active"  -> allAds.filter { it.status == "active" }
                "vendue"  -> allAds.filter { it.status == "vendue" }
                "expiree" -> allAds.filter { it.status == "expiree" }
                else      -> allAds
            }
        }

        private fun getTimeAgo(timestamp: com.google.firebase.Timestamp?): String {
            if (timestamp == null) return ""
            val now = System.currentTimeMillis()
            val diff = now - timestamp.toDate().time
            val minutes = diff / 60000
            val hours = minutes / 60
            val days = hours / 24

            return when {
                minutes < 1 -> "À l'instant"
                minutes < 60 -> "Il y a ${minutes}min"
                hours < 24 -> "Il y a ${hours}h"
                days < 7 -> "Il y a ${days}j"
                else -> "Il y a ${days / 7} sem"
            }
        }


        private fun updateStats(view: View) {
            val total = allAds.size
            val actives = allAds.count { it.status == "active" }
            val vendues = allAds.count { it.status == "vendue" }
            val expirees = allAds.count { it.status == "expiree" }

            view.findViewById<TextView>(R.id.tvStatTotal).text = "$total"
            view.findViewById<TextView>(R.id.tvStatActives).text = "$actives"
            view.findViewById<TextView>(R.id.tvStatVendues).text = "$vendues"
            view.findViewById<TextView>(R.id.tvStatExpirees).text = "$expirees"
        }

        private fun setupChips(view: View) {
            val chipToutes = view.findViewById<TextView>(R.id.chipToutes)
            val chipActives = view.findViewById<TextView>(R.id.chipActives)
            val chipVendues = view.findViewById<TextView>(R.id.chipVendues)
            val chipExpirees = view.findViewById<TextView>(R.id.chipExpirees)
            val chips = listOf(chipToutes, chipActives, chipVendues, chipExpirees)

            fun selectChip(selected: TextView, filter: String) {
                chips.forEach { chip ->
                    if (chip == selected) {
                        chip.setBackgroundResource(R.drawable.bg_chip_selected)
                        chip.setTextColor(0xFF0D1F17.toInt())
                    } else {
                        chip.setBackgroundResource(R.drawable.bg_chip)
                        chip.setTextColor(0xFF7FA68A.toInt())
                    }
                }
                currentFilter = filter
                val filtered = when (filter) {
                    "active" -> allAds.filter { it.status == "active" }
                    "vendue" -> allAds.filter { it.status == "vendue" }
                    "expiree" -> allAds.filter { it.status == "expiree" }
                    else -> allAds
                }
                adapter.updateList(filtered)
            }

            chipToutes.setOnClickListener { selectChip(chipToutes, "all") }
            chipActives.setOnClickListener { selectChip(chipActives, "active") }
            chipVendues.setOnClickListener { selectChip(chipVendues, "vendue") }
            chipExpirees.setOnClickListener { selectChip(chipExpirees, "expiree") }
        }

        private fun showOptionsMenu(ad: MyAdItem, anchor: View) {
            val popup = PopupMenu(requireContext(), anchor)
            popup.menu.add("✏️ Modifier")
            popup.menu.add("🗑 Supprimer")
            if (ad.status == "active") popup.menu.add("✅ Marquer vendue")

            popup.setOnMenuItemClickListener { item ->
                val user = FirebaseAuth.getInstance().currentUser
                    ?: return@setOnMenuItemClickListener true
                val db = FirebaseFirestore.getInstance()

                when (item.title.toString()) {
                    "✏️ Modifier" -> {
                        val bundle = Bundle().apply {
                            putString("adId",         ad.adId)   // ✅ uses adId
                            putString("editTitle",    ad.title)
                            putString("editPrice",    ad.price
                                .replace(" DH","").replace(" MAD",""))
                            putString("editCategory", ad.category)
                        }
                        findNavController().navigate(R.id.postAdFragment, bundle)
                    }

                    "🗑 Supprimer" -> {
                        android.app.AlertDialog.Builder(requireContext())
                            .setTitle("🗑 Supprimer l'annonce")
                            .setMessage("Supprimer \"${ad.title}\"?")
                            .setPositiveButton("Supprimer") { _, _ ->
                                if (ad.adId.isEmpty()) {
                                    Toast.makeText(requireContext(),
                                        "❌ ID annonce manquant",
                                        Toast.LENGTH_SHORT).show()
                                    return@setPositiveButton
                                }
                                db.collection("ads").document(ad.adId)
                                    .delete()
                                    .addOnSuccessListener {
                                        allAds.remove(ad)
                                        adapter.updateList(allAds)
                                        updateStats(requireView())
                                        // Decrement count
                                        db.collection("users").document(user.uid)
                                            .update("adsCount",
                                                FieldValue.increment(-1))
                                        Toast.makeText(requireContext(),
                                            "✅ Annonce supprimée",
                                            Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(),
                                            "❌ ${e.message}",
                                            Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .setNegativeButton("Annuler", null)
                            .show()
                    }

                    "✅ Marquer vendue" -> {
                        if (ad.adId.isEmpty()) return@setOnMenuItemClickListener true
                        db.collection("ads").document(ad.adId)
                            .update("status", "vendue")
                            .addOnSuccessListener {
                                val index = allAds.indexOf(ad)
                                if (index >= 0) {
                                    allAds[index] = ad.copy(status = "vendue")
                                    adapter.updateList(allAds)
                                    updateStats(requireView())
                                    Toast.makeText(requireContext(),
                                        "✅ Marquée vendue", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
                true
            }
            popup.show()
        }

        private fun getStatusBarHeight(): Int {
            val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
        }

        override fun onResume() {
            super.onResume()
            loadMyAds()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            adsListener?.remove()
        }
    }