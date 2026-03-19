package com.madinatti.app

import android.view.View
import android.widget.LinearLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions

object ShortcutCardsHelper {

    fun setup(
        root: View,
        navController: NavController,
        currentScreen: String,
        particleView: ParticleView
    ) {
        val cards = listOf(
            Triple(root.findViewById<LinearLayout>(R.id.shortcutMarketplace),
                "marketplace", R.id.marketplaceFragment),
            Triple(root.findViewById<LinearLayout>(R.id.shortcutPrieres),
                "prieres", R.id.prieresFragment),
            Triple(root.findViewById<LinearLayout>(R.id.shortcutMeteo),
                "meteo", R.id.meteoFragment),
            Triple(root.findViewById<LinearLayout>(R.id.shortcutEvenements),
                "evenements", R.id.evenementsFragment)
        )

        val selectedBg = mapOf(
            "marketplace" to R.drawable.bg_card_marketplace_selected,
            "prieres" to R.drawable.bg_card_prieres_selected,
            "meteo" to R.drawable.bg_card_meteo_selected,
            "evenements" to R.drawable.bg_card_evenements_selected
        )
        val normalBg = mapOf(
            "marketplace" to R.drawable.bg_card_marketplace,
            "prieres" to R.drawable.bg_card_prieres,
            "meteo" to R.drawable.bg_card_meteo,
            "evenements" to R.drawable.bg_card_evenements
        )

        cards.forEach { (card, key, destId) ->
            card?.setBackgroundResource(
                if (key == currentScreen) selectedBg[key]!!
                else normalBg[key]!!
            )
            card?.setOnClickListener {
                if (key == currentScreen) return@setOnClickListener
                particleView.triggerRippleFromView(card)
                navController.navigate(
                    destId,
                    null,
                    NavOptions.Builder()
                        .setEnterAnim(android.R.anim.fade_in)
                        .setExitAnim(android.R.anim.fade_out)
                        .setPopUpTo(R.id.homeFragment, false)
                        .setLaunchSingleTop(true)
                        .build()
                )
            }
        }
    }
}