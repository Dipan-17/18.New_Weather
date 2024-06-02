package com.example.weather_new

import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weather_new.databinding.ActivityMainBinding
import java.util.logging.Handler

class MainActivity : AppCompatActivity() {
    private var binding:ActivityMainBinding?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if(!isLocationEnabled()) {
            Toast.makeText(this, "Location is off", Toast.LENGTH_SHORT).show()

            //wait 5 seconds before starting the location intent
            val handler = android.os.Handler()
            handler.postDelayed(Runnable {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }, 5000)
        }else{
            Toast.makeText(this, "Location is already on", Toast.LENGTH_SHORT).show()
        }


    }

    private fun isLocationEnabled():Boolean{
        val locationManager=getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}