package com.madinatti.app

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import androidx.core.content.edit
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.madinatti.app.databinding.ActivityLanguageBinding

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private var selectedLanguage = "fr"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnArabic.background = makeBackground(0x332ECC71, borderColor = 0x22FFFFFF, borderWidthPx = 3)
        binding.btnFrench.background = makeBackground(0x332ECC71, borderColor = 0x22FFFFFF, borderWidthPx = 3)
        binding.btnEnglish.background = makeBackground(0x332ECC71, borderColor = 0x22FFFFFF, borderWidthPx = 3)
        binding.btnContinuer.background = makeBackground(0xFF2ECC71.toInt())

        fun animateButtonBackground(view: TextView, selected: Boolean) {
            val fromColor = if (selected) 0x332ECC71 else 0x442ECC71
            val toColor = if (selected) 0x442ECC71 else 0x332ECC71
            ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
                duration = 230
                interpolator = android.view.animation.DecelerateInterpolator()
                addUpdateListener { anim ->
                    val color = anim.animatedValue as Int
                    view.background = makeBackground(
                        color = color,
                        borderColor = if (selected) 0xFFB0D8C0.toInt() else 0x22FFFFFF,
                        borderWidthPx = if (selected) 4 else 3
                    )
                }
            }.start()
        }

        fun updateSelection(lang: String) {
            selectedLanguage = lang
            listOf(
                Pair(binding.btnArabic, "ar"),
                Pair(binding.btnFrench, "fr"),
                Pair(binding.btnEnglish, "en")
            ).forEach { (btn, code) ->
                animateButtonBackground(btn, code == lang)
            }
        }

        updateSelection("fr")

        binding.btnArabic.setOnClickListener {
            updateSelection("ar")
            animateView(binding.btnArabic)
            binding.particleView.triggerRippleFromView(binding.btnArabic)
        }
        binding.btnFrench.setOnClickListener {
            updateSelection("fr")
            animateView(binding.btnFrench)
            binding.particleView.triggerRippleFromView(binding.btnFrench)
        }
        binding.btnEnglish.setOnClickListener {
            updateSelection("en")
            animateView(binding.btnEnglish)
            binding.particleView.triggerRippleFromView(binding.btnEnglish)
        }

        binding.btnContinuer.setOnClickListener {
            binding.particleView.triggerRippleFromView(binding.btnContinuer)
            updateSelection(selectedLanguage)
            val prefs = getSharedPreferences("madinatti_prefs", MODE_PRIVATE)
            prefs.edit {putString("language", selectedLanguage) }
            animateView(binding.btnContinuer)
            binding.btnContinuer.postDelayed({
                startActivity(Intent(this, AuthActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }, 300)
        }
    }

    private fun makeBackground(
        color: Int, cornerDp: Float = 7f,
        borderColor: Int = 0, borderWidthPx: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerDp * resources.displayMetrics.density
            setColor(color)
            if (borderWidthPx > 0) setStroke(borderWidthPx, borderColor)
        }
    }

    private fun animateView(view: TextView) {
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                view.animate().scaleX(1.02f).scaleY(1.02f).setDuration(100)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                    .withEndAction {
                        view.animate().scaleX(1f).scaleY(1f).setDuration(60).start()
                    }.start()
            }.start()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        android.util.Log.d("TOUCH", "x=${ev.rawX} y=${ev.rawY}")
        val location = IntArray(2)
        binding.particleView.getLocationOnScreen(location)
        binding.particleView.onExternalTouch(
            ev.rawX - location[0],
            ev.rawY - location[1],
            ev.action != MotionEvent.ACTION_UP && ev.action != MotionEvent.ACTION_CANCEL
        )
        return super.dispatchTouchEvent(ev)
    }
}