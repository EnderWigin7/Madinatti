package com.madinatti.app

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import com.madinatti.app.databinding.LayoutTopbarBinding

object TopBarHelper {

    var onCityChanged: (() -> Unit)? = null

    fun setup(
        topBarBinding: LayoutTopbarBinding,
        showBackButton: Boolean,
        onBack: (() -> Unit)? = null,
        fragmentManager: FragmentManager? = null
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(topBarBinding.topBar) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            topBarBinding.statusBarSpacer.layoutParams.height = statusBarHeight
            topBarBinding.statusBarSpacer.requestLayout()
            insets
        }

        if (showBackButton) {
            topBarBinding.ivBack.visibility = View.VISIBLE
            topBarBinding.imgLogo.visibility = View.GONE
            topBarBinding.ivBack.setOnClickListener { onBack?.invoke() }
        } else {
            topBarBinding.ivBack.visibility = View.GONE
            topBarBinding.imgLogo.visibility = View.VISIBLE
        }

        val savedCity = topBarBinding.root.context
            .getSharedPreferences("madinatti_prefs", 0)
            .getString("selected_city", null)
        if (savedCity != null) {
            topBarBinding.tvCityName.text = savedCity
        }

        topBarBinding.citySelector.setOnClickListener {
            if (fragmentManager != null) {
                CityPickerBottomSheet.newInstance { city ->
                    topBarBinding.tvCityName.text = city
                    topBarBinding.root.context
                        .getSharedPreferences("madinatti_prefs", 0)
                        .edit().putString("selected_city", city).apply()
                    onCityChanged?.invoke()
                }.show(fragmentManager, "cityPicker")
            }
        }
    }
}