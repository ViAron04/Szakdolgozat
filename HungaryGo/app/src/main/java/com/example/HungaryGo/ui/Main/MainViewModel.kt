package com.example.HungaryGo.ui.Main

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.HungaryGo.LocationPackData
import com.example.HungaryGo.data.repository.LocationRepository
import com.example.HungaryGo.data.repository.UserRepository

class MainViewModel(private val locationRepository: LocationRepository): ViewModel() {
    private val userRepository: UserRepository = UserRepository()

    val locationPacksData: LiveData<List<LocationPackData>> = locationRepository.locationPacks

    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> get() = _currentLocation

    private val _nearbyLocation = MutableLiveData<String>()
    val nearbyLocation: LiveData<String> get() = _nearbyLocation

    fun getUserName(): String? {
        return userRepository.getUserName()
    }

    fun loadLocationPacks() {
        locationRepository.fetchLocationPacks()
    }

}