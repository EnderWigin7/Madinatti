package com.madinatti.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("madinatti_prefs", MODE_PRIVATE)
        val hasSeenSplash = prefs.getBoolean("has_seen_splash", false)

        if (hasSeenSplash) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_splash)


        val imgBackground = findViewById<android.widget.ImageView>(R.id.imgBackground)
        val imgLogo = findViewById<android.widget.ImageView>(R.id.imgLogo)
        val tvAppName = findViewById<android.widget.TextView>(R.id.tvAppName)
        val tvTagline = findViewById<android.widget.TextView>(R.id.tvTagline)

        val btnCommencer = findViewById<android.widget.Button>(R.id.btnCommencer)
        btnCommencer.setShadowLayer(7f, 3f, 3f, android.graphics.Color.parseColor("#80000000"))

        imgBackground.scaleX = 1.08f
        imgBackground.scaleY = 1.08f

        // Smoother background shake using sine-based ValueAnimator
        val animator = android.animation.ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat()).apply {
            duration = 12000
            repeatCount = android.animation.ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val value = anim.animatedValue as Float
                imgBackground.translationX = (Math.sin(value.toDouble()) * 14).toFloat()
                imgBackground.translationY = (Math.sin(value.toDouble() * 0.7) * 9).toFloat()
            }
        }
        animator.start()

        // Fade in UI elements one by one
        imgLogo.alpha = 0f
        tvAppName.alpha = 0f
        tvTagline.alpha = 0f
        btnCommencer.alpha = 0f
        btnCommencer.translationY = 40f

        val logoFade = ObjectAnimator.ofFloat(imgLogo, "alpha", 0f, 1f).setDuration(700)
        val nameFade = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f).setDuration(600)
        val tagFade = ObjectAnimator.ofFloat(tvTagline, "alpha", 0f, 1f).setDuration(500)
        val btnFade = ObjectAnimator.ofFloat(btnCommencer, "alpha", 0f, 1f).setDuration(500)
        val btnSlide = ObjectAnimator.ofFloat(btnCommencer, "translationY", 40f, 0f).apply {
            duration = 500
            interpolator = OvershootInterpolator()
        }

        AnimatorSet().apply {
            play(logoFade)
            play(nameFade).after(300)
            play(tagFade).after(500)
            play(btnFade).after(800)
            play(btnSlide).after(800)
            start()
        }

        // Interactive button press
        btnCommencer.setOnClickListener {
            it.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(120)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {
                    it.animate()
                        .scaleX(1.04f)
                        .scaleY(1.04f)
                        .setDuration(150)
                        .setInterpolator(android.view.animation.OvershootInterpolator(2f))
                        .withEndAction {
                            it.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
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