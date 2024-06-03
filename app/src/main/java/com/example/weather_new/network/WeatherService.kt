package com.example.weather_new.network

import com.weatherapp.models.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * An Interface which defines the HTTP operations Functions.
 */

//take care of the order of the parameters
interface WeatherService {

    @GET("2.5/weather")//the part after the base url and before the ?
    fun getWeather(//after the ? mark in API url
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") appid: String?,
        //@Query("units") units: String?
    ): Call<WeatherResponse>//return a weather response object as a json
}