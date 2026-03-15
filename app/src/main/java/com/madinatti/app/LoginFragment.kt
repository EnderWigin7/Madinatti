package com.madinatti.app

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.madinatti.app.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private var passwordVisible = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listOf(binding.etEmail, binding.etPassword).forEach { field ->
            field.setOnFocusChangeListener { _, hasFocus ->
                field.setBackgroundResource(
                    if (hasFocus) R.drawable.bg_input_focused else R.drawable.bg_input)
            }
        }

        binding.ivTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            binding.etPassword.transformationMethod = if (passwordVisible)
                HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
            binding.etPassword.setSelection(binding.etPassword.text.length)

            binding.ivTogglePassword.setImageResource(
                if (passwordVisible) R.drawable.ic_eye_open
                else R.drawable.ic_eye_closed
            )
        }

        binding.tvForgotPassword.setOnClickListener {

            binding.tvForgotPassword.animate()
                .alpha(0.3f).setDuration(80)
                .withEndAction {
                    binding.tvForgotPassword.setTextColor(
                        android.graphics.Color.parseColor("#2ECC71"))
                    binding.tvForgotPassword.animate()
                        .alpha(1f).setDuration(150)
                        .withEndAction {

                            binding.tvForgotPassword.postDelayed({
                                binding.tvForgotPassword.animate()
                                    .alpha(0.7f).setDuration(100)
                                    .withEndAction {
                                        binding.tvForgotPassword.setTextColor(
                                            android.graphics.Color.WHITE)
                                        binding.tvForgotPassword.animate()
                                            .alpha(1f).setDuration(100).start()
                                    }.start()
                            }, 600)
                        }.start()
                }.start()
            // TODO: navigate to forgot password screen
        }

        binding.btnLogin.setOnClickListener {
            (requireActivity() as AuthActivity).triggerParticleRipple(binding.btnLogin)
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (email.isEmpty()) { binding.etEmail.error = "Email requis"; return@setOnClickListener }
            if (password.isEmpty()) { binding.etPassword.error = "Requis"; return@setOnClickListener }

            animateButton(binding.btnLogin) {
                // TODO: Firebase auth
                val prefs = requireContext().getSharedPreferences("madinatti_prefs", 0)
                prefs.edit().putBoolean("has_seen_splash", true).apply()
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                requireActivity().finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun animateButton(view: TextView, onEnd: () -> Unit) {
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(150)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2f))
                    .withEndAction { onEnd() }.start()
            }.start()
    }
}