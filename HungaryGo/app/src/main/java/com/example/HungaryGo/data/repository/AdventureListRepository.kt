package com.example.HungaryGo.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdventureListRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val dbFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    //Listába gyűjti a felhazsnáló által teljesített pályákat, a kilistázáshoz
    suspend fun completedAndStartedLocationsToList(): Result<Pair<MutableList<String>, MutableList<String>>>{
        return try {
            val completedLPList: MutableList<String> = mutableListOf()
            val startedLPList: MutableList<String> = mutableListOf()

            val currentUserEmail = auth.currentUser?.email
                ?: return Result.failure(Exception("felhasználó nincs bejelentkezve"))

            val documentRef = dbFirestore.collection("userpoints")
                .document(currentUserEmail)
                .collection("inprogress")

            val docSnapshot = documentRef.get().await() // await() megvárja az eredményt Firestore-tól

            for (document in docSnapshot.documents) {
                val locations = document.get("locations") as? Map<String, Int>

                val allLocationsDone = locations?.all { it.value == 1 }

                if (allLocationsDone != null && allLocationsDone)
                {
                    completedLPList.add(document.id)
                }
                else if(allLocationsDone != null){
                    startedLPList.add(document.id)
                }

            }

            val lpLists = Pair(completedLPList, startedLPList)
            Result.success(lpLists)

        }catch (e: Exception) {
            Result.failure(e)
        }
    }
}