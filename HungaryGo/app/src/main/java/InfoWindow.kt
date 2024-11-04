import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.HungaryGo.MainActivity
import com.example.HungaryGo.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowForGoogleMap(context: Context) : GoogleMap.InfoWindowAdapter {

    val mContext = context
    var mWindow = (context as Activity).layoutInflater.inflate(R.layout.infowindow, null)

    private fun rendowWindowText(marker: Marker, view: View){

        val location_pack = view.findViewById<TextView>(R.id.location_pack)
        val location = view.findViewById<TextView>(R.id.location)
        val startButton = view.findViewById<Button>(R.id.startButton)


        val location_packName =  "Pálya neve"

        location_pack.text = location_packName
        location.text = marker.title

        /*
        startButton.setOnClickListener{
            if(mContext is MainActivity){
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