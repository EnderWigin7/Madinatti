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
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.madinatti.app.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedCity = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Focus styling
        listOf(
            binding.etNom, binding.etEmail, binding.etPhone,
            binding.etAge, binding.etPassword, binding.etConfirmPassword
        ).forEach { field ->
            field.setOnFocusChangeListener { _, hasFocus ->
                field.setBackgroundResource(
                    if (hasFocus) R.drawable.bg_input_focused else R.drawable.bg_input
                )
            }
        }

        // Password toggles
        var passwordVisible = false
        var confirmPasswordVisible = false

        binding.ivTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            binding.etPassword.transformationMethod = if (passwordVisible)
                HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
            binding.etPassword.setSelection(binding.etPassword.text.length)
            binding.ivTogglePassword.setImageResource(
                if (passwordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
            )
        }

        binding.ivToggleConfirmPassword.setOnClickListener {
            confirmPasswordVisible = !confirmPasswordVisible
            binding.etConfirmPassword.transformationMethod = if (confirmPasswordVisible)
                HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text.length)
            binding.ivToggleConfirmPassword.setImageResource(
                if (confirmPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
            )
        }

        // City picker — BOTTOM SHEET
        binding.cityContainer.setOnClickListener {
            CityPickerBottomSheet.newInstance { city ->
                selectedCity = city
                binding.tvCitySelector.text = city
                binding.tvCitySelector.setTextColor(0xFFFFFFFF.toInt())
            }.show(parentFragmentManager, "cityPicker")
        }
    }

    fun attemptRegister() {
        val nom = binding.etNom.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val ageStr = binding.etAge.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        // Validation
        if (selectedCity.isEmpty()) {
            Toast.makeText(requireContext(), "Veuillez choisir une ville", Toast.LENGTH_SHORT).show()
            return
        }
        if (nom.isEmpty()) { binding.etNom.error = "Nom requis"; binding.etNom.requestFocus(); return }
        if (email.isEmpty()) { binding.etEmail.error = "Email requis"; binding.etEmail.requestFocus(); return }
        if (phone.isEmpty()) { binding.etPhone.error = "Téléphone requis"; binding.etPhone.requestFocus(); return }
        if (ageStr.isEmpty()) { binding.etAge.error = "Âge requis"; binding.etAge.requestFocus(); return }

        val age = ageStr.toIntOrNull()
        if (age == null || age < 13 || age > 120) {
            binding.etAge.error = "Âge invalide (13-120)"
            binding.etAge.requestFocus(); return
        }

        if (password.isEmpty()) { binding.etPassword.error = "Mot de passe requis"; binding.etPassword.requestFocus(); return }
        if (password.length < 6) { binding.etPassword.error = "Minimum 6 caractères"; binding.etPassword.requestFocus(); return }
        if (password != confirm) {
            binding.etConfirmPassword.error = "Les mots de passe ne correspondent pas"
            binding.etConfirmPassword.requestFocus(); return
        }

        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user!!

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(nom)
                    .build()

                user.updateProfile(profileUpdates).addOnCompleteListener {
                    val userProfile = hashMapOf(
                        "uid" to user.uid,
                        "name" to nom,
                        "email" to email,
                        "phone" to phone,
                        "age" to age,
                        "city" to selectedCity,
                        "avatarUrl" to "",
                        "bio" to "",
                        "createdAt" to com.google.firebase.Timestamp.now(),
                        "adsCount" to 0,
                        "rating" to 0.0,
                        "isVerified" to false,
                        "authProvider" to "email"
                    )

                    db.collection("users").document(user.uid)
                        .set(userProfile)
                        .addOnSuccessListener {
                            setLoading(false)
                            Toast.makeText(requireContext(), "✅ Compte créé!", Toast.LENGTH_SHORT).show()
                            navigateToMain()
                        }
                        .addOnFailureListener {
                            setLoading(false)
                            navigateToMain()
                        }
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                val msg = when {
                    e.message?.contains("email address is already") == true -> "❌ Email déjà utilisé"
                    e.message?.contains("badly formatted") == true -> "❌ Email invalide"
                    e.message?.contains("weak password") == true -> "❌ Mot de passe trop faible"
                    e.message?.contains("network error") == true -> "❌ Pas de connexion internet"
                    else -> "❌ Erreur: ${e.localizedMessage}"
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToMain() {
        val prefs = requireContext().getSharedPreferences("madinatti_prefs", 0)
        prefs.edit().putBoolean("has_seen_splash", true).apply()
        startActivity(
            Intent(requireContext(), MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        requireActivity().finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.etNom.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPhone.isEnabled = !loading
        binding.etAge.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        binding.etConfirmPassword.isEnabled = !loading
        binding.cityContainer.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}