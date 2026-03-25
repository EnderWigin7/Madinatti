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
import java.text.SimpleDateFormat
import java.util.*

class PostAdFragment : Fragment() {

    private val categories = listOf(
        "Maison", "Électronique", "Véhicules",
        "Vêtements", "Immobilier", "Artisanat",
        "Mode", "Sports", "Livres", "Gaming", "Services", "Autre"
    )
    private val cities = listOf(
        "Agadir", "Al Hoceima", "Azrou", "Béni Mellal", "Berrechid",
        "Casablanca", "Chefchaouen", "El Jadida", "Errachidia", "Essaouira",
        "Fès", "Guelmim", "Ifrane", "Kénitra", "Khemisset",
        "Khouribga", "Laâyoune", "Larache", "Marrakech", "Meknès",
        "Mohammedia", "Nador", "Ouarzazate", "Oujda", "Rabat",
        "Safi", "Salé", "Settat", "Sidi Kacem", "Tanger",
        "Taza", "Témara", "Tétouan", "Tiznit"
    )

    private val emojiMap = mapOf(
        "Électronique" to "📱", "Véhicules" to "🚗", "Immobilier" to "🏠",
        "Mode" to "👗", "Maison" to "🛋", "Sports" to "⚽",
        "Livres" to "📚", "Gaming" to "🎮", "Services" to "🔧",
        "Vêtements" to "👕", "Artisanat" to "🎨", "Autre" to "📦"
    )

    private var selectedCategory: String? = null
    private var selectedCity: String? = null

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

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val spinnerCategory = view.findViewById<View>(R.id.spinnerCategory)
        val tvCategory = view.findViewById<TextView>(R.id.tvCategory)
        val etPrice = view.findViewById<EditText>(R.id.etPrice)
        val spinnerCity = view.findViewById<View>(R.id.spinnerCity)
        val tvCity = view.findViewById<TextView>(R.id.tvCity)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        val tvCharCount = view.findViewById<TextView>(R.id.tvCharCount)
        val etDuration = view.findViewById<EditText>(R.id.etDuration)
        val tvExpiry = view.findViewById<TextView>(R.id.tvExpiryDate)
        val btnPublish = view.findViewById<View>(R.id.btnPublish)
        val btnAddPhoto = view.findViewById<View>(R.id.btnAddPhoto)

        btnBack?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        spinnerCategory?.setOnClickListener {
            showPickerSheet(
                title = getString(R.string.ad_field_category),
                items = categories,
                selected = selectedCategory,
                searchable = false
            ) { selected ->
                selectedCategory = selected
                tvCategory?.text = selected
                tvCategory?.setTextColor(android.graphics.Color.WHITE)
            }
        }

        spinnerCity?.setOnClickListener {
            showPickerSheet(
                title = getString(R.string.ad_field_city),
                items = cities,
                selected = selectedCity,
                searchable = true
            ) { selected ->
                selectedCity = selected
                tvCity?.text = selected
                tvCity?.setTextColor(android.graphics.Color.WHITE)
            }
        }

