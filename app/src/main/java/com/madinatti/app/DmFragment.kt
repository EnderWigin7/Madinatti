package com.madinatti.app

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.madinatti.app.databinding.FragmentDmBinding
import java.text.SimpleDateFormat
import java.util.*

data class DmMessage(
    val text: String, val time: String, val isSent: Boolean,
    val timestamp: Long = 0L, val imageUrl: String = ""
)

// ─────────────────────────────────────────────
// DmFragment
// ─────────────────────────────────────────────
class DmFragment : Fragment() {

    private var _binding: FragmentDmBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val messages = mutableListOf<DmMessage>()
    private lateinit var adapter: DmAdapter
    private var chatId = ""
    private var otherUserId = ""
    private var otherUserName = ""

    // ← These belong in DmFragment, NOT in DmAdapter
    private var onlineStatusListener:
            com.google.firebase.firestore.ListenerRegistration? = null

    private val pickMultiMedia = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts
            .GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) uploadMultipleMedia(uris)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        chatId        = arguments?.getString("chatId")        ?: ""
        otherUserId   = arguments?.getString("otherUserId")   ?: ""
        otherUserName = arguments?.getString("otherUserName") ?: "Utilisateur"

        ViewCompat.setOnApplyWindowInsetsListener(binding.dmTopBar) { _, insets ->
            val h = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            binding.statusBarSpacer.layoutParams.height = h
            binding.statusBarSpacer.requestLayout()
            insets
        }

        binding.tvDmName.text = otherUserName
        binding.tvDmAvatar.text = otherUserName.firstOrNull()
            ?.toString()?.uppercase() ?: "?"

        binding.ivDmBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.ivDmCall.setOnClickListener {
            if (otherUserId.isNotEmpty()) {
                db.collection("users").document(otherUserId).get()
                    .addOnSuccessListener { doc ->
                        val phone = doc.getString("phone") ?: ""
                        if (phone.isNotEmpty())
                            startActivity(Intent(Intent.ACTION_DIAL,
                                Uri.parse("tel:$phone")))
                        else Toast.makeText(requireContext(),
                            "Numéro non disponible", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // Rate seller button
        val btnRate = view.findViewById<TextView>(R.id.btnRateSeller)
        val currentUid = auth.currentUser?.uid
        btnRate?.visibility = View.GONE
        if (chatId.isNotEmpty() && currentUid != null &&
            otherUserId != currentUid) {
            db.collection("chats").document(chatId).get()
                .addOnSuccessListener { chatDoc ->
                    if (!isAdded) return@addOnSuccessListener
                    val adId = chatDoc.getString("adId") ?: ""
                    if (adId.isNotEmpty()) {
                        db.collection("ads").document(adId).get()
                            .addOnSuccessListener { adDoc ->
                                if (!isAdded) return@addOnSuccessListener
                                val sellerId = adDoc.getString("userId") ?: ""
                                if (otherUserId == sellerId &&
                                    currentUid != sellerId) {
                                    btnRate?.visibility = View.VISIBLE
                                    btnRate?.setOnClickListener {
                                        showRatingDialog(otherUserId)
                                    }
                                }
                            }
                    }
                }
        }

        view.findViewById<TextView>(R.id.btnAttach)?.setOnClickListener {
            pickMultiMedia.launch("image/*")
        }

        adapter = DmAdapter(messages) { imageUrl ->
            showImagePreview(imageUrl)
        }
        binding.rvMessages.layoutManager =
            LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
        binding.rvMessages.adapter = adapter

        if (chatId.isNotEmpty()) loadMessages()
        binding.btnSend.setOnClickListener { sendMessage() }
        markAsRead()

        // ← Online status - these are now correctly in DmFragment
        listenToOnlineStatus()
        setMyOnlineStatus(true)
    }

    // ─────────────────────────────────────────
    // ONLINE STATUS - correctly inside DmFragment
    // ─────────────────────────────────────────
    private fun listenToOnlineStatus() {
        if (otherUserId.isEmpty()) return
        onlineStatusListener?.remove()

        onlineStatusListener = db.collection("users")
            .document(otherUserId)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null ||
                    !isAdded || _binding == null) return@addSnapshotListener

                val isOnline = doc.getBoolean("isOnline") ?: false
                val lastSeen = doc.getTimestamp("lastSeen")

                binding.tvDmStatus.text = when {
                    isOnline -> "🟢 En ligne"
                    lastSeen != null -> {
                        val diffMin = (System.currentTimeMillis() -
                                lastSeen.toDate().time) / 60000
                        when {
                            diffMin < 1    -> "Vu à l'instant"
                            diffMin < 60   -> "Vu il y a ${diffMin}min"
                            diffMin < 1440 -> "Vu il y a ${diffMin / 60}h"
                            else           -> "Vu il y a ${diffMin / 1440}j"
                        }
                    }
                    else -> "⚫ Hors ligne"
                }

                binding.tvDmStatus.setTextColor(
                    if (isOnline)
                        android.graphics.Color.parseColor("#2ECC71")
                    else
                        android.graphics.Color.parseColor("#7FA68A")
                )
            }
    }

    private fun setMyOnlineStatus(online: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update(mapOf(
                "isOnline" to online,
                "lastSeen" to com.google.firebase.Timestamp.now()
            ))
    }

    // ─────────────────────────────────────────
    // MESSAGES
    // ─────────────────────────────────────────
    private fun loadMessages() {
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { result, _ ->
                if (_binding == null || !isAdded || result == null)
                    return@addSnapshotListener
                messages.clear()
                val currentUid = auth.currentUser?.uid ?: ""
                for (doc in result) {
                    messages.add(DmMessage(
                        text = doc.getString("text") ?: "",
                        time = doc.getTimestamp("timestamp")?.let {
                            SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(it.toDate())
                        } ?: "",
                        isSent = doc.getString("senderId") == currentUid,
                        timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0L,
                        imageUrl = doc.getString("imageUrl") ?: ""
                    ))
                }
                adapter.notifyDataSetChanged()
                if (messages.isNotEmpty())
                    binding.rvMessages.scrollToPosition(messages.size - 1)
            }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return
        val currentUid = auth.currentUser?.uid ?: return
        if (chatId.isEmpty()) return
        binding.etMessage.text?.clear()
        val now = Timestamp.now()
        db.collection("chats").document(chatId)
            .collection("messages").add(mapOf(
                "senderId" to currentUid, "text" to text,
                "timestamp" to now, "read" to false
            ))
        db.collection("chats").document(chatId).update(mapOf(
            "lastMessage" to text,
            "lastMessageTime" to now,
            "unread_$otherUserId" to
                    com.google.firebase.firestore.FieldValue.increment(1)
        ))
    }

    private fun uploadMultipleMedia(uris: List<Uri>) {
        val currentUid = auth.currentUser?.uid ?: return
        if (chatId.isEmpty()) return

        val attachedText = binding.etMessage.text.toString().trim()
        binding.etMessage.text?.clear()

        Toast.makeText(requireContext(),
            "📤 Envoi de ${uris.size} fichier(s)...",
            Toast.LENGTH_SHORT).show()

        uris.forEachIndexed { index, uri ->
            Thread {
                try {
                    val bytes = requireContext().contentResolver
                        .openInputStream(uri)?.readBytes() ?: return@Thread
                    val mimeType = requireContext().contentResolver
                        .getType(uri) ?: "image/*"
                    val resourceType = when {
                        mimeType.startsWith("video") -> "video"
                        mimeType.startsWith("audio") -> "raw"
                        else -> "image"
                    }
                    val ext = when {
                        mimeType.startsWith("video") -> "mp4"
                        mimeType.startsWith("audio") -> "mp3"
                        else -> "jpg"
                    }

                    val boundary = "----CB${System.currentTimeMillis()}"
                    val conn = java.net.URL(
                        "https://api.cloudinary.com/v1_1/" +
                                "${PostAdFragment.CLOUDINARY_CLOUD}/$resourceType/upload"
                    ).openConnection() as java.net.HttpURLConnection
                    conn.doOutput = true
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=$boundary")
                    val out = conn.outputStream

                    // Helper function to write parts
                    fun writePart(name: String, value: String) {
                        val part = "--$boundary\r\n" +
                                "Content-Disposition: form-data; name=\"$name\"\r\n" +
                                "\r\n" +
                                "$value\r\n"
                        out.write(part.toByteArray())
                    }

                    writePart("upload_preset", PostAdFragment.CLOUDINARY_PRESET)
                    writePart("folder", "chats/$chatId")

                    // Write file part - FIXED
                    val fileHeader = "--$boundary\r\n" +
                            "Content-Disposition: form-data; name=\"file\"; " +
                            "filename=\"media.$ext\"\r\n" +
                            "Content-Type: $mimeType\r\n" +
                            "\r\n"
                    out.write(fileHeader.toByteArray())
                    out.write(bytes)
                    out.write("\r\n".toByteArray())
                    out.write(("--$boundary--\r\n").toByteArray())
                    out.flush()

                    val mediaUrl = org.json.JSONObject(
                        conn.inputStream.bufferedReader().readText()
                    ).getString("secure_url")

                    val now = Timestamp.now()
                    val msgText = if (index == 0) attachedText else ""

                    activity?.runOnUiThread {
                        if (_binding == null || !isAdded) return@runOnUiThread
                        db.collection("chats").document(chatId)
                            .collection("messages").add(mapOf(
                                "senderId" to currentUid,
                                "text" to msgText,
                                "imageUrl" to mediaUrl,
                                "mediaType" to mimeType,
                                "timestamp" to now,
                                "read" to false
                            ))
                        if (index == uris.size - 1) {
                            val preview = if (attachedText.isNotEmpty())
                                attachedText else "📷 ${uris.size} fichier(s)"
                            db.collection("chats").document(chatId).update(mapOf(
                                "lastMessage" to preview,
                                "lastMessageTime" to now,
                                "unread_$otherUserId" to
                                        com.google.firebase.firestore.FieldValue.increment(1)
                            ))
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        if (isAdded) Toast.makeText(requireContext(),
                            "❌ Erreur fichier ${index + 1}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    private fun showImagePreview(imageUrl: String) {
        val dialog = Dialog(requireContext(),
            android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val layout = android.widget.FrameLayout(requireContext())

        val iv = ImageView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
            setOnClickListener { dialog.dismiss() }
        }
        Glide.with(this).load(imageUrl).into(iv)
        layout.addView(iv)

        val dp = resources.displayMetrics.density

        val btnDownload = TextView(requireContext()).apply {
            text = "⬇️ Télécharger"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            typeface = resources.getFont(R.font.poppins_semibold)
            setBackgroundResource(R.drawable.bg_button_green)
            setPadding((20 * dp).toInt(), (10 * dp).toInt(),
                (20 * dp).toInt(), (10 * dp).toInt())
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM or
                        android.view.Gravity.CENTER_HORIZONTAL
                bottomMargin = (40 * dp).toInt()
            }
            setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(imageUrl)))
                } catch (_: Exception) {
                    Toast.makeText(requireContext(),
                        "❌ Impossible d'ouvrir",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(btnDownload)

        val btnClose = TextView(requireContext()).apply {
            text = "✕"; textSize = 24f
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.FrameLayout.LayoutParams(
                (44 * dp).toInt(), (44 * dp).toInt()
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                topMargin = (40 * dp).toInt()
                marginEnd = (16 * dp).toInt()
            }
            gravity = android.view.Gravity.CENTER
            setOnClickListener { dialog.dismiss() }
        }
        layout.addView(btnClose)

        dialog.setContentView(layout)
        dialog.show()
    }

    private fun markAsRead() {
        val currentUid = auth.currentUser?.uid ?: return
        if (chatId.isEmpty()) return
        db.collection("chats").document(chatId)
            .update("unread_$currentUid", 0)
    }

    private fun showRatingDialog(sellerId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        if (currentUid == sellerId) return
        db.collection("ratings")
            .whereEqualTo("sellerId", sellerId)
            .whereEqualTo("buyerId", currentUid)
            .get()
            .addOnSuccessListener { existing ->
                if (!isAdded) return@addOnSuccessListener
                if (!existing.isEmpty) {
                    Toast.makeText(requireContext(),
                        "⭐ Déjà évalué", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                var selectedRating = 10
                val options = arrayOf(
                    "★ 2/10 — Très mauvais",
                    "★★ 4/10 — Mauvais",
                    "★★★ 6/10 — Correct",
                    "★★★★ 8/10 — Bien",
                    "★★★★★ 10/10 — Excellent!"
                )
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("⭐ Évaluer le vendeur")
                    .setSingleChoiceItems(options, 4) { _, which ->
                        selectedRating = (which + 1) * 2
                    }
                    .setPositiveButton("Envoyer") { _, _ ->
                        db.collection("ratings").add(mapOf(
                            "sellerId" to sellerId,
                            "buyerId" to currentUid,
                            "rating" to selectedRating.toDouble(),
                            "chatId" to chatId,
                            "createdAt" to Timestamp.now()
                        )).addOnSuccessListener {
                            if (isAdded) Toast.makeText(requireContext(),
                                "⭐ Merci!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Plus tard", null)
                    .show()
            }
    }

    // ─────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────
    override fun onDestroyView() {
        onlineStatusListener?.remove()  // ← Stop listening
        setMyOnlineStatus(false)        // ← Set offline
        requireActivity().window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        super.onDestroyView()
        _binding = null
    }
}

// ─────────────────────────────────────────────
// DmAdapter - clean, no Fragment stuff in here
// ─────────────────────────────────────────────
class DmAdapter(
    private val messages: List<DmMessage>,
    private val onImageClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_RECEIVED = 0
        private const val VIEW_SENT = 1
    }

    override fun getItemViewType(position: Int) =
        if (messages[position].isSent) VIEW_SENT else VIEW_RECEIVED

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_SENT)
            SentVH(inflater.inflate(
                R.layout.item_dm_message_sent, parent, false))
        else
            ReceivedVH(inflater.inflate(
                R.layout.item_dm_message, parent, false))
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder, position: Int
    ) {
        when (holder) {
            is SentVH     -> holder.bind(messages[position], onImageClick)
            is ReceivedVH -> holder.bind(messages[position], onImageClick)
        }
    }

    override fun getItemCount() = messages.size

    class SentVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(msg: DmMessage, onImageClick: (String) -> Unit) {
            val tvText = itemView.findViewById<TextView>(R.id.tvMessageText)
            val tvTime = itemView.findViewById<TextView>(R.id.tvTimestamp)
            val ivImage = itemView.findViewById<ImageView>(R.id.ivMessageImage)
            tvTime.text = msg.time

            if (msg.imageUrl.isNotEmpty() && ivImage != null) {
                ivImage.visibility = View.VISIBLE
                Glide.with(itemView.context).load(msg.imageUrl)
                    .centerCrop().into(ivImage)
                ivImage.setOnClickListener { onImageClick(msg.imageUrl) }
                if (msg.text.isNotEmpty() && !msg.text.startsWith("📷")) {
                    tvText.visibility = View.VISIBLE
                    tvText.text = msg.text
                } else {
                    tvText.visibility = View.GONE
                }
            } else {
                ivImage?.visibility = View.GONE
                tvText.visibility = View.VISIBLE
                tvText.text = msg.text
            }
        }
    }

    class ReceivedVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(msg: DmMessage, onImageClick: (String) -> Unit) {
            val tvText = itemView.findViewById<TextView>(R.id.tvMessageText)
            val tvTime = itemView.findViewById<TextView>(R.id.tvTimestamp)
            val ivImage = itemView.findViewById<ImageView>(R.id.ivMessageImage)
            tvTime.text = msg.time

            if (msg.imageUrl.isNotEmpty() && ivImage != null) {
                ivImage.visibility = View.VISIBLE
                Glide.with(itemView.context).load(msg.imageUrl)
                    .centerCrop().into(ivImage)
                ivImage.setOnClickListener { onImageClick(msg.imageUrl) }
                if (msg.text.isNotEmpty() && !msg.text.startsWith("📷")) {
                    tvText.visibility = View.VISIBLE
                    tvText.text = msg.text
                } else {
                    tvText.visibility = View.GONE
                }
            } else {
                ivImage?.visibility = View.GONE
                tvText.visibility = View.VISIBLE
                tvText.text = msg.text
            }
        }
    }
}