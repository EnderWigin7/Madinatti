package com.madinatti.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.madinatti.app.databinding.FragmentProfileBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        TopBarHelper.setup(
            binding.topBarInclude,
            showBackButton = false,
            fragmentManager = parentFragmentManager
        )

        binding.topBarInclude.citySelector.setOnClickListener {
            CityPickerBottomSheet.newInstance { city ->
                requireContext().getSharedPreferences("madinatti_prefs", 0)
                    .edit().putString("selected_city", city).apply()
                Toast.makeText(requireContext(), "📍 Ville: $city", Toast.LENGTH_SHORT).show()
            }.show(parentFragmentManager, "cityPicker")
        }

        binding.topBarInclude.ivNotifications.setOnClickListener {
            NotificationsBottomSheet.newInstance()
                .show(parentFragmentManager, "notifications")
        }

        loadProfile()

        // Setup menu
        setupMenuRows()
    }

    override fun onResume() {
        super.onResume()

        loadProfile()
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        val prefs = requireContext().getSharedPreferences("user_cache", 0)

        // INSTANT: Load cached data first (no flicker)
        binding.tvUserName.text = prefs.getString("name", user.displayName ?: "")
        binding.tvUserEmail.text = user.email ?: ""
        binding.tvUserCity.text = prefs.getString("city", "")
        binding.tvMemberSince.text = prefs.getString("memberSince", "")
        binding.tvAnnoncesCount.text = prefs.getString("adsCount", "0")

        // THEN: Load fresh from Firestore (updates silently)
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                if (doc.exists()) {
                    val name = doc.getString("name") ?: ""
                    val city = doc.getString("city") ?: ""
                    val adsCount = (doc.getLong("adsCount") ?: 0).toString()

                    binding.tvUserName.text = name
                    binding.tvUserCity.text = city
                    binding.tvAnnoncesCount.text = adsCount

                    val createdAt = doc.getTimestamp("createdAt")
                    val memberSince = if (createdAt != null) {
                        "Membre depuis ${java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(createdAt.toDate())}"
                    } else ""
                    binding.tvMemberSince.text = memberSince

                    prefs.edit()
                        .putString("name", name)
                        .putString("city", city)
                        .putString("adsCount", adsCount)
                        .putString("memberSince", memberSince)
                        .apply()

                    val avatarUrl = doc.getString("avatarUrl") ?: ""
                    if (avatarUrl.isNotEmpty()) {
                        binding.tvAvatarFallback.visibility = View.GONE
                        binding.ivAvatar.visibility = View.VISIBLE
                    } else {
                        binding.tvAvatarFallback.visibility = View.VISIBLE
                        binding.ivAvatar.visibility = View.GONE
                    }
                }
            }

        loadRealCounts(user.uid)
    }

    private fun loadRealCounts(uid: String) {
        // Count user's ads
        db.collection("ads")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener
                binding.tvAnnoncesCount.text = result.size().toString()
            }

        // Favorites count — from user subcollection
        db.collection("users").document(uid)
            .collection("favorites")
            .get()
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener
                binding.tvFavorisCount.text = result.size().toString()
            }

        // Messages count (unread)
        db.collection("chats")
            .whereArrayContains("participants", uid)
            .get()
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener
                // For now just show total chats
                binding.tvMessagesCount.text = result.size().toString()
            }
    }

    private fun setupMenuRows() {
        binding.rowMesAnnonces.setOnClickListener {
            triggerRipple(binding.rowMesAnnonces)
            androidx.navigation.Navigation
                .findNavController(requireActivity(), R.id.navHostFragment)
                .navigate(R.id.mesAnnoncesFragment)
        }

        binding.rowFavoris.setOnClickListener {
            triggerRipple(binding.rowFavoris)
            Toast.makeText(requireContext(), "Favoris — bientôt disponible", Toast.LENGTH_SHORT).show()
        }

        binding.rowMessages.setOnClickListener {
            triggerRipple(binding.rowMessages)
            (requireActivity() as? MainActivity)?.binding?.bottomNav
                ?.selectedItemId = R.id.messagesFragment
        }

        binding.rowNotifications.setOnClickListener {
            triggerRipple(binding.rowNotifications)
            NotificationsBottomSheet.newInstance()
                .show(parentFragmentManager, "notifications")
        }

        binding.rowParametres.setOnClickListener {
            triggerRipple(binding.rowParametres)
            Toast.makeText(requireContext(), "Paramètres — bientôt disponible", Toast.LENGTH_SHORT).show()
        }

        binding.rowLangue.setOnClickListener {
            triggerRipple(binding.rowLangue)
            startActivity(Intent(requireContext(), LanguageActivity::class.java))
        }

        binding.btnEditAvatar.setOnClickListener {
            Toast.makeText(requireContext(), "📸 Upload photo — bientôt disponible", Toast.LENGTH_SHORT).show()
        }

        binding.tvCustomizeProfile.setOnClickListener {
            androidx.navigation.Navigation
                .findNavController(requireActivity(), R.id.navHostFragment)
                .navigate(R.id.editProfileFragment)
        }

        binding.btnLogout.setOnClickListener {
            triggerRipple(binding.btnLogout)

            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()

            com.google.android.gms.auth.api.signin.GoogleSignIn
                .getClient(
                    requireContext(),
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions
                        .Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .build()
                ).signOut()

            requireContext().getSharedPreferences("madinatti_prefs", 0)
                .edit().clear().apply()

            startActivity(
                Intent(requireActivity(), AuthActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    private fun triggerRipple(view: View) {
        (requireActivity() as? MainActivity)?.binding?.particleView
            ?.triggerRippleFromView(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}