package com.madinatti.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class PrieresFragment : Fragment() {

    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_prieres, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<LinearLayout>(R.id.qiblaCard)?.setOnClickListener {
            androidx.navigation.Navigation
                .findNavController(requireActivity(), R.id.navHostFragment)
                .navigate(R.id.qiblaFragment)
        }

        loadPrayerTimes(view)
    }

    private fun loadPrayerTimes(view: View) {
        val city = requireContext().getSharedPreferences("madinatti_prefs", 0)
            .getString("selected_city", "Casablanca") ?: "Casablanca"
        val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        Thread {
            try {
                val url = "https://api.aladhan.com/v1/timingsByCity/$today" +
                        "?city=$city&country=Morocco&method=21"
                val response = URL(url).readText()
                val json = JSONObject(response)
                val data = json.getJSONObject("data")
                val timings = data.getJSONObject("timings")

                val fajr    = timings.getString("Fajr")
                val sunrise = timings.getString("Sunrise")
                val dhuhr   = timings.getString("Dhuhr")
                val asr     = timings.getString("Asr")
                val maghrib = timings.getString("Maghrib")
                val isha    = timings.getString("Isha")

                val hijri = data.getJSONObject("date").getJSONObject("hijri")
                val hijriStr = "${hijri.getString("day")} " +
                        "${hijri.getJSONObject("month").getString("en")} " +
                        "${hijri.getString("year")}"
                val gregDate = SimpleDateFormat(
                    "EEEE d MMMM yyyy", Locale.FRANCE).format(Date())

                activity?.runOnUiThread {
                    if (!isAdded || view == null) return@runOnUiThread
                    view.findViewById<TextView>(R.id.tvFajr)?.text    = fajr
                    view.findViewById<TextView>(R.id.tvSunrise)?.text = sunrise
                    view.findViewById<TextView>(R.id.tvDhuhr)?.text   = dhuhr
                    view.findViewById<TextView>(R.id.tvAsr)?.text     = asr
                    view.findViewById<TextView>(R.id.tvMaghrib)?.text = maghrib
                    view.findViewById<TextView>(R.id.tvIsha)?.text    = isha
                    view.findViewById<TextView>(R.id.tvDateHijri)?.text  = gregDate
                    view.findViewById<TextView>(R.id.tvHijriSub)?.text   = hijriStr

                    setupNextPrayer(view, fajr, sunrise, dhuhr, asr, maghrib, isha)
                    highlightActivePrayer(view, fajr, sunrise, dhuhr, asr, maghrib, isha)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    if (isAdded) Toast.makeText(
                        requireContext(),
                        "❌ Erreur prières: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    // ✅ Single helper - parses "HH:mm" into a Calendar for TODAY
    private fun parseTime(timeStr: String): Calendar {
        val parts = timeStr.trim().split(":")
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            set(Calendar.MINUTE,      parts[1].toInt())
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun setupNextPrayer(
        view: View, fajr: String, sunrise: String,
        dhuhr: String, asr: String, maghrib: String, isha: String
    ) {
        val prayers = listOf(
            "Fajr" to fajr, "Lever" to sunrise, "Dhuhr" to dhuhr,
            "Asr"  to asr,  "Maghrib" to maghrib, "Isha" to isha
        )
        val now = Calendar.getInstance()
        var next = prayers.firstOrNull { parseTime(it.second).after(now) }
            ?: prayers[0]   // wrap to Fajr tomorrow

        view.findViewById<TextView>(R.id.tvNextPrayerName)?.text = next.first
        startCountdown(view, next.second)
    }

    // ✅ Only ONE startCountdown
    private fun startCountdown(view: View, targetTime: String) {
        countdownRunnable?.let { handler.removeCallbacks(it) }

        countdownRunnable = object : Runnable {
            override fun run() {
                if (!isAdded) return
                try {
                    val target = parseTime(targetTime)
                    val now    = Calendar.getInstance()
                    if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)

                    val diff    = target.timeInMillis - now.timeInMillis
                    val hours   = diff / 3_600_000
                    val minutes = (diff % 3_600_000) / 60_000
                    val seconds = (diff % 60_000)    / 1_000

                    view.findViewById<TextView>(R.id.tvNextPrayerCountdown)
                        ?.text = String.format("-%dh %02dmin %02ds", hours, minutes, seconds)

                    handler.postDelayed(this, 1_000)
                } catch (_: Exception) {}
            }
        }
        handler.post(countdownRunnable!!)
    }

    private fun highlightActivePrayer(
        view: View, fajr: String, sunrise: String,
        dhuhr: String, asr: String, maghrib: String, isha: String
    ) {
        val rowIds  = listOf(R.id.rowFajr, R.id.rowSunrise, R.id.rowDhuhr,
            R.id.rowAsr,  R.id.rowMaghrib, R.id.rowIsha)
        val timeIds = listOf(R.id.tvFajr,  R.id.tvSunrise,  R.id.tvDhuhr,
            R.id.tvAsr,   R.id.tvMaghrib,  R.id.tvIsha)
        val times   = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)

        val now = Calendar.getInstance()

        // The LAST prayer whose time has already passed = currently active
        var activeIndex = -1
        times.forEachIndexed { i, t ->
            if (parseTime(t).before(now)) activeIndex = i
        }

        rowIds.forEachIndexed { i, rowId ->
            val row    = view.findViewById<LinearLayout>(rowId) ?: return@forEachIndexed
            val timeTv = view.findViewById<TextView>(timeIds[i])
            val nameTv = row.getChildAt(0) as? TextView

            if (i == activeIndex) {
                row.setBackgroundResource(R.drawable.bg_prayer_row_active)
                row.elevation = 2f * resources.displayMetrics.density
                nameTv?.setTextColor(android.graphics.Color.WHITE)
                nameTv?.typeface = resources.getFont(R.font.poppins_semibold)
                timeTv?.setTextColor(android.graphics.Color.parseColor("#2ECC71"))
                timeTv?.typeface = resources.getFont(R.font.poppins_semibold)
            } else {
                row.setBackgroundResource(R.drawable.bg_prayer_row)
                row.elevation = 0f
                nameTv?.setTextColor(android.graphics.Color.parseColor("#7FA68A"))
                nameTv?.typeface = resources.getFont(R.font.poppins_regular)
                timeTv?.setTextColor(android.graphics.Color.WHITE)
                timeTv?.typeface = resources.getFont(R.font.poppins_regular)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownRunnable?.let { handler.removeCallbacks(it) }
    }
}