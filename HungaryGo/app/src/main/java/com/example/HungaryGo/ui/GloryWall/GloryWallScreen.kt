package com.example.HungaryGo.ui.GloryWall

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.HungaryGo.R
import com.example.HungaryGo.ui.Main.MainScreen
import com.example.HungaryGo.ui.SignIn.SignInViewModel

class GloryWallScreen : AppCompatActivity() {

    private val viewModel: GloryWallViewModel by viewModels()
    private lateinit var adapter: RewardListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glory_wall)

        val rewardList: ListView = findViewById(R.id.rewardList)
        adapter = RewardListAdapter(this, mutableListOf())
        rewardList.adapter = adapter

        viewModel.rewardBitmaps.observe(this, Observer { result ->
            adapter.updateData(result)
        })

        viewModel.getCompletedLevelsReward(this)
    }

    fun backToMainScreen(view: View) {
        startActivity(Intent(this@GloryWallScreen, MainScreen::class.java))
    }

}