package com.madinatti.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CityPickerBottomSheet : BottomSheetDialogFragment() {

    private var onCitySelected: ((String) -> Unit)? = null

    companion object {
        private val ALL_CITIES = listOf(
            "Toutes les villes",
            "Agadir", "Al Hoceima", "Azrou", "Beni Mellal", "Berrechid",
            "Casablanca", "Chefchaouen", "El Jadida", "Errachidia", "Essaouira",
            "Fes", "Guelmim", "Ifrane", "Kenitra", "Khemisset",
            "Khouribga", "Laayoune", "Larache", "Marrakech", "Meknes",
            "Mohammedia", "Nador", "Ouarzazate", "Oujda", "Rabat",
            "Safi", "Sale", "Settat", "Sidi Kacem", "Tanger",
            "Taza", "Temara", "Tetouan", "Tiznit"
        )

        fun newInstance(onSelected: (String) -> Unit): CityPickerBottomSheet {
            return CityPickerBottomSheet().apply {
                onCitySelected = onSelected
            }
        }
    }

    override fun getTheme(): Int = R.style.AppBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_city_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etSearch = view.findViewById<EditText>(R.id.etCitySearch)
        val rvCities = view.findViewById<RecyclerView>(R.id.rvCities)

        val adapter = CityAdapter(ALL_CITIES.toMutableList()) { city ->
            onCitySelected?.invoke(city)
            dismiss()
        }

        rvCities.layoutManager = LinearLayoutManager(requireContext())
        rvCities.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = normalize(s.toString().trim())
                val filtered = if (query.isEmpty()) ALL_CITIES
                else ALL_CITIES.filter { normalize(it).contains(query) }
                adapter.updateList(filtered)
            }

            private fun normalize(text: String): String {
                return java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD)
                    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            }

        })
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.65).toInt()
                sheet.setBackgroundResource(R.drawable.bg_bottom_sheet)
            }
        }
    }

    // ── Inner Adapter ──
    class CityAdapter(
        private var cities: MutableList<String>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<CityAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvCity: TextView = view.findViewById(R.id.tvCityName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_city_row, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.tvCity.text = cities[position]
            holder.itemView.setOnClickListener { onClick(cities[position]) }
        }

        override fun getItemCount() = cities.size

        fun updateList(newList: List<String>) {
            cities.clear()
            cities.addAll(newList)
            notifyDataSetChanged()
        }
    }
}