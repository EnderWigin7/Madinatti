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
import androidx.navigation.Navigation
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


        applyStatusBarSpacer()


        TopBarHelper.setup(
            topBarBinding = binding.topBarInclude,
            showBackButton = false
        )
        binding.topBarInclude.citySelector.setOnClickListener { }
        binding.topBarInclude.ivNotifications.setOnClickListener { }


        val navController = Navigation.findNavController(requireActivity(), R.id.navHostFragment)
        val particleView = (requireActivity() as MainActivity).binding.particleView
        ShortcutCardsHelper.setup(binding.root, navController, "", particleView)

        setupChips()
        startWeatherPulse()

        binding.searchBar.setOnClickListener { }
        binding.tvVoirToutAnnonces.setOnClickListener { }
        binding.tvVoirToutRestaurants.setOnClickListener { }
        binding.restaurantCard1.setOnClickListener { }
        binding.restaurantCard2.setOnClickListener { }
        binding.restaurantCard3.setOnClickListener { }
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

        updateChipStyles(chipMap)
        updateExploreSection("tout")
    }

    private fun updateChipStyles(chipMap: Map<LinearLayout, String>) {
        chipMap.forEach { (chip, tag) ->
            val isSelected = tag == selectedChip
            chip.setBackgroundResource(
                if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip)
            val label = chip.getChildAt(if (chip.childCount > 1) 1 else 0)
            if (label is TextView) {
                label.setTextColor(
                    if (isSelected) android.graphics.Color.parseColor("#0D1F17")
                    else android.graphics.Color.parseColor("#7FA68A"))
            }
        }
    }

    private fun updateExploreSection(filter: String) {
        val showAll = filter == "tout"
        val showAds = showAll || filter == "annonces"
        val showRestaurants = showAll || filter == "restaurant"

        binding.adsGrid.visibility = if (showAds) View.VISIBLE else View.GONE
        binding.annoncesHeader.visibility = if (showAds) View.VISIBLE else View.GONE
        binding.restaurantsScrollView.visibility =
            if (showRestaurants) View.VISIBLE else View.GONE
        binding.restaurantsHeader.visibility =
            if (showRestaurants) View.VISIBLE else View.GONE
    }

    private fun triggerRipple(view: View) {
        (requireActivity() as? MainActivity)?.binding?.particleView
            ?.triggerRippleFromView(view)
    }
    private fun startWeatherPulse() {
        val tvWeather = binding.root.findViewById<TextView>(R.id.tvShortcutWeather)
        tvWeather?.let {
            ObjectAnimator.ofFloat(it, "alpha", 1f, 0.4f, 1f).apply {
                duration = 2000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }.start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyStatusBarSpacer() {
        val statusBarHeight = requireContext()
            .getSharedPreferences("ui_prefs", 0)
            .getInt("status_bar_height", 0)
        binding.topBarInclude.statusBarSpacer
            .layoutParams.height = statusBarHeight
        binding.topBarInclude.statusBarSpacer.requestLayout()
    }
}