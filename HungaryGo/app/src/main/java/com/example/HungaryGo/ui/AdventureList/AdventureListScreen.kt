package com.example.HungaryGo.ui.AdventureList

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.HungaryGo.FiltersData
import com.example.HungaryGo.LocationPackData
import com.example.HungaryGo.R
import com.example.HungaryGo.ui.Main.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.material.slider.RangeSlider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout

private lateinit var mainViewModel: MainViewModel
private lateinit var fusedLocationClient: FusedLocationProviderClient

private var completedLPList = mutableListOf<String>()
private var startedLPList = mutableListOf<String>()
private var filters: FiltersData? = null

class AdventureListScreen : AppCompatActivity() {

    private val adventureListViewModel: AdventureListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adventure_list)

        filters = FiltersData()


        val listData: MutableList<LocationPackData> = mutableListOf()
        val adapter = object: ArrayAdapter<LocationPackData>(this, R.layout.listitemslayout, listData){
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.listitemslayout, parent, false)
                val lpName: TextView = view.findViewById(R.id.lpName)
                val lpLocationCount: TextView = view.findViewById(R.id.lpLocationCount)
                val lpRating: TextView = view.findViewById(R.id.lpRating)
                val lpAreaImg: ImageView = view.findViewById(R.id.areaImg)

                val locationPack = getItem(position)

                lpName.text = locationPack?.name
                lpLocationCount.text = "Helyszínek száma: ${locationPack?.locations?.size.toString()}"
                lpRating.text = "Értékelés: %.2f".format(locationPack?.rating)
                val resourceId = context.resources.getIdentifier(locationPack?.area, "drawable", context.packageName)
                lpAreaImg.setImageResource(resourceId)

                val backgroundColor = when {
                    completedLPList.contains(locationPack?.name) -> R.color.lightGreen
                    startedLPList.contains(locationPack?.name) -> R.color.lightOrange
                    else -> R.color.white
                }

                view.setBackgroundColor(ContextCompat.getColor(context, backgroundColor))

                return view
            }
        }

        val textInputLayout: TextInputLayout = findViewById(R.id.dropdownList)
        val autoCompleteTextView: MaterialAutoCompleteTextView = findViewById(R.id.dropdownList_item)
        val searchBar: EditText = findViewById(R.id.searchBar)

        val intent = intent
        val locationPackDataList = intent.getSerializableExtra("locationPackList") as? ArrayList<LocationPackData>
            ?: arrayListOf()

        adventureListViewModel.completedAndStartedLocationsToList()

        for (locationPackData in locationPackDataList)
        {
            listData.add(locationPackData)
        }


        val listView: ListView = findViewById(R.id.lista)
       listView.adapter = adapter

        autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) as String
            val orderedLocationPackDataList: List<LocationPackData>
            when(selectedItem){
                "A-Zs" -> {
                    orderedLocationPackDataList = filteredLocationPacks(locationPackDataList).sortedBy { it.name }
                }
                "Zs-A" -> {
                    orderedLocationPackDataList = filteredLocationPacks(locationPackDataList).sortedByDescending { it.name }
                }
                "értékelés" -> {
                    orderedLocationPackDataList = filteredLocationPacks(locationPackDataList).sortedByDescending { it.rating }
                }
                "népszerűség" -> {
                    orderedLocationPackDataList = filteredLocationPacks(locationPackDataList).sortedByDescending { it.completionNumber }
                }
                "hossz szerint növekvő" -> {
                    orderedLocationPackDataList = filteredLocationPacks(locationPackDataList).sortedBy { it.locations.size }
                }
                "hossz szerint csökkenő" -> {
                    orderedLocationPackDataList = filteredLocationPacks(locationPackDataList).sortedByDescending { it.locations.size }
                }
                else -> {
                    orderedLocationPackDataList = filteredLocationPacks(locationPackDataList)
                }
            }

            listData.clear()
            for (locationPackData in orderedLocationPackDataList)
            {
                listData.add(locationPackData)
            }

            listView.adapter = adapter

        }

        searchBar.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int){}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                filterList(s.toString(), adapter, filteredLocationPacks(locationPackDataList))
            }
        })

        adventureListViewModel.completedLPList.observe(this, Observer { result ->
            completedLPList = result
            listView.adapter = adapter
        })
        adventureListViewModel.startedLPlist.observe(this, Observer { result ->
            startedLPList = result
            listView.adapter = adapter
        })

        val filterButton = findViewById<Button>(R.id.filterButton)

        filterButton.setOnClickListener{
            openFilterDialog(locationPackDataList,adapter)
        }

    }


    fun filterList(searchedChars: String, adapter: ArrayAdapter<LocationPackData>, listData: List<LocationPackData>) {
        val filteredList = listData.filter { it.name.contains(searchedChars) }
        adapter.clear()
        adapter.addAll(filteredList)
        adapter.notifyDataSetChanged()
    }


    fun filteredLocationPacks(originalList: List<LocationPackData>): List<LocationPackData>{
        return originalList.filter { pack ->

            val teljesitesFuggoben = filters!!.teljesitesFuggoben && startedLPList.contains(pack.name)
            val teljesitesTeljesitett = filters!!.teljesitesTeljesitett && completedLPList.contains(pack.name)
            val teljesitesHatralevo = filters!!.teljesitesHatralevo && !startedLPList.contains(pack.name) && !completedLPList.contains(pack.name)

            val teljesitesSzuro = teljesitesFuggoben || teljesitesHatralevo || teljesitesTeljesitett

            val helyszinSzamMin = filters!!.helyszinszamMin
            val helyszinSzamMax = filters!!.helyszinszamMax

            val helyszinSzamSzuro = pack.locations.size in helyszinSzamMin..helyszinSzamMax

            val tipusSzuro = when (pack.maker) {
                "builtIn" -> filters!!.tipusBeepitett
                "community" -> filters!!.tipusKozossegi
                else -> false
            }

            val teruletSzuro = when (pack.area) {
                "falu" -> filters!!.teruletFalusi
                "varos" -> filters!!.teruletVarosi
                "orszag" -> filters!!.teruletOrszagos
                "nemzetkozi" -> filters!!.teruletNemzetkozi
                else -> false
            }

            teljesitesSzuro && teruletSzuro && tipusSzuro && helyszinSzamSzuro
        }

    }

    fun displaySortedList(view: View) {

    }

    fun openFilterDialog(originalList: List<LocationPackData>, adapter: ArrayAdapter<LocationPackData>) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.filter_dialog)
        dialog.setCancelable(true)
        val seekBar = dialog.findViewById<RangeSlider>(R.id.rangeSeekBar)
        seekBar.setValues(1f, 10f)
        val teljesitesTeljesitett = dialog.findViewById<CheckBox>(R.id.teljesitesTeljesitett)
        val teljesitesFuggoben = dialog.findViewById<CheckBox>(R.id.teljesitesFuggoben)
        val teljesitesHatralevo = dialog.findViewById<CheckBox>(R.id.teljesitesHatralevo)
        val tipusBeepitett = dialog.findViewById<CheckBox>(R.id.tipusBeepitett)
        val tipusKozossegi = dialog.findViewById<CheckBox>(R.id.tipusKozossegi)
        val teruletFalusi =  dialog.findViewById<CheckBox>(R.id.teruletFalusi)
        val teruletVarosi =  dialog.findViewById<CheckBox>(R.id.teruletVarosi)
        val teruletOrszagos =  dialog.findViewById<CheckBox>(R.id.teruletOrszagos)
        val teruletNemzetkozi =  dialog.findViewById<CheckBox>(R.id.teruletNemzetkozi)
        val applyButton = dialog.findViewById<Button>(R.id.applyButton)

        teljesitesTeljesitett.isChecked = filters!!.teljesitesTeljesitett
        teljesitesFuggoben.isChecked = filters!!.teljesitesFuggoben
        teljesitesHatralevo.isChecked = filters!!.teljesitesHatralevo
        tipusBeepitett.isChecked = filters!!.tipusBeepitett
        tipusKozossegi.isChecked = filters!!.tipusKozossegi
        teruletFalusi.isChecked = filters!!.teruletFalusi
        teruletVarosi.isChecked = filters!!.teruletVarosi
        teruletOrszagos.isChecked = filters!!.teruletOrszagos
        teruletNemzetkozi.isChecked = filters!!.teruletNemzetkozi

        applyButton.setOnClickListener {
            filters = FiltersData (
                teljesitesTeljesitett = teljesitesTeljesitett.isChecked,
                teljesitesFuggoben = teljesitesFuggoben.isChecked,
                teljesitesHatralevo = teljesitesHatralevo.isChecked,
                tipusBeepitett = tipusBeepitett.isChecked,
                tipusKozossegi = tipusKozossegi.isChecked,
                teruletFalusi = teruletFalusi.isChecked,
                teruletVarosi = teruletVarosi.isChecked,
                teruletOrszagos = teruletOrszagos.isChecked,
                teruletNemzetkozi = teruletNemzetkozi.isChecked,
                helyszinszamMin = seekBar.values[0].toInt(),
                helyszinszamMax = seekBar.values[1].toInt()
            )
            adapter.clear()
            adapter.addAll(filteredLocationPacks(originalList))
            adapter.notifyDataSetChanged()

            dialog.dismiss()
        }

        dialog.show()
    }
}