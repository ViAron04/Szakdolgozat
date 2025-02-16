package com.example.HungaryGo.data.repository

import android.util.Log
import com.bumptech.glide.Glide
import com.example.HungaryGo.LocationPackData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AdventureListRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val dbFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    //Listába gyűjti a felhazsnáló által teljesített pályákat, a kilistázáshoz
    suspend fun completedLocationsToList(): Result<MutableList<String>>{
        return try {
            val completedLPList: MutableList<String> = mutableListOf()

            val currentUserEmail = auth.currentUser?.email
                ?: return Result.failure(Exception("felhasználó nincs bejelentkezve"))

            val documentRef = dbFirestore.collection("userpoints")
                .document(currentUserEmail)
                .collection("inprogress")

            val docSnapshot = documentRef.get().await() // await() megvárja az eredményt Firestore-tól

            for (document in docSnapshot.documents) {
                if((document.getLong("completionCount") ?: 0) > 0)
                {
                    completedLPList.add(document.id)
                }
            }
            Result.success(completedLPList)

        }catch (e: Exception) {
            Result.failure(e)
        }
    }
}