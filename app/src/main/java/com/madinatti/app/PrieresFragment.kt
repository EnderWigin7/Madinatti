package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.madinatti.app.databinding.FragmentPrieresBinding

// PrieresFragment.kt
class PrieresFragment : Fragment() {
    private var _binding: FragmentPrieresBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPrieresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        TopBarHelper.setup(
            topBarBinding = binding.topBarInclude,
            showBackButton = true,
            onBack = {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        )

        val navController = androidx.navigation.Navigation
            .findNavController(requireActivity(), R.id.navHostFragment)
        val particleView = (requireActivity() as MainActivity).binding.particleView
        ShortcutCardsHelper.setup(binding.root, navController, "prieres", particleView)
        applyStatusBarSpacer()
    }

    private fun applyStatusBarSpacer() {
        val h = requireContext().getSharedPreferences("ui_prefs", 0)
            .getInt("status_bar_height", 0)
        binding.topBarInclude.statusBarSpacer.layoutParams.height = h
        binding.topBarInclude.statusBarSpacer.requestLayout()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}