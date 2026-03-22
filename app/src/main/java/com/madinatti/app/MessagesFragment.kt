package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.madinatti.app.databinding.FragmentMessagesBinding

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.messagesTopBar) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.statusBarSpacer.layoutParams.height = statusBar
            binding.statusBarSpacer.requestLayout()
            insets
        }

        binding.messagesSearchBar.setOnClickListener { }


        listOf(
            binding.convAicha,
            binding.convMohamed,
            binding.convSara,
            binding.convKarim,
            binding.convFatima
        ).forEach { conv ->
            conv.setOnClickListener {
                triggerRipple(conv)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.navHostFragment, DmFragment())
                    .addToBackStack(null)
                    .commit()
            }
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