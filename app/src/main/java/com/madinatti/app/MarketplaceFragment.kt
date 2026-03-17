package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.madinatti.app.databinding.FragmentMarketplaceBinding

class MarketplaceFragment : Fragment() {

    private var _binding: FragmentMarketplaceBinding? = null
    private val binding get() = _binding!!
    private var selectedCategory = "tout"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMarketplaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.marketTopBar) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, statusBar, v.paddingRight, v.paddingBottom)
            insets
        }

        setupCategoryChips()

        binding.marketSearchBar.setOnClickListener { }
        binding.tvSort.setOnClickListener { }
        listOf(binding.marketAdCard1, binding.marketAdCard2,
            binding.marketAdCard3, binding.marketAdCard4).forEach {
            it.setOnClickListener { }
        }
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
                    c.setBackgroundResource(
                        if (t == selectedCategory) R.drawable.bg_chip_selected
                        else R.drawable.bg_chip)
                    val label = c.getChildAt(if (c.childCount > 1) 1 else 0)
                    if (label is TextView) label.setTextColor(
                        if (t == selectedCategory) android.graphics.Color.parseColor("#0D1F17")
                        else android.graphics.Color.parseColor("#7FA68A"))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}