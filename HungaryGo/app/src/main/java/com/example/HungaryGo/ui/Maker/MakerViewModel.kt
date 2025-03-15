package com.example.HungaryGo.ui.Maker

import android.content.Context
import android.widget.ArrayAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.HungaryGo.MakerLocationDescription
import com.example.HungaryGo.MakerLocationPackData
import com.example.HungaryGo.data.repository.MakerRepository
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MakerViewModel: ViewModel() {
    private val _usersProjectsList = MutableLiveData<MutableList<MakerLocationPackData>?>()
    val usersProjectsList: LiveData<MutableList<MakerLocationPackData>?> get() = _usersProjectsList

    private val _currentProject = MutableLiveData<MakerLocationPackData>()
    val currentProject: LiveData<MakerLocationPackData> get() = _currentProject

    private val _isSaveFinished = MutableLiveData<Boolean>(false)
    val isSaveFinished: LiveData<Boolean> get() = _isSaveFinished

    private val _isBackSaveFinished = MutableLiveData<Boolean>(false)
    val isBackSaveFinished: LiveData<Boolean> get() = _isBackSaveFinished

    private val _isNewPictureLoaded = MutableLiveData<Boolean>(false)
    val isNewPictureLoaded: LiveData<Boolean> get() = _isNewPictureLoaded

    private val repository: MakerRepository = MakerRepository()

    fun getUsersProjects(context: Context){
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

    fun deleteLocation(locationName: String){
        _currentProject.value?.locations?.removeAll{it?.name == locationName}
    }

    fun deleteProject(projectName: String, context: Context){
        viewModelScope.launch {
            _usersProjectsList.value?.remove(_usersProjectsList.value?.find { it.name == projectName })
            repository.deleteProject(projectName)
        }
    }

    fun setCurrentProject(projectName: String, context: Context){
        viewModelScope.launch {
            repository.downloadImage(projectName, context)
            _currentProject.value = usersProjectsList.value?.find { it.name == projectName }
        }
    }

    fun addNewLocationToCurrentProject(name: String, markerOptions: MarkerOptions){
        _currentProject.value?.locations?.add(MakerLocationDescription(name, markerOptions))
        _currentProject.value = _currentProject.value
    }

    fun saveProjectChanges(context: Context){
        /*
        val makerLocationPackData = currentProject.value!!
        val projectDataJson = Gson().toJson(makerLocationPackData)

        val inputData = workDataOf(
            "projectData" to projectDataJson,
        )

        val saveProjectWorkRequest = OneTimeWorkRequestBuilder<SaveProjectWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(saveProjectWorkRequest)*/

        viewModelScope.launch {
            repository.saveProjectChanges(currentProject.value!!)
            _isSaveFinished.value = true
            delay(30_00L)
            _isSaveFinished.value = false
        }

    }

    fun saveProjectChangesBack(context: Context){
        viewModelScope.launch {
            repository.saveProjectChanges(currentProject.value!!)
            _isBackSaveFinished.value = true
            delay(30_00L)
            _isBackSaveFinished.value = false
        }

    }

    fun uploadCroppedImage(context: Context){
        viewModelScope.launch {
            repository.uploadCroppedImage(currentProject.value?.name.toString(), context)
            _isNewPictureLoaded.value = true
            delay(30_00L)
            _isNewPictureLoaded.value = false
        }
    }

}