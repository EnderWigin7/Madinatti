package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.madinatti.app.databinding.FragmentMarketplaceBinding

class MarketplaceFragment : Fragment() {

    private var _binding: FragmentMarketplaceBinding? = null
    private val binding get() = _binding!!
    private var selectedCategory = "tout"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketplaceBinding.inflate(
            inflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View, savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryChips()
        setupAdsGrid()

        binding.marketSearchBar.setOnClickListener { }
        binding.tvSort.setOnClickListener { }
    }

    private fun setupAdsGrid() {
        val ads = listOf(
            AdItem("🪔", "Lampe traditionnelle",
                "📍 Marrakech · Médina", "450 MAD", "Il y a 5h"),
            AdItem("🏺", "Tapis tissé 2×3M",
                "📍 Marrakech · Guéliz", "1 800 MAD", "Il y a 2h"),
            AdItem("📱", "Samsung Galaxy S25",
                "📍 Casablanca · Maarif", "3 200 MAD", "Hier"),
            AdItem("🛋️", "Canapé salon",
                "📍 Rabat · Agdal", "950 MAD", "Hier"),
            AdItem("🎨", "Tableau peinture",
                "📍 Fès · Ville nouvelle", "600 MAD", "Il y a 3h"),
            AdItem("👗", "Caftan traditionnel",
                "📍 Meknès · Centre", "1 200 MAD", "Il y a 1h"),
            AdItem("🏠", "Appartement F3",
                "📍 Tanger · Centre", "450 000 MAD", "Il y a 6h"),
            AdItem("🚗", "Dacia Logan 2019",
                "📍 Agadir · Hay Mohammadi", "85 000 MAD", "Hier")
        )

        val adapter = AdCardAdapter(
            ads = ads,
            onAdClick = {
                val bottomSheet = AdDetailBottomSheet()
                bottomSheet.show(parentFragmentManager, "AdDetail")
            },
            onBookmarkClick = { position, ad ->
                // TODO: save bookmark state
            }
        )

        binding.rvAds.layoutManager = GridLayoutManager(
            requireContext(), 2
        )
        binding.rvAds.adapter = adapter

        // Add spacing between grid items
        val spacing = (4 * resources.displayMetrics.density).toInt()
        binding.rvAds.addItemDecoration(
            object : androidx.recyclerview.widget.RecyclerView
            .ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: androidx.recyclerview.widget.RecyclerView,
                    state: androidx.recyclerview.widget.RecyclerView.State
                ) {
                    val pos = parent.getChildAdapterPosition(view)
                    if (pos % 2 == 0) {
                        outRect.right = spacing
                    } else {
                        outRect.left = spacing
                    }
                    outRect.bottom = spacing * 2
                }
            }
        )
    }

    private fun setupCategoryChips() {
        val chips = linkedMapOf(
            binding.catTout to "tout",
            binding.catMaison to "maison",
            binding.catElectronique to "electronique",
            binding.catVoitures to "voitures",
            binding.catVetements to "vetements",
            binding.catImmobilier to "immobilier",
            binding.catArtisanat to "artisanat"
        )
        chips.forEach { (chip, tag) ->
            chip.setOnClickListener {
                selectedCategory = tag
                chips.forEach { (c, t) ->
                    val isSelected = t == selectedCategory
                    c.setBackgroundResource(
                        if (isSelected) R.drawable.bg_chip_selected
                        else R.drawable.bg_chip
                    )
                    val label = c.getChildAt(
                        if (c.childCount > 1) 1 else 0
                    )
                    if (label is TextView) {
                        label.setTextColor(
                            if (isSelected)
                                android.graphics.Color
                                    .parseColor("#0D1F17")
                            else
                                android.graphics.Color
                                    .parseColor("#7FA68A")
                        )
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}