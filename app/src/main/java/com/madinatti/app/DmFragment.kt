package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.madinatti.app.databinding.FragmentDmBinding

data class DmMessage(
    val text: String,
    val time: String,
    val isSent: Boolean
)

class DmFragment : Fragment() {

    private var _binding: FragmentDmBinding? = null
    private val binding get() = _binding!!

    private val messages = mutableListOf(
        DmMessage("Bonjour! Le tapis est encore disponible?", "10:21", false),
        DmMessage("Oui toujours disponible!", "10:22", true),
        DmMessage("Super! Quel est votre meilleur prix?", "10:23", false),
        DmMessage("Prix fixe 1800 MAD", "10:24", true),
        DmMessage("D'accord! On peut se retrouver où?", "10:25", false),
        DmMessage("Guéliz, disponible ce weekend!", "10:26", true)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.dmTopBar) { v, insets ->
            val h = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.statusBarSpacer.layoutParams.height = h
            binding.statusBarSpacer.requestLayout()
            insets
        }

        binding.ivDmBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Setup RecyclerView
        val adapter = DmAdapter(messages)
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true // messages start from bottom like WhatsApp
        }
        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter

        // Send message
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                messages.add(DmMessage(text, "maintenant", true))
                adapter.notifyItemInserted(messages.size - 1)
                binding.rvMessages.scrollToPosition(messages.size - 1)
                binding.etMessage.text?.clear()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class DmAdapter(private val messages: List<DmMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_RECEIVED = 0
    private val VIEW_SENT = 1

    override fun getItemViewType(position: Int) =
        if (messages[position].isSent) VIEW_SENT else VIEW_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_SENT) {
            SentViewHolder(inflater.inflate(R.layout.item_dm_message_sent, parent, false))
        } else {
            ReceivedViewHolder(inflater.inflate(R.layout.item_dm_message, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is SentViewHolder -> holder.bind(msg)
            is ReceivedViewHolder -> holder.bind(msg)
        }
    }

    override fun getItemCount() = messages.size

    class SentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(msg: DmMessage) {
            itemView.findViewById<TextView>(R.id.tvMessageText).text = msg.text
            itemView.findViewById<TextView>(R.id.tvTimestamp).text = msg.time
        }
    }

    class ReceivedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(msg: DmMessage) {
            itemView.findViewById<TextView>(R.id.tvMessageText).text = msg.text
            itemView.findViewById<TextView>(R.id.tvTimestamp).text = msg.time
        }
    }
}