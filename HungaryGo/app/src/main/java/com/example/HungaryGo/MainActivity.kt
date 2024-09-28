package com.example.HungaryGo

import android.Manifest
import android.animation.ValueAnimator
import android.app.ActionBar
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.LocationRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    var mGoogleMap: GoogleMap? = null

    private val FINE_PERMISSION_CODE = 1
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback1: LocationCallback

    val db = FirebaseDatabase.getInstance()
    val markerLocations: MutableMap<String? ,MarkerOptions> = mutableMapOf()

    lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //sidemenu
        // Find the DrawerLayout and NavigationView
        val drawerLayout: DrawerLayout = findViewById(R.id.mapScreen)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Initialize the ActionBarDrawerToggle to handle the opening/closing of the side menu
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)

        // Attach the DrawerListener to DrawerLayout
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Enable the toggle button (hamburger icon) in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set click listener for the NavigationView items
        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.statisztikak -> startActivity(Intent(this@MainActivity, SignInScreen::class.java))
            }
            drawerLayout.closeDrawers() // Close the drawer after selection
            true
        }
        //sidemenu

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this) //segít energiatakarékosan és hatékonyan megszerezni a helyadatokat
        getCurrentLocationUser()

        var currentMarker: Marker? = null

        locationCallback1 = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) { //akkor hívódik meg, ha új helymeghatározási eredmények érkeznek
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation //kiszűri a legutóbbi pozíciót

                getLocationPacks() //Firebase-ben tárolt helyeket hívja le és jeleníti meg

                if (location != null) {
                    currentMarker?.remove()

                    currentLocation = location
                    val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                    val icon = BitmapDescriptorFactory.fromResource(R.drawable.szemuveges_atmeneti)

                    currentMarker = mGoogleMap?.addMarker(MarkerOptions().position(latLng).title("Szerénységem").icon(icon))!!

                    for((name,marker) in markerLocations){
                        val distance = FloatArray(1)
                        Location.distanceBetween(marker.position.latitude, marker.position.longitude, //megnézi, mekkora a táv a markerek és a játékos között
                            latLng.latitude, latLng.longitude, distance)

                        if(distance[0] < 20){
                            Toast.makeText(applicationContext, "Közel vagy ${name}-hoz", Toast.LENGTH_LONG).show()
                        }
                    }

                    val newLatLng = LatLng(location.latitude, location.longitude)
                    mGoogleMap?.animateCamera(CameraUpdateFactory.newLatLng(newLatLng)) //a kamera ezáltal követi a felhasználót
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

    private fun getLocationPacks()
    {
        //belép a location packsba
        val locationPacksRef = db.getReference("location packs")

        locationPacksRef.addListenerForSingleValueEvent(object : ValueEventListener { //csak egyszer kéri el az adatokat az adatbázisból
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) { //létezik-e az adat

                    for (buildingSnapshot1 in snapshot.children) { //Pl. Pannon Egyetem, Vasszécseny kör
                        val locationPack = buildingSnapshot1
                            for (buildingSnapshot in locationPack.children) {
                                val buildingName =
                                    buildingSnapshot.key  //Pl. "A épület", "I épület"
                                if (!markerLocations.containsKey(buildingName)) {
                                    val buildingMap = buildingSnapshot.value as Map<String, Any>

                                    val latitude = buildingMap["latitude"] as Double
                                    val longitude = buildingMap["longitude"] as Double

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
                } else {
                    println("Nem találtam helyszíneket az adatbázisban")
                }
            }
            override fun onCancelled(error: DatabaseError) { //hibakezelés
                println("Error getting data: ${error.message}")
            }
        })

    }

    //meghatározza  a jelenlegi pozíciót
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
    }

    //ha a felugró ablakban nem engedélyezzük a felhasználást, akkor kiirja alanti üzenetet
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FINE_PERMISSION_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationUser()
            } else {
                Toast.makeText(this, "Kérem engedélyezze a helymegosztást!", Toast.LENGTH_LONG).show()
            }
        }
    }

    //akkor hívódik meg, ha működik az API
    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap

        //Tedd választhatóvá!!
        //mGoogleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE

        val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)

        // milyen gyakran és minőségben érkezzenek helyadatok?
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(1000L) // Set the minimum update interval in milliseconds
            .setMaxUpdateDelayMillis(1000L)    // Optional: Set the maximum delay between updates
            .setMinUpdateDistanceMeters(3f)    // Position updates only if user moves 1 meter
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

    /*
    //megszünteti a helyfrissítések kérését, amikor az Activity háttérbe kerül (tehát amikor bezáródik)
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback1)
    }*/

    //ráközelít a helyzetemre
    fun goToMe(view: View) {
        val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        mGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
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

    private fun menuButton(){

    }



}