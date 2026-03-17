package com.madinatti.app

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.madinatti.app.databinding.ActivityLanguageBinding

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private var selectedLanguage = "fr"
    private var glowAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateSelection("fr")

        binding.btnArabic.setOnClickListener {
            updateSelection("ar")
            animateView(binding.btnArabic)
        }
        binding.btnFrench.setOnClickListener {
            updateSelection("fr")
            animateView(binding.btnFrench)
        }
        binding.btnEnglish.setOnClickListener {
            updateSelection("en")
            animateView(binding.btnEnglish)
        }

        binding.btnContinuer.setOnClickListener {
            binding.particleView.triggerRippleFromView(binding.btnContinuer)
            val prefs = getSharedPreferences("madinatti_prefs", MODE_PRIVATE)
            prefs.edit { putString("language", selectedLanguage) }
            animateView(binding.btnContinuer)
            binding.btnContinuer.postDelayed({
                startActivity(Intent(this, AuthActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }, 280)
        }
    }

    private fun updateSelection(lang: String) {
        selectedLanguage = lang
        glowAnimator?.cancel()

        listOf(
            Pair(binding.btnArabic, "ar"),
            Pair(binding.btnFrench, "fr"),
            Pair(binding.btnEnglish, "en")
        ).forEach { (btn, code) ->
            if (code == lang) {

                glowAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 400
                    interpolator = android.view.animation.DecelerateInterpolator()
                    addUpdateListener { anim ->
                        val t = anim.animatedValue as Float
                        val fillAlpha = (t * 0.45f * 255).toInt()
                        val strokeAlpha = (t * 255).toInt()
                        btn.background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = 21f
                            setColor(android.graphics.Color.argb(fillAlpha, 46, 204, 113))
                            setStroke(3, android.graphics.Color.argb(strokeAlpha, 46, 204, 113))
                        }
                    }
                }
                glowAnimator?.start()
            } else {
                btn.setBackgroundResource(R.drawable.bg_btn_glass)
            }
        }
    }


    private fun animateView(view: TextView) {
        view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                view.animate().scaleX(1.01f).scaleY(1.01f).setDuration(100)
                    .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                    .withEndAction {
                        view.animate().scaleX(1f).scaleY(1f).setDuration(60).start()
                    }.start()
            }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        glowAnimator?.cancel()
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        val location = IntArray(2)
        binding.particleView.getLocationOnScreen(location)
        binding.particleView.onExternalTouch(
            ev.rawX - location[0],
            ev.rawY - location[1],
            ev.action != android.view.MotionEvent.ACTION_UP &&
                    ev.action != android.view.MotionEvent.ACTION_CANCEL
        )
        return super.dispatchTouchEvent(ev)
    }
}