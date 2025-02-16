package com.example.HungaryGo.ui.Main

import android.location.Location
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.HungaryGo.LocationPackData
import com.example.HungaryGo.R
import com.example.HungaryGo.data.repository.LocationRepository
import com.example.HungaryGo.data.repository.MainRepository
import com.example.HungaryGo.data.repository.UserRepository
import com.google.protobuf.Internal.BooleanList
import kotlinx.coroutines.launch

class MainViewModel(private val locationRepository: LocationRepository): ViewModel() {
    private val userRepository: UserRepository = UserRepository()
    private val mainRepository: MainRepository = MainRepository()

    val locationPacksData: LiveData<MutableList<LocationPackData>> = locationRepository.locationPacks

    private val _currentLocationPackData = MutableLiveData<LocationPackData?>()
    val currentLocationPackData: MutableLiveData<LocationPackData?> get() = _currentLocationPackData

    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> get() = _currentLocation

    private val _levelCompleted = MutableLiveData<Result<Boolean>>()
    val levelCompleted: LiveData<Result<Boolean>> get() = _levelCompleted

    private val _nearbyLocation = MutableLiveData<String>()
    val nearbyLocation: LiveData<String> get() = _nearbyLocation

    fun getUserName(): String {
        return userRepository.getUserName()
    }

    fun loadLocationPacks() {
        locationRepository.fetchLocationPacks()
    }

    fun currentLocationPackDataSet(currentLocationPackData: LocationPackData) {
        _currentLocationPackData.value = currentLocationPackData
    }

    fun currentLocationPackToNull() {
        _currentLocationPackData.value = null
    }

    fun getUserPrevRating(): Long{
        var number: Long = 0
        viewModelScope.launch {
            number = userRepository.getUserPrevRating(currentLocationPackData.value!!.name)
        }
        return number
    }

    fun isLocationpackDone(
        currentLocationpackName: String,
        callback: (Boolean) -> Unit
    ) {
        userRepository.isLocationPackDone(currentLocationpackName) { result ->
            callback(result)
        }
    }

    fun updateLocationInFirestore(markerTitle: String){
        viewModelScope.launch {
            mainRepository.updateLocationInFirestore(currentLocationPackData.value!!, markerTitle)
            val result = mainRepository.checkLocationPackCompletion(currentLocationPackData.value!!.name)
            _levelCompleted.value = result
        }
    }

    fun restartLevel(currentLocationPackName: String){
        viewModelScope.launch {
            mainRepository.restartLevel(currentLocationPackName)
            locationRepository.fetchLocationPacks()
        }
    }
}