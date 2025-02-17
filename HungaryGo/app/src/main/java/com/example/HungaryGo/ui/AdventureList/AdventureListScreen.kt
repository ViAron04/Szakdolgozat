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
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
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

class AdventureListScreen : AppCompatActivity() {

    private val adventureListViewModel: AdventureListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adventure_list)

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

                if(completedLPList.contains(locationPack?.name))
                {
                    view.setBackgroundColor(ContextCompat.getColor(context, R.color.lightGreen))
                }

                if(startedLPList.contains(locationPack?.name))
                {
                    view.setBackgroundColor(ContextCompat.getColor(context, R.color.lightOrange))
                }

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
                    orderedLocationPackDataList = locationPackDataList.sortedBy { it.name }
                }
                "Zs-A" -> {
                    orderedLocationPackDataList = locationPackDataList.sortedByDescending { it.name }
                }
                "értékelés" -> {
                    orderedLocationPackDataList = locationPackDataList.sortedByDescending { it.rating }
                }
                "népszerűség" -> {
                    orderedLocationPackDataList = locationPackDataList.sortedByDescending { it.completionNumber }
                }
                "hossz szerint növekvő" -> {
                    orderedLocationPackDataList = locationPackDataList.sortedBy { it.locations.size }
                }
                "hossz szerint csökkenő" -> {
                    orderedLocationPackDataList = locationPackDataList.sortedByDescending { it.locations.size }
                }
                else -> {
                    orderedLocationPackDataList = locationPackDataList
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

                filterList(s.toString(), adapter, locationPackDataList)
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

    }


    fun filterList(searchedChars: String, adapter: ArrayAdapter<LocationPackData>, listData: MutableList<LocationPackData>) {
        val filteredList = listData.filter { it.name.contains(searchedChars) }
        adapter.clear()
        adapter.addAll(filteredList)
        adapter.notifyDataSetChanged()
    }

    /*
    fun filteredLocationPacks(originalList: List<LocationPackData>, filterData: FiltersData): List<LocationPackData>{
        return originalList.filter {

        }

    }
    */
    fun displaySortedList(view: View) {

    }

    fun openFilterDialog(view: View) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.filter_dialog)
        dialog.setCancelable(true)
        val seekBar = dialog.findViewById<RangeSlider>(R.id.rangeSeekBar)
        seekBar.setValues(1f, 10f)
        dialog.show()
    }
}