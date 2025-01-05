package com.example.HungaryGo

import CustomInfoWindowForGoogleMap
import android.Manifest
import android.animation.ValueAnimator
import android.app.AlertDialog

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.LocationRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase


class MainScreen : AppCompatActivity(), OnMapReadyCallback,
    NavigationView.OnNavigationItemSelectedListener {

    var mGoogleMap: GoogleMap? = null

    private val FINE_PERMISSION_CODE = 1
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback1: LocationCallback
    private lateinit var auth: FirebaseAuth

    val db = FirebaseDatabase.getInstance()

    //Markerek elhelyezése
    public val markerLocations: MutableMap<String?, MarkerOptions> = mutableMapOf()

    lateinit var toggle: ActionBarDrawerToggle

    var currentLocationPack: String? = null

    private var satelliteOn: Boolean = false

    //pályák és a bennük található helyek listája (currentLocationPack megtalálásához)
    val locationPackList: MutableMap<String?, MutableList<String?>> = mutableMapOf()

    //a jelenleg kiválasztott pálya helyszínei
    val currentLocationPackList: MutableMap<String?, MarkerOptions> = mutableMapOf()

    var follows: Boolean = false

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
        //supportActionBar?.title = "Üdv, ${auth.currentUser?.email}"

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

                //getLocationPacks() //Firebase-ben tárolt helyeket hívja le és jeleníti meg

                if (location != null) {
                    currentMarker?.remove()

                    currentLocation = location
                    val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                    val icon = BitmapDescriptorFactory.fromResource(R.drawable.szemuveges_atmeneti)

                    currentMarker = mGoogleMap?.addMarker(
                        MarkerOptions().position(latLng).title("Szerénységem").icon(icon)
                    )!!


                    for ((locationName, marker) in currentLocationPackList) {
                        val distance = FloatArray(1)
                        Location.distanceBetween(
                            marker.position.latitude,
                            marker.position.longitude, //megnézi, mekkora a táv a markerek és a játékos között
                            latLng.latitude,
                            latLng.longitude,
                            distance
                        )

                        if (distance[0] < 20 && locationName != currentNearbyLocation) {

                            showNearInfoWindow(locationName)
                            //Toast.makeText(applicationContext, "Közel vagy ${locationName}-hoz", Toast.LENGTH_LONG).show()
                            currentNearbyLocation = locationName

                        }
                    }

                    val newLatLng = LatLng(location.latitude, location.longitude)

                    if (follows) { //a goToMe funkcióban állítható
                        mGoogleMap?.animateCamera(CameraUpdateFactory.newLatLng(newLatLng)) //a kamera ezáltal követi a felhasználót
                    }
                }
            }

            //szövegablak ha közel érek
            private fun showNearInfoWindow(locationName: String?) {
                val dialogBuilder =
                    AlertDialog.Builder(this@MainScreen)

                dialogBuilder.setTitle("Közel vagy!")
                    .setMessage("A(z) $locationName-hez értél!")
                    .setCancelable(true)
                    .setPositiveButton("OK") { dialog, _ ->
                        val dbfirestore = FirebaseFirestore.getInstance()
                        val currentUserEmail = auth.currentUser?.email.toString()
                        val collectionRef =
                            dbfirestore.collection("userpoints").document(currentUserEmail)
                                .collection("inprogress")

                        val locationPoint = hashMapOf(
                            locationName to 1
                        )
                        var currentLocationPackNonNullable = currentLocationPack.toString()
                        collectionRef.document(currentLocationPackNonNullable)
                            .set(locationPoint, SetOptions.merge())
                        Toast.makeText(
                            applicationContext,
                            "Helyszín megcsinálva!",
                            Toast.LENGTH_SHORT
                        ).show()

                        checkLocations()
                        dialog.dismiss()

                    }

                val alertDialog = dialogBuilder.create()
                alertDialog.show() // Show the dialog

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

                    for (buildingSnapshot1 in snapshot.children) { //Pl. Pannon Egyetem, Vasszécseny kör
                        val locationPack = buildingSnapshot1

                        var currentList: MutableList<String?> = mutableListOf()

                        for (buildingSnapshot in locationPack.children) {
                            /*if (buildingSnapshot.key == "image")
                            {
                                val imgName = buildingSnapshot.value.toString()
                                if (!markerLocations.containsKey("image")) {
                                    currentList.add(imgName)
                                }
                            }*/
                            if (buildingSnapshot.key != "rating" && buildingSnapshot.key != "image") {
                                val buildingName =
                                    buildingSnapshot.key  //Pl. "A épület", "I épület"


                                if (!markerLocations.containsKey(buildingName)) {

                                    currentList.add(buildingName)
                                    Log.d("BuildId", "name: " + buildingName)


                                    val buildingMap = buildingSnapshot.value as Map<String, Any>
                                    val buildId = buildingMap["Id"] as String
                                    val latitude = buildingMap["latitude"] as Double
                                    val longitude = buildingMap["longitude"] as Double

                                    currentList.add(buildId)

                                    Log.d("BuildId", buildId)

                                    // Lehelyezi/ hozzáadja a Map-hez a markereket
                                    val actualMarker: MarkerOptions = MarkerOptions()
                                        .position(LatLng(latitude, longitude))
                                        .title(buildingName)
                                        .draggable(false)

                                    markerLocations[buildingName] = actualMarker

                                    mGoogleMap?.addMarker(actualMarker)


                                }
                            }
                        }

                        locationPackList[locationPack.key] = currentList;
                        mGoogleMap?.setInfoWindowAdapter(CustomInfoWindowForGoogleMap(this@MainScreen, locationPackList))
                    }

                isLoaded=true;
                } else {
                    println("Nem találtam helyszíneket az adatbázisban")
                }

                //infowindowra kattintás
                mGoogleMap?.setOnInfoWindowClickListener { marker ->
                    if (!isLoaded) {
                        Toast.makeText(
                            this@MainScreen,
                            "Még töltök, kérlek várj...",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnInfoWindowClickListener
                    } else {
                        Toast.makeText(
                            this@MainScreen,
                            "Ennél a markernél történt kattintás: ${marker.title}",
                            Toast.LENGTH_SHORT
                        ).show()
                        var locationPackDisplay: TextView? = findViewById(R.id.levelDisplay)

                        //megkeresi a location_packet
                        for ((locationPack1, location) in locationPackList) {
                            if (location.contains(marker.title)) {
                                locationPackDisplay?.setText(locationPack1)
                                currentLocationPack = locationPack1
                                break
                            }
                        }
                        currentLocationPackList.clear()

                        val nonNullableCurrentLocationPack: String = currentLocationPack!!
                        getLocationData(nonNullableCurrentLocationPack)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) { //hibakezelés
                println("Error getting data: ${error.message}")
            }
        })
    }


    fun getLocationData(locationPackName: String) {
        val dbfirestore = FirebaseFirestore.getInstance()
        val currentUserEmail = auth.currentUser?.email.toString()
        val collectionRef =
            dbfirestore.collection("userpoints").document(currentUserEmail).collection("inprogress")


        val locationPacksRef = db.getReference("location packs")
        locationPacksRef.child(locationPackName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    collectionRef.document(locationPackName).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                // A dokumentum már létezik, itt kezelheted ezt az esetet.
                                for (locationSnapshot in snapshot.children) {
                                    if (locationSnapshot.key != "rating" && locationSnapshot.key != "image") {
                                        val buildingMap = locationSnapshot.value as Map<String, Any>
                                        val latitude = buildingMap["latitude"] as Double
                                        val longitude = buildingMap["longitude"] as Double

                                        val actualMarker: MarkerOptions = MarkerOptions()
                                            .position(LatLng(latitude, longitude))

                                        currentLocationPackList[locationSnapshot.key] = actualMarker
                                    }
                                }

                            } else {
                                for (locationSnapshot in snapshot.children) {
                                    if (locationSnapshot.key != "rating" && locationSnapshot.key != "image") {
                                        val buildingMap = locationSnapshot.value as Map<String, Any>
                                        val latitude = buildingMap["latitude"] as Double
                                        val longitude = buildingMap["longitude"] as Double

                                        val actualMarker: MarkerOptions = MarkerOptions()
                                            .position(LatLng(latitude, longitude))

                                        val locationPoint = hashMapOf(
                                            locationSnapshot.key to 0
                                        )
                                        collectionRef.document(locationPackName)
                                            .set(locationPoint, SetOptions.merge())

                                        currentLocationPackList[locationSnapshot.key] = actualMarker
                                    }
                                }
                            }
                        }
                }
                override fun onCancelled(error: DatabaseError) {
                    // Hibakezelés
                    println("Sikertelen adatlekérés: ${error.message}")
                }
            })


    }

    /*
    //az infowindow-ban start gombra kattintás
    fun startButtonClick(marker: Marker) {
        Toast.makeText(this, "Button clicked for marker: ${marker.title}", Toast.LENGTH_SHORT).show()
    }*/


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
                Toast.makeText(this, "Helymegosztás engedélyezése nélkül az alkalmazás nem használható megfelelően :(", Toast.LENGTH_LONG)
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
                startActivity(Intent(this@MainScreen, AdventureList::class.java))
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

            R.id.beallitasok -> Toast.makeText(this, "beallitasok", Toast.LENGTH_LONG).show()

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
        val dbfirestore = FirebaseFirestore.getInstance()
        val currentUserEmail = auth.currentUser?.email.toString()
        val currentLocatipnPackNonNullable1 = currentLocationPack.toString()
        val documentRef =
            dbfirestore.collection("userpoints").document(currentUserEmail).collection("inprogress")
                .document(currentLocatipnPackNonNullable1)



        documentRef.get().addOnSuccessListener { docuRef ->
            val data = docuRef.data

            //a document összes eleme 1?
            val allOne = data?.all { (key, value) ->
                val isOne = value == 1L || value == 1.0
                Log.d("FirestoreCheck", "Field: $key, Value: $value, IsOne: $isOne")
                isOne
            } ?: false

            //dialog megjelenítése
            if (allOne) {
                Log.d("FirestoreCheck", "All values are 1.")

                val dialogBuilder = AlertDialog.Builder(this@MainScreen)
                dialogBuilder.setTitle("Megcsináltad!")
                    .setMessage("Gratulálok, teljesítetted a $currentLocationPack pályát!")
                    .setCancelable(true)
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                val alertDialog = dialogBuilder.create()
                alertDialog.show()
            }
        }
    }

    fun markerReload(view: View) {
        getLocationPacks()
    }

}