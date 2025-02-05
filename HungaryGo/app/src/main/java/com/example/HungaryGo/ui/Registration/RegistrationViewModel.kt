package com.example.HungaryGo.ui.Registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.HungaryGo.data.repository.RegistrationSignInRepository
import kotlinx.coroutines.launch

class RegistrationViewModel: ViewModel (){

    private val _registrationStatus = MutableLiveData<Boolean>()
    val registrationStatus: LiveData<Boolean> get() = _registrationStatus

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val repository: RegistrationSignInRepository = RegistrationSignInRepository()

    fun registerUser(username: String, email: String, password: String, password2: String) {

        if(password != password2)
        {
            _errorMessage.value = "A megadott jelszavak nem egyeznek"
        }
        else if(username == "")
        {
            _errorMessage.value = "Nem adtál meg felhasználónevet"
        }
        else if(email == "")
        {
            _errorMessage.value = "Nem adtál meg email címet"
        }
        else if(!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")))
        {
            _errorMessage.value = "Az email cím formátuma nem megfelelő"
        }
        else if(password == "")
        {
            _errorMessage.value = "Nem adtál meg jelszót"
        }
        else if(password.length < 6)
        {
            _errorMessage.value = "A jelszavad nem elég hosszú"
        }
        else
        {
            //megszakítja a benne levő műveletet, ha a viewModel elpusztul
            viewModelScope.launch {
                val result = repository.userRegister(username,email,password)
                if(result.isSuccess) _registrationStatus.value = true
                else _registrationStatus.value = false
            }
        }
    }
}