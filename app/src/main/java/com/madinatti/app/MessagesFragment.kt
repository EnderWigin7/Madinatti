package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.madinatti.app.databinding.FragmentMessagesBinding
import java.text.SimpleDateFormat
import java.util.*

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.messagesTopBar) { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.statusBarSpacer.layoutParams.height = statusBar
            binding.statusBarSpacer.requestLayout()
            insets
        }

        loadChats()
    }

    private fun loadChats() {
        val uid = auth.currentUser?.uid ?: return


        db.collection("chats")
            .whereArrayContains("participants", uid)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { result, _ ->
                if (_binding == null || !isAdded) return@addSnapshotListener
                if (result == null) return@addSnapshotListener


                binding.dynamicChatList.removeAllViews()

                if (result.isEmpty) {
                    binding.tvEmptyChats.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                binding.tvEmptyChats.visibility = View.GONE

                for (doc in result) {
                    val chatId = doc.id
                    val participants = doc.get("participants") as? List<*> ?: continue
                    val otherUserId = participants.firstOrNull { it != uid }?.toString() ?: continue
                    val participantNames = doc.get("participantNames") as? Map<*, *>
                    val otherUserName = participantNames?.get(otherUserId)?.toString() ?: "Utilisateur"
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val adTitle = doc.getString("adTitle") ?: ""
                    val lastTime = doc.getTimestamp("lastMessageTime")
                    val unreadCount = (doc.getLong("unread_$uid") ?: 0).toInt()

                    val timeStr = if (lastTime != null) formatTime(lastTime.toDate()) else ""

                    val chatView = createChatRow(
                        chatId = chatId,
                        otherUserId = otherUserId,
                        name = otherUserName,
                        lastMessage = lastMessage,
                        adTitle = adTitle,
                        time = timeStr,
                        unread = unreadCount
                    )
                    binding.dynamicChatList.addView(chatView)

                    val divider = View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1
                        ).apply { marginStart = dpToPx(16); marginEnd = dpToPx(16) }
                        setBackgroundColor(android.graphics.Color.parseColor("#1E3D2A"))
                    }
                    binding.dynamicChatList.addView(divider)
                }
            }
    }

    private fun createChatRow(
        chatId: String, otherUserId: String, name: String,
        lastMessage: String, adTitle: String, time: String, unread: Int
    ): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(98)
            )
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
            setBackgroundResource(
                if (unread > 0) R.drawable.bg_surface else R.color.background
            )
            isClickable = true
            isFocusable = true
            foreground = androidx.core.content.ContextCompat.getDrawable(
                requireContext(), R.drawable.ripple_green
            )
        }

        val avatarFrame = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(50), dpToPx(50))
        }
        val avatarTv = TextView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(dpToPx(50), dpToPx(50))
            text = name.firstOrNull()?.toString()?.uppercase() ?: "?"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.bg_avatar_circle)
        }
        avatarFrame.addView(avatarTv)

        if (unread > 0) {
            val dot = View(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(
                    dpToPx(12), dpToPx(12)
                ).apply {
                    gravity = android.view.Gravity.BOTTOM or
                            android.view.Gravity.END
                }
                setBackgroundResource(R.drawable.bg_online_dot)
            }
            avatarFrame.addView(dot)
        }


        db.collection("users").document(otherUserId).get()
            .addOnSuccessListener { userDoc ->
                val avatarUrl = userDoc.getString("avatarUrl") ?: ""
                if (avatarUrl.isNotEmpty() && isAdded) {
                    val ivAvatar = android.widget.ImageView(requireContext()).apply {
                        layoutParams = FrameLayout.LayoutParams(dpToPx(50), dpToPx(50))
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                    avatarFrame.removeAllViews()
                    avatarFrame.addView(ivAvatar)
                    Glide.with(this).load(avatarUrl)
                        .transform(CircleCrop()).into(ivAvatar)
                }
            }

        row.addView(avatarFrame)

        // Content
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(12)
            }
        }

        // Name + time row
        val nameTimeRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val nameTv = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = name
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            typeface = resources.getFont(R.font.poppins_semibold)
        }
        val timeTv = TextView(requireContext()).apply {
            text = time
            setTextColor(android.graphics.Color.parseColor("#7FA68A"))
            textSize = 11f
            typeface = resources.getFont(R.font.poppins_regular)
        }
        nameTimeRow.addView(nameTv)
        nameTimeRow.addView(timeTv)
        content.addView(nameTimeRow)

        // Last message + unread badge
        val msgRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpToPx(2) }
        }
        val msgTv = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = lastMessage.ifEmpty { "..." }
            setTextColor(if (unread > 0) android.graphics.Color.WHITE
            else android.graphics.Color.parseColor("#7FA68A"))
            textSize = 12f
            typeface = resources.getFont(R.font.poppins_regular)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        msgRow.addView(msgTv)

        if (unread > 0) {
            val badge = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20)).apply {
                    marginStart = dpToPx(8)
                }
                text = unread.toString()
                setTextColor(android.graphics.Color.parseColor("#0D1F17"))
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(R.drawable.bg_unread_badge)
            }
            msgRow.addView(badge)
        }
        content.addView(msgRow)

        // Ad chip
        if (adTitle.isNotEmpty()) {
            val chip = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(22)
                ).apply { topMargin = dpToPx(4) }
                setPadding(dpToPx(8), 0, dpToPx(8), 0)
                setBackgroundResource(R.drawable.bg_ad_chip)
            }
            val chipTv = TextView(requireContext()).apply {
                text = "📦 $adTitle"
                setTextColor(android.graphics.Color.parseColor("#2ECC71"))
                textSize = 10f
                typeface = resources.getFont(R.font.poppins_regular)
            }
            chip.addView(chipTv)
            content.addView(chip)
        }

        row.addView(content)

        // Navigate to DM on click
        row.setOnClickListener {
            val bundle = Bundle().apply {
                putString("chatId", chatId)
                putString("otherUserId", otherUserId)
                putString("otherUserName", name)
                putString("adTitle", adTitle)
            }
            androidx.navigation.Navigation
                .findNavController(requireActivity(), R.id.navHostFragment)
                .navigate(R.id.dmFragment, bundle)
        }

        return row
    }


    private fun formatTime(date: Date): String {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply { time = date }
        return when {
            cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) ->
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1 -> "Hier"
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}