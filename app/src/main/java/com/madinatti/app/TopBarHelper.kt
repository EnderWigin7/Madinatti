package com.madinatti.app

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.madinatti.app.databinding.LayoutTopbarBinding

object TopBarHelper {

    fun setup(
        topBarBinding: LayoutTopbarBinding,
        showBackButton: Boolean,
        onBack: (() -> Unit)? = null
    ) {

        ViewCompat.setOnApplyWindowInsetsListener(topBarBinding.topBar) { _, insets ->
            val statusBarHeight = insets.getInsets(
                WindowInsetsCompat.Type.statusBars()).top
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
    }
}