package com.example.HungaryGo.ui.Maker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.HungaryGo.MakerLocationPackData
import com.example.HungaryGo.data.repository.MakerRepository
import com.example.HungaryGo.data.repository.UserRepository
import kotlinx.coroutines.launch

class MakerViewModel: ViewModel() {
    private val _usersProjectsList = MutableLiveData<MutableList<MakerLocationPackData>?>()
    val usersProjectsList: LiveData<MutableList<MakerLocationPackData>?> get() = _usersProjectsList

    private val repository: MakerRepository = MakerRepository()

    fun getUsersProjects(){
        viewModelScope.launch {
            val result = repository.getUsersProjects()
            if(result.isSuccess){
                _usersProjectsList.value = result.getOrNull()
            }
        }
    }
}