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
import android.widget.LinearLayout

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

        setupMenuRows()
    }

    override fun onResume() {
        super.onResume()

        loadProfile()
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        val prefs = requireContext().getSharedPreferences("user_cache", 0)

        binding.tvUserName.text = prefs.getString("name", user.displayName ?: "")
        binding.tvUserEmail.text = user.email ?: ""
        binding.tvUserCity.text = prefs.getString("city", "")
        binding.tvMemberSince.text = prefs.getString("memberSince", "")
        binding.tvAnnoncesCount.text = prefs.getString("adsCount", "0")

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
                        com.bumptech.glide.Glide.with(this)
                            .load(avatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.bg_avatar_circle)
                            .into(binding.ivAvatar)
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

        db.collection("chats")
            .whereArrayContains("participants", uid)
            .get()
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener
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
            androidx.navigation.Navigation
                .findNavController(requireActivity(), R.id.navHostFragment)
                .navigate(R.id.favoritesFragment)
        }

        binding.rowMessages.setOnClickListener {
            triggerRipple(binding.rowMessages)
            (requireActivity() as? MainActivity)?.binding?.bottomNav
                ?.selectedItemId = R.id.messagesFragment
        }


        binding.rowParametres.setOnClickListener {
            triggerRipple(binding.rowParametres)
            showSettingsSheet()
        }


        binding.btnEditAvatar.setOnClickListener {
            androidx.navigation.Navigation
                .findNavController(requireActivity(), R.id.navHostFragment)
                .navigate(R.id.editProfileFragment)
        }

        binding.tvCustomizeProfile.setOnClickListener {
            androidx.navigation.Navigation
                .findNavController(requireActivity(), R.id.navHostFragment)
                .navigate(R.id.editProfileFragment)
        }

        binding.btnLogout.setOnClickListener {
            triggerRipple(binding.btnLogout)
            (requireActivity() as? MainActivity)?.performLogout()
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

    private fun showSettingsSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(
            requireContext(), R.style.GlassBottomSheetDialog
        )
        val dp = resources.displayMetrics.density
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (16 * dp).toInt(), 0, (32 * dp).toInt())
            setBackgroundResource(R.drawable.bg_bottom_sheet_glass)
        }

        // Drag handle
        root.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams((40 * dp).toInt(), (4 * dp).toInt()).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                bottomMargin = (16 * dp).toInt()
            }
            setBackgroundResource(R.drawable.bg_drag_handle)
        })

        // Title
        root.addView(android.widget.TextView(requireContext()).apply {
            text = "⚙️ Paramètres"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 18f
            typeface = resources.getFont(R.font.poppins_bold)
            setPadding((24 * dp).toInt(), 0, (24 * dp).toInt(), (16 * dp).toInt())
        })

        // Settings items
        val items = listOf(
            Triple("🔔", "Notifications", "Gérer les alertes"),
            Triple("🔒", "Confidentialité", "Données & sécurité"),
            Triple("🎨", "Apparence", "Thème sombre activé"),
            Triple("💾", "Stockage & cache", "Vider le cache"),
            Triple("ℹ️", "À propos", "Madinatti v1.0"),
            Triple("📋", "Conditions d'utilisation", ""),
            Triple("🛡️", "Politique de confidentialité", "")
        )

        items.forEach { (emoji, title, subtitle) ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding((24 * dp).toInt(), (14 * dp).toInt(), (24 * dp).toInt(), (14 * dp).toInt())
                isClickable = true; isFocusable = true
                val ta = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                foreground = ta.getDrawable(0); ta.recycle()
                setOnClickListener {
                    when (title) {
                        "Notifications" -> {
                            android.widget.Toast.makeText(requireContext(),
                                "🔔 Notifications activées", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        "Stockage & cache" -> {
                            requireContext().cacheDir.deleteRecursively()
                            android.widget.Toast.makeText(requireContext(),
                                "💾 Cache vidé!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        "À propos" -> {
                            android.widget.Toast.makeText(requireContext(),
                                "Madinatti v1.0 — Made with ❤️", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            android.widget.Toast.makeText(requireContext(),
                                "$title — bientôt disponible", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            row.addView(android.widget.TextView(requireContext()).apply {
                text = emoji; textSize = 20f
                layoutParams = LinearLayout.LayoutParams((36 * dp).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
            })

            val textCol = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (8 * dp).toInt()
                }
            }
            textCol.addView(android.widget.TextView(requireContext()).apply {
                text = title
                setTextColor(android.graphics.Color.WHITE)
                textSize = 14f
                typeface = resources.getFont(R.font.poppins_medium)
            })
            if (subtitle.isNotEmpty()) {
                textCol.addView(android.widget.TextView(requireContext()).apply {
                    text = subtitle
                    setTextColor(android.graphics.Color.parseColor("#7FA68A"))
                    textSize = 11f
                    typeface = resources.getFont(R.font.poppins_regular)
                })
            }
            row.addView(textCol)

            row.addView(android.widget.TextView(requireContext()).apply {
                text = "›"; setTextColor(android.graphics.Color.parseColor("#7FA68A")); textSize = 18f
            })

            root.addView(row)

            // Divider
            root.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply {
                    marginStart = (24 * dp).toInt(); marginEnd = (24 * dp).toInt()
                }
                setBackgroundColor(android.graphics.Color.parseColor("#1AFFFFFF"))
            })
        }

        root.addView(android.widget.TextView(requireContext()).apply {
            text = "Madinatti v1.0 · GHM-LABS"
            setTextColor(android.graphics.Color.parseColor("#4D7FA68A"))
            textSize = 10f
            typeface = resources.getFont(R.font.poppins_regular)
            gravity = android.view.Gravity.CENTER
            setPadding(0, (20 * dp).toInt(), 0, 0)
        })

        dialog.setContentView(root)
        dialog.show()
    }

}