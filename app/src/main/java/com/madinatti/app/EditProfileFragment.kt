package com.madinatti.app

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.madinatti.app.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedCity = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.editProfileTopBar) { _, insets ->
            val h = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.statusBarSpacer.layoutParams.height = h
            binding.statusBarSpacer.requestLayout()
            insets
        }

        // Back button
        binding.ivEditBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Load current user data from Firestore
        loadUserProfile()

        // Password eye toggles
        setupPasswordToggle(binding.ivToggleCurrentPw, binding.etCurrentPassword)
        setupPasswordToggle(binding.ivToggleNewPw, binding.etNewPassword)
        setupPasswordToggle(binding.ivToggleConfirmPw, binding.etConfirmPassword)

        // City picker — uses your existing bottom sheet style
        binding.cityPicker.setOnClickListener {
            CityPickerBottomSheet.newInstance { city ->
                selectedCity = city
                binding.tvSelectedCity.text = city
            }.show(parentFragmentManager, "cityPicker")
        }

        // Save button
        binding.tvSave.setOnClickListener {
            saveProfile()
        }

        // Change photo
        binding.btnChangePhoto.setOnClickListener {
            // TODO: Cloudinary upload — will add in Phase 2C
            Toast.makeText(requireContext(), "📸 Upload photo — bientôt disponible", Toast.LENGTH_SHORT).show()
        }
        binding.tvChangePhotoLabel.setOnClickListener {
            binding.btnChangePhoto.performClick()
        }

        // Delete account
        binding.btnDeleteAccount.setOnClickListener {
            confirmDeleteAccount()
        }

        // Focus styling
        listOf(
            binding.etNomComplet, binding.etTelephone, binding.etAge,
            binding.etBio, binding.etCurrentPassword, binding.etNewPassword,
            binding.etConfirmPassword
        ).forEach { field ->
            field.setOnFocusChangeListener { _, hasFocus ->
                field.setBackgroundResource(
                    if (hasFocus) R.drawable.bg_input_focused else R.drawable.bg_input
                )
            }
        }
    }

    private fun setupPasswordToggle(toggleIcon: ImageView, editText: android.widget.EditText) {
        var visible = false
        toggleIcon.setOnClickListener {
            visible = !visible
            editText.transformationMethod = if (visible)
                HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
            editText.setSelection(editText.text.length)
            toggleIcon.setImageResource(
                if (visible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
            )
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return

        // Set email (read-only)
        binding.etEmail.setText(user.email ?: "")

        // Load from Firestore
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.etNomComplet.setText(doc.getString("name") ?: "")
                    binding.etTelephone.setText(doc.getString("phone") ?: "")
                    binding.etBio.setText(doc.getString("bio") ?: "")

                    val age = doc.getLong("age")
                    if (age != null && age > 0) {
                        binding.etAge.setText(age.toString())
                    }

                    val city = doc.getString("city") ?: ""
                    if (city.isNotEmpty()) {
                        selectedCity = city
                        binding.tvSelectedCity.text = city
                    }

                    // Avatar
                    val avatarUrl = doc.getString("avatarUrl") ?: ""
                    if (avatarUrl.isNotEmpty()) {
                        binding.tvAvatarFallback.visibility = View.GONE
                        binding.ivEditAvatar.visibility = View.VISIBLE
                        // Load with Glide when Cloudinary is set up
                        // Glide.with(this).load(avatarUrl).circleCrop().into(binding.ivEditAvatar)
                    }
                }
            }
    }

    private fun saveProfile() {
        val user = auth.currentUser ?: return

        val name = binding.etNomComplet.text.toString().trim()
        val phone = binding.etTelephone.text.toString().trim()
        val ageStr = binding.etAge.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()
        val currentPw = binding.etCurrentPassword.text.toString()
        val newPw = binding.etNewPassword.text.toString()
        val confirmPw = binding.etConfirmPassword.text.toString()

        // Validation
        if (name.isEmpty()) {
            binding.etNomComplet.error = "Nom requis"
            binding.etNomComplet.requestFocus()
            return
        }

        val age = ageStr.toIntOrNull()
        if (ageStr.isNotEmpty() && (age == null || age < 13 || age > 120)) {
            binding.etAge.error = "Âge invalide (13-120)"
            binding.etAge.requestFocus()
            return
        }

        // ── 1. Update Firestore profile ──
        val updates = hashMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "bio" to bio,
            "city" to selectedCity
        )
        if (age != null) updates["age"] = age

        binding.tvSave.isEnabled = false
        binding.tvSave.text = "..."

        // Update Firebase Auth display name
        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
        )

        db.collection("users").document(user.uid)
            .update(updates)
            .addOnSuccessListener {
                // ── 2. Change password if requested ──
                if (newPw.isNotEmpty()) {
                    changePassword(currentPw, newPw, confirmPw)
                } else {
                    binding.tvSave.isEnabled = true
                    binding.tvSave.text = "Enregistrer"
                    Toast.makeText(requireContext(), "✅ Profil mis à jour!", Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
            .addOnFailureListener { e ->
                binding.tvSave.isEnabled = true
                binding.tvSave.text = "Enregistrer"
                Toast.makeText(requireContext(), "❌ Erreur: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun changePassword(currentPw: String, newPw: String, confirmPw: String) {
        val user = auth.currentUser ?: return

        if (currentPw.isEmpty()) {
            binding.etCurrentPassword.error = "Mot de passe actuel requis"
            binding.etCurrentPassword.requestFocus()
            resetSaveButton()
            return
        }
        if (newPw.length < 6) {
            binding.etNewPassword.error = "Minimum 6 caractères"
            binding.etNewPassword.requestFocus()
            resetSaveButton()
            return
        }
        if (newPw != confirmPw) {
            binding.etConfirmPassword.error = "Ne correspondent pas"
            binding.etConfirmPassword.requestFocus()
            resetSaveButton()
            return
        }

        // Re-authenticate first
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPw)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPw)
                    .addOnSuccessListener {
                        resetSaveButton()
                        Toast.makeText(requireContext(), "✅ Profil et mot de passe mis à jour!", Toast.LENGTH_SHORT).show()
                        // Clear password fields
                        binding.etCurrentPassword.text.clear()
                        binding.etNewPassword.text.clear()
                        binding.etConfirmPassword.text.clear()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                    .addOnFailureListener { e ->
                        resetSaveButton()
                        Toast.makeText(requireContext(), "❌ Erreur mot de passe: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                resetSaveButton()
                binding.etCurrentPassword.error = "Mot de passe actuel incorrect"
                binding.etCurrentPassword.requestFocus()
            }
    }

    private fun confirmDeleteAccount() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Supprimer le compte")
            .setMessage("Cette action est irréversible. Toutes vos annonces et données seront supprimées.")
            .setPositiveButton("Supprimer") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser ?: return

        // Delete Firestore profile
        db.collection("users").document(user.uid)
            .delete()
            .addOnCompleteListener {
                // Delete Firebase Auth account
                user.delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Compte supprimé", Toast.LENGTH_SHORT).show()
                        requireContext().getSharedPreferences("madinatti_prefs", 0)
                            .edit().clear().apply()
                        startActivity(
                            android.content.Intent(requireActivity(), AuthActivity::class.java)
                                .addFlags(
                                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                )
                        )
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "❌ Erreur. Reconnectez-vous et réessayez.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
    }

    private fun resetSaveButton() {
        binding.tvSave.isEnabled = true
        binding.tvSave.text = "Enregistrer"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}