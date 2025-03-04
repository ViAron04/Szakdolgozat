package com.example.HungaryGo.ui.Maker

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.HungaryGo.MakerLocationDescription
import com.example.HungaryGo.MakerLocationPackData
import com.example.HungaryGo.data.repository.MakerRepository
import com.example.HungaryGo.data.repository.UserRepository
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class MakerViewModel: ViewModel() {
    private val _usersProjectsList = MutableLiveData<MutableList<MakerLocationPackData>?>()
    val usersProjectsList: LiveData<MutableList<MakerLocationPackData>?> get() = _usersProjectsList

    private val _currentProject = MutableLiveData<MakerLocationPackData>()
    val currentProject: LiveData<MakerLocationPackData> get() = _currentProject

    private val repository: MakerRepository = MakerRepository()

    fun getUsersProjects(){
        viewModelScope.launch {
            val result = repository.getUsersProjects()
            if(result.isSuccess){
                _usersProjectsList.value = result.getOrNull()
            }
        }
    }

    fun addUserProject(projectName: String){
        viewModelScope.launch {
            repository.addUsersProject(projectName)
        }
    }

    fun setCurrentProject(projectName: String){
        _currentProject.value = usersProjectsList.value?.find { it.name == projectName }
    }

    fun addNewLocationToCurrentProject(name: String, markerOptions: MarkerOptions){
        _currentProject.value?.locations?.add(MakerLocationDescription(name, markerOptions))
        _currentProject.value = _currentProject.value
    }

    fun saveProjectChanges(context: Context){
        repository.saveProjectChanges(context ,currentProject.value!!)
    }

}