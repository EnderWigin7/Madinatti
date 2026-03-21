package com.madinatti.app

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.ShapeDrawable.ShaderFactory
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class MeteoFragment : Fragment() {

    // Weather condition enum for dynamic theming
    enum class WeatherType(
        val colorStart: String,
        val colorEnd: String,
        val emoji: String,
        val label: String
    ) {
        SUNNY("#3498DB", "#E67E22", "☀️", "Ensoleillé"),
        PARTLY_CLOUDY("#35819A", "#9C5E24", "⛅", "Partiellement nuageux"),
        CLOUDY("#546E7A", "#37474F", "☁️", "Nuageux"),
        RAINY("#1A237E", "#37474F", "🌧️", "Pluvieux"),
        STORMY("#1A1A2E", "#16213E", "⛈️", "Orageux"),
        NIGHT_CLEAR("#0D1B2A", "#1B2838", "🌙", "Nuit claire"),
        SNOWY("#B0BEC5", "#78909C", "❄️", "Neigeux"),
    }

    data class DayForecast(
        val day: String,
        val emoji: String,
        val tempHigh: Int,
        val rainPercent: Int
    )

    data class HourForecast(
        val hour: String,
        val emoji: String,
        val temp: Int,
        val rainPercent: Int
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_meteo, container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: Replace with real API data in Phase 2
        val weather = WeatherType.PARTLY_CLOUDY
        val temp = 31
        val feelsLike = 29
        val humidity = 45
        val wind = 12

        applyGradient(view, weather)
        populateMain(view, weather, temp, feelsLike, humidity, wind)
        populateForecast(view)
        populateHourly(view)
    }

    private fun applyGradient(view: View, type: WeatherType) {
        val bg = view.findViewById<View>(R.id.weatherGradientBg)
        val drawable = GradientDrawable(
            GradientDrawable.Orientation.TR_BL,
            intArrayOf(
                Color.parseColor(type.colorStart),
                Color.parseColor(type.colorEnd)
            )
        )
        bg.background = drawable
    }

    private fun populateMain(
        view: View,
        type: WeatherType,
        temp: Int,
        feelsLike: Int,
        humidity: Int,
        wind: Int
    ) {
        view.findViewById<TextView>(R.id.tvWeatherEmoji)?.text =
            type.emoji
        view.findViewById<TextView>(R.id.tvTemperature)?.text =
            "${temp}°C"
        view.findViewById<TextView>(R.id.tvCondition)?.text =
            type.label
        view.findViewById<TextView>(R.id.tvFeelsLike)?.text =
            getString(R.string.weather_feels_like, "${feelsLike}°C")
        view.findViewById<TextView>(R.id.tvHumidity)?.text =
            "${humidity}%"
        view.findViewById<TextView>(R.id.tvWind)?.text =
            "${wind} km/h"
    }

    private fun populateForecast(view: View) {
        val forecasts = listOf(
            DayForecast("Lun", "☀️", 33, 3),
            DayForecast("Mar", "⛅", 29, 15),
            DayForecast("Mer", "🌧️", 24, 65),
            DayForecast("Jeu", "☀️", 34, 2),
            DayForecast("Ven", "☀️", 33, 5),
            DayForecast("Sam", "⛅", 30, 12),
            DayForecast("Dim", "☀️", 32, 4)
        )

        val row = view.findViewById<LinearLayout>(R.id.forecastRow)
        row.removeAllViews()

        forecasts.forEach { fc ->
            row.addView(makeForecastItem(fc))
        }
    }

    private fun makeForecastItem(fc: DayForecast): LinearLayout {
        val d = resources.displayMetrics.density
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                (10 * d).toInt(), (10 * d).toInt(),
                (10 * d).toInt(), (10 * d).toInt()
            )
            layoutParams = LinearLayout.LayoutParams(
                (52 * d).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (6 * d).toInt() }
            setBackgroundResource(R.drawable.bg_hourly_item)

            // Day
            addView(TextView(context).apply {
                text = fc.day
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = resources.getFont(R.font.poppins_medium)
                gravity = Gravity.CENTER
            })

            // Emoji
            addView(TextView(context).apply {
                text = fc.emoji
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, (4 * d).toInt(), 0, (4 * d).toInt())
            })

            // Temp
            addView(TextView(context).apply {
                text = "${fc.tempHigh}°"
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = resources.getFont(R.font.poppins_semibold)
                gravity = Gravity.CENTER
            })

            // Rain %
            if (fc.rainPercent > 0) {
                addView(TextView(context).apply {
                    text = "💧${fc.rainPercent}%"
                    setTextColor(Color.parseColor("#88FFFFFF"))
                    textSize = 8f
                    typeface = resources.getFont(R.font.poppins_light)
                    gravity = Gravity.CENTER
                    setPadding(0, (2 * d).toInt(), 0, 0)
                })
            }
        }
    }

    private fun populateHourly(view: View) {
        val hours = listOf(
            HourForecast("Now", "⛅", 31, 3),
            HourForecast("14h", "☀️", 33, 2),
            HourForecast("15h", "☀️", 32, 2),
            HourForecast("16h", "⛅", 30, 5),
            HourForecast("17h", "⛅", 28, 8),
            HourForecast("18h", "🌇", 26, 4),
            HourForecast("19h", "🌙", 23, 3),
            HourForecast("20h", "🌙", 21, 5),
            HourForecast("21h", "🌙", 19, 6)
        )

        val row = view.findViewById<LinearLayout>(R.id.hourlyRow)
        row.removeAllViews()

        hours.forEach { hf ->
            row.addView(makeHourlyItem(hf))
        }
    }

    private fun makeHourlyItem(hf: HourForecast): LinearLayout {
        val d = resources.displayMetrics.density
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                (8 * d).toInt(), (10 * d).toInt(),
                (8 * d).toInt(), (10 * d).toInt()
            )
            layoutParams = LinearLayout.LayoutParams(
                (48 * d).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (6 * d).toInt() }
            setBackgroundResource(R.drawable.bg_hourly_item)

            // Hour
            addView(TextView(context).apply {
                text = hf.hour
                setTextColor(
                    if (hf.hour == "Now") Color.parseColor("#2ECC71")
                    else Color.WHITE
                )
                textSize = 11f
                typeface = resources.getFont(
                    if (hf.hour == "Now") R.font.poppins_semibold
                    else R.font.poppins_regular
                )
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = hf.emoji
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, (4 * d).toInt(), 0, (4 * d).toInt())
            })

            addView(TextView(context).apply {
                text = "${hf.temp}°"
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = resources.getFont(R.font.poppins_semibold)
                gravity = Gravity.CENTER
            })

            if (hf.rainPercent > 0) {
                addView(TextView(context).apply {
                    text = "${hf.rainPercent}%"
                    setTextColor(Color.parseColor("#66FFFFFF"))
                    textSize = 8f
                    typeface = resources.getFont(R.font.poppins_light)
                    gravity = Gravity.CENTER
                    setPadding(0, (2 * d).toInt(), 0, 0)
                })
            }
        }
    }
}