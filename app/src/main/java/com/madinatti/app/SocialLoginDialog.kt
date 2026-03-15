package com.madinatti.app

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.DialogFragment
import com.madinatti.app.databinding.DialogSocialLoginBinding

class SocialLoginDialog : DialogFragment() {

    private var _binding: DialogSocialLoginBinding? = null
    private val binding get() = _binding!!

    // Y position of social button passed from AuthActivity
    var anchorY: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        _binding = DialogSocialLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Position dialog at bottom, just above the social button
        dialog?.window?.apply {
            val params: WindowManager.LayoutParams = attributes
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.y = anchorY  // offset from bottom
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pop-in from below
        binding.root.translationY = 80f
        binding.root.alpha = 0f
        binding.root.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        binding.btnApple.setOnClickListener {
            animateRow(it) { /* TODO: Apple */ dismiss() }
        }
        binding.btnFacebook.setOnClickListener {
            animateRow(it) { /* TODO: Facebook */ dismiss() }
        }
        binding.btnGoogle.setOnClickListener {
            animateRow(it) { /* TODO: Google */ dismiss() }
        }
        binding.btnPhone.setOnClickListener {
            animateRow(it) { /* TODO: Phone */ dismiss() }
        }

        dialog?.setCanceledOnTouchOutside(true)
    }

    private fun animateRow(view: View, onEnd: () -> Unit) {
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(100)
                    .withEndAction { onEnd() }.start()
            }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SocialLoginDialog"
    }
}