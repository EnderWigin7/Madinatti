package com.madinatti.app

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.madinatti.app.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var selectedChip = "tout"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChips()
        startWeatherPulse()

        binding.citySelector.setOnClickListener { /* TODO: city picker */ }
        binding.ivNotifications.setOnClickListener { /* TODO: notifications */ }
        binding.searchBar.setOnClickListener { /* TODO: search */ }

        binding.cardMarketplace.setOnClickListener {
            triggerRipple(binding.cardMarketplace)
            requireActivity().let {
                androidx.navigation.Navigation
                    .findNavController(it, R.id.navHostFragment)
                    .navigate(R.id.marketplaceFragment)
            }
        }
        binding.cardPrieres.setOnClickListener { /* TODO: ville prières */ }
        binding.cardMeteo.setOnClickListener { /* TODO: ville météo */ }
        binding.cardEvenements.setOnClickListener { /* TODO: événements */ }

        binding.tvVoirToutAnnonces.setOnClickListener { /* TODO */ }
        binding.tvVoirToutRestaurants.setOnClickListener { /* TODO */ }

        binding.restaurantCard1.setOnClickListener { /* TODO */ }
        binding.restaurantCard2.setOnClickListener { /* TODO */ }
        binding.restaurantCard3.setOnClickListener { /* TODO */ }

    }

    private fun setupChips() {

        val chipMap = linkedMapOf(
            binding.chipTout to "tout",
            binding.chipMarketplace to "annonces",
            binding.chipPharmacie to "pharmacie",
            binding.chipRestaurant to "restaurant",
            binding.chipHotel to "hotel",
            binding.chipCafe to "cafe",
            binding.chipHammam to "hammam",
            binding.chipMosquee to "mosquee"
        )

        chipMap.forEach { (chipView, tag) ->
            chipView.setOnClickListener {
                selectedChip = tag
                updateChipStyles(chipMap)
                updateExploreSection(tag)
                triggerRipple(chipView)
            }
        }

        // Default state
        updateChipStyles(chipMap)
        updateExploreSection("tout")
    }

    private fun updateChipStyles(chipMap: Map<LinearLayout, String>) {
        chipMap.forEach { (chipView, tag) ->
            val isSelected = tag == selectedChip

            chipView.setBackgroundResource(
                if (isSelected) R.drawable.bg_chip_selected
                else R.drawable.bg_chip
            )

            // Update text color — second child is the label TextView
            if (chipView.childCount > 1) {
                val label = chipView.getChildAt(1)
                if (label is TextView) {
                    label.setTextColor(
                        if (isSelected) android.graphics.Color.parseColor("#0D1F17")
                        else android.graphics.Color.parseColor("#7FA68A")
                    )
                }
            } else {
                // "Tout" chip has only one child
                val label = chipView.getChildAt(0)
                if (label is TextView) {
                    label.setTextColor(
                        if (isSelected) android.graphics.Color.parseColor("#0D1F17")
                        else android.graphics.Color.parseColor("#7FA68A")
                    )
                }
            }
        }
    }

    private fun updateExploreSection(filter: String) {
        val showAll = filter == "tout"
        val showRestaurants = showAll || filter == "restaurant"
        val showHotels = showAll || filter == "hotel"
        val showAds = showAll || filter == "annonces"

        // Ads section
        binding.adsGrid.visibility = if (showAds) View.VISIBLE else View.GONE
        binding.annoncesHeader.visibility = if (showAds) View.VISIBLE else View.GONE

        // Restaurants section
        binding.restaurantsScrollView.visibility = if (showRestaurants) View.VISIBLE else View.GONE
        binding.restaurantsHeader.visibility = if (showRestaurants) View.VISIBLE else View.GONE

        // TODO Phase 2: filter Firestore data
    }

    private fun triggerRipple(view: View) {
        val activity = requireActivity() as? MainActivity
        activity?.binding?.particleView?.triggerRippleFromView(view)
    }

    private fun startWeatherPulse() {
        ObjectAnimator.ofFloat(binding.tvWeatherTemp, "alpha", 1f, 0.4f, 1f).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}