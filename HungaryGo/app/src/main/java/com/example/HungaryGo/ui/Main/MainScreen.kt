package com.example.HungaryGo.ui.Main

import CustomInfoWindowForGoogleMap
import android.Manifest
import android.animation.ValueAnimator
import android.app.Dialog

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import com.google.android.gms.location.LocationRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.HungaryGo.LocationPackData
import com.example.HungaryGo.R
import com.example.HungaryGo.data.repository.LocationRepository
import com.example.HungaryGo.data.repository.MainRepository
import com.example.HungaryGo.data.repository.UserRepository
import com.example.HungaryGo.databinding.ActivityMainScreenBinding
import com.example.HungaryGo.ui.AdventureList.AdventureListScreen
import com.example.HungaryGo.ui.Maker.MakerScreen
import com.example.HungaryGo.ui.Options.OptionsScreen
import com.example.HungaryGo.ui.SignIn.SignInScreen
import com.example.HungaryGo.ui.GloryWall.GloryWallScreen
import com.example.HungaryGo.ui.Maker.MakerViewModel
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
import com.google.android.gms.tasks.Tasks
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.Normalizer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class MainScreen : AppCompatActivity(), OnMapReadyCallback,
    NavigationView.OnNavigationItemSelectedListener {




    //var mGoogleMap: GoogleMap? = null

    private val FINE_PERMISSION_CODE = 1
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback1: LocationCallback


    object BitmapStore {
        public val loadedBitmaps = mutableMapOf<String?, android.graphics.Bitmap>()
    }

    val db = FirebaseDatabase.getInstance()

    //Markerek elhelyezése
    //public val markerLocations: MutableMap<String?, MarkerOptions> = mutableMapOf()

    lateinit var toggle: ActionBarDrawerToggle

    var currentLocationPack: String? = null

    private var satelliteOn: Boolean = false

    //var locationPackDataList: MutableList<LocationPackData> = mutableListOf()



    private lateinit var binding: ActivityMainScreenBinding
    private lateinit var drawerLayout: DrawerLayout
    //private val viewModel: MainViewModel by viewModels()
    private val makerViewModel: MakerViewModel by viewModels()

    private var currentMarker: Marker? = null
    private var follows: Boolean = false


    private lateinit var viewModel: MainViewModel

    private lateinit var mGoogleMap: GoogleMap
    private val markers = mutableListOf<Marker?>()
    val mainRepository = MainRepository()
    val userRepository = UserRepository()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRepository = LocationRepository(FirebaseDatabase.getInstance(), fusedLocationClient)

        // ViewModel létrehozása Factoryval
        viewModel = ViewModelProvider(this, MainViewModelFactory(locationRepository))
            .get(MainViewModel::class.java)

        setupToolbar()
        setupNavigationDrawer()
        setupObservers()


        getCurrentLocationUser()
        viewModel.loadLocationPacks()

        showLoading()

       // fusedLocationClient =
       //     LocationServices.getFusedLocationProviderClient(this) //segít energiatakarékosan és hatékonyan megszerezni a helyadatokat


        var currentMarker: Marker? = null

        var currentNearbyLocation: String? = null


        locationCallback1 = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) { //akkor hívódik meg, ha új helymeghatározási eredmények érkeznek
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation //kiszűri a legutóbbi pozíciót

                if (location != null) {
                    currentMarker?.remove()

                    currentLocation = location
                    val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                    var icon = BitmapDescriptorFactory.fromResource(R.drawable.usericondemo)

                    currentMarker = mGoogleMap.addMarker(
                        MarkerOptions().position(latLng).title("szerenysegem").icon(icon)
                    )!!

                    //kattinthatatlanná tétel
                    mGoogleMap.setOnMarkerClickListener { marker ->
                        if (marker.title == "szerenysegem") {
                            true
                        } else {
                            false
                        }
                    }

                    //a jelenleg kiválasztott pálya helyszínei

                    viewModel.currentLocationPackData.value?.let { currentLocationPackList ->
                        for (currentLocationPack in currentLocationPackList.locations) {
                            val distance = FloatArray(1)
                            Location.distanceBetween(
                                currentLocationPack.value?.markerOptions?.position!!.latitude,
                                currentLocationPack.value?.markerOptions?.position!!.longitude, //megnézi, mekkora a táv a markerek és a játékos között
                                latLng.latitude,
                                latLng.longitude,
                                distance
                            )

                            if (distance[0] < 30 && currentLocationPack.key != currentNearbyLocation) {

                                for (mark in markers) {
                                    if (mark?.position?.latitude == currentLocationPack.value?.markerOptions?.position!!.latitude
                                        && mark.position.longitude == currentLocationPack.value?.markerOptions?.position!!.longitude
                                    ) {
                                        mark.showInfoWindow()
                                    }
                                }

                                //showNearWindow(locationName)
                                currentNearbyLocation = currentLocationPack.key
                            }
                        }
                    }
                    val newLatLng = LatLng(location.latitude, location.longitude)

                    if (follows) { //a goToMe funkcióban állítható
                        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(newLatLng)) //a kamera ezáltal követi a felhasználót
                    }
                }
            }
        }
    }

    private fun setupNavigationDrawer() {
        drawerLayout = binding.mapScreen
        val navigationView = binding.navView
        navigationView.setNavigationItemSelectedListener(this)
        val toggle = ActionBarDrawerToggle(this, drawerLayout, binding.toolbar, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setupObservers() {

        //currentLocation
        viewModel.currentLocation.observe(this) { location ->
            //updateUserMarker(location)
        }

        //nearbyLocation
        viewModel.nearbyLocation.observe(this) { locationName ->
            Toast.makeText(this, "Közel van: $locationName", Toast.LENGTH_SHORT).show()
        }

        //currentLocationPackData
        viewModel.currentLocationPackData.observe(this) { currentLocationPackName ->
        if(currentLocationPackName == null)
        {
            supportActionBar?.title = "Üdv, ${viewModel.getUserName()}"
            val xButton = findViewById<ImageButton>(R.id.xButton)
            xButton.visibility = View.INVISIBLE
            mGoogleMap.setInfoWindowAdapter(
                CustomInfoWindowForGoogleMap(
                    this@MainScreen,
                    viewModel.locationPacksData.value!!,
                    null
                )
            )

            for (marker in markers) {
                marker?.isVisible = true
            }
        }
        else{
            mGoogleMap.setInfoWindowAdapter(
                CustomInfoWindowForGoogleMap(
                    this@MainScreen,
                    viewModel.locationPacksData.value!!,
                    currentLocationPackName.name
                )
            )
            supportActionBar?.title = currentLocationPackName?.name
            val xButton = findViewById<ImageButton>(R.id.xButton)
            xButton.visibility = View.VISIBLE

            //val matchingItem = locationPackDataList.find { it.name == currentLocationPack }
            for (marker in markers) {
                if (!currentLocationPackName?.locations!!.containsKey(marker?.title)) {
                    marker?.isVisible = false
                }
            }
        }
        }

        //locationPacksData
        viewModel.locationPacksData.observe(this) { locationPackList ->
            for (locationPackData in locationPackList) {
                for ((name, locationDesc) in locationPackData.locations) {
                    val marker = mGoogleMap.addMarker(
                        locationDesc?.markerOptions?.title(name)!!.draggable(false)
                    )
                    markers.add(marker)
                }
            }

            if (locationPackList != null) {
                mGoogleMap.setInfoWindowAdapter(
                    CustomInfoWindowForGoogleMap(
                        this@MainScreen,
                        locationPackList,
                        viewModel.currentLocationPackData.value?.name
                    )
                )

                mGoogleMap.setOnInfoWindowClickListener { marker ->
                    var markersLocationPackData: LocationPackData = LocationPackData()
                    for (locationPack in locationPackList) {
                        if (locationPack.locations.containsKey(marker.title.toString())) {
                            markersLocationPackData = locationPack
                            break
                        }
                    }

                    if (viewModel.currentLocationPackData.value == null) {
                        showLPDialog(markersLocationPackData, marker, viewModel.locationPacksData.value!!)
                    } else if (viewModel.currentLocationPackData.value!!.locations.containsKey(marker.title)) {
                        showLDialog(markersLocationPackData, marker)
                    }
                }

                val startablePackName = intent.getSerializableExtra("startableLocationPack") as? String
                if (startablePackName != null) {
                    val startablePack = locationPackList.find { it.name == startablePackName }
                    if (startablePack != null) {
                        showLPDialog(startablePack, null, locationPackList)
                    }
                }
            }
        }

        //levelCompleted
        viewModel.levelCompleted.observe(this){ result ->
            val isCompleted = result.getOrDefault(false) // Ha van érték, akkor azt adja vissza, ha nincs, akkor false
            if(isCompleted)
            {
                showLevelEndingDialog()
            }
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Üdv, ${viewModel.getUserName()}"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    var isLoaded = false

    //meghatározza  a jelenlegi pozíciót
    private fun getCurrentLocationUser() {
        //engedély ellenőrzése, ha nincsenek meg, engedélyt kér
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                FINE_PERMISSION_CODE
            )
            return
        }

        //lekéri a felhasználó tartózkodási helyét
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if(location != null)
            {
                currentLocation = location

            }
            else
            {
                val manualLocation = Location("dev").apply {
                    latitude = 46.904001
                    longitude = 17.862048
                }
                currentLocation = manualLocation
            }
                //kiírja a koordinátákat, ha elérhető lokáció
                Toast.makeText(
                    applicationContext, "${currentLocation.latitude}, ${currentLocation.longitude}",
                    Toast.LENGTH_LONG
                ).show()

                val mapFragment =
                    supportFragmentManager.findFragmentById(R.id.mapFelulet) as SupportMapFragment
                mapFragment.getMapAsync(this)

        }.addOnFailureListener{
            Log.e("FUSEDLOCATIONCLIENT", "ROSSZ")
        }
    }


    //ha a felugró ablakban nem engedélyezzük a felhasználást, akkor kiirja alanti üzenetet
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FINE_PERMISSION_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationUser()
            } else {
                Toast.makeText(
                    this,
                    "Helymegosztás engedélyezése nélkül az alkalmazás nem használható megfelelően :(",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }
    }


    //akkor hívódik meg, ha működik az API
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
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                //locationRequest megszerzi a helyadatokat, locationCallbacknek továbbküldi
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback1, null)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    FINE_PERMISSION_CODE
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "Kérem engedélyezze a helymegosztást!", Toast.LENGTH_LONG).show()
        }
    }

    /*
    //megszünteti a helyfrissítések kérését, amikor az Activity háttérbe kerül (tehát amikor bezáródik)
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback1)
    }*/

    //ráközelít a helyzetemre
    fun goToMe(view: View) {
        val goToMeButtonButton: Button = findViewById(R.id.goToMe)
        if (follows) {
            follows = false;
            goToMeButtonButton.setText("Követés bekapcsolása")
        } else {
            val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
            follows = true;
            goToMeButtonButton.setText("Követés kikapcsolása")
        }
    }


    //Jelenleg nincs használatba, haladás folyamatosabb megjelenítését segítené
    private fun animateMarkerToPosition(marker: Marker, finalPosition: LatLng) {
        val startPosition = marker.position
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 1000
        valueAnimator.interpolator = LinearInterpolator()

        valueAnimator.addUpdateListener { animation ->
            val v = animation.animatedValue as Float
            val lat = v * finalPosition.latitude + (1 - v) * startPosition.latitude
            val lng = v * finalPosition.longitude + (1 - v) * startPosition.longitude
            val newPosition = LatLng(lat, lng)
            marker.position = newPosition // Update marker position
        }
        valueAnimator.start()
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.kalandKeres -> {
                //a pályák adatainak küldése
                val intent = Intent(this@MainScreen, AdventureListScreen::class.java)

                val locationPackList = viewModel.locationPacksData.value ?: mutableListOf()
                intent.putExtra("locationPackList", ArrayList(locationPackList))

                startActivity(intent)
            }

            R.id.ujKaland -> {
                startActivity(Intent(this@MainScreen, MakerScreen::class.java))
            }

            R.id.dicsFal -> {
                startActivity(Intent(this@MainScreen, GloryWallScreen::class.java))
            }

            R.id.stat -> {
                Toast.makeText(this, "statisztika", Toast.LENGTH_LONG).show()
            }

            R.id.terkep_kinezet -> {
                val terkepKinezetTitle = item
                if (satelliteOn == false) {
                    mGoogleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    terkepKinezetTitle.setTitle("Alap nézet")
                    satelliteOn = true
                } else {
                    mGoogleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                    terkepKinezetTitle.setTitle("Műhold nézet")
                    satelliteOn = false
                }
            }

            R.id.beallitasok -> {
                startActivity(Intent(this@MainScreen, OptionsScreen::class.java))
            }

            R.id.kijelentkezes -> {
                userRepository.userSignOut()
                startActivity(Intent(this@MainScreen, SignInScreen::class.java))
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }


    //pálya végi ablak megjelnítése
    fun showLevelEndingDialog() {

                Log.d("FirestoreCheck", "All values are 1.")

                val dialog = Dialog(this)
                dialog.setContentView(R.layout.finish_dialog)
                dialog.setCanceledOnTouchOutside(false)
                val finishedLocationPack = dialog.findViewById<TextView>(R.id.finishedLocationPack)
                val continueButton = dialog.findViewById<Button>(R.id.continueButton)
                val rewardImg = dialog.findViewById<ImageView>(R.id.rewardImg)
                val ratingBar = dialog.findViewById<RatingBar>(R.id.ratingBar)


                val currentLocationPackUri = RemoveAccents.removeAccents(viewModel.currentLocationPackData.value!!.name)
                val storageRef = FirebaseStorage.getInstance().reference.child("location_packs_rewards/$currentLocationPackUri.png")

                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(this)
                        .load(uri)
                        .into(rewardImg) // ahol imageView az ImageView ID-ja
                }.addOnFailureListener {
                    Toast.makeText(this, "Hiba történt a kép betöltésekor", Toast.LENGTH_SHORT).show()
                }

                lifecycleScope.launch {
                    ratingBar.rating = userRepository.getUserPrevRating(viewModel.currentLocationPackData.value!!.name)
                        .toFloat()
                }


                finishedLocationPack.text = currentLocationPack
                val currentLocationPack = viewModel.currentLocationPackData.value!!


                continueButton.setOnClickListener()
                {
                    val ratingBar = dialog.findViewById<RatingBar>(R.id.ratingBar)
                    val newRating = ratingBar.rating.toDouble()
                    val currentAvgRating = currentLocationPack.rating
                    val currentCompletionNumber = currentLocationPack.completionNumber

                    lifecycleScope.launch {
                        mainRepository.updateUsersRatingAndCompletionCount( viewModel.currentLocationPackData.value!!,newRating, currentAvgRating!!, currentCompletionNumber)
                        dialog.dismiss()
                        currentLocationPackToNull(findViewById<ImageButton>(R.id.xButton))
                    }
                }
                dialog.show()
    }

    fun showLPDialog(currentLocationPackData: LocationPackData, marker: Marker?, allLocationPackData: MutableList<LocationPackData>) {


        val dialog = Dialog(this)
        dialog.setContentView(R.layout.location_pack_dialog)
        dialog.setCancelable(true)
        val lpImage = dialog.findViewById<ImageView>(R.id.lpImage)
        val lpDescription = dialog.findViewById<TextView>(R.id.lpDescription)
        val continueButton = dialog.findViewById<Button>(R.id.continueButton)
        val lpLocationCount = dialog.findViewById<TextView>(R.id.lpLocationCount)
        val lpRating = dialog.findViewById<TextView>(R.id.lpRating)

        continueButton.setOnClickListener()
        {
            currentLocationPack = currentLocationPackData.name
            viewModel.isLocationpackDone(currentLocationPack!!) { isDone ->
                if (isDone) {
                    showReplayDialog(currentLocationPackData, allLocationPackData)
                    dialog.dismiss()
                    marker?.hideInfoWindow()
                }
                else
                {
                    mGoogleMap.setInfoWindowAdapter(
                        CustomInfoWindowForGoogleMap(
                            this@MainScreen,
                            allLocationPackData,
                            currentLocationPack
                        )
                    )
                    viewModel.currentLocationPackDataSet(currentLocationPackData)
                    dialog.dismiss()
                    marker?.hideInfoWindow()
                }
            }
        }

        //Ha a bitmapstore még nem tartalmazná a képet
        if(!BitmapStore.loadedBitmaps.containsKey(currentLocationPackData.name )){
            val currentLocationPackUri = RemoveAccents.removeAccents(currentLocationPackData.name)
            val storageRef = FirebaseStorage.getInstance().reference.child("location_packs_images/$currentLocationPackUri.jpg")

            storageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this)
                    .asBitmap()
                    .load(uri)
                    .into(object: CustomTarget<Bitmap>(){

                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            BitmapStore.loadedBitmaps[currentLocationPackData.name] = resource
                            lpImage.setImageBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            Toast.makeText(this@MainScreen, "Kép letöltése sikertelen", Toast.LENGTH_SHORT).show()
                        }
                    })
            }.addOnFailureListener {
                Toast.makeText(this, "Hiba történt a kép betöltésekor", Toast.LENGTH_SHORT).show()
            }
        }
        else{
            val picture: Bitmap = BitmapStore.loadedBitmaps[currentLocationPackData.name]!!
            lpImage.setImageBitmap(picture)
        }



        lpDescription.text = currentLocationPackData.description
        lpLocationCount.text = "Helyszínek száma: ${currentLocationPackData.locations.count()}"
        if(currentLocationPackData.rating != 0.0) lpRating.text = "Értékelés: %.2f".format(currentLocationPackData.rating)
        else lpRating.text = "Még nem érkezett értékelés"

        dialog.show()
    }

    fun showLDialog(currentLocationPackData: LocationPackData, marker: Marker) {
        val distance = FloatArray(1)
        Location.distanceBetween(
            marker.position.latitude,
            marker.position.longitude, //megnézi, mekkora a táv a markerek és a játékos között
            currentLocation.latitude,
            currentLocation.longitude,
            distance
        )

        //TODO Kivenni a kommentet
        //if (distance[0] < 30) {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.location_dialog)
            dialog.setCancelable(true)
            val lImage = dialog.findViewById<ImageView>(R.id.lImage)
            val lName = dialog.findViewById<TextView>(R.id.lName)
            val lDescription = dialog.findViewById<TextView>(R.id.lDescription)
            val lQuestion = dialog.findViewById<TextView>(R.id.lQuestion)
            val lQuestionAnswer = dialog.findViewById<EditText>(R.id.lQuestionAnswer)
            val continueButton = dialog.findViewById<Button>(R.id.continueButton)
            val currentLocationData = currentLocationPackData.locations[marker.title]

            lName.text = marker.title
            if (currentLocationData?.description != null) {
                lDescription.text = currentLocationData.description
            }

            if (currentLocationData?.question != null) {
                val params = lQuestion.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = 20
                lQuestion.layoutParams = params
                lQuestion.text = currentLocationData.question
                lQuestionAnswer.visibility = View.VISIBLE
            }

            continueButton.setOnClickListener {
                if (lQuestionAnswer.text.toString() == currentLocationData?.answer || currentLocationData?.question == null) {
                    viewModel.updateLocationInFirestore(marker.title.toString())
                    dialog.dismiss()
                    marker.hideInfoWindow()

                } else {
                    Toast.makeText(
                        applicationContext,
                        "Rossz válasz, a helyszín teljesítéséhez próbáld újra!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            dialog.show()
        //}

    }

    fun showReplayDialog(currentLocationPackData: LocationPackData, allLocationPackData: MutableList<LocationPackData>){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.restart_dialog)
        dialog.setCancelable(true)
        val restartButton = dialog.findViewById<Button>(R.id.restartButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        restartButton.setOnClickListener{
            viewModel.restartLevel(currentLocationPackData.name)
            mGoogleMap.setInfoWindowAdapter(
                CustomInfoWindowForGoogleMap(
                    this@MainScreen,
                    allLocationPackData,
                    currentLocationPack
                )
            )
            viewModel.currentLocationPackDataSet(currentLocationPackData)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener{
            dialog.dismiss()
        }

        dialog.show()
    }
    fun closeInfoWindow(view: View) {

    }


    object RemoveAccents {
        fun removeAccents(input: String?): String {
            var normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            normalized = normalized.replace(Regex("\\p{Mn}"), "").lowercase().replace(' ','_')
            return normalized
        }
    }

    // a currentLocationPack-et Null-ra állítja
    fun currentLocationPackToNull(view: View) {
        viewModel.currentLocationPackToNull()
    }

    fun showLoading(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.loading)
        dialog.setCancelable(false)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)

        dialog.window?.apply {
            // dialog háttere átlátszó
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // dialogon kívüli terület sötétettbé tétele
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.5f)
        }
        progressBar.visibility = View.VISIBLE
        dialog.show()


        viewModel.locationPacksData.observe(this, Observer {
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