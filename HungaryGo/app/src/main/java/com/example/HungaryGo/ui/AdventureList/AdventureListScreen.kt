package com.example.HungaryGo.ui.AdventureList

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import com.example.HungaryGo.LocationPackData
import com.example.HungaryGo.R

class AdventureListScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adventure_list)


        val listData: MutableList<String> = mutableListOf()
        val adapter: ArrayAdapter<String>

        val intent = intent
        val locationPackDataList = intent.getSerializableExtra("locationPackList") as? ArrayList<LocationPackData>


        if (locationPackDataList != null) {
            for (locationPackData in locationPackDataList)
            {
                listData.add("${locationPackData.name}\nHelyszínek száma: ${locationPackData.locations.count()}")
            }
        }
        //public val markerLocations: MutableMap<String? , MarkerOptions> = mutableMapOf()


        val listView: ListView = findViewById(R.id.lista)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listData)
       listView.adapter = adapter




    }
}