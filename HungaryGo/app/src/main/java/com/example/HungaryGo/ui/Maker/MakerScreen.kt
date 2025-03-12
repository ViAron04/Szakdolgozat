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
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import com.google.android.gms.location.LocationRequest

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.HungaryGo.MakerLocationDescription
import com.example.HungaryGo.MakerLocationPackData
import com.example.HungaryGo.R
import com.example.HungaryGo.ui.Main.MainScreen
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.w3c.dom.Text

class MakerScreen : AppCompatActivity(), OnMapReadyCallback {

    var mGoogleMap: GoogleMap? = null

    private val FINE_PERMISSION_CODE = 1
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback1: LocationCallback
    private var openedDialog: Dialog? = null
    private val viewModel: MakerViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maker)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(applicationContext) //segít energiatakarékosan és hatékonyan megszerezni a helyadatokat
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
                    val icon = BitmapDescriptorFactory.fromResource(R.drawable.usericondemo)

                    currentMarker = mGoogleMap?.addMarker(
                        MarkerOptions().position(latLng).title("Szerénységem").icon(icon)
                    )!!

                    val newLatLng = LatLng(location.latitude, location.longitude)
                    mGoogleMap?.animateCamera(CameraUpdateFactory.newLatLng(newLatLng)) //a kamera ezáltal követi a felhasználót
                }
            }
        }

        viewModel.getUsersProjects()

        showLoading()

        viewModel.usersProjectsList.observe(this, Observer { result ->
            showMakerProjectsDialog()
        })

        val bottomSheet = findViewById<View>(R.id.bottomSheet)
        val arrowButton = findViewById<ImageView>(R.id.arrow)
        val recyclerView = findViewById<RecyclerView>(R.id.locationsDataList)

        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.peekHeight = 160 // látható rész


        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        arrowButton.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                arrowButton.setImageResource(android.R.drawable.arrow_up_float)
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                arrowButton.setImageResource(android.R.drawable.arrow_down_float)
            }
        }

        viewModel.currentProject.observe(this, Observer { result ->
            recyclerView.layoutManager = LinearLayoutManager(this)
            val recycleViewAdapter = result.locations
            recyclerView.adapter = RecycleViewAdapter(result,recycleViewAdapter, viewModel){ locationName, resultCallback ->
                // Itt valójában a showMakerLocationDeletionDialog függvényt hívod meg
                showMakerLocationDeletionDialog(locationName) { userWantsDelete ->
                    resultCallback(userWantsDelete)
                }
            }

            for (location in result.locations)
            {
                mGoogleMap?.addMarker(location?.markerOptions!!)
            }
        })
    }

    //Adapter a recycleView-hoz, mivel az saját konstruktort igényel, nem jó, ami a listához kell
    class RecycleViewAdapter(
        private val makerLocationPack: MakerLocationPackData,
        private val locations: MutableList<MakerLocationDescription?>?,
        private val viewModel: MakerViewModel,
        private val showMakerLocationDeletionDialog: (String, (Boolean) -> Unit) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            private const val VIEW_TYPE_HEADER = 0
            private const val VIEW_TYPE_ITEM = 1
        }

        class MakerLocationPackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val lpName: TextView = itemView.findViewById(R.id.lpName)
            val lpDescription: EditText = itemView.findViewById(R.id.lpDescription)
            val lpArea: EditText = itemView.findViewById(R.id.lpArea)
        }


        class MakerLocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val lName: EditText = itemView.findViewById(R.id.lName)
            val lDescription: EditText = itemView.findViewById(R.id.lDescription)
            val lQuestion: EditText = itemView.findViewById(R.id.lQuestion)
            val lAnswer: EditText = itemView.findViewById(R.id.lAnswer)
            val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0){
                return VIEW_TYPE_HEADER
            }else{
                return VIEW_TYPE_ITEM
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.maker_recycle_view_locationpack_list_element, parent, false)
                    MakerLocationPackViewHolder(view)
                }
                else -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.maker_recycle_view_list_element, parent, false)
                    MakerLocationViewHolder(view)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is MakerLocationPackViewHolder) {
                holder.lpName.text = makerLocationPack.name

                holder.lpDescription.tag?.let { watcher ->
                    if(watcher is TextWatcher){
                        holder.lpDescription.removeTextChangedListener(watcher)
                    }
                }
                holder.lpDescription.setText(makerLocationPack.description)

                holder.lpArea.tag?.let { watcher ->
                    if(watcher is TextWatcher){
                        holder.lpArea.removeTextChangedListener(watcher)
                    }
                }
                holder.lpArea.setText(makerLocationPack.area)


                val descLPWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        makerLocationPack.description = s.toString()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }
                holder.lpDescription.addTextChangedListener(descLPWatcher)
                holder.lpDescription.tag = descLPWatcher

                val areaLPWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        makerLocationPack.description = s.toString()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }
                holder.lpArea.addTextChangedListener(areaLPWatcher)
                holder.lpArea.tag = areaLPWatcher

                //TODO megírni a locationPack részt
            } else if (holder is MakerLocationViewHolder) {
                val location = locations?.get(position)
                holder.deleteButton.setOnClickListener{

                    showMakerLocationDeletionDialog(location?.name!!){ isDeletable ->
                        if(isDeletable){
                            val currentPosition = holder.adapterPosition
                            if (currentPosition != RecyclerView.NO_POSITION) {
                                location.name?.let { name ->
                                    viewModel.deleteLocation(name)
                                    locations?.remove(location)
                                    notifyItemRemoved(currentPosition)
                                }
                            }
                        }else{

                        }
                    }
                }

                holder.lName.tag?.let { watcher ->
                    if(watcher is TextWatcher){
                        holder.lName.removeTextChangedListener(watcher)
                    }
                }
                holder.lName.setText(location?.name)

                holder.lDescription.tag?.let { watcher ->
                    if(watcher is TextWatcher){
                        holder.lDescription.removeTextChangedListener(watcher)
                    }
                }
                holder.lDescription.setText(location?.description)

                holder.lQuestion.tag?.let { watcher ->
                    if(watcher is TextWatcher){
                        holder.lQuestion.removeTextChangedListener(watcher)
                    }
                }
                holder.lQuestion.setText(location?.question)

                holder.lAnswer.tag?.let { watcher ->
                    if(watcher is TextWatcher){
                        holder.lAnswer.removeTextChangedListener(watcher)
                    }
                }
                holder.lAnswer.setText(location?.answer)

                val nameWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        location?.name = s?.toString()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }
                holder.lName.addTextChangedListener(nameWatcher)
                // A .tag mezőben eltároljuk, hogy később le tudjuk venni
                holder.lName.tag = nameWatcher

                val descWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        location?.description = s?.toString()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }
                holder.lDescription.addTextChangedListener(descWatcher)
                holder.lDescription.tag = descWatcher

                val questionWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        location?.question = s?.toString()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }
                holder.lQuestion.addTextChangedListener(questionWatcher)
                holder.lQuestion.tag = questionWatcher

                val answerWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        location?.answer = s?.toString()
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }
                holder.lAnswer.addTextChangedListener(answerWatcher)
                holder.lAnswer.tag = answerWatcher
            }
        }

        override fun getItemCount(): Int = locations!!.size
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

        //showMakerProjectsDialog()

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
        var markerTitle: String

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.maker_location_name_dialog)
        dialog.setCancelable(false)

        val lpName = dialog.findViewById<EditText>(R.id.lpName)
        val saveButton = dialog.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        saveButton.setOnClickListener{
            if(lpName == null || lpName.toString() == "")
            {
                Toast.makeText(this, "Nem adtál meg nevet!", Toast.LENGTH_SHORT).show()
            }
            else
            {
                markerTitle = lpName.text.toString()

                val actualMarker: MarkerOptions = MarkerOptions()
                    .position(LatLng(currentLocation.latitude, currentLocation.longitude))
                    .title(markerTitle)
                    .draggable(false)

                mGoogleMap?.addMarker(actualMarker)

                viewModel.addNewLocationToCurrentProject(markerTitle, actualMarker)
                dialog.dismiss()
            }
        }

        cancelButton.setOnClickListener{
            dialog.dismiss()
        }

        dialog.show()
    }


    //callback (onResult), hogy ne adjon vissza egyből értéket
    fun showMakerLocationDeletionDialog(locationDeletable: String, onResult: (Boolean) -> Unit){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.maker_location_delete_dialog)
        dialog.setCancelable(false)
        val saveButton = dialog.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val lName = dialog.findViewById<TextView>(R.id.lName)

        lName.text = locationDeletable
        saveButton.setOnClickListener{
            onResult(true)
            dialog.dismiss()
        }
        cancelButton.setOnClickListener{
            onResult(false)
            dialog.dismiss()
        }
        dialog.show()
    }

    fun showMakerProjectsDialog(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.maker_projects_dialog)
        dialog.setCancelable(false)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val listView = dialog.findViewById<ListView>(R.id.projectsListView)
        val backToMainButton = dialog.findViewById<ImageButton>(R.id.backToMainButton)
        val addProjectButton = dialog.findViewById<ImageButton>(R.id.addProjectButton)

        if(viewModel.usersProjectsList.value != null)
        {
            val adapter = object: ArrayAdapter<MakerLocationPackData>(this, R.layout.maker_projects_element, viewModel.usersProjectsList.value!!){
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.maker_projects_element, parent, false)
                    val lpName: TextView = view.findViewById(R.id.lpName)
                    val lpLocationCount: TextView = view.findViewById(R.id.lpLocationCount)
                    val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)

                    val locationPack = getItem(position)

                    lpName.text = locationPack?.name
                    lpLocationCount.text = "Helyszínek száma: ${locationPack?.locations?.size.toString()}"

                    view.setOnClickListener{
                        val headerTitle = findViewById<TextView>(R.id.headerTitle)
                        headerTitle.text = lpName.text
                        showLoading()
                        viewModel.setCurrentProject(lpName.text.toString())
                        dialog.dismiss()
                    }

                    deleteButton.setOnClickListener{
                        showMakerProjectDeletionDialog(locationPack?.name!!){  isDeletable ->
                            if(isDeletable){
                                viewModel.deleteProject(locationPack.name, context)
                                remove(locationPack)
                            }else{

                            }
                        }
                    }

                    return view
                }
            }
            listView.adapter = adapter

        }

        backToMainButton.setOnClickListener{
            backToMainScreen()
        }

        addProjectButton.setOnClickListener{
            showMakerLevelNameDialog()
        }

        openedDialog = dialog
        dialog.show()
    }

    override fun onDestroy() {
        viewModel.saveProjectChanges(this)
        viewModel.isSaveFinished.observe(this, Observer { result ->
            if(result == true){
                super.onDestroy()
            }
        })
    }

    fun showMakerProjectDeletionDialog(projectDeletable: String, onResult: (Boolean) -> Unit){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.maker_project_delete_dialog)
        dialog.setCancelable(false)
        val saveButton = dialog.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        val projectName = dialog.findViewById<TextView>(R.id.projectName)

        projectName.text = projectDeletable
        saveButton.setOnClickListener{
            onResult(true)
            dialog.dismiss()
        }
        cancelButton.setOnClickListener{
            onResult(false)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showMakerLevelNameDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.maker_level_name_dialog)
        dialog.setCancelable(false)
        val lpName = dialog.findViewById<EditText>(R.id.lpName)
        val saveButton = dialog.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        saveButton.setOnClickListener{
            if(lpName.text.toString() != "" && lpName.text != null){
                viewModel.addUserProject(lpName.text.toString())
                showLoading()
                viewModel.setCurrentProject(lpName.text.toString())
                dialog.dismiss()
                openedDialog?.dismiss()
            }
            else{
                Toast.makeText(this, "Nem adtál meg nevet!", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener{
            dialog.dismiss()
        }

        dialog.show()
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


        viewModel.usersProjectsList.observe(this, Observer {
            dialog.window?.apply {
                // Dim hátteret engedélyez
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                // 0.0f -> nincs elsötétítés, 1.0f -> teljesen fekete
                setDimAmount(0f)
            }
            progressBar.visibility = View.GONE
            dialog.dismiss()
        })
        viewModel.currentProject.observe(this, Observer {
            dialog.window?.apply {
                // Dim hátteret engedélyez
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                // 0.0f -> nincs elsötétítés, 1.0f -> teljesen fekete
                setDimAmount(0f)
            }
            progressBar.visibility = View.GONE
            dialog.dismiss()
        })
        viewModel.isSaveFinished.observe(this, Observer { result ->
            if(result == true){
                dialog.window?.apply {
                    // Dim hátteret engedélyez
                    addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    // 0.0f -> nincs elsötétítés, 1.0f -> teljesen fekete
                    setDimAmount(0f)
                }
                progressBar.visibility = View.GONE
                dialog.dismiss()
            }
        })
    }

    fun backToMainScreen() {
        startActivity(Intent(this@MakerScreen, MainScreen::class.java))
    }

    fun saveProjectData(view: View) {
        showLoading()
        viewModel.saveProjectChanges(this)
    }

    fun backToProjects(view: View) {
        viewModel.saveProjectChanges(this)

        showLoading()
        viewModel.isSaveFinished.observe(this, Observer { result ->
            if(result == true){
                showMakerProjectsDialog()
            }
        })
    }
}