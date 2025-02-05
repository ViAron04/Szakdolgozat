package com.example.HungaryGo.ui.SignIn

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.HungaryGo.data.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch

class SignInViewModel: ViewModel() {
    private val repository: UserRepository = UserRepository()

    private val _signInStatus = MutableLiveData<Result<Boolean>>()
    val signInStatus: LiveData<Result<Boolean>> = _signInStatus

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage


    fun isUserLoggedIn(): Boolean {
        return repository.getCurrentUser() != null
    }

    fun signInWithEmail(email: String, password: String) {
        if(email == "")
        {
            _errorMessage.value = "Nem adtál meg email címet"
        }
        else if(password == "")
        {
            _errorMessage.value = "Nem adtál meg jelszót"
        }
        else
        {
            viewModelScope.launch {
                val result = repository.signInWithEmail(email, password)
                _signInStatus.value = result
            }
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            val result = repository.signInWithGoogle(account)
            _signInStatus.value = result
        }
    }
}