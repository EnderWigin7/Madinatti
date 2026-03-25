package com.madinatti.app

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.madinatti.app.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private var passwordVisible = false
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        listOf(binding.etEmail, binding.etPassword).forEach { field ->
            field.setOnFocusChangeListener { _, hasFocus ->
                field.setBackgroundResource(
                    if (hasFocus) R.drawable.bg_input_focused
                    else R.drawable.bg_input
                )
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

        // Forgot password — REAL Firebase reset
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.etEmail.error = "Entrez votre email d'abord"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            binding.tvForgotPassword.isEnabled = false
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "📧 Email de réinitialisation envoyé à $email",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.tvForgotPassword.isEnabled = true
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        getFirebaseErrorMessage(e),
                        Toast.LENGTH_LONG
                    ).show()
                    binding.tvForgotPassword.isEnabled = true
                }
        }
    }

    fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Validation
        if (email.isEmpty()) {
            binding.etEmail.error = "Email requis"
            binding.etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Mot de passe requis"
            binding.etPassword.requestFocus()
            return
        }
        if (password.length < 6) {
            binding.etPassword.error = "Minimum 6 caractères"
            binding.etPassword.requestFocus()
            return
        }

        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                setLoading(false)
                navigateToMain()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(
                    requireContext(),
                    getFirebaseErrorMessage(e),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    fun navigateToMain() {
        val prefs = requireContext().getSharedPreferences("madinatti_prefs", 0)
        prefs.edit().putBoolean("has_seen_splash", true).apply()
        startActivity(
            Intent(requireContext(), MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        requireActivity().overridePendingTransition(
            android.R.anim.fade_in, android.R.anim.fade_out
        )
        requireActivity().finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        binding.ivTogglePassword.isEnabled = !loading
        binding.tvForgotPassword.isEnabled = !loading
    }

    private fun getFirebaseErrorMessage(e: Exception): String {
        return when {
            e.message?.contains("no user record") == true ->
                "❌ Aucun compte trouvé avec cet email"
            e.message?.contains("password is invalid") == true ->
                "❌ Mot de passe incorrect"
            e.message?.contains("badly formatted") == true ->
                "❌ Format d'email invalide"
            e.message?.contains("network error") == true ->
                "❌ Pas de connexion internet"
            e.message?.contains("too many requests") == true ->
                "⏳ Trop de tentatives. Réessayez plus tard"
            e.message?.contains("user has been disabled") == true ->
                "🚫 Ce compte a été désactivé"
            else -> "❌ Erreur: ${e.localizedMessage}"
        }
    }

    private fun animateButton(view: TextView, onEnd: () -> Unit) {
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(150)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2f))
                    .withEndAction { onEnd() }.start()
            }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}