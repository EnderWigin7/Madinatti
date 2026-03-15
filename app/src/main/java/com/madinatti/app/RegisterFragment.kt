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
import com.madinatti.app.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listOf(binding.etNom, binding.etEmail, binding.etPhone,
            binding.etPassword, binding.etConfirmPassword).forEach { field ->
            field.setOnFocusChangeListener { _, hasFocus ->
                field.setBackgroundResource(
                    if (hasFocus) R.drawable.bg_input_focused else R.drawable.bg_input)
            }
        }

        binding.cityContainer.setOnClickListener { /* TODO: city picker */ }

        binding.btnRegister.setOnClickListener {
            val nom = binding.etNom.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            if (nom.isEmpty()) { binding.etNom.error = "Requis"; return@setOnClickListener }
            if (email.isEmpty()) { binding.etEmail.error = "Requis"; return@setOnClickListener }
            if (phone.isEmpty()) { binding.etPhone.error = "Requis"; return@setOnClickListener }
            if (password.isEmpty()) { binding.etPassword.error = "Requis"; return@setOnClickListener }
            if (password != confirm) {
                binding.etConfirmPassword.error = "Les mots de passe ne correspondent pas"
                return@setOnClickListener
            }

            animateButton(binding.btnRegister) {
                // TODO: Firebase register
                val prefs = requireContext().getSharedPreferences("madinatti_prefs", 0)
                prefs.edit().putBoolean("has_seen_splash", true).apply()
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                requireActivity().finish()
            }
        }

        var passwordVisible = false
        var confirmPasswordVisible = false

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

        binding.ivToggleConfirmPassword.setOnClickListener {
            confirmPasswordVisible = !confirmPasswordVisible
            binding.etConfirmPassword.transformationMethod = if (confirmPasswordVisible)
                HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text.length)
            binding.ivToggleConfirmPassword.setImageResource(
                if (confirmPasswordVisible) R.drawable.ic_eye_open
                else R.drawable.ic_eye_closed
            )
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