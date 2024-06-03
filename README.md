1. Permissions

2. Using Handler to wait for actions

3. Using Dexter

4. Getting current location

5. Checking connectivity

6. Using retrofit:
	1. Create a builder in main
	2. Create interface (pass the required info)
	3. In Main, after builder, create the interface (Order should be same as in url)
	4. Use the interface and pass required parameter
	5.Enqueue the service

7. Populate UI from JSON data

8. Setting up menu:
	Create a resource file (menu_main)
	In Activity, override onCreateOptionsMenu
	Override onOptionsItemSelected()

9.Storing items in SharedPreferences:
	Shared preferences can be used to store simple information like strings, int etc
	All our classes are serilizabile -> Available in String

	How to do:
	//global
	//shared preferences
    	private lateinit var mSharedPreferences: SharedPreferences

	//oncreate
        mSharedPreferences=getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE)

	//storing
	 val weatherList:WeatherResponse?=response.body() //json format
	 val weatherResponseJsonString= Gson().toJson(weatherList) //convert to string to store
         val editor=mSharedPreferences.edit()
                               editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJsonString)
                        editor.apply()


	//fetching data
	val weatherResponse=mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA,"") //string
        
	if(!weatherResponse.isNullOrEmpty()){
            val weatherListJSON=Gson().fromJson(weatherResponse,WeatherResponse::class.java) //convert string to JSON
	}
