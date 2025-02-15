package com.example.HungaryGo.ui.AdventureList

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import com.example.HungaryGo.LocationPackData
import com.example.HungaryGo.R
import com.example.HungaryGo.ui.Main.MainViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout

private lateinit var viewModel: MainViewModel
private lateinit var fusedLocationClient: FusedLocationProviderClient

class AdventureListScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adventure_list)


        val listData: MutableList<String> = mutableListOf()
        val adapter: ArrayAdapter<String>


        val textInputLayout: TextInputLayout = findViewById(R.id.dropdownList)
        val autoCompleteTextView: MaterialAutoCompleteTextView = findViewById(R.id.dropdownList_item)

        /*
        val orderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, orderList)
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) //legördülő listakét jeleníti meg
        orderSpinner.adapter = orderAdapter*/

        val intent = intent
        val locationPackDataList = intent.getSerializableExtra("locationPackList") as? ArrayList<LocationPackData>
            ?: arrayListOf()

        for (locationPackData in locationPackDataList)
        {
            listData.add("${locationPackData.name}\nHelyszínek száma: ${locationPackData.locations.count()}")
        }

        //public val markerLocations: MutableMap<String? , MarkerOptions> = mutableMapOf()

        val listView: ListView = findViewById(R.id.lista)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listData)
       listView.adapter = adapter
    }
}