package com.example.HungaryGo.data.repository

import com.example.HungaryGo.LocationDescription
import com.example.HungaryGo.MakerLocationPackData
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await


class MakerRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val dbFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun getUsersProjects(): Result<MutableList<MakerLocationPackData>> {
        return try {
            var makerLocationPackList: MutableList<MakerLocationPackData> = mutableListOf()

            val currentUserEmail = auth.currentUser?.email.toString()
            val documentRef = dbFirestore.collection("userpoints")
                .document(currentUserEmail)
                .collection("workInProgress")

            val querySnapshot = documentRef.get().await()

            if (!querySnapshot.isEmpty) {
                for (makerLP in querySnapshot) {
                    val makerLocationPackData: MakerLocationPackData = MakerLocationPackData()

                    makerLocationPackData.name = makerLP.id
                    makerLocationPackData.origin = makerLP.getString("origin") ?: ""
                    makerLocationPackData.description = makerLP.getString("description") ?: ""

                    val locationsRef = documentRef.document(makerLP.id).collection("locations")
                    val locationSanpshot = locationsRef.get().await()
                    if (!locationSanpshot.isEmpty) {
                        val locationData: MutableMap<String, LocationDescription?> = mutableMapOf()
                        for (locationMakerData in locationSanpshot) {
                            val locationDescription: LocationDescription = LocationDescription()

                            locationDescription.description =
                                locationMakerData.getString("Description")
                            locationDescription.markerOptions = MarkerOptions().position(
                                LatLng(
                                    locationMakerData.getGeoPoint("Marker")!!.latitude,
                                    locationMakerData.getGeoPoint("Marker")!!.longitude,
                                )
                            )
                            if (locationMakerData.getBoolean("IsQuestion") == true) {
                                locationDescription.answer = locationMakerData.getString("Answer")
                                locationDescription.question =
                                    locationMakerData.getString("Question")
                            }
                            locationData[locationMakerData.id] = locationDescription
                        }
                        makerLocationPackData.locations = locationData
                    }
                    makerLocationPackList.add(makerLocationPackData)
                }
            }

            Result.success(makerLocationPackList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}