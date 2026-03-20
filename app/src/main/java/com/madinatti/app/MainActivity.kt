package com.madinatti.app

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
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

        binding.fabPostAd.scaleX = 0f
        binding.fabPostAd.scaleY = 0f
        binding.fabPostAd.visibility = View.GONE

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val h = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
            getSharedPreferences("ui_prefs", MODE_PRIVATE).edit()
                .putInt("status_bar_height", h).apply()
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        val defaultNavOptions = NavOptions.Builder()
            .setPopUpTo(R.id.homeFragment, false)
            .setLaunchSingleTop(true)
            .build()

        binding.bottomNav.setOnItemSelectedListener { item ->
            // Animate fade transition on nav items
            val menuView = binding.bottomNav.getChildAt(0) as? ViewGroup
            menuView?.let { mv ->
                for (i in 0 until mv.childCount) {
                    val child = mv.getChildAt(i)
                    child.animate()
                        .alpha(if (i == item.order) 1f else 0.6f)
                        .setDuration(150)
                        .start()
                }
            }

            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.navHostFragment) as NavHostFragment
            val navController = navHostFragment.navController

            val defaultNavOptions = NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, false)
                .setLaunchSingleTop(true)
                .build()

            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.popBackStack(R.id.homeFragment, false)
                    true
                }
                R.id.villeFragment -> {
                    navController.navigate(
                        R.id.villeFragment,
                        bundleOf("selectedTab" to "marketplace"),
                        NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, false)
                            .setLaunchSingleTop(false)
                            .build()
                    )
                    true
                }
                R.id.messagesFragment -> {
                    navController.navigate(R.id.messagesFragment, null, defaultNavOptions)
                    true
                }
                R.id.profileFragment -> {
                    navController.navigate(R.id.profileFragment, null, defaultNavOptions)
                    true
                }
                else -> false
            }
        }

// Set initial alpha state (home selected by default)
        binding.bottomNav.post {
            val menuView = binding.bottomNav.getChildAt(0) as? ViewGroup
            menuView?.let { mv ->
                for (i in 0 until mv.childCount) {
                    mv.getChildAt(i).alpha = if (i == 0) 1f else 0.6f
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id != R.id.villeFragment) {
                hideFab()
            }
            val menuItem = binding.bottomNav.menu.findItem(destination.id)
            if (menuItem != null) {
                menuItem.isChecked = true
            }
        }

        binding.fabPostAd.setOnClickListener {
            binding.particleView.triggerRippleFromView(binding.fabPostAd)
            // TODO: Open "Create Ad" screen
        }
    }

    fun navigateToVilleTab(tab: String) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.navigate(
            R.id.villeFragment,
            bundleOf("selectedTab" to tab),
            NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, false)
                .setLaunchSingleTop(false)
                .build()
        )
        binding.bottomNav.selectedItemId = R.id.villeFragment
    }

    fun updateFabVisibility(show: Boolean) {
        if (show) showFab() else hideFab()
    }

    private fun showFab() {
        if (fabVisible) return
        fabVisible = true

        binding.fabPostAd.visibility = View.VISIBLE
        binding.fabPostAd.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator(2f))
            .start()

        animateNavItemsSpread(true)
    }

    private fun hideFab() {
        if (!fabVisible) return
        fabVisible = false

        binding.fabPostAd.animate()
            .scaleX(0f).scaleY(0f)
            .setDuration(200)
            .withEndAction { binding.fabPostAd.visibility = View.GONE }
            .start()

        animateNavItemsSpread(false)
    }

    private fun animateNavItemsSpread(spread: Boolean) {
        val menuView = binding.bottomNav.getChildAt(0) as? ViewGroup ?: return
        val itemCount = menuView.childCount
        if (itemCount < 4) return

        // Inner items (Messages, Ville) move MORE
        // Outer items (Home, Profile) move LESS
        val innerShift = dpToPx(24).toFloat()
        val outerShift = dpToPx(6).toFloat()

        for (i in 0 until itemCount) {
            val itemView = menuView.getChildAt(i)
            val targetX = if (spread) {
                when (i) {
                    0 -> -outerShift      // Home: slight left
                    1 -> -innerShift      // Messages: more left
                    2 -> innerShift       // Ville: more right
                    3 -> outerShift       // Profile: slight right
                    else -> 0f
                }
            } else {
                0f
            }

            itemView.animate()
                .translationX(targetX)
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val location = IntArray(2)
        binding.particleView.getLocationOnScreen(location)
        binding.particleView.onExternalTouch(
            ev.rawX - location[0], ev.rawY - location[1],
            ev.action != MotionEvent.ACTION_UP &&
                    ev.action != MotionEvent.ACTION_CANCEL
        )
        return super.dispatchTouchEvent(ev)
    }


}