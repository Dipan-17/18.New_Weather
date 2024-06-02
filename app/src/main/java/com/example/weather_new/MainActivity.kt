package com.example.weather_new

import android.annotation.SuppressLint
import android.content.Intent
import android.location.LocationManager

import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.weather_new.databinding.ActivityMainBinding
import com.example.weather_new.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices

import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.gson.Gson


import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.weatherapp.models.WeatherResponse

import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory


import java.util.logging.Handler

class MainActivity : AppCompatActivity() {
    //for viewbinding
    private var binding:ActivityMainBinding?=null

    //to get latitude and longitude
    private  lateinit var mFusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)



        //check location services
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Location is off", Toast.LENGTH_SHORT).show()

            //wait 5 seconds before starting the location intent
            val handler = android.os.Handler()
            handler.postDelayed(Runnable {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }, 5000)
        }

        else {
            Dexter.withActivity(this@MainActivity).withPermissions(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object : MultiplePermissionsListener {

                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report!!.areAllPermissionsGranted()) {

                        requestLocationData()
                    }

                    if (report.isAnyPermissionPermanentlyDenied) {
                        Toast.makeText(
                            this@MainActivity,
                            "Permissions are denied",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ){
                    showRationalDialogForPermissions()
                }

            }).onSameThread().check()


        }
    }




    private fun getLocationWeatherDetails(longitude:Double,latitude:Double){
        if(Constants.isNetworkAvailable(this@MainActivity)){
            val retrofit:Retrofit= Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()) //this converts to proper format
                .build()

            val service:WeatherService=retrofit.create(WeatherService::class.java)

            val listCall:Call<WeatherResponse> = service.getWeather(
                latitude,
                longitude,
                Constants.APP_ID,
                Constants.METRIC_UNIT
            )

            listCall.enqueue(object:Callback<WeatherResponse>{

                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if(response.isSuccessful){
                        val weatherList:WeatherResponse?=response.body()
                        val weatherResponseJsonString= Gson().toJson(weatherList)
                        Log.e("Response_Result", weatherResponseJsonString)
                    }else{
                        val rc=response.code()
                        when(rc){
                            400->{
                                Log.e("Error 400","Bad Connection")
                            }
                            404->{
                                Log.e("Error 404","Not Found")
                            }
                            else->{
                                Log.e("Error","Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Errorr",t.message.toString())
                }

            })

        }else{
            Toast.makeText(this@MainActivity,"You are not connected to the internet",Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.getMainLooper()
        )
    }

    private val mLocationCallback=object:LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation=locationResult.lastLocation
            val latitude=mLastLocation!!.latitude
            val longitude=mLastLocation!!.longitude

            Toast.makeText(this@MainActivity,"Latitude:$latitude,Longitude:$longitude",Toast.LENGTH_SHORT).show()
            Log.e("current","$latitude,$longitude")

            getLocationWeatherDetails(latitude,longitude)
        }
    }


    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this)
            .setMessage("You have not enabled location services")
            .setPositiveButton("GO TO SETTINGS"){
                dialog,_ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = android.net.Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("CANCEL"){
                dialog,_->
                dialog.dismiss()
            }
                .show()
    }

    private fun isLocationEnabled():Boolean{
        val locationManager=getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}