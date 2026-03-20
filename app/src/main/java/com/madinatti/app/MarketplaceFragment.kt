package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.madinatti.app.databinding.FragmentMarketplaceBinding

class MarketplaceFragment : Fragment() {

    private var _binding: FragmentMarketplaceBinding? = null
    private val binding get() = _binding!!
    private var selectedCategory = "tout"
    private val bookmarkStates = mutableMapOf<Int, Boolean>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketplaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryChips()
        setupBookmarks()
        setupAdClicks()

        binding.marketSearchBar.setOnClickListener {
            // TODO: Open search
        }

        binding.tvSort.setOnClickListener {
            // TODO: Sort options
        }
    }

    private fun setupAdClicks() {
        val showAdDetail = {
            val bottomSheet = AdDetailBottomSheet()
            bottomSheet.show(parentFragmentManager, "AdDetail")
        }

        binding.marketAdCard1.setOnClickListener { showAdDetail() }
        binding.marketAdCard2.setOnClickListener { showAdDetail() }
        binding.marketAdCard3.setOnClickListener { showAdDetail() }
        binding.marketAdCard4.setOnClickListener { showAdDetail() }
    }

    private fun setupBookmarks() {
        val bookmarks = listOf(
            binding.ivBookmark1,
            binding.ivBookmark2,
            binding.ivBookmark3,
            binding.ivBookmark4
        )
        bookmarks.forEachIndexed { index, imageView ->
            bookmarkStates[index] = false
            imageView.setOnClickListener {
                val current = bookmarkStates[index] ?: false
                bookmarkStates[index] = !current
                imageView.setImageResource(
                    if (!current) R.drawable.ic_bookmark_filled
                    else R.drawable.ic_bookmark_outline
                )
                imageView.animate()
                    .scaleX(1.3f).scaleY(1.3f).setDuration(100)
                    .withEndAction {
                        imageView.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(100).start()
                    }.start()
            }
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
                                android.graphics.Color.parseColor("#0D1F17")
                            else
                                android.graphics.Color.parseColor("#7FA68A")
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