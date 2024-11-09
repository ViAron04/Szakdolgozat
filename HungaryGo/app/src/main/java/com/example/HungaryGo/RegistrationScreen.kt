package com.example.HungaryGo

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class RegistrationScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var emailEditText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_screen)

        //előző activityből felhasználónév fogadása
        emailEditText = findViewById(R.id.emailRegister)
        val intent = intent
        val prevEmail = intent.getStringExtra("prevEmail")
        emailEditText.setText(prevEmail)
    }

    fun registerClick(view: View) {
        val username = findViewById<EditText>(R.id.usernameRegister)
        val usernameRegister = username.text.toString();

        val email = findViewById<EditText>(R.id.emailRegister)
        val emailRegister = email.text.toString();

        val passwd = findViewById<EditText>(R.id.passwordRegister)
        val passwordRegister = passwd.text.toString();

        val passwd2 = findViewById<EditText>(R.id.passwordRegister2)
        val passwordRegister2 = passwd2.text.toString();

        if(passwordRegister != passwordRegister2)
        {
            Toast.makeText(this, "A megadott jelszavak nem egyeznek", Toast.LENGTH_LONG).show()
        }
        else if(usernameRegister == "")
        {
            Toast.makeText(this, "Nem adtál meg felhasználónevet", Toast.LENGTH_LONG).show()
        }
        else if(emailRegister == "")
        {
            Toast.makeText(this, "Nem adtál meg email címet", Toast.LENGTH_LONG).show()
        }
        else if(passwordRegister == "")
        {
            Toast.makeText(this, "Nem adtál meg jelszót", Toast.LENGTH_LONG).show()
        }
        else if(passwordRegister.length < 6)
        {
            Toast.makeText(this, "A jelszavad nem elég hosszú", Toast.LENGTH_LONG).show()
        }
        else
        {
            auth = Firebase.auth
            auth.createUserWithEmailAndPassword(emailRegister, passwordRegister)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser
                        Toast.makeText(this, "Sikeres regisztráció :)", Toast.LENGTH_LONG).show()

                        val db = FirebaseFirestore.getInstance()

                        val emailNonNullable: String = user?.email!!
                        // A felhasználó email címének hozzáadása a userpoints gyűjteményhez
                        val userPoints = hashMapOf(
                            "email" to emailNonNullable
                        )

                        // Save the new user data to Firestore
                        db.collection("userpoints")
                            .document(emailNonNullable)
                            .set(userPoints) // <-- This line actually writes the data to Firestore
                            .addOnSuccessListener {
                                Log.d(TAG, "User added to Firestore with email: $emailNonNullable")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error adding user to Firestore", e)
                            }

                        //val userInProgress = db.collection("userpoints").document(emailNonNullable)
                        /*userInProgress.collection("inprogress").document("levelsInProgress").set(emptyMap<String,Any>())
                            .addOnSuccessListener {
                                Log.d(TAG, " InProgress collection added")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error adding InProgress collection", e)
                            }*/

                        val intent = Intent(this@RegistrationScreen, SignInScreen::class.java)
                        startActivity(intent)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this, "Nem sikerült a regisztráció :(", Toast.LENGTH_LONG).show()
                    }
                }
        }

    }


}