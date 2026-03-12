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

        fun updateSelection(lang: String) {
            selectedLanguage = lang
            btnArabic.setBackgroundResource(
                if (lang == "ar") R.drawable.bg_glass_button_selected
                else R.drawable.bg_glass_button)
            btnFrench.setBackgroundResource(
                if (lang == "fr") R.drawable.bg_glass_button_selected
                else R.drawable.bg_glass_button)
            btnEnglish.setBackgroundResource(
                if (lang == "en") R.drawable.bg_glass_button_selected
                else R.drawable.bg_glass_button)
        }

        updateSelection("fr")

        btnArabic.setOnClickListener { animateView(it as TextView) { updateSelection("ar") } }
        btnFrench.setOnClickListener { animateView(it as TextView) { updateSelection("fr") } }
        btnEnglish.setOnClickListener { animateView(it as TextView) { updateSelection("en") } }

        btnContinuer.setOnClickListener {
            val prefs = getSharedPreferences("madinatti_prefs", MODE_PRIVATE)
            prefs.edit().putString("language", selectedLanguage).apply()
            animateView(it as TextView) {
                startActivity(Intent(this, MainActivity::class.java))
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