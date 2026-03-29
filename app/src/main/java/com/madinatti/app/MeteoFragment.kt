package com.madinatti.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
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

class MeteoFragment : Fragment() {

    enum class WeatherType(val colors: IntArray, val emoji: String, val label: String) {
        SUNNY(intArrayOf(Color.parseColor("#FF6F00"), Color.parseColor("#1E88E5"), Color.parseColor("#0D47A1")), "☀️", "Ensoleillé"),
        PARTLY_CLOUDY(intArrayOf(Color.parseColor("#EF6C00"), Color.parseColor("#0288D1"), Color.parseColor("#01579B")), "⛅", "Partiellement nuageux"),
        CLOUDY(intArrayOf(Color.parseColor("#78909C"), Color.parseColor("#455A64"), Color.parseColor("#263238")), "☁️", "Nuageux"),
        RAINY(intArrayOf(Color.parseColor("#1565C0"), Color.parseColor("#0D47A1"), Color.parseColor("#1A237E")), "🌧️", "Pluvieux"),
        STORMY(intArrayOf(Color.parseColor("#311B92"), Color.parseColor("#1A237E"), Color.parseColor("#0D1B2A")), "⛈️", "Orageux"),
        FOGGY(intArrayOf(Color.parseColor("#78909C"), Color.parseColor("#546E7A"), Color.parseColor("#37474F")), "🌫️", "Brouillard"),
        NIGHT_CLEAR(intArrayOf(Color.parseColor("#1A237E"), Color.parseColor("#0D1B2A"), Color.parseColor("#000051")), "🌙", "Nuit claire"),
    }

    data class DayForecast(val day: String, val emoji: String, val tempHigh: Int, val tempLow: Int, val rainPercent: Int)
    data class HourForecast(val hour: String, val emoji: String, val temp: Int, val rainPercent: Int)

    private val cityCoords = mapOf(
        "Casablanca" to Pair(33.5731, -7.5898), "Rabat" to Pair(34.0209, -6.8416),
        "Marrakech" to Pair(31.6295, -7.9811), "Fès" to Pair(34.0331, -5.0003),
        "Tanger" to Pair(35.7595, -5.8340), "Agadir" to Pair(30.4278, -9.5981),
        "Meknès" to Pair(33.8935, -5.5473), "Oujda" to Pair(34.6814, -1.9086),
        "Kénitra" to Pair(34.2610, -6.5802), "Tétouan" to Pair(35.5785, -5.3684),
        "Salé" to Pair(34.0531, -6.7986), "Nador" to Pair(35.1681, -2.9335),
        "Mohammedia" to Pair(33.6861, -7.3828), "Béni Mellal" to Pair(32.3373, -6.3498),
        "El Jadida" to Pair(33.2316, -8.5007), "Safi" to Pair(32.2994, -9.2372),
        "Settat" to Pair(33.0011, -7.6164), "Taza" to Pair(34.2133, -4.0103),
        "Khemisset" to Pair(33.8240, -6.0664), "Errachidia" to Pair(31.9314, -4.4288),
        "Al Hoceima" to Pair(35.2517, -3.9372), "Azrou" to Pair(33.4342, -5.2216),
        "Berrechid" to Pair(33.2651, -7.5876), "Chefchaouen" to Pair(35.1688, -5.2636),
        "Essaouira" to Pair(31.5085, -9.7595), "Guelmim" to Pair(28.9833, -10.0572),
        "Ifrane" to Pair(33.5228, -5.1108), "Khouribga" to Pair(32.8811, -6.9063),
        "Laâyoune" to Pair(27.1536, -13.2034), "Larache" to Pair(35.1932, -6.1556),
        "Ouarzazate" to Pair(30.9178, -6.8936), "Sidi Kacem" to Pair(34.2260, -5.7085),
        "Témara" to Pair(33.9275, -6.9076), "Tiznit" to Pair(29.6974, -9.8022)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_meteo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        _view = view  
        loadRealWeather(view)
    }

