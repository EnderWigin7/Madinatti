package com.madinatti.app

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.madinatti.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityMainBinding
    private var fabVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = android.graphics.Color.parseColor("#132D1F")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Custom home button handling
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    // Pop entire back stack back to home
                    navController.popBackStack(R.id.homeFragment, false)
                    true
                }
                else -> {
                    androidx.navigation.ui.NavigationUI
                        .onNavDestinationSelected(item, navController)
                }
            }
        }

        // FAB visibility by destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.marketplaceFragment -> showFab()
                else -> hideFab()
            }
        }

        binding.fabPostAd.setOnClickListener {
            binding.particleView.triggerRippleFromView(binding.fabPostAd)
            // TODO: Post ad screen
        }
    }

    private fun showFab() {
        if (fabVisible) return
        fabVisible = true
        binding.fabPostAd.visibility = View.VISIBLE
        binding.fabPostAd.scaleX = 0f
        binding.fabPostAd.scaleY = 0f

        // Pop FAB up
        binding.fabPostAd.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator(2f))
            .start()

        // Push nav items apart using padding
        android.animation.ValueAnimator.ofInt(0, 48).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val v = anim.animatedValue as Int
                binding.bottomNav.setPadding(v, 0, v, 0)
            }
        }.start()
    }

    private fun hideFab() {
        if (!fabVisible) return
        fabVisible = false
        binding.fabPostAd.animate()
            .scaleX(0f).scaleY(0f)
            .setDuration(200)
            .withEndAction { binding.fabPostAd.visibility = View.GONE }
            .start()

        android.animation.ValueAnimator.ofInt(
            binding.bottomNav.paddingLeft, 0).apply {
            duration = 250
            addUpdateListener { anim ->
                val v = anim.animatedValue as Int
                binding.bottomNav.setPadding(v, 0, v, 0)
            }
        }.start()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val location = IntArray(2)
        binding.particleView.getLocationOnScreen(location)
        binding.particleView.onExternalTouch(
            ev.rawX - location[0],
            ev.rawY - location[1],
            ev.action != MotionEvent.ACTION_UP &&
                    ev.action != MotionEvent.ACTION_CANCEL
        )
        return super.dispatchTouchEvent(ev)
    }
}