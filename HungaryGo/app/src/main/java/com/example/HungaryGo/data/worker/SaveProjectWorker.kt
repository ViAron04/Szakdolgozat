package com.example.HungaryGo.data.worker

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.HungaryGo.MakerLocationPackData
import com.example.HungaryGo.data.repository.MakerRepository
import com.google.gson.Gson
import kotlinx.coroutines.launch

class SaveProjectWorker(context: Context, workerParams: WorkerParameters): CoroutineWorker(context, workerParams) {

    private val repository: MakerRepository = MakerRepository()

    override suspend fun doWork(): Result{
        val projectDataJson = inputData.getString("projectData") ?: return Result.failure()
        val currentUserEmail = inputData.getString("currentUserEmail") ?: return Result.failure()
        val makerLocationPackData = Gson().fromJson(projectDataJson, MakerLocationPackData::class.java)

        return try {
            repository.saveProjectChanges(makerLocationPackData)
            Result.success()
        } catch (e: Exception) {
            Log.e("SaveProjectWorker", "Hiba a projekt mentésekor", e)
            Result.retry()
        }
    }
}