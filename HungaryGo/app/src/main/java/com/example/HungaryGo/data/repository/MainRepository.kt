package com.example.HungaryGo.data.repository

import android.util.Log
import com.example.HungaryGo.LocationPackData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainRepository() {
    private val auth: FirebaseAuth = Firebase.auth
    private val dbFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun updateLocationInFirestore(currentLocationPackData: LocationPackData, markerTitle: String)
    {
        try {

            val currentUserEmail = auth.currentUser?.email.toString()
            val userRef =
                dbFirestore.collection("userpoints").document(currentUserEmail)
                    .collection("inprogress").document(currentLocationPackData.name)

            // Dokumentum lekérése
            val document = userRef.get().await()
            //A pálya végigvitelének mennyisége és a felhasználó értékelése róla
            if (!document.contains("usersRating")) {
                val generalData = mutableMapOf<String, Any>()
                generalData["completionCount"] = 0
                generalData["usersRating"] = 0

                userRef.set(generalData, SetOptions.merge()).await()
            }
            //Minden helyszín a pályában legyen nulla
            if (!document.contains("locations")) {
                val updatedLocations = mutableMapOf<String, Int>()
                for (location in currentLocationPackData.locations) {
                    updatedLocations[location.key] = 0
                }
                val locationData = mapOf("locations" to updatedLocations)
                userRef.set(locationData, SetOptions.merge()).await()
            }

            //aktuális helyszín-t egyre kell állítani
            val locationData = mapOf(
                "locations" to mapOf(markerTitle to 1)
            )
            userRef.set(locationData, SetOptions.merge()).await()


        } catch (e: Exception) {
            Log.e("MainRepository", "Hiba történt: ", e)
        }
    }

}