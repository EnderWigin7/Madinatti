package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.madinatti.app.databinding.FragmentVilleBinding

class VilleFragment : Fragment() {

    private var _binding: FragmentVilleBinding? = null
    private val binding get() = _binding!!
    private var currentTab = "marketplace"
    private var cardMap: Map<LinearLayout?, String> = emptyMap()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVilleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        TopBarHelper.onCityChanged = {
            if (isAdded && _binding != null) {
                selectTab(currentTab)
                updateShortcutTexts()
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

        val receivedTab = arguments?.getString("selectedTab")
        currentTab = receivedTab ?: "marketplace"

        setupShortcutCards()
        selectTab(currentTab)
        updateShortcutTexts()
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

    private fun updateShortcutTexts() {
        val city = requireContext().getSharedPreferences("madinatti_prefs", 0)
            .getString("selected_city", "Casablanca") ?: "Casablanca"

        // ── Marketplace count ──
        db.collection("ads").get().addOnSuccessListener { result ->
            if (_binding == null || !isAdded) return@addOnSuccessListener
            val count = result.documents.count {
                it.getString("status") == "active" &&
                        (city == "Toutes les villes" || it.getString("city") == city)
            }
            binding.root.findViewById<TextView>(R.id.tvShortcutMarket)
                ?.text = "$count annonces"
        }

        // ── Weather ──
        fetchWeatherSubtext(city)

        // ── Prayer ──
        fetchNextPrayerSubtext(city)

        // ── Events ──
        db.collection("events").get().addOnSuccessListener { result ->
            if (_binding == null || !isAdded) return@addOnSuccessListener
            val count = result.size()
            binding.root.findViewById<TextView>(R.id.tvShortcutEvents)
                ?.text = if (count > 0) "$count événements" else "Voir événements"
        }
    }

    private fun fetchWeatherSubtext(city: String) {
        val cityCoords = mapOf(
            "Casablanca" to Pair(33.57, -7.59), "Rabat" to Pair(34.02, -6.84),
            "Marrakech" to Pair(31.63, -7.98), "Fès" to Pair(34.03, -5.00),
            "Tanger" to Pair(35.76, -5.83), "Agadir" to Pair(30.43, -9.60),
            "Meknès" to Pair(33.89, -5.55), "Oujda" to Pair(34.68, -1.91),
            "Kénitra" to Pair(34.26, -6.58), "Tétouan" to Pair(35.58, -5.37),
            "Salé" to Pair(34.05, -6.80), "Nador" to Pair(35.17, -2.93),
            "Essaouira" to Pair(31.51, -9.76), "El Jadida" to Pair(33.23, -8.50)
        )
        val coords = cityCoords[city] ?: Pair(33.57, -7.59)
        Thread {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=${coords.first}&longitude=${coords.second}" +
                        "&current=temperature_2m,weather_code"
                val response = java.net.URL(url).readText()
                val json = org.json.JSONObject(response)
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toInt()
                val code = current.getInt("weather_code")
                val desc = when (code) {
                    0 -> "Ensoleillé"; 1, 2 -> "Partiellement nuageux"; 3 -> "Nuageux"
                    45, 48 -> "Brouillard"; 51, 53, 55, 61, 63, 65 -> "Pluvieux"
                    71, 73, 75 -> "Neigeux"; 80, 81, 82 -> "Averses"
                    95, 96, 99 -> "Orageux"; else -> "Variable"
                }
                activity?.runOnUiThread {
                    if (_binding == null || !isAdded) return@runOnUiThread
                    binding.root.findViewById<TextView>(R.id.tvShortcutWeather)
                        ?.text = "$temp°C · $desc"
                }
            } catch (_: Exception) {
                activity?.runOnUiThread {
                    if (_binding == null || !isAdded) return@runOnUiThread
                    binding.root.findViewById<TextView>(R.id.tvShortcutWeather)
                        ?.text = "Voir météo"
                }
            }
        }.start()
    }

    private fun fetchNextPrayerSubtext(city: String) {
        val cityCoords = mapOf(
            "Casablanca" to Pair(33.57, -7.59), "Rabat" to Pair(34.02, -6.84),
            "Marrakech" to Pair(31.63, -7.98), "Fès" to Pair(34.03, -5.00),
            "Tanger" to Pair(35.76, -5.83), "Agadir" to Pair(30.43, -9.60),
            "Meknès" to Pair(33.89, -5.55), "Oujda" to Pair(34.68, -1.91),
            "Kénitra" to Pair(34.26, -6.58), "Tétouan" to Pair(35.58, -5.37),
            "Salé" to Pair(34.05, -6.80), "Nador" to Pair(35.17, -2.93),
            "Essaouira" to Pair(31.51, -9.76), "El Jadida" to Pair(33.23, -8.50)
        )
        val coords = cityCoords[city] ?: Pair(33.57, -7.59)
        Thread {
            try {
                val today = java.text.SimpleDateFormat(
                    "dd-MM-yyyy", java.util.Locale.getDefault()
                ).format(java.util.Date())
                val url = "https://api.aladhan.com/v1/timings/$today?latitude=${coords.first}&longitude=${coords.second}&method=21"
                val response = java.net.URL(url).readText()
                val json = org.json.JSONObject(response)
                val timings = json.getJSONObject("data").getJSONObject("timings")
                val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
                val now = java.util.Calendar.getInstance()
                var nextName = ""; var nextTime = ""
                for (p in prayers) {
                    val t = timings.getString(p).replace(Regex("\\s*\\(.*\\)"), "")
                    val parts = t.trim().split(":")
                    if (parts.size < 2) continue
                    val cal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, parts[0].trim().toInt())
                        set(java.util.Calendar.MINUTE, parts[1].trim().toInt())
                    }
                    if (cal.after(now)) { nextName = p; nextTime = t.trim(); break }
                }
                activity?.runOnUiThread {
                    if (_binding == null || !isAdded) return@runOnUiThread
                    binding.root.findViewById<TextView>(R.id.tvShortcutPrayer)
                        ?.text = if (nextName.isNotEmpty()) "$nextName · $nextTime"
                    else "Isha terminé"
                }
            } catch (_: Exception) {
                activity?.runOnUiThread {
                    if (_binding == null || !isAdded) return@runOnUiThread
                    binding.root.findViewById<TextView>(R.id.tvShortcutPrayer)
                        ?.text = "Voir horaires"
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}