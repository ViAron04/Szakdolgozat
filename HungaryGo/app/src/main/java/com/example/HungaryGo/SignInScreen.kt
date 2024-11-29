package com.example.HungaryGo

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class SignInScreen : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailAddr: EditText
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_screen)

        auth = Firebase.auth

        //google bejelentkezes
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Web Client ID
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            //val emailString = emailAddr.text.toString()
            //val intent = Intent(this@SignInScreen, MainScreen::class.java)
            //intent.putExtra("emailAddr", emailString)
            startActivity(Intent(this@SignInScreen, MainScreen::class.java))
        }
    }

    fun signInClick(view: View)
    {
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

                        //email továbbítása mainactivitynek
                        val emailString = emailAddr.text.toString()
                        val intent = Intent(this@SignInScreen, MainScreen::class.java)
                        intent.putExtra("emailAddr", emailString)

                        startActivity(Intent(this@SignInScreen, MainScreen::class.java))
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(this, "Nem sikerült a bejelentkezés :(", Toast.LENGTH_LONG).show()
                    }
                }
        }
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
    fun googleSignIn(view: View)
    {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Gbejelentkezés sikeres volt, jön a firebase autentikáció
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w(TAG, "Google bejelentkezés nem sikerült :(", e)
                Toast.makeText(this, "Google bejelentkezés nem sikerült :(", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account?.id)

        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    Toast.makeText(this, "Bejelentkezés sikeres :)", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SignInScreen, MainScreen::class.java))
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Nem sikerült a bejelentkezés :(", Toast.LENGTH_LONG).show()
                }
            }
    }

}