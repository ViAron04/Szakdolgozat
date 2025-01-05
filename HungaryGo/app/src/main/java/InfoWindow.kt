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


class CustomInfoWindowForGoogleMap(context: Context, private val locationPacksList: MutableMap<String?, MutableList<String?>>) : GoogleMap.InfoWindowAdapter {

    private val loadedImages = mutableMapOf<String, Boolean>()
    val mContext = context
    var mWindow = (context as Activity).layoutInflater.inflate(R.layout.infowindow, null)

    private fun rendowWindowText(marker: Marker, view: View){

        Log.d("Szia", "Lefutottam megint");
        val locationPackImg: ImageView = view.findViewById(R.id.location_img)
        val locationPack = view.findViewById<TextView>(R.id.location_pack)
        val location = view.findViewById<TextView>(R.id.location)
        val startButton = view.findViewById<Button>(R.id.startButton)

        var locationPackName: String? = null

        for ((locationPack, loacation) in locationPacksList)
        {
            if (loacation.contains(marker.title))
                locationPackName=locationPack
        }



        locationPack.text = locationPackName
        location.text = marker.title

        //kép megjelenítése
        val storage = FirebaseStorage.getInstance()
        val storageImgReference = storage.getReferenceFromUrl("gs://kotlin-gyak-firebase.appspot.com/location_packs_images/pannon_egyetem_epuletei.jpg")

        val maxDownloadableSize: Long = 500 * 500
        storageImgReference.getBytes(maxDownloadableSize).addOnSuccessListener {  bytes->
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                locationPackImg.setImageBitmap(bitmap)

        }
            .addOnFailureListener { exception ->
                Log.d("Firebase", "Kép letöltése sikertelen", exception)
            }

        /*
        startButton.setOnClickListener{
            if(mContext is MainScreen){
                mContext.startButtonClick(marker)
            }
        }*/
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