    private fun loadRealWeather(view: View) {
        val city = requireContext().getSharedPreferences("madinatti_prefs", 0)
            .getString("selected_city", "Casablanca") ?: "Casablanca"

        val coords = cityCoords[city] ?: Pair(33.5731, -7.5898)


        Thread {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=${coords.first}&longitude=${coords.second}" +
                        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,weather_code" +
                        "&hourly=temperature_2m,weather_code,precipitation_probability" +
                        "&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max" +
                        "&timezone=Africa%2FCasablanca&forecast_days=7"

                val response = URL(url).readText()
                val json = JSONObject(response)

                // Current
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toInt()
                val feelsLike = current.getDouble("apparent_temperature").toInt()
                val humidity = current.getInt("relative_humidity_2m")
                val wind = current.getDouble("wind_speed_10m").toInt()
                val weatherCode = current.getInt("weather_code")

                val weatherType = codeToWeatherType(weatherCode)

                // Hourly (next 9 hours)
                val hourly = json.getJSONObject("hourly")
                val hourlyTemps = hourly.getJSONArray("temperature_2m")
                val hourlyCodes = hourly.getJSONArray("weather_code")
                val hourlyRain = hourly.getJSONArray("precipitation_probability")

                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val hourForecasts = mutableListOf<HourForecast>()
                for (i in 0 until 9) {
                    val idx = currentHour + i
                    if (idx >= hourlyTemps.length()) break
                    val h = if (i == 0) "Now" else "${(currentHour + i) % 24}h"
                    hourForecasts.add(HourForecast(
                        h,
                        codeToEmoji(hourlyCodes.getInt(idx)),
                        hourlyTemps.getDouble(idx).toInt(),
                        hourlyRain.getInt(idx)
                    ))
                }

                val daily = json.getJSONObject("daily")
                val dailyMax = daily.getJSONArray("temperature_2m_max")
                val dailyMin = daily.getJSONArray("temperature_2m_min")
                val dailyCodes = daily.getJSONArray("weather_code")
                val dailyRain = daily.getJSONArray("precipitation_probability_max")

                val dayNames = listOf("Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam")
                val cal = Calendar.getInstance()
                val dayForecasts = mutableListOf<DayForecast>()
                for (i in 0 until dailyMax.length()) {
                    val dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - 1 + i) % 7
                    val dayName = if (i == 0) "Auj." else dayNames[dayOfWeek]
                    dayForecasts.add(DayForecast(
                        dayName,
                        codeToEmoji(dailyCodes.getInt(i)),
                        dailyMax.getDouble(i).toInt(),
                        dailyMin.getDouble(i).toInt(),
                        dailyRain.getInt(i)
                    ))
                }

                activity?.runOnUiThread {
                    if (!isAdded || _view == null) return@runOnUiThread
                    populateMain(view, weatherType, temp, feelsLike, humidity, wind)
                    populateHourlyReal(view, hourForecasts)
                    populateForecastListReal(view, dayForecasts)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    if (isAdded) {
                        val weather = WeatherType.PARTLY_CLOUDY
                        populateMain(view, weather, 31, 29, 45, 12)
                        populateHourly(view)
                        populateForecastList(view)
                    }
                }
            }
        }.start()
    }

    private var _view: View? = null

    override fun onDestroyView() {
        super.onDestroyView()
        _view = null
    }

    private fun codeToWeatherType(code: Int): WeatherType = when (code) {
        0 -> WeatherType.SUNNY
        1, 2 -> WeatherType.PARTLY_CLOUDY
        3 -> WeatherType.CLOUDY
        45, 48 -> WeatherType.FOGGY
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> WeatherType.RAINY
        95, 96, 99 -> WeatherType.STORMY
        71, 73, 75 -> WeatherType.CLOUDY
        else -> WeatherType.PARTLY_CLOUDY
    }

    private fun codeToEmoji(code: Int): String = when (code) {
        0 -> "☀️"; 1, 2 -> "⛅"; 3 -> "☁️"
        45, 48 -> "🌫️"; 51, 53, 55, 61, 63, 65 -> "🌧️"
        71, 73, 75 -> "🌨️"; 80, 81, 82 -> "🌦️"
        95, 96, 99 -> "⛈️"; else -> "🌤️"
    }


    private fun populateMain(view: View, type: WeatherType, temp: Int, feelsLike: Int, humidity: Int, wind: Int) {
        view.findViewById<TextView>(R.id.tvWeatherEmoji)?.text = type.emoji
        view.findViewById<TextView>(R.id.tvTemperature)?.text = "${temp}°C"
        view.findViewById<TextView>(R.id.tvCondition)?.text = type.label
        view.findViewById<TextView>(R.id.tvFeelsLike)?.text = getString(R.string.weather_feels_like, "${feelsLike}°C")
        view.findViewById<TextView>(R.id.tvHumidity)?.text = "${humidity}%"
        view.findViewById<TextView>(R.id.tvWind)?.text = "${wind} km/h"

        applyGradient(view, type)
    }

    private fun populateHourlyReal(view: View, hours: List<HourForecast>) {
        val row = view.findViewById<LinearLayout>(R.id.hourlyRow)
        row.removeAllViews()
        hours.forEach { row.addView(makeHourlyItem(it)) }
    }

    private fun populateForecastListReal(view: View, forecasts: List<DayForecast>) {
        val list = view.findViewById<LinearLayout>(R.id.forecastList)
        list.removeAllViews()
        forecasts.forEachIndexed { i, fc ->
            list.addView(makeForecastRow(fc))
            if (i < forecasts.size - 1) list.addView(makeDivider())
        }
    }

    private fun populateHourly(view: View) {
        val hours = listOf(
            HourForecast("Now", "⛅", 31, 3), HourForecast("14h", "☀️", 33, 2),
            HourForecast("15h", "☀️", 32, 2), HourForecast("16h", "⛅", 30, 5),
            HourForecast("17h", "⛅", 28, 8), HourForecast("18h", "🌇", 26, 4),
            HourForecast("19h", "🌙", 23, 3), HourForecast("20h", "🌙", 21, 5),
            HourForecast("21h", "🌙", 19, 6)
        )
        val row = view.findViewById<LinearLayout>(R.id.hourlyRow)
        row.removeAllViews()
        hours.forEach { row.addView(makeHourlyItem(it)) }
    }

    private fun makeHourlyItem(hf: HourForecast): LinearLayout {
        val d = resources.displayMetrics.density
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((6 * d).toInt(), (8 * d).toInt(), (6 * d).toInt(), (8 * d).toInt())
            layoutParams = LinearLayout.LayoutParams((46 * d).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = (5 * d).toInt() }
            setBackgroundResource(R.drawable.bg_hourly_item)

            addView(TextView(context).apply {
                text = hf.hour
                setTextColor(if (hf.hour == "Now") Color.parseColor("#2ECC71") else Color.WHITE)
                textSize = 10f
                typeface = resources.getFont(if (hf.hour == "Now") R.font.poppins_semibold else R.font.poppins_regular)
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply { text = hf.emoji; textSize = 16f; gravity = Gravity.CENTER; setPadding(0, (3 * d).toInt(), 0, (3 * d).toInt()) })
            addView(TextView(context).apply { text = "${hf.temp}°"; setTextColor(Color.WHITE); textSize = 13f; typeface = resources.getFont(R.font.poppins_semibold); gravity = Gravity.CENTER })
            if (hf.rainPercent > 0) {
                addView(TextView(context).apply { text = "💧${hf.rainPercent}%"; setTextColor(Color.parseColor("#77FFFFFF")); textSize = 7f; typeface = resources.getFont(R.font.poppins_light); gravity = Gravity.CENTER; setPadding(0, (2 * d).toInt(), 0, 0) })
            }
        }
    }

    private fun populateForecastList(view: View) {
        val forecasts = listOf(
            DayForecast("Lundi", "☀️", 33, 18, 3), DayForecast("Mardi", "⛅", 29, 16, 15),
            DayForecast("Mercredi", "🌧️", 24, 14, 65), DayForecast("Jeudi", "☀️", 34, 19, 2),
            DayForecast("Vendredi", "☀️", 33, 17, 5), DayForecast("Samedi", "⛅", 30, 16, 12),
            DayForecast("Dimanche", "☀️", 32, 18, 4)
        )
        val list = view.findViewById<LinearLayout>(R.id.forecastList)
        list.removeAllViews()
        forecasts.forEachIndexed { i, fc ->
            list.addView(makeForecastRow(fc))
            if (i < forecasts.size - 1) list.addView(makeDivider())
        }
    }

    private fun makeForecastRow(fc: DayForecast): LinearLayout {
        val d = resources.displayMetrics.density
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * d).toInt(), (8 * d).toInt(), (14 * d).toInt(), (8 * d).toInt())
            addView(TextView(context).apply { text = fc.day; setTextColor(Color.WHITE); textSize = 12f; typeface = resources.getFont(R.font.poppins_regular); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f) })
            addView(TextView(context).apply { text = if (fc.rainPercent > 5) "💧${fc.rainPercent}%" else ""; setTextColor(Color.parseColor("#88FFFFFF")); textSize = 9f; typeface = resources.getFont(R.font.poppins_light); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f) })
            addView(TextView(context).apply { text = fc.emoji; textSize = 18f; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f) })
            addView(TextView(context).apply { text = "${fc.tempLow}°"; setTextColor(Color.parseColor("#88FFFFFF")); textSize = 12f; typeface = resources.getFont(R.font.poppins_regular); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.35f) })
            addView(makeTempBar(fc.tempLow, fc.tempHigh))
            addView(TextView(context).apply { text = "${fc.tempHigh}°"; setTextColor(Color.WHITE); textSize = 12f; typeface = resources.getFont(R.font.poppins_semibold); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.35f) })
        }
    }

    private fun makeTempBar(low: Int, high: Int): View {
        val d = resources.displayMetrics.density
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, (4 * d).toInt(), 1f).apply { marginStart = (4 * d).toInt(); marginEnd = (4 * d).toInt() }
            val startColor = when { high > 30 -> Color.parseColor("#FF6D00"); high > 25 -> Color.parseColor("#FFB300"); high > 20 -> Color.parseColor("#2ECC71"); else -> Color.parseColor("#29B6F6") }
            val endColor = when { low > 20 -> Color.parseColor("#FFB300"); low > 15 -> Color.parseColor("#66BB6A"); else -> Color.parseColor("#29B6F6") }
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(endColor, startColor)).apply { cornerRadius = 4 * d }
        }
    }

    private fun makeDivider(): View {
        val d = resources.displayMetrics.density
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { marginStart = (14 * d).toInt(); marginEnd = (14 * d).toInt() }
            setBackgroundColor(Color.parseColor("#15FFFFFF"))
        }
    }

    private fun applyGradient(view: View, type: WeatherType) {
        val mainCard = view.findViewById<View>(R.id.mainTempCard)
        val gradDrawable = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            type.colors
        ).apply {
            cornerRadius = 18f * resources.displayMetrics.density
        }
        mainCard?.background = gradDrawable

        val forecastCard = view.findViewById<View>(R.id.forecastList)
        val forecastGrad = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                adjustAlpha(type.colors[0], 0.4f),
                adjustAlpha(type.colors[2], 0.4f)
            )
        ).apply {
            cornerRadius = 14f * resources.displayMetrics.density
        }
        forecastCard?.background = forecastGrad
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(android.graphics.Color.alpha(color) * factor)
        val red   = android.graphics.Color.red(color)
        val green = android.graphics.Color.green(color)
        val blue  = android.graphics.Color.blue(color)
        return android.graphics.Color.argb(alpha.coerceIn(0, 255), red, green, blue)
    }

}