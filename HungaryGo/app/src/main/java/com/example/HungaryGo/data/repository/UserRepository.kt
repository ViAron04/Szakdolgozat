package com.example.HungaryGo.data.repository

import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
}