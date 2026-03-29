package com.madinatti.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.madinatti.app.databinding.BottomSheetNotificationsBinding

class NotificationsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNotificationsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Real-time listener reference so we can remove it later
    private var chatsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvMarkAllRead.setOnClickListener {
            // Disable button while processing
            binding.tvMarkAllRead.isClickable = false
            binding.tvMarkAllRead.alpha = 0.5f
            markAllRead()
        }

        loadNotifications()
    }

    // ─────────────────────────────────────────────────────────────
    // LOAD NOTIFICATIONS
    // ─────────────────────────────────────────────────────────────
    private fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: run {
            showEmpty("Connectez-vous pour voir vos notifications")
            return
        }

        showEmpty("⏳ Chargement...")

        val notifications = mutableListOf<NotifItem>()
        var queriesCompleted = 0
        val totalQueries = 3

        fun checkDone() {
            if (!isAdded || _binding == null) return
            queriesCompleted++
            if (queriesCompleted < totalQueries) return

            // Re-enable mark all read button
            binding.tvMarkAllRead.isClickable = true
            binding.tvMarkAllRead.alpha = 1f

            renderNotifications(notifications)
        }

        // ── 1) Unread messages (with real-time listener) ──
        // Remove old listener first
        chatsListener?.remove()

        chatsListener = db.collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { result, error ->
                if (error != null || result == null) {
                    checkDone()
                    return@addSnapshotListener
                }

                // Remove old message notifications and re-add fresh ones
                notifications.removeAll { it.type == "message" }

                for (doc in result) {
                    val unread = (doc.getLong("unread_$uid") ?: 0).toInt()
                    if (unread > 0) {
                        val names = doc.get("participantNames") as? Map<*, *>
                        val otherUid = (doc.get("participants") as? List<*>)
                            ?.firstOrNull { it != uid }?.toString() ?: ""
                        val otherName = names?.get(otherUid)?.toString() ?: "Quelqu'un"
                        val lastMsg = doc.getString("lastMessage") ?: ""
                        val time = doc.getTimestamp("lastMessageTime")?.seconds ?: 0L
                        val adTitle = doc.getString("adTitle") ?: ""

                        notifications.add(NotifItem(
                            emoji = "💬",
                            title = "$unread nouveau(x) message(s) de $otherName",
                            subtitle = lastMsg.take(50),
                            timestamp = time,
                            type = "message",
                            isUnread = true,
                            chatId = doc.id,
                            otherUserId = otherUid,
                            otherUserName = otherName,
                            adTitle = adTitle
                        ))
                    }
                }

                // If this is triggered after initial load, just re-render
                if (queriesCompleted >= totalQueries) {
                    if (isAdded && _binding != null) {
                        renderNotifications(notifications)
                    }
                } else {
                    checkDone()
                }
            }

        // ── 2) Recent ads in user's city ──
        val city = requireContext()
            .getSharedPreferences("madinatti_prefs", 0)
            .getString("selected_city", null)

        val oneDayAgo = com.google.firebase.Timestamp(
            (System.currentTimeMillis() / 1000) - 86400, 0
        )

        db.collection("ads")
            .whereGreaterThan("createdAt", oneDayAgo)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val count = result.documents.count { doc ->
                    val adCity = doc.getString("city") ?: ""
                    doc.getString("userId") != uid &&
                            (city == null ||
                                    city == "Toutes les villes" ||
                                    adCity == city)
                }
                if (count > 0) {
                    notifications.add(NotifItem(
                        emoji = "📦",
                        title = "$count nouvelle(s) annonce(s) près de chez vous",
                        subtitle = city ?: "Toutes les villes",
                        timestamp = System.currentTimeMillis() / 1000,
                        type = "marketplace",
                        isUnread = true
                    ))
                }
                checkDone()
            }
            .addOnFailureListener { checkDone() }

        db.collection("events")
            .limit(5)
            .get()
            .addOnSuccessListener { result ->
                val cityEvents = result.documents.filter { doc ->
                    val evCity = doc.getString("city") ?: ""
                    city == null || city == "Toutes les villes" || evCity == city
                }
                if (cityEvents.isNotEmpty()) {
                    val first = cityEvents.first()
                    val eventDateStr = first.getString("date") ?: ""
                    val createdAt = first.getTimestamp("createdAt")?.seconds ?: 0L
                    val hoursSinceCreated = (System.currentTimeMillis()/1000 - createdAt) / 3600

                    if (hoursSinceCreated < 48) {
                        notifications.add(NotifItem(
                            emoji = "🎉",
                            title = "Événement: ${first.getString("title") ?: ""}",
                            subtitle = "${first.getString("date") ?: ""} · " +
                                    "${first.getString("venue") ?: ""}",
                            timestamp = createdAt,
                            type = "event"
                        ))
                    }
                }
                checkDone()
            }
            .addOnFailureListener { checkDone() }
    }

    private fun renderNotifications(notifications: MutableList<NotifItem>) {
        if (!isAdded || _binding == null) return

        val container = binding.notifContentContainer
        container.removeAllViews()

        if (notifications.isEmpty()) {
            showEmpty("🔔 Aucune notification pour le moment")
            return
        }

        binding.tvEmptyNotifs.visibility = View.GONE

        // Count unread for badge on header
        val unreadCount = notifications.count { it.isUnread }
        binding.tvMarkAllRead.visibility =
            if (unreadCount > 0) View.VISIBLE else View.GONE

        notifications
            .sortedByDescending { it.timestamp }
            .take(20)
            .forEach { notif ->
                container.addView(createNotifRow(notif))
                container.addView(createDivider())
            }
    }

    private fun showEmpty(message: String) {
        if (!isAdded || _binding == null) return
        binding.notifContentContainer.removeAllViews()
        binding.tvEmptyNotifs.text = message
        binding.tvEmptyNotifs.visibility = View.VISIBLE
        binding.notifContentContainer.addView(binding.tvEmptyNotifs)
        binding.tvMarkAllRead.visibility = View.GONE
    }

    // ─────────────────────────────────────────────────────────────
    // CREATE ROW
    // ─────────────────────────────────────────────────────────────
    private fun createNotifRow(notif: NotifItem): View {
        val dp = resources.displayMetrics.density

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(
                (16 * dp).toInt(), (14 * dp).toInt(),
                (16 * dp).toInt(), (14 * dp).toInt()
            )
            isClickable = true
            isFocusable = true

            // Highlight unread rows
            if (notif.isUnread) {
                setBackgroundColor(android.graphics.Color.parseColor("#0A2ECC71"))
            }

            val ta = context.obtainStyledAttributes(
                intArrayOf(android.R.attr.selectableItemBackground)
            )
            foreground = ta.getDrawable(0)
            ta.recycle()

            setOnClickListener {
                handleNotifClick(notif)
            }
        }

        // Emoji
        row.addView(TextView(requireContext()).apply {
            text = notif.emoji
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                (40 * dp).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })

        // Text column
        val col = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = (8 * dp).toInt() }
        }

        col.addView(TextView(requireContext()).apply {
            text = notif.title
            setTextColor(
                if (notif.isUnread) android.graphics.Color.WHITE
                else android.graphics.Color.parseColor("#AAFFFFFF")
            )
            textSize = 13f
            typeface = resources.getFont(R.font.poppins_medium)
            maxLines = 2
        })

        if (notif.subtitle.isNotEmpty()) {
            col.addView(TextView(requireContext()).apply {
                text = notif.subtitle
                setTextColor(android.graphics.Color.parseColor("#7FA68A"))
                textSize = 11f
                typeface = resources.getFont(R.font.poppins_regular)
                maxLines = 1
            })
        }
        row.addView(col)

        // Right side: time + unread dot
        val rightCol = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = (8 * dp).toInt() }
        }

        rightCol.addView(TextView(requireContext()).apply {
            text = getTimeAgo(notif.timestamp)
            setTextColor(android.graphics.Color.parseColor("#4D7FA68A"))
            textSize = 10f
            typeface = resources.getFont(R.font.poppins_regular)
        })

        // Unread green dot
        if (notif.isUnread) {
            rightCol.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (8 * dp).toInt(), (8 * dp).toInt()
                ).apply {
                    topMargin = (4 * dp).toInt()
                    gravity = android.view.Gravity.CENTER_HORIZONTAL
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.parseColor("#2ECC71"))
                }
            })
        }

        row.addView(rightCol)
        return row
    }

    // ─────────────────────────────────────────────────────────────
    // HANDLE CLICK
    // ─────────────────────────────────────────────────────────────
    private fun handleNotifClick(notif: NotifItem) {
        dismiss()
        val mainActivity = activity as? MainActivity ?: return

        mainActivity.window.decorView.postDelayed({
            if (!isAdded) return@postDelayed
            when (notif.type) {
                "message" -> {
                    if (notif.chatId.isNotEmpty()) {
                        val bundle = Bundle().apply {
                            putString("chatId", notif.chatId)
                            putString("otherUserId", notif.otherUserId)
                            putString("otherUserName", notif.otherUserName)
                            putString("adTitle", notif.adTitle)
                        }
                        try {
                            val navHost = mainActivity.supportFragmentManager
                                .findFragmentById(R.id.navHostFragment)
                                    as? androidx.navigation.fragment.NavHostFragment
                            navHost?.navController?.navigate(R.id.dmFragment, bundle)
                        } catch (_: Exception) {
                            mainActivity.binding.bottomNav
                                .selectedItemId = R.id.messagesFragment
                        }
                    } else {
                        mainActivity.binding.bottomNav
                            .selectedItemId = R.id.messagesFragment
                    }
                }
                "marketplace" -> mainActivity.navigateToVilleTab("marketplace")
                "event"       -> mainActivity.navigateToVilleTab("evenements")
            }
        }, 200)
    }

    // ─────────────────────────────────────────────────────────────
    // MARK ALL READ - FIXED
    // ─────────────────────────────────────────────────────────────
    private fun markAllRead() {
        val uid = auth.currentUser?.uid ?: run {
            resetMarkAllButton()
            return
        }

        db.collection("chats")
            .whereArrayContains("participants", uid)
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded || _binding == null) {
                    resetMarkAllButton()
                    return@addOnSuccessListener
                }

                // Filter only chats that actually have unread messages
                val unreadChats = result.documents.filter { doc ->
                    (doc.getLong("unread_$uid") ?: 0) > 0
                }

                if (unreadChats.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "✅ Aucun message non lu",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetMarkAllButton()

                    // Hide the mark all read button since nothing to mark
                    binding.tvMarkAllRead.visibility = View.GONE
                    return@addOnSuccessListener
                }

                // Batch update only unread chats
                val batch = db.batch()
                for (doc in unreadChats) {
                    batch.update(doc.reference, "unread_$uid", 0)
                }

                batch.commit()
                    .addOnSuccessListener {
                        if (!isAdded || _binding == null) return@addOnSuccessListener

                        Toast.makeText(
                            requireContext(),
                            "✅ ${unreadChats.size} conversation(s) marquée(s) comme lue(s)",
                            Toast.LENGTH_SHORT
                        ).show()

                        // The real-time listener will automatically
                        // update the UI since we're using addSnapshotListener
                        // But we hide the button immediately for better UX
                        binding.tvMarkAllRead.visibility = View.GONE
                        resetMarkAllButton()
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded || _binding == null) return@addOnFailureListener
                        Toast.makeText(
                            requireContext(),
                            "❌ Erreur: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                        resetMarkAllButton()
                    }
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                Toast.makeText(
                    requireContext(),
                    "❌ Erreur de connexion",
                    Toast.LENGTH_SHORT
                ).show()
                resetMarkAllButton()
            }
    }

    private fun resetMarkAllButton() {
        if (!isAdded || _binding == null) return
        binding.tvMarkAllRead.isClickable = true
        binding.tvMarkAllRead.alpha = 1f
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────
    private fun createDivider(): View {
        val dp = resources.displayMetrics.density
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply {
                marginStart = (16 * dp).toInt()
                marginEnd = (16 * dp).toInt()
            }
            setBackgroundColor(android.graphics.Color.parseColor("#1E3D2A"))
        }
    }

    private fun getTimeAgo(timestampSec: Long): String {
        val now = System.currentTimeMillis() / 1000
        val diff = now - timestampSec
        return when {
            diff < 60     -> "maintenant"
            diff < 3600   -> "${diff / 60}min"
            diff < 86400  -> "${diff / 3600}h"
            diff < 604800 -> "${diff / 86400}j"
            else          -> "${diff / 604800}sem"
        }
    }

    data class NotifItem(
        val emoji: String,
        val title: String,
        val subtitle: String,
        val timestamp: Long,
        val type: String = "",
        val isUnread: Boolean = false,
        val chatId: String = "",
        val otherUserId: String = "",
        val otherUserName: String = "",
        val adTitle: String = ""
    )

    override fun getTheme() = R.style.GlassBottomSheetDialog

    override fun onDestroyView() {
        super.onDestroyView()
        chatsListener?.remove()
        _binding = null
    }

    companion object {
        fun newInstance() = NotificationsBottomSheet()
    }
}