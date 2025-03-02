package com.example.HungaryGo.data.repository

import android.util.Log
import com.example.HungaryGo.LocationDescription
import com.example.HungaryGo.MakerLocationDescription
import com.example.HungaryGo.MakerLocationPackData
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
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
                        val locationData: MutableList<MakerLocationDescription?> = mutableListOf()
                        for (locationMakerData in locationSanpshot) {
                            val makerLocationDescription: MakerLocationDescription = MakerLocationDescription()

                            makerLocationDescription.name = locationMakerData.id
                            makerLocationDescription.description =
                                locationMakerData.getString("Description")
                            makerLocationDescription.markerOptions = MarkerOptions().position(
                                LatLng(
                                    locationMakerData.getGeoPoint("Marker")!!.latitude,
                                    locationMakerData.getGeoPoint("Marker")!!.longitude,
                                )
                            )
                            if (locationMakerData.getBoolean("IsQuestion") == true) {
                                makerLocationDescription.answer = locationMakerData.getString("Answer")
                                makerLocationDescription.question =
                                    locationMakerData.getString("Question")
                            }
                            locationData.add(makerLocationDescription)
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

    //új projekt hozzáadása Firestoreba
    fun addUsersProject(projectName: String){
        val currentUserEmail = auth.currentUser?.email.toString()
        dbFirestore
            .collection("userpoints")
            .document(currentUserEmail)
            .collection("workInProgress")
            .document(projectName).set(mapOf("description" to ""))
            .addOnSuccessListener {
                Log.d("MakerRepository", "Sikeres hozzáadás")
            }
            .addOnFailureListener{ e ->
                Log.e("MakerRepository", "Siktelen hozzáadás", e)
            }
    }
}