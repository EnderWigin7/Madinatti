package com.madinatti.app

import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LanguageActivity : AppCompatActivity() {

    private var selectedLanguage = "fr"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language)

        val btnArabic = findViewById<TextView>(R.id.btnArabic)
        val btnFrench = findViewById<TextView>(R.id.btnFrench)
        val btnEnglish = findViewById<TextView>(R.id.btnEnglish)
        val btnContinuer = findViewById<TextView>(R.id.btnContinuer)


        fun animateButtonBackground(view: TextView, selected: Boolean) {
            val fromColor = if (selected)
                android.graphics.Color.parseColor("#332ECC71")
            else
                android.graphics.Color.parseColor("#442ECC71")

            val toColor = if (selected)
                android.graphics.Color.parseColor("#442ECC71")
            else
                android.graphics.Color.parseColor("#332ECC71")

            val colorAnim = android.animation.ValueAnimator.ofObject(
                android.animation.ArgbEvaluator(), fromColor, toColor
            ).apply {
                duration = 250
                interpolator = android.view.animation.DecelerateInterpolator()
                addUpdateListener { animator ->
                    val color = animator.animatedValue as Int
                    val drawable = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 21f
                        setColor(color)
                        if (selected) {
                            setStroke(4, android.graphics.Color.parseColor("#B0D8C0"))
                        } else {
                            setStroke(1, android.graphics.Color.parseColor("#22FFFFFF"))
                        }
                    }
                    view.background = drawable
                }
            }
            colorAnim.start()
        }

        fun updateSelection(lang: String) {
            selectedLanguage = lang
            listOf(
                Pair(btnArabic, "ar"),
                Pair(btnFrench, "fr"),
                Pair(btnEnglish, "en")
            ).forEach { (btn, code) ->
                animateButtonBackground(btn, code == lang)
            }
        }

        updateSelection("fr")

        btnArabic.setOnClickListener { animateView(it as TextView) { updateSelection("ar") } }
        btnFrench.setOnClickListener { animateView(it as TextView) { updateSelection("fr") } }
        btnEnglish.setOnClickListener { animateView(it as TextView) { updateSelection("en") } }

        btnContinuer.setOnClickListener {
            val prefs = getSharedPreferences("madinatti_prefs", MODE_PRIVATE)
            prefs.edit().putString("language", selectedLanguage).apply()
            animateView(it as TextView) {
                startActivity(Intent(this, LoginActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }
    }

    private fun animateView(view: TextView, onEnd: (() -> Unit)? = null) {
        view.animate()
            .scaleX(0.92f).scaleY(0.92f)
            .setDuration(100)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                view.animate()
                    .scaleX(1.04f).scaleY(1.04f)
                    .setDuration(120)
                    .setInterpolator(OvershootInterpolator(2f))
                    .withEndAction {
                        view.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(80)
                            .withEndAction { onEnd?.invoke() }
                            .start()
                    }.start()
            }.start()
    }
}