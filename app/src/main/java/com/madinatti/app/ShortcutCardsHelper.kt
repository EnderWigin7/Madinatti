package com.madinatti.app

import android.os.Bundle
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
        val cards: List<Triple<LinearLayout?, String, String>> = listOf(
            Triple(root.findViewById(R.id.shortcutMarketplace),
                "marketplace", "marketplace"),
            Triple(root.findViewById(R.id.shortcutPrieres),
                "prieres", "prieres"),
            Triple(root.findViewById(R.id.shortcutMeteo),
                "meteo", "meteo"),
            Triple(root.findViewById(R.id.shortcutEvenements),
                "evenements", "evenements")
        )

        val selectedBg = mapOf(
            "marketplace" to R.drawable.bg_card_marketplace_selected,
            "prieres"     to R.drawable.bg_card_prieres_selected,
            "meteo"       to R.drawable.bg_card_meteo_selected,
            "evenements"  to R.drawable.bg_card_evenements_selected
        )
        val normalBg = mapOf(
            "marketplace" to R.drawable.bg_card_marketplace,
            "prieres"     to R.drawable.bg_card_prieres,
            "meteo"       to R.drawable.bg_card_meteo,
            "evenements"  to R.drawable.bg_card_evenements
        )

        cards.forEach { (card, key, tabArg) ->
            card?.setBackgroundResource(
                if (key == currentScreen) selectedBg[key]!!
                else normalBg[key]!!
            )
            card?.setOnClickListener {
                if (key == currentScreen) return@setOnClickListener
                particleView.triggerRippleFromView(card)

                val bundle = Bundle().apply {
                    putString("selectedTab", tabArg)
                }

                navController.navigate(
                    R.id.villeFragment,
                    bundle,
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