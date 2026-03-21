package com.madinatti.app

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AdDetailBottomSheet : BottomSheetDialogFragment() {

    private var isSaved = false
    private var currentImageIndex = 0
    private val totalImages = 4
    private val imageEmojis = listOf("🏺", "🪔", "🎨", "🏺")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_ad_detail_bottom_sheet, container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivSave = view.findViewById<ImageView>(R.id.ivSaveAd)
        val ivPrev = view.findViewById<ImageView>(R.id.ivPrevImage)
        val ivNext = view.findViewById<ImageView>(R.id.ivNextImage)
        val tvImageEmoji = view.findViewById<TextView>(R.id.tvImageEmoji)
        val tvImageCount = view.findViewById<TextView>(R.id.tvImageCount)
        val btnReport = view.findViewById<View>(R.id.btnReport)

        // Save/bookmark toggle
        ivSave?.setOnClickListener {
            isSaved = !isSaved
            ivSave.setImageResource(
                if (isSaved) R.drawable.ic_bookmark_filled
                else R.drawable.ic_bookmark_outline
            )
            ivSave.animate()
                .scaleX(1.2f).scaleY(1.2f).setDuration(100)
                .withEndAction {
                    ivSave.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(100).start()
                }.start()
        }

        // Image navigation
        ivPrev?.setOnClickListener {
            if (currentImageIndex > 0) {
                currentImageIndex--
                updateImageDisplay(tvImageEmoji, tvImageCount)
            }
        }

        ivNext?.setOnClickListener {
            if (currentImageIndex < totalImages - 1) {
                currentImageIndex++
                updateImageDisplay(tvImageEmoji, tvImageCount)
            }
        }

        // Report button
        btnReport?.setOnClickListener {
            // TODO: Show report dialog
            android.widget.Toast.makeText(
                requireContext(),
                "Signalement envoyé",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        updateImageDisplay(tvImageEmoji, tvImageCount)
    }

    private fun updateImageDisplay(emojiView: TextView?, countView: TextView?) {
        emojiView?.let {
            it.alpha = 0f
            it.text = imageEmojis.getOrElse(currentImageIndex) { "🏺" }
            it.animate().alpha(1f).setDuration(200).start()
        }
        countView?.text = "${currentImageIndex + 1}/$totalImages 📷"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                )

            bottomSheet?.let {
                it.setBackgroundResource(R.drawable.bg_bottom_sheet_glass)

                val behavior = BottomSheetBehavior.from(it)
                val screenHeight = resources.displayMetrics.heightPixels
                behavior.peekHeight = (screenHeight * 0.85).toInt()
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        dialog.window?.apply {
            setDimAmount(0.3f)
            addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }

        return dialog
    }

    override fun getTheme(): Int = R.style.GlassBottomSheetDialog

    companion object {
        const val TAG = "AdDetailBottomSheet"
    }
}