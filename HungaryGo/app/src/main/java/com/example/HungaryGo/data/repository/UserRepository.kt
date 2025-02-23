package com.example.HungaryGo.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.Normalizer

class UserRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val dbFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun userRegister(username: String, email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            //ha nem sikerül a felhasználót létrehozni
            val user = result.user ?: throw Exception("Felhasználó létrehozása sikertelen!")
            val profileUpdates = userProfileChangeRequest {
                displayName = username
            }
            user.updateProfile(profileUpdates).await()

            //Firebase: a felhasználó email címének hozzáadása a userpoints gyűjteményhez
            val userMap = hashMapOf(
                "email" to email
            )
            dbFirestore.collection("userpoints").document(email).set(userMap)


            val userDocRef = dbFirestore.collection("userpoints").document(email)
            userDocRef.update("achievements", FieldValue.arrayUnion("Üdv itt!"))


            Result.success("Sikeres regisztráció")

        }catch (e: Exception) {
            Log.e("UserRepository", "Hiba a regisztráció során", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Boolean>{
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(true)
        }catch (e: Exception) {
            Log.e("UserRepository", "Email sign-in failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount?): Result<Boolean>{
        return try {
            val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
            auth.signInWithCredential(credential).await()
                Result.success(true)
        }catch (e: Exception) {
            Log.e("UserRepository", "Google sign-in failed", e)
            Result.failure(e)
        }
    }

    fun getCurrentUser() = auth.currentUser

    fun getUserName(): String {
        return auth.currentUser?.displayName.toString()
    }

    suspend fun getUserPrevRating(currentLocationPackName: String): Long {
        val currentUserEmail = auth.currentUser?.email ?: return 0
        val documentRef = dbFirestore.collection("userpoints")
            .document(currentUserEmail)
            .collection("inprogress")
            .document(currentLocationPackName)

        return try {
            val docSnapshot = documentRef.get().await() // Coroutine megvárja az eredményt
            docSnapshot.getLong("usersRating") ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun userSignOut(){
        auth.signOut()
    }

    suspend fun getCompletedLevelsReward(context: Context):Result<MutableMap<String?, android.graphics.Bitmap>>
    {
        return try {
            val rewardBitmaps= mutableMapOf<String?, Bitmap>()
            val currentUserEmail = auth.currentUser?.email
            val documentRef = dbFirestore.collection("userpoints")
                .document(currentUserEmail!!)
                .collection("inprogress")

            val querySnapshot = documentRef.get().await()

            for (document in querySnapshot.documents) {
                if((document.getLong("completionCount") ?: 0) > 0)
                {
                    val currentLocationPackUri = removeAccents(document.id.lowercase().replace(' ','_'))
                    val storageRef = FirebaseStorage.getInstance().reference.child("location_packs_rewards/$currentLocationPackUri.png")

                    try {
                        val uri = storageRef.downloadUrl.await() // Várunk az URL-re
                        val bitmap = withContext(Dispatchers.IO) {
                            Glide.with(context)
                                .asBitmap()
                                .load(uri)
                                .submit()
                                .get() // A kép betöltése blokkoló módon
                        }
                        rewardBitmaps[document.id] = bitmap
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Hiba történt a kép betöltésekor", e)
                    }
                }
            }
            Result.success(rewardBitmaps)
        }catch (e: Exception) {
            Log.e("UserRepository", "Hiba a jutalmak betöltésekor", e)
            Result.failure(e)
        }
    }

    fun removeAccents(input: String?): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{Mn}"), "")
    }

    fun isLocationPackDone(
        currentLocationPackName: String,
        callback: (Boolean) -> Unit
    ) {
        val currentUserEmail = auth.currentUser?.email
        if (currentUserEmail == null) {
            callback(false)
            return
        }

        val documentRef = dbFirestore.collection("userpoints")
            .document(currentUserEmail)
            .collection("inprogress")
            .document(currentLocationPackName)

        documentRef.get()
            .addOnSuccessListener { docSnapshot ->
                val isDone = (docSnapshot.getLong("completionCount") ?: 0) > 0
                callback(isDone)
            }
            .addOnFailureListener {
                callback(false)
            }
    }
}