package com.madinatti.app

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.madinatti.app.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var isLoginActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = android.graphics.Color.parseColor("#132D1F")
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(false)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showFragment(LoginFragment(), slideRight = false)
        updateToggle(loginActive = true)

        binding.tvSegmentLogin.setOnClickListener {
            if (!isLoginActive) {
                isLoginActive = true
                updateToggle(loginActive = true)
                showFragment(LoginFragment(), slideRight = false)
                binding.tvSocialLabel.text = "Connexion via réseau social"
                binding.particleView.triggerRippleFromView(binding.tvSegmentLogin)
            }
        }

        binding.tvSegmentRegister.setOnClickListener {
            if (isLoginActive) {
                isLoginActive = false
                updateToggle(loginActive = false)
                showFragment(RegisterFragment(), slideRight = true)
                binding.tvSocialLabel.text = "S'inscrire via réseau social"
                binding.particleView.triggerRippleFromView(binding.tvSegmentRegister)
            }
        }

        binding.btnAuthAction.setOnClickListener {
            binding.particleView.triggerRippleFromView(binding.btnAuthAction)
            if (isLoginActive) {

                (supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                        as? LoginFragment)?.attemptLogin()
            } else {
                (supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                        as? RegisterFragment)?.attemptRegister()
            }
        }

        binding.btnSocialLogin.setOnClickListener {
            binding.particleView.triggerRippleFromView(binding.btnSocialLogin)

            val location = IntArray(2)
            binding.btnSocialLogin.getLocationOnScreen(location)
            val screenHeight = resources.displayMetrics.heightPixels

            val offsetFromBottom = screenHeight - location[1] +
                    (8 * resources.displayMetrics.density).toInt()

            SocialLoginDialog().apply {
                anchorY = offsetFromBottom
            }.show(supportFragmentManager, SocialLoginDialog.TAG)
        }
    }

    fun triggerParticleRipple(view: View) {
        binding.particleView.triggerRippleFromView(view)
    }

    private fun updateToggle(loginActive: Boolean) {
        val indicator = binding.segmentIndicator

        indicator.post {
            indicator.animate()
                .translationX(if (loginActive) 0f else indicator.width.toFloat())
                .setDuration(250)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }

        indicator.setBackgroundResource(
            if (loginActive) R.drawable.bg_segment_left
            else R.drawable.bg_segment_right
        )

        binding.tvSegmentLogin.setTextColor(
            if (loginActive) android.graphics.Color.parseColor("#0D1F17")
            else android.graphics.Color.WHITE
        )
        binding.tvSegmentRegister.setTextColor(
            if (loginActive) android.graphics.Color.WHITE
            else android.graphics.Color.parseColor("#0D1F17")
        )

        binding.btnAuthAction.text = if (loginActive) "Se connecter" else "S'inscrire"
    }

    private fun showFragment(fragment: Fragment, slideRight: Boolean) {
        val enter = if (slideRight) R.anim.slide_in_right else R.anim.slide_in_left
        val exit = if (slideRight) R.anim.slide_out_left else R.anim.slide_out_right
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(enter, exit)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
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