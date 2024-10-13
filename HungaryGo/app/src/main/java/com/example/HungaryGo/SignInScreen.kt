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
        val email = findViewById<EditText>(R.id.emailText)
        val emailSignIn = email.text.toString();

        val passwd = findViewById<EditText>(R.id.passwordText)
        val passwordSignIn = passwd.text.toString();

        if(emailSignIn == "")
        {
            Toast.makeText(this, "Nem adtál meg email címet", Toast.LENGTH_LONG).show()
        }
        else if(passwordSignIn == "")
        {
            Toast.makeText(this, "Nem adtál meg jelszót", Toast.LENGTH_LONG).show()
        }
        else
        {
            auth.signInWithEmailAndPassword(emailSignIn, passwordSignIn)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        Toast.makeText(this, "Bejelentkezés sikeres :)", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@SignInScreen, MainActivity::class.java))
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(this, "Nem sikerült a bejelentkezés :(", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    fun registrationClick(view: View) {
        emailAddr = findViewById(R.id.emailText)
        val emailString = emailAddr.text.toString()
        val intent = Intent(this@SignInScreen, RegistrationScreen::class.java)
        intent.putExtra("prevEmail", emailString)
        startActivity(intent)
    }

}