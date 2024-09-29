package com.example.HungaryGo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.MarkerOptions

class AdventureList : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adventure_list)


        val listData: MutableList<String> = mutableListOf()
        val adapter: ArrayAdapter<String>

        listData.add("Vasszécseny kör   hossz: 1 km\n legközelebbi állomása: 20 km")
        listData.add("Veszprém kör      hossz: 200 m\n legközelebbi állomása: 40 km")

        //public val markerLocations: MutableMap<String? , MarkerOptions> = mutableMapOf()


        val listView: ListView = findViewById(R.id.lista)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listData)
       listView.adapter = adapter




    }
}