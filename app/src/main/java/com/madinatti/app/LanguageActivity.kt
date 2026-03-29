package com.madinatti.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.madinatti.app.databinding.ActivityLanguageBinding

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private var selectedLanguage = "fr"

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = android.graphics.Color.parseColor("#132D1F")
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load previously saved language
        val savedLang = getSharedPreferences("madinatti_prefs", 0)
            .getString("app_language", "fr") ?: "fr"
        selectedLanguage = savedLang
        updateSelection(savedLang)

        binding.btnArabic.setOnClickListener { updateSelection("ar"); animateView(binding.btnArabic) }
        binding.btnFrench.setOnClickListener { updateSelection("fr"); animateView(binding.btnFrench) }
        binding.btnEnglish.setOnClickListener { updateSelection("en"); animateView(binding.btnEnglish) }

        binding.btnContinuer.setOnClickListener {
            binding.particleView.triggerRippleFromView(binding.btnContinuer)
            animateView(binding.btnContinuer)

            // Save language
            getSharedPreferences("madinatti_prefs", 0)
                .edit().putString("app_language", selectedLanguage).apply()

            // Apply locale
            applyLocale(selectedLanguage)

            binding.btnContinuer.postDelayed({
                // Check if coming from profile (already logged in) vs first launch
                val hasSeen = getSharedPreferences("madinatti_prefs", 0)
                    .getBoolean("has_seen_splash", false)

                if (hasSeen) {
                    // Coming from profile → go back to main
                    startActivity(Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                } else {
                    startActivity(Intent(this, AuthActivity::class.java))
                }
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }, 280)
        }
    }

    private fun applyLocale(lang: String) {
        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun updateSelection(lang: String) {
        selectedLanguage = lang

        listOf(
            Pair(binding.btnArabic, "ar"),
            Pair(binding.btnFrench, "fr"),
            Pair(binding.btnEnglish, "en")
        ).forEach { (btn, code) ->
            if (code == lang) {
                btn.background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 21f
                    setColor(android.graphics.Color.argb(115, 46, 204, 113))
                    setStroke(3, android.graphics.Color.argb(255, 46, 204, 113))
                }
            } else {
                btn.setBackgroundResource(R.drawable.bg_btn_glass)
            }
        }
    }

    private fun animateView(view: TextView) {
        view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
            .withEndAction {
                view.animate().scaleX(1.01f).scaleY(1.01f).setDuration(100)
                    .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                    .withEndAction {
                        view.animate().scaleX(1f).scaleY(1f).setDuration(60).start()
                    }.start()
            }.start()
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        val location = IntArray(2)
        binding.particleView.getLocationOnScreen(location)
        binding.particleView.onExternalTouch(
            ev.rawX - location[0], ev.rawY - location[1],
            ev.action != android.view.MotionEvent.ACTION_UP && ev.action != android.view.MotionEvent.ACTION_CANCEL
        )
        return super.dispatchTouchEvent(ev)
    }
}