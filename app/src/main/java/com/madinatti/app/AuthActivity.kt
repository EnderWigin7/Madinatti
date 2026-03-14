package com.madinatti.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.madinatti.app.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var isLoginActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showFragment(LoginFragment(), false)
        updateToggle(true)

        binding.tvSegmentLogin.setOnClickListener {
            if (!isLoginActive) {
                isLoginActive = true
                updateToggle(true)
                showFragment(LoginFragment(), slideRight = false)
                binding.tvSocialLabel.text = "Connexion via réseau social"
            }
        }

        binding.tvSegmentRegister.setOnClickListener {
            if (isLoginActive) {
                isLoginActive = false
                updateToggle(false)
                showFragment(RegisterFragment(), slideRight = true)
                binding.tvSocialLabel.text = "S'inscrire via réseau social"
            }
        }
    }

    private fun updateToggle(loginActive: Boolean) {
        val indicator = binding.segmentIndicator

        // Slide indicator
        indicator.animate()
            .translationX(if (loginActive) 0f else indicator.width.toFloat())
            .setDuration(250)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Swap background shape (rounded left vs right)
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
    }

    private fun showFragment(fragment: Fragment, slideRight: Boolean) {
        val enter = if (slideRight) R.anim.slide_in_right else R.anim.slide_in_left
        val exit = if (slideRight) R.anim.slide_out_left else R.anim.slide_out_right

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(enter, exit)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}