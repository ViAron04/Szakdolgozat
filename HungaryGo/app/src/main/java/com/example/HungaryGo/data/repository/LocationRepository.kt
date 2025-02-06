package com.example.HungaryGo.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.HungaryGo.LocationDescription
import com.example.HungaryGo.LocationPackData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LocationRepository(private val db: FirebaseDatabase, private val fusedLocationProviderClient: FusedLocationProviderClient) {

    private val _locationPacks = MutableLiveData<MutableList<LocationPackData>>()
    val locationPacks: LiveData<MutableList<LocationPackData>> get() = _locationPacks

    fun fetchLocationPacks() {
        val locationPacksRef = db.getReference("location packs")

        locationPacksRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val locationPackList = mutableListOf<LocationPackData>()

                    for (buildingSnapshotLP in snapshot.children) {
                        val locationPackData = LocationPackData()
                        locationPackData.name = buildingSnapshotLP.key.toString()

                        for (buildingSnapshot in buildingSnapshotLP.children) {
                            when (buildingSnapshot.key) {
                                "rating" -> locationPackData.rating = buildingSnapshot.value.toString().toDouble()
                                "description" -> locationPackData.description = buildingSnapshot.value.toString()
                                "completionNumber" -> locationPackData.completionNumber = buildingSnapshot.value.toString().toInt()
                                else -> {
                                    val buildingMap = buildingSnapshot.value as Map<String, Any>
                                    val markerOptions = MarkerOptions().position(
                                        LatLng(
                                            buildingMap["latitude"] as Double,
                                            buildingMap["longitude"] as Double
                                        )
                                    )
                                    val description = buildingMap["Description"] as String?
                                    val name = buildingSnapshot.key.toString()
                                    var question: String? = buildingMap["Question"] as String?
                                    var answer: String? = buildingMap["Answer"] as String?

                                    locationPackData.locations[name] = LocationDescription(
                                        markerOptions, description, question, answer
                                    )
                                }
                            }
                        }
                        locationPackList.add(locationPackData)
                    }
                    _locationPacks.postValue(locationPackList)
                } else {
                    Log.e("LocationRepository", "Nem találtam helyszíneket az adatbázisban")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LocationRepository", "Error getting data: ${error.message}")
            }
        })
    }
}