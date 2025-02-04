package com.example.HungaryGo

import CustomInfoWindowForGoogleMap
import android.Manifest
import android.animation.ValueAnimator
import android.app.Dialog

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import com.google.android.gms.location.LocationRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class MainScreen : AppCompatActivity(), OnMapReadyCallback,
    NavigationView.OnNavigationItemSelectedListener {

    var mGoogleMap: GoogleMap? = null

    private val FINE_PERMISSION_CODE = 1
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback1: LocationCallback
    private lateinit var auth: FirebaseAuth

    object BitmapStore {
        public val loadedBitmaps = mutableMapOf<String?, android.graphics.Bitmap>()
    }

    val db = FirebaseDatabase.getInstance()

    //Markerek elhelyezése
    //public val markerLocations: MutableMap<String?, MarkerOptions> = mutableMapOf()

    lateinit var toggle: ActionBarDrawerToggle

    var currentLocationPack: String? = null

    private var satelliteOn: Boolean = false

    var locationPackDataList: MutableList<LocationPackData> = mutableListOf()

    var follows: Boolean = false

    val markers = mutableListOf<Marker?>()

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        auth = Firebase.auth

        //sidemenu

        drawerLayout = findViewById(R.id.mapScreen)
        //val navView: NavigationView = findViewById(R.id.nav_view)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        //toolbar felirata
        supportActionBar?.title = "Üdv, ${auth.currentUser?.displayName}"

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle =
            ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close)

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        //sidemenu vége


        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this) //segít energiatakarékosan és hatékonyan megszerezni a helyadatokat
        getCurrentLocationUser()

        var currentMarker: Marker? = null

        var currentNearbyLocation: String? = null

        getLocationPacks()

        locationCallback1 = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) { //akkor hívódik meg, ha új helymeghatározási eredmények érkeznek
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation //kiszűri a legutóbbi pozíciót

                if (location != null) {
                    currentMarker?.remove()

                    currentLocation = location
                    val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                    var icon = BitmapDescriptorFactory.fromResource(R.drawable.usericondemo)

                    currentMarker = mGoogleMap?.addMarker(
                        MarkerOptions().position(latLng).title("szerenysegem").icon(icon)
                    )!!

                    //kattinthatatlanná tétel
                    mGoogleMap?.setOnMarkerClickListener { marker ->
                        if (marker.title == "szerenysegem") {
                            true
                        } else {
                            false
                        }
                    }

                    //a jelenleg kiválasztott pálya helyszínei
                    val currentLocationPackList =
                        locationPackDataList.find { it.name == currentLocationPack }
                    if (currentLocationPackList != null) {
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
                        mGoogleMap?.animateCamera(CameraUpdateFactory.newLatLng(newLatLng)) //a kamera ezáltal követi a felhasználót
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    var isLoaded = false

    //markerek lehelyezése, az infowindowra történő kattintás kezelése
    private fun getLocationPacks() {
        //belép a location packsba
        val locationPacksRef = db.getReference("location packs")

        locationPacksRef.addListenerForSingleValueEvent(object :
            ValueEventListener { //csak egyszer kéri el az adatokat az adatbázisból
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) { //létezik-e az adat

                    for (buildingSnapshotLP in snapshot.children) { //Pl. Pannon Egyetem, Vasszécseny kör

                        val locationPackData = LocationPackData()

                        locationPackData.name = buildingSnapshotLP.key.toString()

                        for (buildingSnapshot in buildingSnapshotLP.children) {
                            if (buildingSnapshot.key == "rating") {
                                locationPackData.rating =
                                    buildingSnapshot.value.toString().toDouble()
                            } else if (buildingSnapshot.key == "description") {
                                locationPackData.description = buildingSnapshot.value.toString()
                            } else if (buildingSnapshot.key == "completionNumber") {
                                locationPackData.completionNumber =
                                    buildingSnapshot.value.toString().toInt()
                            } else {
                                val buildingMap = buildingSnapshot.value as Map<String, Any>
                                //val latitude = buildingMap["latitude"] as Double
                                //val longitude = buildingMap["longitude"] as Double
                                val markerOptions = MarkerOptions()
                                    .position(
                                        LatLng(
                                            buildingMap["latitude"] as Double,
                                            buildingMap["longitude"] as Double
                                        )
                                    )
                                val description = buildingMap["Description"] as String?
                                val name = buildingSnapshot.key.toString()
                                var question = null as String?
                                var answer = null as String?
                                if (buildingMap.containsKey("Question")) {
                                    question = buildingMap["Question"] as String?
                                    answer = buildingMap["Answer"] as String?
                                }

                                locationPackData.locations[name] = LocationDescription(
                                    markerOptions,
                                    description = description,
                                    question = question,
                                    answer = answer
                                )

                                val marker = mGoogleMap?.addMarker(
                                    markerOptions
                                        .title(name)
                                        .draggable(false)
                                )
                                markers.add(marker)
                            }
                        }
                        locationPackDataList.add(locationPackData)
                    }
                    mGoogleMap?.setInfoWindowAdapter(
                        CustomInfoWindowForGoogleMap(
                            this@MainScreen,
                            locationPackDataList,
                            currentLocationPack
                        )
                    )
                    isLoaded = true;
                } else {
                    println("Nem találtam helyszíneket az adatbázisban")
                }

                //infowindowra kattintás
                mGoogleMap?.setOnInfoWindowClickListener { marker ->
                    var currentLocationPackData: LocationPackData = LocationPackData()
                    for (locationPack in locationPackDataList) {
                        if (locationPack.locations.containsKey(marker.title.toString())) {
                            currentLocationPackData = locationPack
                        }
                    }

                    if (currentLocationPack == null) {
                        showLPDialog(currentLocationPackData, marker)
                    } else if (currentLocationPackData.locations.containsKey(marker.title)) {
                        showLDialog(currentLocationPackData, marker)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) { //hibakezelés
                println("Error getting data: ${error.message}")
            }
        })
    }

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
            if (location != null) {
                currentLocation = location
                //kiírja a koordinátákat, ha elérhető lokáció
                Toast.makeText(
                    applicationContext, "${currentLocation.latitude}, ${currentLocation.longitude}",
                    Toast.LENGTH_LONG
                ).show()

                val mapFragment =
                    supportFragmentManager.findFragmentById(R.id.mapFelulet) as SupportMapFragment
                mapFragment.getMapAsync(this)
            }
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
            mGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
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
                val intent = Intent(this@MainScreen, AdventureList::class.java)
                intent.putExtra("locationPackList", ArrayList(locationPackDataList))

                startActivity(intent)
            }

            R.id.ujKaland -> {
                startActivity(Intent(this@MainScreen, Maker::class.java))
            }

            R.id.dicsFal -> {
                startActivity(Intent(this@MainScreen, gloryWall::class.java))
            }

            R.id.stat -> {
                Toast.makeText(this, "statisztika", Toast.LENGTH_LONG).show()
            }

            R.id.terkep_kinezet -> {
                val terkepKinezetTitle = item
                if (satelliteOn == false) {
                    mGoogleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    terkepKinezetTitle.setTitle("Alap nézet")
                    satelliteOn = true
                } else {
                    mGoogleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                    terkepKinezetTitle.setTitle("Műhold nézet")
                    satelliteOn = false
                }
            }

            R.id.beallitasok -> {
                startActivity(Intent(this@MainScreen, OptionsScreen::class.java))
            }

            R.id.kijelentkezes -> {
                Firebase.auth.signOut()
                startActivity(Intent(this@MainScreen, SignInScreen::class.java))
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }


    //ellenőrzi, megvan-e az összes helyszín a pályából
    fun checkLocations() {
        checkLocationPackCompletion(currentLocationPack.toString()) { allOne ->
            //dialog megjelenítése
            if (allOne) {
                Log.d("FirestoreCheck", "All values are 1.")

                val dialog = Dialog(this)
                dialog.setContentView(R.layout.finish_dialog)
                val finishedLocationPack = dialog.findViewById<TextView>(R.id.finishedLocationPack)
                val continueButton = dialog.findViewById<Button>(R.id.continueButton)
                val ratingBar = dialog.findViewById<RatingBar>(R.id.ratingBar)

                val dbfirestore = FirebaseFirestore.getInstance()
                val currentUserEmail = auth.currentUser?.email.toString()
                val documentRef = dbfirestore.collection("userpoints")
                    .document(currentUserEmail)
                    .collection("inprogress")
                    .document(currentLocationPack!!)

                documentRef.get().addOnSuccessListener { docSnapshot ->
                    val usersRating = docSnapshot.getLong("usersRating") ?: 0
                    ratingBar.rating = usersRating.toFloat()
                }

                finishedLocationPack.text = currentLocationPack
                val currentLocationPackRating =
                    locationPackDataList.find { it.name == currentLocationPack }


                continueButton.setOnClickListener()
                {
                    val ratingBar = dialog.findViewById<RatingBar>(R.id.ratingBar)
                    val newRating = ratingBar.rating.toDouble()
                    val currentAvgRating = currentLocationPackRating?.rating
                    val currentCompletionNumber = currentLocationPackRating?.completionNumber

                    lifecycleScope.launch {
                        updateUsersRatingAndCompletionCount(newRating, currentAvgRating!!, currentCompletionNumber!!)
                        dialog.dismiss()
                        currentLocationPackToNull(findViewById<ImageButton>(R.id.xButton))
                    }
                }
                dialog.show()
            }
        }
    }

    fun showLPDialog(currentLocationPackData: LocationPackData, marker: Marker) {
        val picture: Bitmap = BitmapStore.loadedBitmaps[currentLocationPackData.name]!!
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.location_pack_dialog)
        dialog.setCancelable(true)
        val lpImage = dialog.findViewById<ImageView>(R.id.lpImage)
        val lpDescription = dialog.findViewById<TextView>(R.id.lpDescription)
        val continueButton = dialog.findViewById<Button>(R.id.continueButton)
        val lpLocationCount = dialog.findViewById<TextView>(R.id.lpLocationCount)

        continueButton.setOnClickListener()
        {
            currentLocationPack = currentLocationPackData.name
            mGoogleMap?.setInfoWindowAdapter(
                CustomInfoWindowForGoogleMap(
                    this@MainScreen,
                    locationPackDataList,
                    currentLocationPack
                )
            )
            currentLocationPackSet(currentLocationPackData.name)
            val nonNullableCurrentLocationPack: String = currentLocationPack!!
            dialog.dismiss()
            marker.hideInfoWindow()
        }
        lpImage.setImageBitmap(picture)
        lpDescription.text = currentLocationPackData.description
        lpLocationCount.text = "Helyszínek száma: ${currentLocationPackData.locations.count()}"
        dialog.show()
    }

    fun showLDialog(currentLocationPackData: LocationPackData, marker: Marker) {
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
                lifecycleScope.launch {
                    updateLocationInFirestore(currentLocationPackData, marker.title.toString())
                    checkLocations()
                    dialog.dismiss()
                    marker.hideInfoWindow()
                }
            } else {
                Toast.makeText(
                    applicationContext,
                    "Rossz válasz, a helyszín teljesítéséhez próbáld újra!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        dialog.show()
    }

    private suspend fun updateLocationInFirestore(currentLocationPackData: LocationPackData, markerTitle: String) = withContext(Dispatchers.IO) {
        try {


            val dbfirestore = FirebaseFirestore.getInstance()
            val currentUserEmail = auth.currentUser?.email.toString()
            val userRef =
                dbfirestore.collection("userpoints").document(currentUserEmail)
                    .collection("inprogress").document(currentLocationPack.toString())

            // Dokumentum lekérése
            val document = userRef.get().await()
            //A pálya végigvitelének mennyisége és a felhasználó értékelése róla
            if (!document.contains("usersRating")) {
                val generalData = mutableMapOf<String, Any>()
                generalData["completionCount"] = 0
                generalData["usersRating"] = 0

                userRef.set(generalData, SetOptions.merge()).await()
            }
            //Minden helyszín a pályában legyen nulla
            if (!document.contains("locations")) {
                val updatedLocations = mutableMapOf<String, Int>()
                for (location in currentLocationPackData.locations) {
                    updatedLocations[location.key] = 0
                }
                val locationData = mapOf("locations" to updatedLocations)
                userRef.set(locationData, SetOptions.merge()).await()
            }

            //aktuális helyszín-t egyre kell állítani
            val locationData = mapOf(
                "locations" to mapOf(markerTitle to 1)
            )
            userRef.set(locationData, SetOptions.merge()).await()


        } catch (e: Exception) {
            Log.e("Firestore", "Hiba történt: ", e)
        }
    }

    fun markerReload(view: View) {
        getLocationPacks()
    }

    fun closeInfoWindow(view: View) {

    }

    // a currentLocationPack-et Null-ra állítja
    fun currentLocationPackToNull(view: View) {
        currentLocationPack = null
        supportActionBar?.title = "Üdv, ${auth.currentUser?.displayName}"
        val xButton = findViewById<ImageButton>(R.id.xButton)
        xButton.visibility = View.INVISIBLE
        mGoogleMap?.setInfoWindowAdapter(
            CustomInfoWindowForGoogleMap(
                this@MainScreen,
                locationPackDataList,
                currentLocationPack
            )
        )

        for (marker in markers) {
            marker?.isVisible = true
        }
    }

    // a currentLocationPack értékét megváltoztatja
    fun currentLocationPackSet(locationName: String) {
        currentLocationPack = locationName
        mGoogleMap?.setInfoWindowAdapter(
            CustomInfoWindowForGoogleMap(
                this@MainScreen,
                locationPackDataList,
                currentLocationPack
            )
        )
        supportActionBar?.title = currentLocationPack
        val xButton = findViewById<ImageButton>(R.id.xButton)
        xButton.visibility = View.VISIBLE

        val matchingItem = locationPackDataList.find { it.name == currentLocationPack }
        for (marker in markers) {
            if (!matchingItem?.locations!!.containsKey(marker?.title)) {
                marker?.isVisible = false
            }
        }
    }

    //usersRating és completionCount átírása Firestore-ban, a rating frissítése realtime database-ben
    suspend fun  updateUsersRatingAndCompletionCount(newRating: Double, currentAvgRating: Double, currentCompletionNumber: Int)
    {
        return withContext(Dispatchers.IO) {
            try {


                val dbfirestore = FirebaseFirestore.getInstance()
                val currentUserEmail = auth.currentUser?.email.toString()
                val documentRef = dbfirestore.collection("userpoints")
                    .document(currentUserEmail)
                    .collection("inprogress")
                    .document(currentLocationPack!!)

                // Convert to coroutine
                val docSnapshot = suspendCancellableCoroutine { continuation ->
                    documentRef.get()
                        .addOnSuccessListener { continuation.resume(it) }
                        .addOnFailureListener { continuation.resumeWithException(it) }
                }

                if (docSnapshot.exists()) {
                    val currentCompletionCount = docSnapshot.getLong("completionCount") ?: 0
                    val currentUsersRating = docSnapshot.getDouble("usersRating")

                    // Use Tasks.await() for Firebase operations
                    Tasks.await(documentRef.update("completionCount", currentCompletionCount + 1))

                    val database: DatabaseReference = FirebaseDatabase.getInstance().reference

                    if (currentUsersRating == 0.0) {
                        Tasks.await(documentRef.update("usersRating", newRating))
                        val rating = (currentAvgRating * currentCompletionNumber + newRating) / (currentCompletionNumber + 1)

                        val completionNumberRef = database.child("location packs")
                            .child(currentLocationPack!!)
                            .child("completionNumber")

                        Tasks.await(completionNumberRef.setValue(currentCompletionNumber + 1))

                        val ratingRef = database.child("location packs")
                            .child(currentLocationPack!!)
                            .child("rating")

                        Tasks.await(ratingRef.setValue(rating))

                        locationPackDataList.find { it.name == currentLocationPack }?.rating = rating
                        locationPackDataList.find { it.name == currentLocationPack }?.completionNumber = currentCompletionNumber + 1
                        } else if (currentUsersRating != newRating) {
                        Tasks.await(documentRef.update("usersRating", newRating))

                        val rating = ((currentAvgRating * currentCompletionNumber - currentUsersRating!!.toFloat()) + newRating) / (currentCompletionNumber)

                        val ratingRef = database.child("location packs")
                            .child(currentLocationPack!!)
                            .child("rating")

                        // Várakozás a rating frissítésére
                        Tasks.await(ratingRef.setValue(rating))

                        locationPackDataList.find { it.name == currentLocationPack }?.rating = rating
                        locationPackDataList.find { it.name == currentLocationPack }?.completionNumber = currentCompletionNumber + 1
                        }
                    }
            } catch (e: Exception) {
                Log.e("Firestore", "Hiba történt: ", e)
            }
        }
    }

    fun checkLocationPackCompletion(currentLocationPackParam: String, callback: (Boolean) -> Unit) {
        val dbfirestore = FirebaseFirestore.getInstance()
        val currentUserEmail = auth.currentUser?.email.toString()
        val documentRef = dbfirestore.collection("userpoints")
            .document(currentUserEmail)
            .collection("inprogress")
            .document(currentLocationPackParam)

        documentRef.get()
            .addOnSuccessListener { docSnapshot ->
                if (docSnapshot.exists()) {
                    val locations = docSnapshot.get("locations") as? Map<String, Any>
                    val allOne = locations?.all { (_, value) -> value == 1L || value == 1.0 } ?: false
                    callback(allOne)
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }
}