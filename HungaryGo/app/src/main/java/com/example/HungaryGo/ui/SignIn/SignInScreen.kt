package com.example.HungaryGo.ui.SignIn

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.HungaryGo.R
import com.example.HungaryGo.ui.Main.MainScreen
import com.example.HungaryGo.ui.Registration.RegistrationScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class SignInScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailAddr: EditText
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    private val viewModel: SignInViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_screen)

        //ha a felhasználó már be van jelentkezve
        if (viewModel.isUserLoggedIn()) {
            startActivity(Intent(this, MainScreen::class.java))
        }

        setupGoogleSignIn()

        //Toast üzenet, ha valamelyik mező rossz
        viewModel.errorMessage.observe(this, Observer { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        })

        viewModel.signInStatus.observe(this, Observer { result ->
            result.onSuccess {
                Toast.makeText(this, "Bejelentkezés sikeres :)", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainScreen::class.java))
            }.onFailure {
                Toast.makeText(this, "Bejelentkezés sikertelen :(", Toast.LENGTH_LONG).show()
            }
        })

    }

    fun signInClick(view: View)
    {
        val email = findViewById<EditText>(R.id.emailText).text.toString()
        val password = findViewById<EditText>(R.id.passwordText).text.toString()
        viewModel.signInWithEmail(email, password)
    }

    fun registrationClick(view: View)
    {
        emailAddr = findViewById(R.id.emailText)
        val emailString = emailAddr.text.toString()
        val intent = Intent(this@SignInScreen, RegistrationScreen::class.java)
        intent.putExtra("prevEmail", emailString)
        startActivity(intent)
    }

    //Google bejelentkezés:
    fun googleSignInClick(view: View)
    {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Web Client ID
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.signInWithGoogle(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google bejelentkezés sikertelen :(", Toast.LENGTH_LONG).show()
            }
        }
    }
}