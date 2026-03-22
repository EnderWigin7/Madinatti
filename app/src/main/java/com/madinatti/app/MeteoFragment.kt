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
import androidx.fragment.app.Fragment

class MeteoFragment : Fragment() {

    enum class WeatherType(
        val colors: IntArray,
        val emoji: String,
        val label: String
    ) {
        SUNNY(
            intArrayOf(
                Color.parseColor("#FF6F00"),
                Color.parseColor("#1E88E5"),
                Color.parseColor("#0D47A1")
            ), "☀️", "Ensoleillé"
        ),
        PARTLY_CLOUDY(
            intArrayOf(
                Color.parseColor("#EF6C00"),
                Color.parseColor("#0288D1"),
                Color.parseColor("#01579B")
            ), "⛅", "Partiellement nuageux"
        ),
        CLOUDY(
            intArrayOf(
                Color.parseColor("#78909C"),
                Color.parseColor("#455A64"),
                Color.parseColor("#263238")
            ), "☁️", "Nuageux"
        ),
        RAINY(
            intArrayOf(
                Color.parseColor("#1565C0"),
                Color.parseColor("#0D47A1"),
                Color.parseColor("#1A237E")
            ), "🌧️", "Pluvieux"
        ),
        STORMY(
            intArrayOf(
                Color.parseColor("#311B92"),
                Color.parseColor("#1A237E"),
                Color.parseColor("#0D1B2A")
            ), "⛈️", "Orageux"
        ),
        NIGHT_CLEAR(
            intArrayOf(
                Color.parseColor("#1A237E"),
                Color.parseColor("#0D1B2A"),
                Color.parseColor("#000051")
            ), "🌙", "Nuit claire"
        ),
    }

    data class DayForecast(
        val day: String,
        val emoji: String,
        val tempHigh: Int,
        val tempLow: Int,
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

    override fun onViewCreated(
        view: View, savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val weather = WeatherType.PARTLY_CLOUDY
        val temp = 31
        val feelsLike = 29
        val humidity = 45
        val wind = 12

        applyGradient(view, weather)
        populateMain(view, weather, temp, feelsLike, humidity, wind)
        populateHourly(view)
        populateForecastList(view)
    }

    private fun applyGradient(view: View, type: WeatherType) {
        val bg = view.findViewById<View>(R.id.weatherGradientBg)
        val drawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            type.colors
        ).apply {
            cornerRadius = 18f * resources.displayMetrics.density
        }
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
        view.findViewById<TextView>(R.id.tvWeatherEmoji)
            ?.text = type.emoji
        view.findViewById<TextView>(R.id.tvTemperature)
            ?.text = "${temp}°C"
        view.findViewById<TextView>(R.id.tvCondition)
            ?.text = type.label
        view.findViewById<TextView>(R.id.tvFeelsLike)
            ?.text = getString(
            R.string.weather_feels_like, "${feelsLike}°C"
        )
        view.findViewById<TextView>(R.id.tvHumidity)
            ?.text = "${humidity}%"
        view.findViewById<TextView>(R.id.tvWind)
            ?.text = "${wind} km/h"
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
        hours.forEach { row.addView(makeHourlyItem(it)) }
    }

    private fun makeHourlyItem(
        hf: HourForecast
    ): LinearLayout {
        val d = resources.displayMetrics.density
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                (6 * d).toInt(), (8 * d).toInt(),
                (6 * d).toInt(), (8 * d).toInt()
            )
            layoutParams = LinearLayout.LayoutParams(
                (46 * d).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (5 * d).toInt() }
            setBackgroundResource(R.drawable.bg_hourly_item)

            addView(TextView(context).apply {
                text = hf.hour
                setTextColor(
                    if (hf.hour == "Now")
                        Color.parseColor("#2ECC71")
                    else Color.WHITE
                )
                textSize = 10f
                typeface = resources.getFont(
                    if (hf.hour == "Now")
                        R.font.poppins_semibold
                    else R.font.poppins_regular
                )
                gravity = Gravity.CENTER
            })

            addView(TextView(context).apply {
                text = hf.emoji
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(
                    0, (3 * d).toInt(),
                    0, (3 * d).toInt()
                )
            })

