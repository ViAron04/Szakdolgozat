package com.example.HungaryGo.ui.GloryWall

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.HungaryGo.R
import com.example.HungaryGo.ui.Main.MainScreen
import com.example.HungaryGo.ui.SignIn.SignInViewModel
import kotlinx.coroutines.launch

class GloryWallScreen : AppCompatActivity() {

    private val viewModel: GloryWallViewModel by viewModels()
    private lateinit var adapter: RewardListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glory_wall)

        showLoading(true)


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

    fun showLoading(isVisible: Boolean){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.loading)
        dialog.setCancelable(false)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        if(isVisible){
            dialog.window?.apply {
                // dialog háttere átlátszó
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                // dialogon kívüli terület sötétettbé tétele
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(0.5f)
            }
            progressBar.visibility = View.VISIBLE
            dialog.show()
        }
        viewModel.rewardBitmaps.observe(this, Observer {
            dialog.window?.apply {
                // Dim hátteret engedélyez
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                // 0.0f -> nincs elsötétítés, 1.0f -> teljesen fekete
                setDimAmount(0f)


            }
            progressBar.visibility = View.GONE
            dialog.dismiss()
        })
    }
}