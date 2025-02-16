package com.example.HungaryGo.data.repository

import android.util.Log
import com.example.HungaryGo.LocationPackData
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    suspend fun checkLocationPackCompletion(currentLocationPackParam: String): Result<Boolean> {
        return try {
            val currentUserEmail = auth.currentUser?.email
                ?: return Result.failure(Exception("felhasználó nincs bejelentkezve"))

            val documentRef = dbFirestore.collection("userpoints")
                .document(currentUserEmail)
                .collection("inprogress")
                .document(currentLocationPackParam)

            val docSnapshot = documentRef.get().await() // await() megvárja az eredményt Firestore-tól

            if (docSnapshot.exists()) {
                val locations = docSnapshot.get("locations") as? Map<String, Any>
                val allOne = locations?.all { (_, value) -> value == 1L || value == 1.0 } ?: false
                Result.success(allOne)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //usersRating és completionCount átírása Firestore-ban, a rating frissítése realtime database-ben
    suspend fun  updateUsersRatingAndCompletionCount(currentLocationPackData: LocationPackData, newRating: Double, currentAvgRating: Double, currentCompletionNumber: Int)
    {
        return withContext(Dispatchers.IO) {
            try {

                val currentUserEmail = auth.currentUser?.email.toString()
                val documentRef = dbFirestore.collection("userpoints")
                    .document(currentUserEmail)
                    .collection("inprogress")
                    .document(currentLocationPackData.name)

                // Convert to coroutine
                val docSnapshot = suspendCancellableCoroutine { continuation ->
                    documentRef.get()
                        .addOnSuccessListener { continuation.resume(it) }
                        .addOnFailureListener { continuation.resumeWithException(it) }
                }

                if (docSnapshot.exists()) {
                    val currentCompletionCount = docSnapshot.getLong("completionCount") ?: 0
                    val currentUsersRating = docSnapshot.getDouble("usersRating")

                    Tasks.await(documentRef.update("completionCount", currentCompletionCount + 1))

                    val database: DatabaseReference = FirebaseDatabase.getInstance().reference

                    if (currentUsersRating == 0.0) {
                        Tasks.await(documentRef.update("usersRating", newRating))
                        val rating = (currentAvgRating * currentCompletionNumber + newRating) / (currentCompletionNumber + 1)

                        val completionNumberRef = database.child("location packs")
                            .child(currentLocationPackData.name)
                            .child("completionNumber")

                        Tasks.await(completionNumberRef.setValue(currentCompletionNumber + 1))

                        val ratingRef = database.child("location packs")
                            .child(currentLocationPackData.name)
                            .child("rating")

                        Tasks.await(ratingRef.setValue(rating))

                        currentLocationPackData.rating = rating
                        currentLocationPackData.completionNumber = currentCompletionNumber + 1
                    } else if (currentUsersRating != newRating) {
                        Tasks.await(documentRef.update("usersRating", newRating))

                        val rating = ((currentAvgRating * currentCompletionNumber - currentUsersRating!!.toFloat()) + newRating) / (currentCompletionNumber)

                        val ratingRef = database.child("location packs")
                            .child(currentLocationPackData.name)
                            .child("rating")

                        // Várakozás a rating frissítésére
                        Tasks.await(ratingRef.setValue(rating))

                        currentLocationPackData.rating = rating
                        currentLocationPackData.completionNumber = currentCompletionNumber + 1
                    }
                }
            } catch (e: Exception) {
                Log.e("Firestore", "Hiba történt: ", e)
            }
        }
    }

    suspend fun restartLevel(currentLocationPackName: String){
        try {
            val currentUserEmail = auth.currentUser?.email

            val documentRef = dbFirestore.collection("userpoints")
                .document(currentUserEmail!!)
                .collection("inprogress")
                .document(currentLocationPackName)

            val documentSnapshot = documentRef.get().await()
            val locationsMap = documentSnapshot.get("locations") as? Map<String, Any>

            //minden értéket nullára állít
            val updatedLocationsMap = locationsMap!!.mapValues { 0 }

            //firestore véglegesítés
            documentRef.update("locations", updatedLocationsMap).await()

        } catch (e: Exception) {
            Log.e("MainRepository", "Hiba történt: ", e)
        }
    }
}