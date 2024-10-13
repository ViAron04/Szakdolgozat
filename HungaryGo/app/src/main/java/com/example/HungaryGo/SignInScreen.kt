package com.example.HungaryGo

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class SignInScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailAddr: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_screen)

        // Initialize Firebase Auth
        auth = Firebase.auth

        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this@SignInScreen, MainActivity::class.java))
        }

    }

    fun googleSignInClick(view: View) {
        startActivity(Intent(this@SignInScreen, MainActivity::class.java))
    }

    fun registrationClick(view: View) {
        emailAddr = findViewById(R.id.emailText)
        val emailString = emailAddr.text.toString()
        val intent = Intent(this@SignInScreen, RegistrationScreen::class.java)
        intent.putExtra("prevEmail", emailString)
        startActivity(intent)
    }

}