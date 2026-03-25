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

class MesAnnoncesFragment : Fragment() {

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

        // Status bar spacer
        val statusBarSpacer = view.findViewById<View>(R.id.statusBarSpacer)
        statusBarSpacer.layoutParams.height = getStatusBarHeight()

        // Back button — use NavController
        view.findViewById<TextView>(R.id.ivBack).setOnClickListener {
            findNavController().navigateUp()
        }


        view.findViewById<TextView>(R.id.tvNewAd).setOnClickListener {
            findNavController().navigate(R.id.postAdFragment)
        }


        rvMyAds = view.findViewById(R.id.rvMyAds)
        rvMyAds.layoutManager = LinearLayoutManager(requireContext())


        loadMyAds()

        adapter = MyAdCardAdapter(
            allAds.toMutableList(),
            onAdClick = { ad ->
                Toast.makeText(requireContext(), "Ad: ${ad.title}", Toast.LENGTH_SHORT).show()
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
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        db.collection("ads")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { result ->
                allAds.clear()
                for (doc in result) {
                    val ad = MyAdItem(
                        emoji = doc.getString("emoji") ?: "📦",
                        title = doc.getString("title") ?: "",
                        price = "${doc.getDouble("price")?.toInt() ?: 0} DH",
                        status = doc.getString("status") ?: "active",
                        time = getTimeAgo(doc.getTimestamp("createdAt")),
                        views = (doc.getLong("views") ?: 0).toInt(),
                        messages = 0,
                        category = doc.getString("category") ?: "—"
                    )
                    allAds.add(ad)
                }

                // Safe check — fragment might be detached
                if (view != null && isAdded) {
                    adapter.updateList(allAds)
                    updateStats(requireView())
                }
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
        if (ad.status == "active") {
            popup.menu.add("✅ Marquer vendue")
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "✏️ Modifier" -> {
                    Toast.makeText(requireContext(), "Modifier: ${ad.title}", Toast.LENGTH_SHORT).show()
                }
                "🗑 Supprimer" -> {
                    allAds.remove(ad)
                    adapter.updateList(allAds)
                    updateStats(requireView())
                    Toast.makeText(requireContext(), "Annonce supprimée", Toast.LENGTH_SHORT).show()
                }
                "✅ Marquer vendue" -> {
                    val index = allAds.indexOf(ad)
                    if (index >= 0) {
                        allAds[index] = ad.copy(status = "vendue")
                        adapter.updateList(allAds)
                        updateStats(requireView())
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
}