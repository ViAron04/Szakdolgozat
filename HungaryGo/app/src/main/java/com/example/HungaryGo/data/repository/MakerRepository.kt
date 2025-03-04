package com.example.HungaryGo.data.repository

import android.content.Context
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
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await


class MakerRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val dbFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val currentUserEmail = auth.currentUser?.email.toString()

    suspend fun getUsersProjects(): Result<MutableList<MakerLocationPackData>> {
        return try {
            val makerLocationPackList: MutableList<MakerLocationPackData> = mutableListOf()

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
                            ).title(locationMakerData.id)
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

    fun saveProjectChanges(coontext: Context, makerLocationPackData: MakerLocationPackData){
        val documentRef = dbFirestore
            .collection("userpoints")
            .document(currentUserEmail)
            .collection("workInProgress")
            .document(makerLocationPackData.name)

        val updates = mutableMapOf<String, Any>()
        documentRef.get()
            .addOnSuccessListener { docSnapshot ->
                if(docSnapshot.getString("area") != makerLocationPackData.area)
                {
                    updates["area"] = makerLocationPackData.area
                }
                if(docSnapshot.getString("description") != makerLocationPackData.description)
                {
                    updates["description"] = makerLocationPackData.description
                }
                if(!updates.isEmpty()){
                    documentRef.update(updates)
                }

                val locationsRef = documentRef.collection("locations")
                locationsRef.get().addOnSuccessListener { locationsSnapshot ->
                    val locationNameList: MutableList<String> = mutableListOf()

                    for (locationData in locationsSnapshot)
                    {
                        locationNameList.add(locationData.id.toString())
                        val currrentMakerLocationPackDataLocation = makerLocationPackData.locations.find { it?.name == locationData.id.toString()}

                        if(currrentMakerLocationPackDataLocation == null)
                        {
                            locationData.reference.delete()
                                .addOnSuccessListener {
                                Log.d("MakerRepository", "${locationData.id.toString()} törölve")
                                }.addOnFailureListener{ e ->
                                    Log.e("MakerRepository", "${locationData.id.toString()} törölése sikertelen: ", e)
                                }
                            continue
                        }
                        if(locationData.getString("Description") != currrentMakerLocationPackDataLocation.description)
                        {
                            locationData.reference.update("Description", currrentMakerLocationPackDataLocation.description)
                        }
                        if(locationData.getBoolean("IsQuestion") == true)
                        {
                            if(locationData.getString("Question") != currrentMakerLocationPackDataLocation.question)
                            {
                                locationData.reference.update("Question", currrentMakerLocationPackDataLocation.question)
                            }
                            if(locationData.getString("Answer") != currrentMakerLocationPackDataLocation.answer)
                            {
                                locationData.reference.update("Answer", currrentMakerLocationPackDataLocation.answer)
                            }
                        }
                        val markerData = currrentMakerLocationPackDataLocation.markerOptions?.position
                        if(locationData.getGeoPoint("Marker")?.latitude != markerData?.latitude
                            || locationData.getGeoPoint("Marker")?.longitude != markerData?.longitude)
                        {
                            val newGeoPoint: GeoPoint = GeoPoint(markerData?.latitude!!, markerData?.longitude!!)
                            locationData.reference.update("Marker", newGeoPoint)
                        }
                    }

                    if(locationNameList.size != locationsSnapshot.size()){
                        for (locationData in makerLocationPackData.locations) {
                            if(!locationNameList.contains(locationData?.name)){
                                val newData = hashMapOf(
                                    "name" to locationData?.name,
                                    "Description" to locationData?.description,
                                    if(locationData?.question != ""){
                                        "IsQuestion" to true
                                        "Question" to locationData?.question
                                        "Answer" to locationData?.answer
                                    }
                                    else{
                                        "IsQuestion" to false
                                    },
                                    // Marker hozzáadása, feltételezve, hogy markerOptions nem lehet null
                                    "Marker" to GeoPoint(
                                        locationData?.markerOptions?.position?.latitude ?: 0.0,
                                        locationData?.markerOptions?.position?.longitude ?: 0.0
                                    )
                                )
                                locationsRef.add(newData)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener{ e ->
                Log.e("MakerRepository", "Siktelen mentés", e)
            }

    }
}