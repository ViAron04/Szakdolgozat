package com.example.HungaryGo.data.repository

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.HungaryGo.MakerLocationDescription
import com.example.HungaryGo.MakerLocationPackData
import com.example.HungaryGo.ui.Main.MainScreen.BitmapStore.loadedBitmaps
import com.example.HungaryGo.ui.Main.MainScreen.RemoveAccents.removeAccents
import com.example.HungaryGo.ui.Maker.MakerScreen
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File


class MakerRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val dbFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val dbRealtime = FirebaseDatabase.getInstance()
    val dbStorage = FirebaseStorage.getInstance().reference
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
                            val makerLocationDescription: MakerLocationDescription =
                                MakerLocationDescription()

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
                                makerLocationDescription.answer =
                                    locationMakerData.getString("Answer")
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
    fun addUsersProject(projectName: String) {
        dbFirestore
            .collection("userpoints")
            .document(currentUserEmail)
            .collection("workInProgress")
            .document(projectName).set(mapOf("description" to ""))
            .addOnSuccessListener {
                Log.d("MakerRepository", "Sikeres hozzáadás")
            }
            .addOnFailureListener { e ->
                Log.e("MakerRepository", "Siktelen hozzáadás", e)
            }
    }

    suspend fun saveProjectChanges(makerLocationPackData: MakerLocationPackData) {
        try {
            val documentRef = dbFirestore
                .collection("userpoints")
                .document(currentUserEmail)
                .collection("workInProgress")
                .document(makerLocationPackData.name)

            val updates = mutableMapOf<String, Any>()
            val docSnapshot = documentRef.get().await()

            if (docSnapshot.getString("area") != makerLocationPackData.area) {
                updates["area"] = makerLocationPackData.area
            }
            if (docSnapshot.getString("description") != makerLocationPackData.description) {
                updates["description"] = makerLocationPackData.description
            }
            if (!updates.isEmpty()) {
                documentRef.update(updates).await()
            }

            val locationsRef = documentRef.collection("locations")
            val locationsSnapshot = locationsRef.get().await()

            val locationNameList: MutableList<String> = mutableListOf()

            for (locationData in locationsSnapshot) {
                locationNameList.add(locationData.id.toString())
                val currrentMakerLocationPackDataLocation =
                    makerLocationPackData.locations.find { it?.name == locationData.id.toString() }

                if (currrentMakerLocationPackDataLocation == null) {
                    locationData.reference.delete()
                        .addOnSuccessListener {
                            Log.d("MakerRepository", "${locationData.id.toString()} törölve")
                        }.addOnFailureListener { e ->
                            Log.e(
                                "MakerRepository",
                                "${locationData.id.toString()} törölése sikertelen: ",
                                e
                            )
                        }
                    continue
                }
                if (locationData.getString("Description") != currrentMakerLocationPackDataLocation.description) {
                    locationData.reference.update(
                        "Description",
                        currrentMakerLocationPackDataLocation.description
                    ).await()
                }

                    if (locationData.getString("Question") != currrentMakerLocationPackDataLocation.question) {
                        locationData.reference.update(
                            "Question",
                            currrentMakerLocationPackDataLocation.question
                        ).await()
                    }
                    if (locationData.getString("Answer") != currrentMakerLocationPackDataLocation.answer) {
                        locationData.reference.update(
                            "Answer",
                            currrentMakerLocationPackDataLocation.answer
                        ).await()
                    }

                if (currrentMakerLocationPackDataLocation.answer  != "" && currrentMakerLocationPackDataLocation.answer != null
                    && currrentMakerLocationPackDataLocation.question != "" && currrentMakerLocationPackDataLocation.question != null ){
                    locationData.reference.update(
                        "IsQuestion",
                        true
                    ).await()
                } else{
                    locationData.reference.update(
                        "IsQuestion",
                        false
                    ).await()
                }

                val markerData = currrentMakerLocationPackDataLocation.markerOptions?.position
                if (locationData.getGeoPoint("Marker")?.latitude != markerData?.latitude
                    || locationData.getGeoPoint("Marker")?.longitude != markerData?.longitude
                ) {
                    val newGeoPoint: GeoPoint =
                        GeoPoint(markerData?.latitude!!, markerData?.longitude!!)
                    locationData.reference.update("Marker", newGeoPoint).await()
                }
            }

            val makerLocationPackDataLocations: MutableList<String> = mutableListOf()
            for(locationData in makerLocationPackData.locations){
                makerLocationPackDataLocations.add(locationData?.name!!)
            }

            //toSet miatt mindegy az elemk sorrendje  a listában
            if (locationNameList.toSet() != makerLocationPackDataLocations.toSet()) {
                for (locationData in makerLocationPackData.locations) {
                    if (!locationNameList.contains(locationData?.name)) {
                        val newData = hashMapOf<String, Any?>(
                            "Description" to locationData?.description)

                            if (locationData?.answer != "" && locationData?.answer != null && locationData.question != "" && locationData.question != null ) {
                                newData["IsQuestion"] = true
                                newData["Question"] = locationData.question
                                newData["Answer"] = locationData.answer
                            } else if(locationData?.answer != "" && locationData?.answer != null) {
                                newData["IsQuestion"] = false
                                newData["Answer"] = locationData.answer
                            } else if(locationData?.question != "" && locationData?.question != null) {
                                newData["IsQuestion"] = false
                                newData["Question"] = locationData.question
                            } else {
                                newData["IsQuestion"] = false
                            }

                            // Marker hozzáadása, feltételezve, hogy markerOptions nem lehet null
                        newData["Marker"] = GeoPoint(
                            locationData?.markerOptions?.position?.latitude ?: 0.0,
                            locationData?.markerOptions?.position?.longitude ?: 0.0
                        )

                        locationsRef.document(locationData?.name!!).set(newData).await()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MakerRepository", "Hiba a saveProjectChanges-ben: ", e)
        }
    }

    suspend fun deleteProject(projectName: String){
        try{
            val documentRef = dbFirestore
                .collection("userpoints")
                .document(currentUserEmail)
                .collection("workInProgress")
                .document(projectName)

            documentRef.delete().await()
        } catch (e: Exception) {
            Log.e("MakerRepository", "Hiba a deleteProject-ben: ", e)
        }
    }

    suspend fun uploadCroppedImage(currentProjectName: String, context: Context){
        try{
            val currentProjectNameWithoutAccents = removeAccents(currentProjectName)
            val imageUri = Uri.fromFile(File(context.cacheDir, "cropped_image.jpg"))
            val imageRef = dbStorage.child("maker_location_packs_images/$currentProjectNameWithoutAccents.jpg")

            imageRef.putFile(imageUri).await()

        } catch (e: Exception) {
            Log.e("MakerRepository", "Hiba az uploadImage-ben: ", e)
        }
    }

    suspend fun downloadImage(projectName: String, context: Context){
        withContext(Dispatchers.IO) { //biztosítja, hogy háttérszálon fusson a fgv.
                val imageName = removeAccents(projectName)
                val storageRef = dbStorage.child("maker_location_packs_images/$imageName.jpg")
            try {
                storageRef.metadata.await()
                val uri = storageRef.downloadUrl.await()

                val bitmap = Glide.with(context)
                    .asBitmap()
                    .load(uri)
                    .submit()
                    .get()

                loadedBitmaps[projectName] = bitmap
                Log.d("MakerRepository", "Kép letöltve")

            } catch (e: Exception) {
                Log.e("MakerRepository", "Hiba a downloadImage-ben: ", e)
            }
        }
    }

    suspend fun uploadProjectData(currentPorject: MakerLocationPackData){
        withContext(Dispatchers.IO) {
        try {
            val submittedPacksRef = dbRealtime.getReference("submitted location packs")
            val newPackRef = submittedPacksRef.child(currentPorject.name)

            val locationsMap = currentPorject.locations.associateBy(
                { it?.name ?: "unknown_location" }, // Kulcs: helyszín neve
                { location ->
                    mapOf(
                        "latitude" to location?.markerOptions?.position?.latitude,
                        "longitude" to location?.markerOptions?.position?.longitude,
                        "description" to location?.description,
                        "question" to location?.question,
                        "answer" to location?.answer
                    )
                }
            )

            val locationPackData = mapOf(
                "description" to currentPorject.description,
                "area" to currentPorject.area,
                "origin" to auth.currentUser?.displayName,
                "timestamp" to ServerValue.TIMESTAMP,
                "locations" to locationsMap)

            newPackRef.setValue(locationPackData).await()

        } catch (e: Exception){
            Log.e("MakerRepository", "Hiba az uploadProjectData-ban: ", e)
        }
        }
    }
}