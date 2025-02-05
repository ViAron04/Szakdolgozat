package com.example.HungaryGo.ui.Registration

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.HungaryGo.R
import com.example.HungaryGo.ui.SignIn.SignInScreen
import com.google.firebase.auth.FirebaseAuth

class RegistrationScreen : AppCompatActivity() {
    private val viewModel: RegistrationViewModel by viewModels()

    private lateinit var emailEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_screen)

        //előző activityből felhasználónév fogadása
        emailEditText = findViewById(R.id.emailRegister)
        val intent = intent
        intent.getStringExtra("prevEmail")?.let {
            emailEditText.setText(it)
        }

        //bejelentkezőképernyőre lépés
        viewModel.registrationStatus.observe(this, Observer { success ->
            if(success) {
                Toast.makeText(this, "Sikeres regisztráció :)", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, SignInScreen::class.java))
            }
        })

        //Toast üzenet, ha valamelyik mező rossz
        viewModel.errorMessage.observe(this, Observer { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        })
    }

    //regisztráció
    fun registerClick(view: View) {
        val username = findViewById<EditText>(R.id.usernameRegister).text.toString()
        val email = findViewById<EditText>(R.id.emailRegister).text.toString()
        val password = findViewById<EditText>(R.id.passwordRegister).text.toString()
        val password2 = findViewById<EditText>(R.id.passwordRegister2).text.toString()

        viewModel.registerUser(username, email, password, password2)
    }
}