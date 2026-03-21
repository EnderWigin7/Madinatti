package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.madinatti.app.databinding.FragmentPrieresBinding

class PrieresFragment : Fragment() {

    private var _binding: FragmentPrieresBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrieresBinding.inflate(
            inflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View, savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.qiblaCard.setOnClickListener {
            val navController = Navigation.findNavController(
                requireActivity(), R.id.navHostFragment
            )
            navController.navigate(R.id.qiblaFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}