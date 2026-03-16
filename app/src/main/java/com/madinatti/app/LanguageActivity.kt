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
                startGlowPulse(btn)
            } else {
                btn.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                btn.setBackgroundResource(R.drawable.bg_btn_glass)
            }
        }
    }

    private fun startGlowPulse(view: TextView) {
        glowAnimator?.cancel()
        view.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

        glowAnimator = ValueAnimator.ofFloat(0.4f, 1f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val alpha = anim.animatedValue as Float
                val glowPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    setShadowLayer(20f, 0f, 0f,
                        android.graphics.Color.argb((alpha * 255).toInt(), 46, 204, 113))
                }
                view.background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 21f
                    setColor(android.graphics.Color.argb(
                        (alpha * 0.5f * 255).toInt(), 46, 204, 113))
                    setStroke(3, android.graphics.Color.argb(
                        (alpha * 255).toInt(), 46, 204, 113))
                }
                view.invalidate()
            }
        }
        glowAnimator?.start()
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