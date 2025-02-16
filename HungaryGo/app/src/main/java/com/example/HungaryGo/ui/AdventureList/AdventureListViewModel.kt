package com.example.HungaryGo.ui.AdventureList

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.HungaryGo.data.repository.AdventureListRepository
import com.example.HungaryGo.data.repository.UserRepository
import kotlinx.coroutines.launch

class AdventureListViewModel: ViewModel() {
    private val adventureListRepository: AdventureListRepository = AdventureListRepository()

    private val _completedLPList = MutableLiveData<MutableList<String>>()
    val completedLPList: MutableLiveData<MutableList<String>> = _completedLPList

    //completedLPList megváltoztatása, Listába gyűjti a felhazsnáló által teljesített pályákat, a kilistázáshoz
    fun completedLocationsToList(){
        viewModelScope.launch {
            val result = adventureListRepository.completedLocationsToList()
            result.onSuccess { list ->
                _completedLPList.value = list
            }.onFailure { e ->
                Log.e("GloryWallViewModel", "Hiba történt: ", e)
            }
        }
    }

}