        etDescription?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tvCharCount?.text = "${s?.length ?: 0}/500"
            }
        })

        etDuration?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val days = s?.toString()?.toIntOrNull()
                if (days != null && days > 0) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, days)
                    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                    tvExpiry?.text = "Expire le: ${fmt.format(cal.time)}"
                } else {
                    tvExpiry?.text = ""
                }
            }
        })

        btnAddPhoto?.setOnClickListener {
            Toast.makeText(requireContext(), "📸 Photos — bientôt disponible", Toast.LENGTH_SHORT).show()
        }

        // ── PUBLISH TO FIRESTORE ──
        btnPublish?.setOnClickListener {
            val title = etTitle?.text?.toString()?.trim() ?: ""
            val price = etPrice?.text?.toString()?.trim() ?: ""
            val description = etDescription?.text?.toString()?.trim() ?: ""
            val durationStr = etDuration?.text?.toString()?.trim() ?: "30"

            // Validation
            if (title.isEmpty()) { etTitle?.error = "Titre requis"; return@setOnClickListener }
            if (selectedCategory == null) {
                Toast.makeText(requireContext(), "Choisissez une catégorie", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (price.isEmpty()) { etPrice?.error = "Prix requis"; return@setOnClickListener }
            if (selectedCity == null) {
                Toast.makeText(requireContext(), "Choisissez une ville", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (description.isEmpty()) { etDescription?.error = "Description requise"; return@setOnClickListener }

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "Connectez-vous d'abord", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnPublish.isEnabled = false
            val db = FirebaseFirestore.getInstance()
            val adId = db.collection("ads").document().id
            val priceVal = price.toDoubleOrNull() ?: 0.0
            val duration = durationStr.toIntOrNull() ?: 30
            val now = Timestamp.now()
            val expiresAt = Timestamp(now.seconds + (duration * 86400L), 0)

            val adData = hashMapOf(
                "id" to adId,
                "userId" to user.uid,
                "userName" to (user.displayName ?: "Anonyme"),
                "title" to title,
                "description" to description,
                "price" to priceVal,
                "category" to selectedCategory!!,
                "city" to selectedCity!!,
                "imageUrls" to emptyList<String>(),
                "emoji" to (emojiMap[selectedCategory] ?: "📦"),
                "status" to "active",
                "views" to 0,
                "createdAt" to now,
                "expiresAt" to expiresAt,
                "duration" to duration
            )

            db.collection("ads").document(adId)
                .set(adData)
                .addOnSuccessListener {
                    // Update user's ad count
                    db.collection("users").document(user.uid)
                        .update("adsCount", FieldValue.increment(1))

                    Toast.makeText(requireContext(), "✅ Annonce publiée!", Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
                .addOnFailureListener { e ->
                    btnPublish.isEnabled = true
                    Toast.makeText(requireContext(), "❌ Erreur: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun showPickerSheet(
        title: String, items: List<String>, selected: String?,
        searchable: Boolean, onSelect: (String) -> Unit
    ) {
        val dialog = BottomSheetDialog(requireContext(), R.style.GlassBottomSheetDialog)
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 24, 0, 32)
            setBackgroundResource(R.drawable.bg_picker_dropdown)
        }

        val handle = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(4)).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(16)
            }
            setBackgroundResource(R.drawable.bg_drag_handle)
        }
        root.addView(handle)

        val tvTitle = TextView(requireContext()).apply {
            text = title
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            typeface = resources.getFont(R.font.poppins_bold)
            setPadding(dpToPx(24), 0, dpToPx(24), dpToPx(12))
        }
        root.addView(tvTitle)
        root.addView(makeDivider())

        val scrollContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#CC0F2318"))
        }

        if (searchable) {
            val searchBar = EditText(requireContext()).apply {
                hint = "🔍 Rechercher..."
                setHintTextColor(android.graphics.Color.parseColor("#4DFFFFFF"))
                setTextColor(android.graphics.Color.WHITE)
                textSize = 13f
                typeface = resources.getFont(R.font.poppins_regular)
                setBackgroundResource(R.drawable.bg_input_post)
                setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(40)
                ).apply {
                    marginStart = dpToPx(24); marginEnd = dpToPx(24)
                    topMargin = dpToPx(8); bottomMargin = dpToPx(8)
                }
            }
            searchBar.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val query = s?.toString()?.lowercase() ?: ""
                    scrollContainer.removeAllViews()
                    items.filter { it.lowercase().contains(query) }.forEach { item ->
                        scrollContainer.addView(makePickerItem(item, item == selected) {
                            onSelect(item); dialog.dismiss()
                        })
                    }
                }
            })
            root.addView(searchBar)
        }

        val scrollView = android.widget.ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(280)
            )
        }
        items.forEach { item ->
            scrollContainer.addView(makePickerItem(item, item == selected) {
                onSelect(item); dialog.dismiss()
            })
        }
        scrollView.addView(scrollContainer)
        root.addView(scrollView)
        dialog.setContentView(root)
        dialog.show()
    }

    private fun makePickerItem(text: String, isSelected: Boolean, onClick: () -> Unit): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpToPx(24), dpToPx(14), dpToPx(24), dpToPx(14))
            setOnClickListener { onClick() }
            isClickable = true; isFocusable = true
            val attrs = intArrayOf(android.R.attr.selectableItemBackground)
            val ta = context.obtainStyledAttributes(attrs)
            foreground = ta.getDrawable(0); ta.recycle()

            addView(TextView(context).apply {
                this.text = text
                setTextColor(if (isSelected) android.graphics.Color.parseColor("#2ECC71")
                else android.graphics.Color.parseColor("#CCFFFFFF"))
                textSize = 14f
                typeface = resources.getFont(if (isSelected) R.font.poppins_semibold else R.font.poppins_regular)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (isSelected) addView(TextView(context).apply {
                this.text = "✓"; setTextColor(android.graphics.Color.parseColor("#2ECC71"))
                textSize = 16f; typeface = resources.getFont(R.font.poppins_bold)
            })
        }
    }

    private fun makeDivider(): View {
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                marginStart = dpToPx(24); marginEnd = dpToPx(24)
            }
            setBackgroundColor(android.graphics.Color.parseColor("#1AFFFFFF"))
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}