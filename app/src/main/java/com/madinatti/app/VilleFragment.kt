package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.madinatti.app.databinding.FragmentVilleBinding

class VilleFragment : Fragment() {

    private var _binding: FragmentVilleBinding? = null
    private val binding get() = _binding!!
    private var currentTab = "marketplace"
    private var cardMap: Map<LinearLayout?, String> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVilleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        TopBarHelper.setup(
            topBarBinding = binding.topBarInclude,
            showBackButton = false
        )

        // DEBUG: Log what we receive
        val receivedTab = arguments?.getString("selectedTab")
        android.util.Log.d("VILLE", "Received selectedTab = $receivedTab")
        android.util.Log.d("VILLE", "Full arguments = ${arguments?.keySet()?.map { "$it=${arguments?.get(it)}" }}")

        currentTab = receivedTab ?: "marketplace"

        setupShortcutCards()
        selectTab(currentTab)

        binding.topBarInclude.citySelector.setOnClickListener { }
        binding.topBarInclude.ivNotifications.setOnClickListener { }
    }

    private fun setupShortcutCards() {
        cardMap = mapOf(
            binding.root.findViewById<LinearLayout>(R.id.shortcutMarketplace) to "marketplace",
            binding.root.findViewById<LinearLayout>(R.id.shortcutPrieres) to "prieres",
            binding.root.findViewById<LinearLayout>(R.id.shortcutMeteo) to "meteo",
            binding.root.findViewById<LinearLayout>(R.id.shortcutEvenements) to "evenements"
        )

        cardMap.forEach { (card, tab) ->
            card?.setOnClickListener {
                if (tab == currentTab) return@setOnClickListener
                selectTab(tab)
            }
        }
    }

    private fun selectTab(tab: String) {
        currentTab = tab

        val selectedBg = mapOf(
            "marketplace" to R.drawable.bg_card_marketplace_selected,
            "prieres" to R.drawable.bg_card_prieres_selected,
            "meteo" to R.drawable.bg_card_meteo_selected,
            "evenements" to R.drawable.bg_card_evenements_selected
        )
        val normalBg = mapOf(
            "marketplace" to R.drawable.bg_card_marketplace,
            "prieres" to R.drawable.bg_card_prieres,
            "meteo" to R.drawable.bg_card_meteo,
            "evenements" to R.drawable.bg_card_evenements
        )

        cardMap.forEach { (card, t) ->
            card?.setBackgroundResource(
                if (t == currentTab) selectedBg[t]!! else normalBg[t]!!
            )
        }

        showTab(currentTab)
        (activity as? MainActivity)?.updateFabVisibility(currentTab == "marketplace")
    }

    private fun showTab(tab: String) {
        val fragment: Fragment = when (tab) {
            "marketplace" -> MarketplaceFragment()
            "prieres" -> PrieresFragment()
            "meteo" -> MeteoFragment()
            "evenements" -> EvenementsFragment()
            else -> MarketplaceFragment()
        }
        childFragmentManager.beginTransaction()
            .replace(R.id.villeContainer, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}