            addView(TextView(context).apply {
                text = "${hf.temp}°"
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = resources.getFont(
                    R.font.poppins_semibold
                )
                gravity = Gravity.CENTER
            })

            if (hf.rainPercent > 0) {
                addView(TextView(context).apply {
                    text = "💧${hf.rainPercent}%"
                    setTextColor(
                        Color.parseColor("#77FFFFFF")
                    )
                    textSize = 7f
                    typeface = resources.getFont(
                        R.font.poppins_light
                    )
                    gravity = Gravity.CENTER
                    setPadding(0, (2 * d).toInt(), 0, 0)
                })
            }
        }
    }

    private fun populateForecastList(view: View) {
        val forecasts = listOf(
            DayForecast("Lundi", "☀️", 33, 18, 3),
            DayForecast("Mardi", "⛅", 29, 16, 15),
            DayForecast("Mercredi", "🌧️", 24, 14, 65),
            DayForecast("Jeudi", "☀️", 34, 19, 2),
            DayForecast("Vendredi", "☀️", 33, 17, 5),
            DayForecast("Samedi", "⛅", 30, 16, 12),
            DayForecast("Dimanche", "☀️", 32, 18, 4)
        )

        val list = view.findViewById<LinearLayout>(
            R.id.forecastList
        )
        list.removeAllViews()

        forecasts.forEachIndexed { i, fc ->
            list.addView(makeForecastRow(fc))
            if (i < forecasts.size - 1) {
                list.addView(makeDivider())
            }
        }
    }

    private fun makeForecastRow(
        fc: DayForecast
    ): LinearLayout {
        val d = resources.displayMetrics.density
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                (14 * d).toInt(), (8 * d).toInt(),
                (14 * d).toInt(), (8 * d).toInt()
            )

            // Day
            addView(TextView(context).apply {
                text = fc.day
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = resources.getFont(
                    R.font.poppins_regular
                )
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.3f
                )
            })

            // Rain
            addView(TextView(context).apply {
                text = if (fc.rainPercent > 5)
                    "💧${fc.rainPercent}%" else ""
                setTextColor(
                    Color.parseColor("#88FFFFFF")
                )
                textSize = 9f
                typeface = resources.getFont(
                    R.font.poppins_light
                )
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0.6f
                )
            })

            // Emoji
            addView(TextView(context).apply {
                text = fc.emoji
                textSize = 18f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0.4f
                )
            })

            // Low
            addView(TextView(context).apply {
                text = "${fc.tempLow}°"
                setTextColor(
                    Color.parseColor("#88FFFFFF")
                )
                textSize = 12f
                typeface = resources.getFont(
                    R.font.poppins_regular
                )
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0.35f
                )
            })

            // Bar
            addView(makeTempBar(fc.tempLow, fc.tempHigh))

            // High
            addView(TextView(context).apply {
                text = "${fc.tempHigh}°"
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = resources.getFont(
                    R.font.poppins_semibold
                )
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0.35f
                )
            })
        }
    }

    private fun makeTempBar(
        low: Int, high: Int
    ): View {
        val d = resources.displayMetrics.density
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, (4 * d).toInt(), 1f
            ).apply {
                marginStart = (4 * d).toInt()
                marginEnd = (4 * d).toInt()
            }

            val startColor = when {
                high > 30 -> Color.parseColor("#FF6D00")
                high > 25 -> Color.parseColor("#FFB300")
                high > 20 -> Color.parseColor("#2ECC71")
                else -> Color.parseColor("#29B6F6")
            }
            val endColor = when {
                low > 20 -> Color.parseColor("#FFB300")
                low > 15 -> Color.parseColor("#66BB6A")
                else -> Color.parseColor("#29B6F6")
            }

            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(endColor, startColor)
            ).apply {
                cornerRadius = 4 * d
            }
        }
    }

    private fun makeDivider(): View {
        val d = resources.displayMetrics.density
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply {
                marginStart = (14 * d).toInt()
                marginEnd = (14 * d).toInt()
            }
            setBackgroundColor(
                Color.parseColor("#15FFFFFF")
            )
        }
    }
}