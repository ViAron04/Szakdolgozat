package com.example.HungaryGo.ui.Maker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.view.LayoutInflater
import com.google.android.gms.location.LocationRequest

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.HungaryGo.LocationPackData
import com.example.HungaryGo.MakerLocationPackData
import com.example.HungaryGo.R
import com.example.HungaryGo.ui.Main.MainScreen
import com.example.HungaryGo.ui.SignIn.SignInViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class MakerScreen : AppCompatActivity(), OnMapReadyCallback {

    var mGoogleMap: GoogleMap? = null

    private val FINE_PERMISSION_CODE = 1
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback1: LocationCallback
    private val viewModel: MakerViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maker)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this) //segít energiatakarékosan és hatékonyan megszerezni a helyadatokat
        getCurrentLocationUser()

        var currentMarker: Marker? = null

        locationCallback1 = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) { //akkor hívódik meg, ha új helymeghatározási eredmények érkeznek
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation //kiszűri a legutóbbi pozíciót



                if (location != null) {
                    currentMarker?.remove()

                    currentLocation = location
                    val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                    val icon = BitmapDescriptorFactory.fromResource(R.drawable.szemuveges_atmeneti)

                    currentMarker = mGoogleMap?.addMarker(MarkerOptions().position(latLng).title("Szerénységem").icon(icon))!!


                    val newLatLng = LatLng(location.latitude, location.longitude)
                    mGoogleMap?.animateCamera(CameraUpdateFactory.newLatLng(newLatLng)) //a kamera ezáltal követi a felhasználót
                }
            }
        }

        lifecycleScope.launch {
            showLoading(true)
            viewModel.getUsersProjects()
            showLoading(false)
        }

        viewModel.usersProjectsList.observe(this, Observer { result ->
            showMakerProjectsDialog()
        })


    }

    private fun getCurrentLocationUser() {
        //engedély ellenőrzése, haa nincsenek meg, engedélyt kér
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), FINE_PERMISSION_CODE)
            return
        }

        //lekéri a felhasználó tartózkodási helyét
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                //kiírja a koordinátákat, ha elérhető lokáció
                Toast.makeText(applicationContext, "${currentLocation.latitude}, ${currentLocation.longitude}",
                    Toast.LENGTH_LONG).show()

                val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFelulet) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
        }

        showMakerProjectsDialog()

    }
    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap

        val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)

        // milyen gyakran és minőségben érkezzenek helyadatok?
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(1000L) // minimális idő updatig
            .setMaxUpdateDelayMillis(1000L)    // maximlis idő updatig
            .setMinUpdateDistanceMeters(3f)    // csak akkor jelezzen, ha x méterrel többet mozog a karakter
            .build()
        Priority.PRIORITY_HIGH_ACCURACY

        // ellenőrzés
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //locationRequest megszerzi a helyadatokat, locationCallbacknek továbbküldi
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback1, null)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), FINE_PERMISSION_CODE)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "Kérem engedélyezze a helymegosztást!", Toast.LENGTH_LONG).show()
        }
    }

    fun addmarker(view: View) {
        val actualMarker: MarkerOptions = MarkerOptions()
            .position(LatLng(currentLocation.latitude, currentLocation.longitude))
            .title("Új marker")
            .draggable(false)

        mGoogleMap?.addMarker(actualMarker)
    }


    fun showMakerProjectsDialog(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.maker_projects_dialog)
        dialog.setCancelable(false)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val listView = dialog.findViewById<ListView>(R.id.projectsListView)
        val backToMainButton = dialog.findViewById<ImageButton>(R.id.backToMainButton)


        if(viewModel.usersProjectsList.value != null)
        {
            val adapter = object: ArrayAdapter<MakerLocationPackData>(this, R.layout.maker_projects_element, viewModel.usersProjectsList.value!!){
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.maker_projects_element, parent, false)
                    val lpName: TextView = view.findViewById(R.id.lpName)
                    val lpLocationCount: TextView = view.findViewById(R.id.lpLocationCount)

                    val locationPack = getItem(position)

                    lpName.text = locationPack?.name
                    lpLocationCount.text = "Helyszínek száma: ${locationPack?.locations?.size.toString()}"

                    return view
                }
            }
            listView.adapter = adapter

        }

        backToMainButton.setOnClickListener{
            backToMainScreen()
        }

        dialog.show()
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
        else{
            dialog.window?.apply {
                // Dim hátteret engedélyez
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                // 0.0f -> nincs elsötétítés, 1.0f -> teljesen fekete
                setDimAmount(0f)


            }
            progressBar.visibility = View.GONE
            dialog.dismiss()
        }
    }

    fun backToMainScreen() {
        startActivity(Intent(this@MakerScreen, MainScreen::class.java))
    }
}