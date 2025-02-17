package com.example.HungaryGo.ui.AdventureList

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.HungaryGo.data.repository.AdventureListRepository
import kotlinx.coroutines.launch

class AdventureListViewModel: ViewModel() {
    private val adventureListRepository: AdventureListRepository = AdventureListRepository()

    private val _completedLPList = MutableLiveData<MutableList<String>>()
    val completedLPList: MutableLiveData<MutableList<String>> = _completedLPList

    private val _startedLPList = MutableLiveData<MutableList<String>>()
    val startedLPlist: MutableLiveData<MutableList<String>> = _startedLPList

    //completedLPList megváltoztatása, Listába gyűjti a felhazsnáló által teljesített pályákat, a kilistázáshoz
    fun completedAndStartedLocationsToList(){
        viewModelScope.launch {
            val result = adventureListRepository.completedAndStartedLocationsToList()
            result.onSuccess { list ->
                _completedLPList.value = list.first
                _startedLPList.value = list.second
            }.onFailure { e ->
                Log.e("GloryWallViewModel", "Hiba történt: ", e)
            }
        }
    }

    fun startedLocationsToList(){
        viewModelScope.launch {
            val result = adventureListRepository
        }
    }

}