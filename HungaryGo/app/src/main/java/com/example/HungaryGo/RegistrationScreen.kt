package com.example.HungaryGo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegistrationScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var usernameEditText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_screen)

        //előző activityből felhasználónév fogadása
        usernameEditText = findViewById(R.id.usernameRegister)
        val intent = intent
        val prevUsername = intent.getStringExtra("prevUsername")
        usernameEditText.setText(prevUsername)
/*
        auth = Firebase.auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext,
                        "A regisztrácó meghiúsult.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateUI(null)
                }
            }*/
    }


}