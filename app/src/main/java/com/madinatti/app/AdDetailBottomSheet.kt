package com.madinatti.app

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AdDetailBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ad_detail_bottom_sheet, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheet?.let {
                // Glassy background
                it.setBackgroundResource(R.drawable.bg_bottom_sheet_glass)

                // Expand to 85% of screen
                val behavior = BottomSheetBehavior.from(it)
                val screenHeight = resources.displayMetrics.heightPixels
                behavior.peekHeight = (screenHeight * 0.85).toInt()
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        // Make the dim behind more subtle
        dialog.window?.apply {
            setDimAmount(0.4f)
            addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }

        return dialog
    }

    override fun getTheme(): Int = R.style.GlassBottomSheetDialog

    companion object {
        const val TAG = "AdDetailBottomSheet"
    }
}