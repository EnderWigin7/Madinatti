package com.madinatti.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class PostAdFragment : Fragment() {

    private var loadingDialog: android.app.AlertDialog? = null
    private var existingImageUrls = mutableListOf<String>()
    private val selectedImageUris = mutableListOf<android.net.Uri>()

    // Stores original createdAt for edit mode expiresAt recalculation
    private var originalCreatedAt: Timestamp? = null

    companion object {
        const val CLOUDINARY_CLOUD  = "dccaanogt"
        const val CLOUDINARY_PRESET = "madinatti_unsigned"
    }

    private val categories = listOf(
        "Maison", "Électronique", "Véhicules", "Vêtements",
        "Immobilier", "Artisanat", "Mode", "Sports",
        "Livres", "Gaming", "Services", "Autre"
    )
    private val cities = listOf(
        "Agadir", "Al Hoceima", "Azrou", "Beni Mellal", "Berrechid",
        "Casablanca", "Chefchaouen", "El Jadida", "Errachidia", "Essaouira",
        "Fes", "Guelmim", "Ifrane", "Kenitra", "Khemisset",
        "Khouribga", "Laayoune", "Larache", "Marrakech", "Meknes",
        "Mohammedia", "Nador", "Ouarzazate", "Oujda", "Rabat",
        "Safi", "Sale", "Settat", "Sidi Kacem", "Tanger",
        "Taza", "Temara", "Tetouan", "Tiznit"
    )
    private val emojiMap = mapOf(
        "Électronique" to "📱", "Véhicules" to "🚗", "Immobilier" to "🏠",
        "Mode" to "👗", "Maison" to "🛋", "Sports" to "⚽",
        "Livres" to "📚", "Gaming" to "🎮", "Services" to "🔧",
        "Vêtements" to "👕", "Artisanat" to "🎨", "Autre" to "📦"
    )

    private var selectedCategory: String? = null
    private var selectedCity: String? = null

    private val pickImages = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val totalAllowed = 5 - existingImageUrls.size - selectedImageUris.size
            if (totalAllowed <= 0) {
                Toast.makeText(requireContext(),
                    "Maximum 5 photos", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            selectedImageUris.addAll(uris.take(totalAllowed))
            showAllImages()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_ad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val statusBarHeight = requireContext()
            .getSharedPreferences("ui_prefs", 0)
            .getInt("status_bar_height", 0)
        view.findViewById<View>(R.id.statusBarSpacer)?.apply {
            layoutParams.height = statusBarHeight
            requestLayout()
        }

        val btnBack       = view.findViewById<ImageView>(R.id.btnBack)
        val etTitle       = view.findViewById<EditText>(R.id.etTitle)
        val spinnerCat    = view.findViewById<View>(R.id.spinnerCategory)
        val tvCategory    = view.findViewById<TextView>(R.id.tvCategory)
        val etPrice       = view.findViewById<EditText>(R.id.etPrice)
        val spinnerCity   = view.findViewById<View>(R.id.spinnerCity)
        val tvCity        = view.findViewById<TextView>(R.id.tvCity)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        val tvCharCount   = view.findViewById<TextView>(R.id.tvCharCount)
        val etDuration    = view.findViewById<EditText>(R.id.etDuration)
        val tvExpiry      = view.findViewById<TextView>(R.id.tvExpiryDate)
        val btnPublish    = view.findViewById<View>(R.id.btnPublish)
        val btnAddPhoto   = view.findViewById<View>(R.id.btnAddPhoto)

        val editAdId   = arguments?.getString("adId")
        val isEditMode = !editAdId.isNullOrEmpty()

        // ──────────────────────────────────────────
        // EDIT MODE SETUP
        // ──────────────────────────────────────────
        if (isEditMode) {
            view.findViewById<TextView>(R.id.tvScreenTitle)
                ?.text = "Modifier l'annonce"
            val publishLayout = view.findViewById<LinearLayout>(R.id.btnPublish)
            (publishLayout?.getChildAt(0) as? TextView)?.text = "Modifier"

            FirebaseFirestore.getInstance()
                .collection("ads").document(editAdId!!)
                .get()
                .addOnSuccessListener { doc ->
                    if (!isAdded || doc == null || !doc.exists())
                        return@addOnSuccessListener

                    etTitle?.setText(doc.getString("title") ?: "")
                    etPrice?.setText(
                        (doc.getDouble("price")?.toInt() ?: "").toString())
                    etDescription?.setText(doc.getString("description") ?: "")

                    val cat = doc.getString("category")
                    if (!cat.isNullOrEmpty()) {
                        selectedCategory = cat
                        tvCategory?.text = cat
                        tvCategory?.setTextColor(android.graphics.Color.WHITE)
                    }

                    val cityVal = doc.getString("city")
                    if (!cityVal.isNullOrEmpty()) {
                        selectedCity = cityVal
                        tvCity?.text = cityVal
                        tvCity?.setTextColor(android.graphics.Color.WHITE)
                    }

                    // Store original createdAt for recalculating expiresAt
                    originalCreatedAt = doc.getTimestamp("createdAt")

                    val duration = doc.get("duration")?.let {
                        when (it) {
                            is Long   -> it.toInt()
                            is Double -> it.toInt()
                            is String -> it.toIntOrNull() ?: 30
                            else      -> 30
                        }
                    } ?: 30
                    etDuration?.setText(duration.toString())

                    // Show expiry preview using Calendar (DST safe)
                    val createdAt = originalCreatedAt
                    if (createdAt != null) {
                        val cal = Calendar.getInstance().apply {
                            time = createdAt.toDate()
                            add(Calendar.DAY_OF_YEAR, duration)
                        }
                        tvExpiry?.text = "Expire le: ${
                            SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                                .format(cal.time)
                        }"
                    }

                    tvCharCount?.text =
                        "${etDescription?.text?.length ?: 0}/500"

                    existingImageUrls.clear()
                    existingImageUrls.addAll(
                        (doc.get("imageUrls") as? List<*>)
                            ?.mapNotNull { it?.toString() } ?: emptyList()
                    )
                    showAllImages()
                }
        }

        btnBack?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        spinnerCat?.setOnClickListener {
            showPickerSheet(
                getString(R.string.ad_field_category),
                categories, selectedCategory, false
            ) { selected ->
                selectedCategory = selected
                tvCategory?.text = selected
                tvCategory?.setTextColor(android.graphics.Color.WHITE)
            }
        }

        spinnerCity?.setOnClickListener {
            showPickerSheet(
                getString(R.string.ad_field_city),
                cities, selectedCity, true
            ) { selected ->
                selectedCity = selected
                tvCity?.text = selected
                tvCity?.setTextColor(android.graphics.Color.WHITE)
            }
        }

        etDescription?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tvCharCount?.text = "${s?.length ?: 0}/500"
            }
        })

        // Duration watcher - uses Calendar for DST-safe date math
        etDuration?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val days = s?.toString()?.toIntOrNull()
                if (days != null && days > 0) {
                    // Edit mode: calculate from original createdAt
                    // New mode: calculate from now
                    val baseDate = if (isEditMode && originalCreatedAt != null)
                        originalCreatedAt!!.toDate()
                    else
                        Date()

                    val cal = Calendar.getInstance().apply {
                        time = baseDate
                        add(Calendar.DAY_OF_YEAR, days)
                    }
                    tvExpiry?.text = "Expire le: ${
                        SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                            .format(cal.time)
                    }"
                } else {
                    tvExpiry?.text = ""
                }
            }
        })

        btnAddPhoto?.setOnClickListener {
            if (existingImageUrls.size + selectedImageUris.size >= 5) {
                Toast.makeText(requireContext(),
                    "Maximum 5 photos", Toast.LENGTH_SHORT).show()
            } else {
                pickImages.launch("image/*")
            }
        }

        // ──────────────────────────────────────────
        // PUBLISH / MODIFY CLICK
        // ──────────────────────────────────────────
        btnPublish?.setOnClickListener {
            val title       = etTitle?.text?.toString()?.trim() ?: ""
            val price       = etPrice?.text?.toString()?.trim() ?: ""
            val description = etDescription?.text?.toString()?.trim() ?: ""
            val durationStr = etDuration?.text?.toString()?.trim() ?: "30"

            // ── Validations ──
            if (title.isEmpty()) {
                etTitle?.error = "Titre requis"
                return@setOnClickListener
            }
            if (selectedCategory == null) {
                Toast.makeText(requireContext(),
                    "Choisissez une catégorie", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (price.isEmpty()) {
                etPrice?.error = "Prix requis"
                return@setOnClickListener
            }
            if (selectedCity == null) {
                Toast.makeText(requireContext(),
                    "Choisissez une ville", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (description.isEmpty()) {
                etDescription?.error = "Description requise"
                return@setOnClickListener
            }
            if (existingImageUrls.isEmpty() && selectedImageUris.isEmpty()) {
                Toast.makeText(requireContext(),
                    "📸 Au moins une photo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(),
                    "Connectez-vous", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnPublish.isEnabled = false
            showLoading(
                if (isEditMode) "⏳ Modification..." else "⏳ Publication...")

            val db       = FirebaseFirestore.getInstance()
            val priceVal = price.toDoubleOrNull() ?: 0.0
            val duration = durationStr.toIntOrNull() ?: 30

            if (isEditMode) {
                // ──────────────────────────────────────────
                // EDIT MODE
                // Calculate expiresAt using Calendar (DST safe)
                // Base = original createdAt, not now
                // ──────────────────────────────────────────
                val baseDate = if (originalCreatedAt != null)
                    originalCreatedAt!!.toDate()
                else
                    Date()

                val expiryCalendar = Calendar.getInstance().apply {
                    time = baseDate
                    add(Calendar.DAY_OF_YEAR, duration)
                    // Set to end of day so it doesn't expire mid-day
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }

                val newExpiresAt = Timestamp(expiryCalendar.time)

                android.util.Log.d("PostAd",
                    "Edit: createdAt=$baseDate" +
                            " duration=$duration days" +
                            " newExpiresAt=${expiryCalendar.time}")

                val updateData = hashMapOf<String, Any>(
                    "title"       to title,
                    "description" to description,
                    "price"       to priceVal,
                    "category"    to selectedCategory!!,
                    "city"        to selectedCity!!,
                    "emoji"       to (emojiMap[selectedCategory] ?: "📦"),
                    "duration"    to duration,
                    "expiresAt"   to newExpiresAt  // ← Always recalculated
                )

                if (selectedImageUris.isNotEmpty()) {
                    uploadToCloudinary(editAdId!!) { newUrls ->
                        updateData["imageUrls"] = existingImageUrls + newUrls
                        db.collection("ads").document(editAdId)
                            .update(updateData)
                            .addOnSuccessListener {
                                onPublishSuccess(
                                    "✅ Annonce modifiée!", btnPublish)
                            }
                            .addOnFailureListener { e ->
                                onPublishFailure(e, btnPublish)
                            }
                    }
                } else {
                    updateData["imageUrls"] = existingImageUrls
                    db.collection("ads").document(editAdId!!)
                        .update(updateData)
                        .addOnSuccessListener {
                            onPublishSuccess("✅ Annonce modifiée!", btnPublish)
                        }
                        .addOnFailureListener { e ->
                            onPublishFailure(e, btnPublish)
                        }
                }

            } else {
                // ──────────────────────────────────────────
                // NEW AD
                // Also use Calendar for DST safety
                // ──────────────────────────────────────────
                val adId    = db.collection("ads").document().id
                val nowDate = Date()

                val expiryCalendar = Calendar.getInstance().apply {
                    time = nowDate
                    add(Calendar.DAY_OF_YEAR, duration)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }

                val nowTimestamp      = Timestamp(nowDate)
                val expiresAtTimestamp = Timestamp(expiryCalendar.time)

                android.util.Log.d("PostAd",
                    "New ad: now=$nowDate" +
                            " duration=$duration days" +
                            " expiresAt=${expiryCalendar.time}")

                uploadToCloudinary(adId) { imageUrls ->
                    db.collection("ads").document(adId).set(
                        hashMapOf(
                            "id"          to adId,
                            "userId"      to user.uid,
                            "userName"    to (user.displayName ?: "Anonyme"),
                            "title"       to title,
                            "description" to description,
                            "price"       to priceVal,
                            "category"    to selectedCategory!!,
                            "city"        to selectedCity!!,
                            "imageUrls"   to imageUrls,
                            "emoji"       to (emojiMap[selectedCategory] ?: "📦"),
                            "status"      to "active",
                            "views"       to 0,
                            "createdAt"   to nowTimestamp,
                            "expiresAt"   to expiresAtTimestamp,
                            "duration"    to duration
                        )
                    ).addOnSuccessListener {
                        db.collection("users").document(user.uid)
                            .update("adsCount", FieldValue.increment(1))
                        onPublishSuccess("✅ Annonce publiée!", btnPublish)
                    }.addOnFailureListener { e ->
                        onPublishFailure(e, btnPublish)
                    }
                }
            }
        }
    }

    private fun showLoading(msg: String) {
        loadingDialog = android.app.AlertDialog.Builder(
            requireContext(), R.style.AppAlertDialog
        ).setView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(64, 48, 64, 48)
            setBackgroundResource(R.drawable.bg_bottom_sheet_glass)
            addView(ProgressBar(requireContext()))
            addView(TextView(requireContext()).apply {
                text = msg
                setTextColor(android.graphics.Color.WHITE)
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 24, 0, 0)
            })
        }).setCancelable(false).create()
        loadingDialog?.show()
    }

    private fun onPublishSuccess(msg: String, btn: View) {
        loadingDialog?.dismiss()
        loadingDialog = null
        btn.isEnabled = true
        if (isAdded) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun onPublishFailure(e: Exception, btn: View) {
        loadingDialog?.dismiss()
        loadingDialog = null
        btn.isEnabled = true
        if (isAdded) Toast.makeText(
            requireContext(),
            "❌ ${e.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun uploadToCloudinary(
        adId: String,
        onComplete: (List<String>) -> Unit
    ) {
        if (selectedImageUris.isEmpty()) {
            onComplete(emptyList())
            return
        }

        val urls   = Collections.synchronizedList(mutableListOf<String>())
        val errors = Collections.synchronizedList(mutableListOf<Int>())
        var completed = 0
        val total     = selectedImageUris.size

        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!isAdded) return@Runnable
            loadingDialog?.dismiss()
            loadingDialog = null
            view?.findViewById<View>(R.id.btnPublish)?.isEnabled = true
            Toast.makeText(
                requireContext(),
                "❌ Délai dépassé. Vérifiez votre connexion et réessayez.",
                Toast.LENGTH_LONG
            ).show()
        }
        timeoutHandler.postDelayed(timeoutRunnable, 60_000L)

        selectedImageUris.forEachIndexed { index, uri ->
            Thread {
                try {
                    val bytes = requireContext().contentResolver
                        .openInputStream(uri)?.readBytes()

                    if (bytes == null || bytes.isEmpty()) {
                        errors.add(index)
                        checkIfDone(
                            ++completed, total, urls, errors,
                            timeoutHandler, timeoutRunnable, onComplete
                        )
                        return@Thread
                    }

                    val boundary = "----CB${System.currentTimeMillis()}"
                    val conn = URL(
                        "https://api.cloudinary.com/v1_1/" +
                                "$CLOUDINARY_CLOUD/image/upload"
                    ).openConnection() as HttpURLConnection

                    conn.connectTimeout = 30_000
                    conn.readTimeout    = 30_000
                    conn.doOutput       = true
                    conn.requestMethod  = "POST"
                    conn.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=$boundary"
                    )

                    val out = conn.outputStream

                    fun writePart(name: String, value: String) {
                        out.write((
                                "--$boundary\r\n" +
                                        "Content-Disposition: form-data;" +
                                        " name=\"$name\"\r\n\r\n$value\r\n"
                                ).toByteArray())
                    }

                    writePart("upload_preset", CLOUDINARY_PRESET)
                    writePart("folder", "ads/$adId")

                    val fileHeader = "--$boundary\r\n" +
                            "Content-Disposition: form-data;" +
                            " name=\"file\";" +
                            " filename=\"p$index.jpg\"\r\n" +
                            "Content-Type: image/jpeg\r\n\r\n"
                    out.write(fileHeader.toByteArray())
                    out.write(bytes)
                    out.write("\r\n".toByteArray())
                    out.write("--$boundary--\r\n".toByteArray())
                    out.flush()
                    out.close()

                    if (conn.responseCode == 200) {
                        val url = JSONObject(
                            conn.inputStream.bufferedReader().readText()
                        ).getString("secure_url")
                        urls.add(url)
                        android.util.Log.d("Cloudinary",
                            "Image $index uploaded: $url")
                    } else {
                        val err = conn.errorStream
                            ?.bufferedReader()?.readText() ?: "Unknown"
                        android.util.Log.e("Cloudinary",
                            "Failed [${conn.responseCode}]: $err")
                        errors.add(index)
                    }

                } catch (e: java.net.SocketTimeoutException) {
                    android.util.Log.e("Cloudinary",
                        "Timeout image $index")
                    errors.add(index)
                } catch (e: Exception) {
                    android.util.Log.e("Cloudinary",
                        "Error image $index: ${e.message}")
                    errors.add(index)
                } finally {
                    checkIfDone(
                        ++completed, total, urls, errors,
                        timeoutHandler, timeoutRunnable, onComplete
                    )
                }
            }.start()
        }
    }

    private fun checkIfDone(
        completed: Int, total: Int,
        urls: List<String>, errors: List<Int>,
        timeoutHandler: android.os.Handler,
        timeoutRunnable: Runnable,
        onComplete: (List<String>) -> Unit
    ) {
        if (completed < total) return
        timeoutHandler.removeCallbacks(timeoutRunnable)

        activity?.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            when {
                urls.isEmpty() -> {
                    loadingDialog?.dismiss()
                    loadingDialog = null
                    view?.findViewById<View>(R.id.btnPublish)?.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "❌ Échec de l'upload. Vérifiez votre connexion.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                errors.isNotEmpty() -> {
                    Toast.makeText(
                        requireContext(),
                        "⚠️ ${errors.size} image(s) non uploadée(s)," +
                                " publication avec ${urls.size} image(s)",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete(urls)
                }
                else -> onComplete(urls)
            }
        }
    }

    private fun showAllImages() {
        val container = view?.findViewById<LinearLayout>(R.id.photoContainer)
            ?: return
        while (container.childCount > 1) container.removeViewAt(1)
        val dp = resources.displayMetrics.density

        existingImageUrls.toList().forEach { url ->
            val frame = android.widget.FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (100 * dp).toInt(), (100 * dp).toInt()
                ).apply { marginEnd = (8 * dp).toInt() }
            }
            val img = ImageView(requireContext()).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_photo_add)
                clipToOutline = true
            }
            com.bumptech.glide.Glide.with(this).load(url).centerCrop().into(img)
            val btn = TextView(requireContext()).apply {
                text = "✕"
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    (24 * dp).toInt(), (24 * dp).toInt()
                ).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.END
                }
                setOnClickListener {
                    existingImageUrls.remove(url)
                    showAllImages()
                }
            }
            frame.addView(img)
            frame.addView(btn)
            container.addView(frame)
        }

        selectedImageUris.toList().forEach { uri ->
            val frame = android.widget.FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (100 * dp).toInt(), (100 * dp).toInt()
                ).apply { marginEnd = (8 * dp).toInt() }
            }
            val img = ImageView(requireContext()).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageURI(uri)
                setBackgroundResource(R.drawable.bg_photo_add)
                clipToOutline = true
            }
            val btn = TextView(requireContext()).apply {
                text = "✕"
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    (24 * dp).toInt(), (24 * dp).toInt()
                ).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.END
                }
                setOnClickListener {
                    selectedImageUris.remove(uri)
                    showAllImages()
                }
            }
            frame.addView(img)
            frame.addView(btn)
            container.addView(frame)
        }

        container.getChildAt(0)?.visibility =
            if (existingImageUrls.size + selectedImageUris.size >= 5)
                View.GONE else View.VISIBLE
    }

    private fun showPickerSheet(
        title: String, items: List<String>,
        selected: String?, searchable: Boolean,
        onSelect: (String) -> Unit
    ) {
        val dialog = BottomSheetDialog(
            requireContext(), R.style.GlassBottomSheetDialog)
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 24, 0, 32)
            setBackgroundResource(R.drawable.bg_picker_dropdown)
        }
        root.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(40), dpToPx(4)
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(16)
            }
            setBackgroundResource(R.drawable.bg_drag_handle)
        })
        root.addView(TextView(requireContext()).apply {
            text = title
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            typeface = resources.getFont(R.font.poppins_bold)
            setPadding(dpToPx(24), 0, dpToPx(24), dpToPx(12))
        })
        root.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply {
                marginStart = dpToPx(24)
                marginEnd   = dpToPx(24)
            }
            setBackgroundColor(android.graphics.Color.parseColor("#1AFFFFFF"))
        })

        val scrollContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#CC0D1F17"))
        }

        if (searchable) {
            val sb = EditText(requireContext()).apply {
                hint = "🔍 Rechercher..."
                setHintTextColor(android.graphics.Color.parseColor("#4DFFFFFF"))
                setTextColor(android.graphics.Color.WHITE)
                textSize = 13f
                setBackgroundResource(R.drawable.bg_input_post)
                setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(40)
                ).apply {
                    marginStart  = dpToPx(24)
                    marginEnd    = dpToPx(24)
                    topMargin    = dpToPx(8)
                    bottomMargin = dpToPx(8)
                }
            }
            sb.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(
                    s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    scrollContainer.removeAllViews()
                    val q = s?.toString()?.lowercase() ?: ""
                    items.filter { it.lowercase().contains(q) }.forEach {
                        scrollContainer.addView(
                            makePickerItem(it, it == selected) {
                                onSelect(it); dialog.dismiss()
                            })
                    }
                }
            })
            root.addView(sb)
        }

        val sv = android.widget.ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(280)
            )
        }
        items.forEach {
            scrollContainer.addView(makePickerItem(it, it == selected) {
                onSelect(it); dialog.dismiss()
            })
        }
        sv.addView(scrollContainer)
        root.addView(sv)
        dialog.setContentView(root)
        dialog.show()
    }

    private fun makePickerItem(
        text: String, isSelected: Boolean,
        onClick: () -> Unit
    ): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpToPx(24), dpToPx(14), dpToPx(24), dpToPx(14))
            setOnClickListener { onClick() }
            isClickable = true
            isFocusable = true
            val ta = context.obtainStyledAttributes(
                intArrayOf(android.R.attr.selectableItemBackground))
            foreground = ta.getDrawable(0)
            ta.recycle()
            addView(TextView(context).apply {
                this.text = text
                setTextColor(
                    if (isSelected)
                        android.graphics.Color.parseColor("#2ECC71")
                    else
                        android.graphics.Color.parseColor("#CCFFFFFF")
                )
                textSize = 14f
                typeface = resources.getFont(
                    if (isSelected) R.font.poppins_semibold
                    else R.font.poppins_regular
                )
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (isSelected) addView(TextView(context).apply {
                this.text = "✓"
                setTextColor(android.graphics.Color.parseColor("#2ECC71"))
                textSize = 16f
            })
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        loadingDialog?.dismiss()
        loadingDialog = null
        super.onDestroyView()
    }
}