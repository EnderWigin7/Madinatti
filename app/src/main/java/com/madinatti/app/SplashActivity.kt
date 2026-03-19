package com.madinatti.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.madinatti.app.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = android.graphics.Color.parseColor("#132D1F")
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(false)

        val prefs = getSharedPreferences("madinatti_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("has_seen_splash", false)) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imgBackground.scaleX = 1.08f
        binding.imgBackground.scaleY = 1.08f

        ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat()).apply {
            duration = 12000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val v = anim.animatedValue as Float
                binding.imgBackground.translationX = (Math.sin(v.toDouble()) * 14).toFloat()
                binding.imgBackground.translationY = (Math.sin(v.toDouble() * 0.7) * 9).toFloat()
            }
        }.start()

        // Fade in elements
        listOf(binding.imgLogo, binding.tvAppName, binding.tvTagline, binding.btnCommencer).forEach {
            it.alpha = 0f
        }
        binding.btnCommencer.translationY = 40f

        AnimatorSet().apply {
            play(ObjectAnimator.ofFloat(binding.imgLogo, "alpha", 0f, 1f).setDuration(700))
            play(ObjectAnimator.ofFloat(binding.tvAppName, "alpha", 0f, 1f).setDuration(600)).after(300)
            play(ObjectAnimator.ofFloat(binding.tvTagline, "alpha", 0f, 1f).setDuration(500)).after(500)
            play(ObjectAnimator.ofFloat(binding.btnCommencer, "alpha", 0f, 1f).setDuration(500)).after(800)
            play(ObjectAnimator.ofFloat(binding.btnCommencer, "translationY", 40f, 0f).apply {
                duration = 500
                interpolator = OvershootInterpolator()
            }).after(800)
            start()
        }

        binding.btnCommencer.setOnClickListener {
            it.animate().scaleX(0.92f).scaleY(0.92f).setDuration(120)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    it.animate().scaleX(1.04f).scaleY(1.04f).setDuration(150)
                        .setInterpolator(OvershootInterpolator(2f))
                        .withEndAction {
                            it.animate().scaleX(1f).scaleY(1f).setDuration(100)
                                .withEndAction {
                                    startActivity(Intent(this, LanguageActivity::class.java))
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                                    finish()
                                }.start()
                        }.start()
                }.start()
        }
    }
}