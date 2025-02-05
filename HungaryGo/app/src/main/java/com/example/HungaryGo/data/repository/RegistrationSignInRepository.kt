package com.example.HungaryGo.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RegistrationSignInRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun userRegister(username: String, email: String, password: String): Result<String>
    {
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
            db.collection("userpoints").document(email).set(userMap)

            Result.success("Sikeres regisztráció")

        }catch (e: Exception) {
            Log.e("UserRepository", "Hiba a regisztráció során", e)
            Result.failure(e)
        }
    }
}