import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.HungaryGo.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.firebase.storage.FirebaseStorage
import com.example.HungaryGo.LocationPackData
import com.example.HungaryGo.ui.Main.MainScreen
import java.text.Normalizer


class CustomInfoWindowForGoogleMap(context: Context, private val locationPacksList: MutableList<LocationPackData>, val currentLocation: String?) : GoogleMap.InfoWindowAdapter {

    //eltárolja a már eltöltött képeket
    private val loadedImages = mutableMapOf<String, Boolean>()
    val mContext = context
    var infowindow = R.layout.infowindow

    init {
        if (currentLocation != null) {
            infowindow = R.layout.infowindowlocation
        }
    }
    var mWindow = (context as Activity).layoutInflater.inflate(infowindow, null)

    private fun rendowWindowText(marker: Marker, view: View){

        var locationPackName: String? = null
        for (locationPack in locationPacksList)
        {
            if (locationPack.locations.containsKey(marker.title))
                locationPackName=locationPack.name
        }


        //kép megjelenítése
        fun removeAccents(input: String?): String {
            val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            return normalized.replace(Regex("\\p{Mn}"), "")
        }

        val imgName = removeAccents(locationPackName?.lowercase()?.replace(' ','_'))





        if(infowindow == R.layout.infowindow)
        {
            val locationPackImg: ImageView = view.findViewById(R.id.location_img)
            val locationPack = view.findViewById<TextView>(R.id.location_pack)
            val location = view.findViewById<TextView>(R.id.location)
            locationPack.text = locationPackName
            location.text =marker.title

            if(MainScreen.BitmapStore.loadedBitmaps.containsKey(locationPackName))
            {
                locationPackImg.setImageBitmap(MainScreen.BitmapStore.loadedBitmaps[locationPackName])
            }else {

                val storage = FirebaseStorage.getInstance()
                val storageImgReference =
                    storage.getReferenceFromUrl("gs://kotlin-gyak-firebase.appspot.com/location_packs_images/$imgName.jpg")

                val maxDownloadableSize: Long = 500 * 500
                storageImgReference.getBytes(maxDownloadableSize).addOnSuccessListener { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    MainScreen.BitmapStore.loadedBitmaps[locationPackName] = bitmap

                    //locationPackImg.setImageBitmap(bitmap)
                    marker.showInfoWindow()
                }
                    .addOnFailureListener { exception ->
                        Log.d("Firebase", "Kép letöltése sikertelen", exception)
                    }
            }
        }
        else
        {
            val locationPack = view.findViewById<TextView>(R.id.location_pack)
            val location = view.findViewById<TextView>(R.id.location)
            locationPack.text = "A következő helyszínre értél: "
            location.text =marker.title
        }

    }

    override fun getInfoContents(marker: Marker): View {
        rendowWindowText(marker, mWindow)
        return mWindow
    }

    override fun getInfoWindow(marker: Marker): View? {
        rendowWindowText(marker, mWindow)
        return mWindow
    }
}