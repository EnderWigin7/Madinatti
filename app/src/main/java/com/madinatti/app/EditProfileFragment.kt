package com.madinatti.app

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.madinatti.app.databinding.FragmentEditProfileBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedCity = ""

    // ✅ MUST be at class level, not inside a function
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { uploadProfilePhoto(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.editProfileTopBar) { _, insets ->
            val h = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.statusBarSpacer.layoutParams.height = h
            binding.statusBarSpacer.requestLayout()
            insets
        }

        binding.ivEditBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        loadUserProfile()

        setupPasswordToggle(binding.ivToggleCurrentPw, binding.etCurrentPassword)
        setupPasswordToggle(binding.ivToggleNewPw, binding.etNewPassword)
        setupPasswordToggle(binding.ivToggleConfirmPw, binding.etConfirmPassword)

        binding.cityPicker.setOnClickListener {
            CityPickerBottomSheet.newInstance { city ->
                selectedCity = city
                binding.tvSelectedCity.text = city
            }.show(parentFragmentManager, "cityPicker")
        }

        binding.tvSave.setOnClickListener {
            (requireActivity() as? MainActivity)?.binding?.particleView?.triggerRippleFromView(binding.tvSave)
            saveProfile()
        }

        binding.btnChangePhoto.setOnClickListener {
            pickImage.launch("image/*")
        }
        binding.tvChangePhotoLabel.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnDeleteAccount.setOnClickListener {
            confirmDeleteAccount()
        }

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

    private fun uploadProfilePhoto(uri: android.net.Uri) {
        val user = auth.currentUser ?: return
        binding.tvSave.isEnabled = false
        Toast.makeText(requireContext(), "⏳ Upload photo...", Toast.LENGTH_SHORT).show()

        binding.tvAvatarFallback.visibility = View.GONE
        binding.ivEditAvatar.visibility = View.VISIBLE
        binding.ivEditAvatar.setImageURI(uri)

        Thread {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@Thread
                inputStream.close()

                val boundary = "----Boundary${System.currentTimeMillis()}"
                val url = URL("https://api.cloudinary.com/v1_1/${PostAdFragment.CLOUDINARY_CLOUD}/image/upload")
                val conn = url.openConnection() as HttpURLConnection
                conn.doOutput = true
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                val output = conn.outputStream
                output.write("--$boundary\r\nContent-Disposition: form-data; name=\"upload_preset\"\r\n\r\n${PostAdFragment.CLOUDINARY_PRESET}\r\n".toByteArray())
                output.write("--$boundary\r\nContent-Disposition: form-data; name=\"folder\"\r\n\r\navatars\r\n".toByteArray())
                output.write("--$boundary\r\nContent-Disposition: form-data; name=\"public_id\"\r\n\r\navatar_${user.uid}_${System.currentTimeMillis()}\r\n".toByteArray())

                output.write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"avatar.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray())
                output.write(bytes)
                output.write("\r\n--$boundary--\r\n".toByteArray())
                output.flush()

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val avatarUrl = json.getString("secure_url")

                activity?.runOnUiThread {
                    if (_binding == null || !isAdded) return@runOnUiThread
                    db.collection("users").document(user.uid)
                        .update("avatarUrl", avatarUrl)
                        .addOnSuccessListener {
                            binding.tvSave.isEnabled = true
                            Toast.makeText(requireContext(), "✅ Photo mise à jour!", Toast.LENGTH_SHORT).show()
                            Glide.with(this)
                                .load(avatarUrl)
                                .circleCrop()
                                .into(binding.ivEditAvatar)
                        }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread
                    binding.tvSave.isEnabled = true
                    Toast.makeText(requireContext(), "❌ Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
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
        binding.etEmail.setText(user.email ?: "")

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (_binding == null || !isAdded) return@addOnSuccessListener
                if (doc.exists()) {
                    binding.etNomComplet.setText(doc.getString("name") ?: "")
                    binding.etTelephone.setText(doc.getString("phone") ?: "")
                    binding.etBio.setText(doc.getString("bio") ?: "")

                    val age = doc.getLong("age")
                    if (age != null && age > 0) binding.etAge.setText(age.toString())

                    val city = doc.getString("city") ?: ""
                    if (city.isNotEmpty()) {
                        selectedCity = city
                        binding.tvSelectedCity.text = city
                    }

                    val avatarUrl = doc.getString("avatarUrl") ?: ""
                    if (avatarUrl.isNotEmpty()) {
                        binding.tvAvatarFallback.visibility = View.GONE
                        binding.ivEditAvatar.visibility = View.VISIBLE
                        Glide.with(this)
                            .load(avatarUrl)
                            .circleCrop()
                            .into(binding.ivEditAvatar)
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

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "bio" to bio,
            "city" to selectedCity
        )
        if (age != null) updates["age"] = age

        binding.tvSave.isEnabled = false
        binding.tvSave.text = "..."

        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(name).build()
        )

        db.collection("users").document(user.uid)
            .update(updates)
            .addOnSuccessListener {
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
                Toast.makeText(requireContext(), "❌ ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun changePassword(currentPw: String, newPw: String, confirmPw: String) {
        val user = auth.currentUser ?: return
        if (currentPw.isEmpty()) {
            binding.etCurrentPassword.error = "Mot de passe actuel requis"
            resetSaveButton(); return
        }
        if (newPw.length < 6) {
            binding.etNewPassword.error = "Minimum 6 caractères"
            resetSaveButton(); return
        }
        if (newPw != confirmPw) {
            binding.etConfirmPassword.error = "Ne correspondent pas"
            resetSaveButton(); return
        }
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPw)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPw)
                    .addOnSuccessListener {
                        resetSaveButton()
                        Toast.makeText(requireContext(), "✅ Mot de passe mis à jour!", Toast.LENGTH_SHORT).show()
                        binding.etCurrentPassword.text.clear()
                        binding.etNewPassword.text.clear()
                        binding.etConfirmPassword.text.clear()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                    .addOnFailureListener { e ->
                        resetSaveButton()
                        Toast.makeText(requireContext(), "❌ ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                resetSaveButton()
                binding.etCurrentPassword.error = "Mot de passe actuel incorrect"
            }
    }

    private fun confirmDeleteAccount() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Supprimer le compte")
            .setMessage("Cette action est irréversible.")
            .setPositiveButton("Supprimer") { _, _ -> deleteAccount() }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).delete().addOnCompleteListener {
            user.delete().addOnSuccessListener {
                Toast.makeText(requireContext(), "Compte supprimé", Toast.LENGTH_SHORT).show()
                requireContext().getSharedPreferences("madinatti_prefs", 0).edit().clear().apply()
                startActivity(
                    android.content.Intent(requireActivity(), AuthActivity::class.java)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
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