package com.example.weather_new.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.location.LocationManager

import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.weather_new.R
import com.example.weather_new.constants.Constants
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
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    //for viewbinding
    private var binding:ActivityMainBinding?=null

    //to get latitude and longitude
    private  lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog: Dialog?=null

    //shared preferences
    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)


        //shared preferences
        mSharedPreferences=getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE)
        setUI()//populate old data from shared preferences


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
                        //Toast.makeText(this@MainActivity,"Permissions Granted", Toast.LENGTH_SHORT).show()
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




    private fun getLocationWeatherDetails(latitude:Double,longitude:Double){

        //Toast.makeText(this@MainActivity,"Start API CALL", Toast.LENGTH_SHORT).show()

        //here we are doing stuff in the bg -> Start progress Dialog


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

            //print to console
            println("URL: " + listCall.request().url())


            //start of background process
            showCustomDialog()

            listCall.enqueue(object:Callback<WeatherResponse>{

                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if(response.isSuccessful){
                        dismissDialog()

                        val weatherList:WeatherResponse?=response.body()
                        val weatherResponseJsonString= Gson().toJson(weatherList)
                        Log.e("Response_Result", weatherResponseJsonString)

                        //store output in sharedPreferenes
                        val editor=mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJsonString)
                        editor.apply()
                        //get data from sharedPreferences directly
                        setUI()
                    }else{
                        val rc=response.code()
                        when(rc){
                            400->{
                                Log.e("EError","Bad Connection")
                            }
                            401->{
                                Log.e("EError","Unauthorized")
                            }
                            402->{
                                Log.e("EEError","Payment")
                            }
                            403->{
                                Log.e("EError","Forbidden")
                            }
                            404->{
                                Log.e("EError","Not Found")
                            }
                            405->{
                                Log.e("EError","Not Allowed")
                            }
                            406->{
                                Log.e("EError","Not Acceptable")
                            }
                            408->{
                                Log.e("EError","Timeout")
                            }
                            409->{
                                Log.e("EError","Conflict")
                            }

                            else->{
                                Log.e("Error","Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    dismissDialog()
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
        mLocationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        //Toast.makeText(this@MainActivity," Location Requested", Toast.LENGTH_SHORT).show()

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.getMainLooper()
        )


    }

    private val mLocationCallback=object:LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {

            //Toast.makeText(this@MainActivity,"CAllback started", Toast.LENGTH_SHORT).show()

            val mLastLocation=locationResult.lastLocation
            var latitude=mLastLocation!!.latitude
            var longitude=mLastLocation!!.longitude

            getLocationWeatherDetails(latitude,longitude)

            //Toast.makeText(this@MainActivity,"Latitude:$latitude,Longitude:$longitude",Toast.LENGTH_SHORT).show()
            Log.e("current","$latitude,$longitude")


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

    private fun showCustomDialog(){
        mProgressDialog= Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }
    private fun dismissDialog(){
        if(mProgressDialog!=null){
            mProgressDialog!!.dismiss()
        }
    }

    override fun onDestroy() {
        binding=null
        mProgressDialog=null
        super.onDestroy()

    }

    private fun setUI(){
        val weatherResponse=mSharedPreferences.getString(
            Constants.WEATHER_RESPONSE_DATA,
            ""
        )

        if(!weatherResponse.isNullOrEmpty()){
            val weatherListJSON=Gson().fromJson(weatherResponse,WeatherResponse::class.java)

            for(i in weatherListJSON.weather.indices){
                Log.e("Weather",""+weatherListJSON.weather[i].description)

                binding?.tvMain?.text=weatherListJSON.weather[i].main
                binding?.tvMainDescription?.text=weatherListJSON.weather[i].description

                binding?.tvTemp?.text=weatherListJSON.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())

                binding?.tvSunriseTime?.text=unixTime(weatherListJSON.sys.sunrise)
                binding?.tvSunsetTime?.text=unixTime(weatherListJSON.sys.sunset)

                binding?.tvHumidity?.text=weatherListJSON.main.humidity.toString() + " per cent"
                binding?.tvMin?.text=weatherListJSON.main.temp_min.toString() + " min"
                binding?.tvMax?.text=weatherListJSON.main.temp_max.toString() + " max"

                binding?.tvSpeed?.text=weatherListJSON.wind.speed.toString()
                binding?.tvName?.text=weatherListJSON.name

                binding?.tvCountry?.text=weatherListJSON.sys.country


                when (weatherListJSON.weather[i].icon) {

                    "01d" -> binding?.ivMain?.setImageResource(R.drawable.sunny)
                    "02d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "03d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "04d" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "04n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "10d" -> binding?.ivMain?.setImageResource(R.drawable.rain)
                    "11d" -> binding?.ivMain?.setImageResource(R.drawable.storm)
                    "13d" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)
                    "01n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "02n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "03n" -> binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "10n" ->binding?.ivMain?.setImageResource(R.drawable.cloud)
                    "11n" -> binding?.ivMain?.setImageResource(R.drawable.rain)
                    "13n" -> binding?.ivMain?.setImageResource(R.drawable.snowflake)
                }

            }
        }


    }

    private fun getUnit(value:String):String?{
        var value="°C"
        if("US"==value||"LR"==value||"MM"==value){
            value="°F"
        }
        return value
    }

    private fun unixTime(timex:Long):String?{
        val date= Date(timex*1000L)
        val sdf=SimpleDateFormat("HH:mm", Locale.UK)
        sdf.timeZone=android.icu.util.TimeZone.getDefault()
        return sdf.format(date)
    }

    //menu button in action bar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }
    //what happen selected
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh->{
                Toast.makeText(this,"Refreshing",Toast.LENGTH_SHORT).show()
                requestLocationData()
                true
            }

            else -> return super.onOptionsItemSelected(item)
        }

    }
}