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
import java.text.SimpleDateFormat
import java.util.*

class PostAdFragment : Fragment() {

    private val categories = listOf(
        "Maison", "Électronique", "Voitures",
        "Vêtements", "Immobilier", "Artisanat", "Autre"
    )
    private val cities = listOf(
        "Marrakech", "Casablanca", "Rabat", "Fès",
        "Tanger", "Agadir", "Meknès", "Oujda",
        "Kénitra", "Tétouan", "Safi", "El Jadida",
        "Nador", "Béni Mellal", "Khémisset", "Settat"
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

        // Category picker
        spinnerCategory?.setOnClickListener {
            showPickerSheet(
                title = getString(R.string.ad_field_category),
                items = categories,
                selected = selectedCategory,
                searchable = false
            ) { selected ->
                selectedCategory = selected
                tvCategory?.text = selected
                tvCategory?.setTextColor(
                    android.graphics.Color.WHITE
                )
            }
        }

        // City picker (searchable)
        spinnerCity?.setOnClickListener {
            showPickerSheet(
                title = getString(R.string.ad_field_city),
                items = cities,
                selected = selectedCity,
                searchable = true
            ) { selected ->
                selectedCity = selected
                tvCity?.text = selected
                tvCity?.setTextColor(
                    android.graphics.Color.WHITE
                )
            }
        }

        // Description character counter
        etDescription?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}
            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {}
            override fun afterTextChanged(s: Editable?) {
                tvCharCount?.text = "${s?.length ?: 0}/500"
            }
        })

        // Duration → expiry date
        etDuration?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}
            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {}
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
            Toast.makeText(
                requireContext(),
                "Bientôt: sélection de photos",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnPublish?.setOnClickListener {
            val title = etTitle?.text?.toString()?.trim()
            val price = etPrice?.text?.toString()?.trim()
            val description = etDescription?.text?.toString()?.trim()

            when {
                title.isNullOrEmpty() -> {
                    etTitle?.error = "Titre requis"
                    return@setOnClickListener
                }
                selectedCategory == null -> {
                    Toast.makeText(
                        requireContext(),
                        "Choisissez une catégorie",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                price.isNullOrEmpty() -> {
                    etPrice?.error = "Prix requis"
                    return@setOnClickListener
                }
                selectedCity == null -> {
                    Toast.makeText(
                        requireContext(),
                        "Choisissez une ville",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                description.isNullOrEmpty() -> {
                    etDescription?.error = "Description requise"
                    return@setOnClickListener
                }
            }

            Toast.makeText(
                requireContext(),
                "Annonce publiée! 🎉",
                Toast.LENGTH_SHORT
            ).show()

            btnPublish.animate()
                .scaleX(0.95f).scaleY(0.95f).setDuration(100)
                .withEndAction {
                    btnPublish.animate()
                        .scaleX(1f).scaleY(1f).setDuration(150)
                        .withEndAction {
                            requireActivity()
                                .onBackPressedDispatcher.onBackPressed()
                        }.start()
                }.start()
        }
    }

    private fun showPickerSheet(
        title: String,
        items: List<String>,
        selected: String?,
        searchable: Boolean,
        onSelect: (String) -> Unit
    ) {
        val dialog = BottomSheetDialog(
            requireContext(), R.style.GlassBottomSheetDialog
        )

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 24, 0, 32)
            setBackgroundResource(R.drawable.bg_picker_dropdown)
        }

        val handle = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(40), dpToPx(4)
            ).apply {
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
            setBackgroundColor(
                android.graphics.Color.parseColor("#CC0F2318")
            )
        }

        var filteredItems = items.toMutableList()

        if (searchable) {
            val searchBar = EditText(requireContext()).apply {
                hint = "Rechercher..."
                setHintTextColor(
                    android.graphics.Color.parseColor("#4DFFFFFF")
                )
                setTextColor(android.graphics.Color.WHITE)
                textSize = 13f
                typeface = resources.getFont(R.font.poppins_regular)
                setBackgroundResource(R.drawable.bg_input_post)
                setPadding(
                    dpToPx(16), dpToPx(10),
                    dpToPx(16), dpToPx(10)
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(40)
                ).apply {
                    marginStart = dpToPx(24)
                    marginEnd = dpToPx(24)
                    topMargin = dpToPx(8)
                    bottomMargin = dpToPx(8)
                }
            }

            searchBar.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {}
                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {}
                override fun afterTextChanged(s: Editable?) {
                    val query = s?.toString()?.lowercase() ?: ""
                    scrollContainer.removeAllViews()
                    val filtered = items.filter {
                        it.lowercase().contains(query)
                    }
                    filtered.forEach { item ->
                        scrollContainer.addView(
                            makePickerItem(item, item == selected) {
                                onSelect(item)
                                dialog.dismiss()
                            }
                        )
                    }
                }
            })

            root.addView(searchBar)
        }

        // Scrollable item list
        val scrollView = android.widget.ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(280)
            )
        }

        items.forEach { item ->
            scrollContainer.addView(
                makePickerItem(item, item == selected) {
                    onSelect(item)
                    dialog.dismiss()
                }
            )
        }

        scrollView.addView(scrollContainer)
        root.addView(scrollView)

        dialog.setContentView(root)
        dialog.show()
    }

    private fun makePickerItem(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpToPx(24), dpToPx(14), dpToPx(24), dpToPx(14))
            setBackgroundColor(
                android.graphics.Color.parseColor("#00000000")
            )

            setOnClickListener { onClick() }
            isClickable = true
            isFocusable = true

            val attrs = intArrayOf(android.R.attr.selectableItemBackground)
            val ta = context.obtainStyledAttributes(attrs)
            foreground = ta.getDrawable(0)
            ta.recycle()

            val tv = TextView(context).apply {
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
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }
            addView(tv)

            // Checkmark for selected
            if (isSelected) {
                val check = TextView(context).apply {
                    this.text = "✓"
                    setTextColor(
                        android.graphics.Color.parseColor("#2ECC71")
                    )
                    textSize = 16f
                    typeface = resources.getFont(R.font.poppins_bold)
                }
                addView(check)
            }
        }
    }

    private fun makeDivider(): View {
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply {
                marginStart = dpToPx(24)
                marginEnd = dpToPx(24)
            }
            setBackgroundColor(
                android.graphics.Color.parseColor("#1AFFFFFF")
            )
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}