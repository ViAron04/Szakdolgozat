import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.HungaryGo.R
import com.example.HungaryGo.databinding.ActivityMainScreenBinding
import com.example.HungaryGo.databinding.InfowindowBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.request.RequestOptions
import com.example.HungaryGo.LocationPackData
import com.example.HungaryGo.MainScreen
import java.text.Normalizer


class CustomInfoWindowForGoogleMap(context: Context, private val locationPacksList: MutableList<LocationPackData>) : GoogleMap.InfoWindowAdapter {

    private val loadedImages = mutableMapOf<String, Boolean>()
    val mContext = context
    var mWindow = (context as Activity).layoutInflater.inflate(R.layout.infowindow, null)

    //eltárolja a már eltöltött képeket


    private fun rendowWindowText(marker: Marker, view: View){
        Log.d("Szia", "Lefutottam megint");
        val locationPackImg: ImageView = view.findViewById(R.id.location_img)
        val locationPack = view.findViewById<TextView>(R.id.location_pack)
        val location = view.findViewById<TextView>(R.id.location)
        val startButton = view.findViewById<Button>(R.id.startButton)
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


        locationPack.text = locationPackName
        location.text = marker.title
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