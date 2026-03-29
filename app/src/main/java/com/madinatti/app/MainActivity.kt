package com.madinatti.app

import android.content.Intent
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.madinatti.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var unreadListener: ListenerRegistration? = null
    private var fabVisible = false
    private var suppressNavListener = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lang = getSharedPreferences("madinatti_prefs", 0)
            .getString("app_language", "fr") ?: "fr"
        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)

        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(
            android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = android.graphics.Color.parseColor("#132D1F")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabPostAd.scaleX = 0f
        binding.fabPostAd.scaleY = 0f
        binding.fabPostAd.visibility = View.GONE

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val h = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.statusBars()).top
            getSharedPreferences("ui_prefs", MODE_PRIVATE).edit()
                .putInt("status_bar_height", h).apply()
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setOnItemSelectedListener { item ->
            if (suppressNavListener) return@setOnItemSelectedListener true

            val menuView = binding.bottomNav.getChildAt(0) as? ViewGroup
            menuView?.let { mv ->
                for (i in 0 until mv.childCount) {
                    mv.getChildAt(i).animate()
                        .alpha(if (i == item.order) 1f else 0.6f)
                        .setDuration(150).start()
                }
            }

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
                    navController.navigate(
                        R.id.messagesFragment, null, defaultNavOptions)
                    true
                }
                R.id.profileFragment -> {
                    navController.navigate(
                        R.id.profileFragment, null, defaultNavOptions)
                    true
                }
                else -> false
            }
        }

        binding.bottomNav.post {
            val menuView = binding.bottomNav.getChildAt(0) as? ViewGroup
            menuView?.let { mv ->
                for (i in 0 until mv.childCount) {
                    mv.getChildAt(i).alpha = if (i == 0) 1f else 0.6f
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id != R.id.villeFragment) hideFab()
            val menuItem = binding.bottomNav.menu.findItem(destination.id)
            menuItem?.isChecked = true

            val menuView = binding.bottomNav.getChildAt(0) as? ViewGroup
            menuView?.let { mv ->
                for (i in 0 until mv.childCount) {
                    mv.getChildAt(i).animate()
                        .alpha(if (binding.bottomNav.menu.getItem(i)
                                .isChecked) 1f else 0.6f)
                        .setDuration(150).start()
                }
            }
        }

        binding.fabPostAd.setOnClickListener {
            binding.particleView.triggerRippleFromView(binding.fabPostAd)
            (supportFragmentManager.findFragmentById(R.id.navHostFragment)
                    as NavHostFragment).navController.navigate(R.id.postAdFragment)
        }

        // Start observing unread messages
        observeUnreadMessages()
    }

    // ─────────────────────────────────────────
    // ONLINE STATUS - single clean function
    // ─────────────────────────────────────────
    private fun setUserOnlineStatus(
        online: Boolean,
        onDone: (() -> Unit)? = null
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onDone?.invoke()
            return
        }
        db.collection("users").document(uid)
            .update(mapOf(
                "isOnline" to online,
                "lastSeen" to com.google.firebase.Timestamp.now()
            ))
            .addOnCompleteListener {
                onDone?.invoke()
            }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            setUserOnlineStatus(true)
        }
    }

    override fun onStop() {
        super.onStop()
        if (auth.currentUser != null) {
            setUserOnlineStatus(false)
        }
    }

    // ─────────────────────────────────────────
    // LOGOUT - waits for Firestore before signing out
    // ─────────────────────────────────────────
    fun performLogout() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            setUserOnlineStatus(false) {
                // Only runs AFTER Firestore confirms offline
                auth.signOut()
                try {
                    com.google.android.gms.auth.api.signin.GoogleSignIn
                        .getClient(
                            this,
                            com.google.android.gms.auth.api.signin
                                .GoogleSignInOptions.Builder(
                                    com.google.android.gms.auth.api.signin
                                        .GoogleSignInOptions.DEFAULT_SIGN_IN
                                ).build()
                        ).signOut()
                } catch (_: Exception) {}

                getSharedPreferences("madinatti_prefs", 0)
                    .edit().clear().apply()

                startActivity(
                    Intent(this, AuthActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
        } else {
            auth.signOut()
            startActivity(
                Intent(this, AuthActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    // ─────────────────────────────────────────
    // UNREAD BADGE
    // ─────────────────────────────────────────
    private fun observeUnreadMessages() {
        val uid = auth.currentUser?.uid ?: return

        unreadListener?.remove()
        unreadListener = db.collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { result, _ ->
                if (result == null) return@addSnapshotListener
                val totalUnread = result.documents.sumOf { doc ->
                    (doc.getLong("unread_$uid") ?: 0).toInt()
                }
                runOnUiThread {
                    val badge = binding.bottomNav
                        .getOrCreateBadge(R.id.messagesFragment)
                    if (totalUnread > 0) {
                        badge.isVisible = true
                        badge.number = totalUnread
                        badge.backgroundColor =
                            android.graphics.Color.parseColor("#2ECC71")
                        badge.badgeTextColor =
                            android.graphics.Color.parseColor("#0D1F17")
                    } else {
                        badge.isVisible = false
                    }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        unreadListener?.remove()
    }

    // ─────────────────────────────────────────
    // NAVIGATION HELPERS
    // ─────────────────────────────────────────
    fun navigateToVilleTab(tab: String) {
        val navController = (supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment)
            .navController

        navController.navigate(
            R.id.villeFragment,
            bundleOf("selectedTab" to tab),
            NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, false)
                .setLaunchSingleTop(false)
                .build()
        )
        suppressNavListener = true
        binding.bottomNav.selectedItemId = R.id.villeFragment
        suppressNavListener = false
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

        val innerShift = dpToPx(24).toFloat()
        val outerShift = dpToPx(6).toFloat()

        for (i in 0 until itemCount) {
            val targetX = if (spread) {
                when (i) {
                    0 -> -outerShift
                    1 -> -innerShift
                    2 -> innerShift
                    3 -> outerShift
                    else -> 0f
                }
            } else 0f

            menuView.getChildAt(i).animate()
                .translationX(targetX)
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun dpToPx(dp: Int) =
        (dp * resources.displayMetrics.density).toInt()

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