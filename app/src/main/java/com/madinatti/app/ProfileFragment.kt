package com.madinatti.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.madinatti.app.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        TopBarHelper.setup(binding.topBarInclude, showBackButton = false)
        binding.topBarInclude.citySelector.setOnClickListener { }
        binding.topBarInclude.ivNotifications.setOnClickListener { }

        setupMenuRows()
    }

    private fun setupMenuRows() {
        // Mes annonces
        binding.rowMesAnnonces.setOnClickListener {
            triggerRipple(binding.rowMesAnnonces)
            // TODO: navigate to mes annonces
        }

        // Favoris
        binding.rowFavoris.setOnClickListener {
            triggerRipple(binding.rowFavoris)
            // TODO: navigate to favoris
        }

        // Messages — go to messages tab
        binding.rowMessages.setOnClickListener {
            triggerRipple(binding.rowMessages)
            (requireActivity() as? MainActivity)?.binding?.bottomNav
                ?.selectedItemId = R.id.messagesFragment
        }

        // Notifications
        binding.rowNotifications.setOnClickListener {
            triggerRipple(binding.rowNotifications)
            // TODO: notifications
        }

        // Paramètres
        binding.rowParametres.setOnClickListener {
            triggerRipple(binding.rowParametres)
            // TODO: settings
        }

        // Langue
        binding.rowLangue.setOnClickListener {
            triggerRipple(binding.rowLangue)
            // TODO: go back to language screen
        }

        // Changer de ville
        binding.rowChangerVille.setOnClickListener {
            triggerRipple(binding.rowChangerVille)
            // TODO: city picker
        }

        // Edit avatar
        binding.btnEditAvatar.setOnClickListener {
            // TODO: image picker
        }

        binding.tvCustomizeProfile.setOnClickListener {
            // TODO: edit profile screen
        }
        binding.btnLogout.setOnClickListener {
            triggerRipple(binding.btnLogout)

            requireContext().getSharedPreferences("madinatti_prefs", 0)
                .edit().putBoolean("has_seen_splash", false).apply()

            startActivity(Intent(requireActivity(), AuthActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
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