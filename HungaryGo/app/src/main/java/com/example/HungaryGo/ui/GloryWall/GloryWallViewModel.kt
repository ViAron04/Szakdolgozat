package com.example.HungaryGo.ui.GloryWall

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.HungaryGo.LocationPackData
import com.example.HungaryGo.data.repository.MainRepository
import com.example.HungaryGo.data.repository.UserRepository
import kotlinx.coroutines.launch

class GloryWallViewModel: ViewModel() {
    private val userRepository: UserRepository = UserRepository()

    private val _rewardBitmaps = MutableLiveData<MutableMap<String?, android.graphics.Bitmap>>()
    val rewardBitmaps: MutableLiveData<MutableMap<String?, android.graphics.Bitmap>> = _rewardBitmaps

    fun getCompletedLevelsReward(context: Context)
    {
        if(rewardBitmaps.value == null) {
            viewModelScope.launch {
                val result = userRepository.getCompletedLevelsReward(context)
                result.onSuccess { bitmaps ->
                    _rewardBitmaps.value = bitmaps // Update the stored bitmaps
                }.onFailure { e ->
                    Log.e("GloryWallViewModel", "Hiba történt: ", e)
                }
            }
        }
    }


}