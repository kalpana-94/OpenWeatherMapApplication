package com.example.openweathermapapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.openweathermapapplication.databinding.ActivitySearchBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*


class SearchActivity : AppCompatActivity() {

    var context: Context? = null
    private lateinit var binding: ActivitySearchBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    val appid:String = "7b7bd76041b026df0bc79eefd11eabc5"
    var appPreferences: AppPreferences? = null
    var LATITUDE: Double = 0.0
    var LONGITUDE: Double = 0.0
    var output: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    fun init(){
        context = this
        appPreferences = AppPreferences(context)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (appPreferences?.cityPreference != null || appPreferences?.cityPreference != "") {
            binding.edtSearch?.setText(appPreferences?.cityPreference)
            setLatLong()
            getWeatherDetails()
        }

        //Method calling
        getCurrentLocation()
        setListener()
    }

    private fun setListener()
    {
        binding.edtSearch?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                appPreferences?.cityPreference = binding.edtSearch?.text.toString()
                setLatLong()
                getWeatherDetails()
            }
        })
    }

    /**
     * LOCATION PERMISSION
     */
    private fun getCurrentLocation()
    {
        if (checkPermission())
        {
            if (isLocationEnabled())
            {
                //Get latitude & latitude
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this){task ->
                    val location: Location? = task?.result
                    if (location == null)
                    { }
                    else
                    {
                        LATITUDE = location.latitude
                        LONGITUDE = location.longitude
                        getWeatherDetails()
                    }
                }
            }else{
                //Setting open here
                //Toast.makeText(context, "Open settings", Toast.LENGTH_SHORT).show()
            }
        }
        else
        {
            requestPermission()
        }
    }

    fun checkPermission(): Boolean
    {
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermission()
    {
        ActivityCompat.requestPermissions((context as Activity?)!!,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION), 1)
    }

    private fun isLocationEnabled(): Boolean
    {
        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun setLatLong()
    {
        val coordinates = getLatLngFromCityName(applicationContext, binding.edtSearch?.text.toString())
        if (coordinates != null)
        {
            LATITUDE = coordinates.first
            LONGITUDE = coordinates.second
        }
    }

    fun getLatLngFromCityName(context: Context, cityName: String): Pair<Double, Double>?
    {
        val geocoder = Geocoder(context)
        try {
            val addresses = geocoder.getFromLocationName(cityName, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val latitude = addresses[0].latitude
                val longitude = addresses[0].longitude
                return Pair(latitude, longitude)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * FUNCTIONS
     */
    fun getCapSentence(value: String?): String?
    {
        val weather = if (value != null) {
            value.substring(0, 1).uppercase(Locale.getDefault()) + value.substring(1)
        } else {
            ""
        }
        return "$weather"
    }

    /**
     * API CALLING
     */
    fun getWeatherDetails()
    {
        val cityName = binding.edtSearch?.text.toString()
        var url: String
        if (LATITUDE != null && LONGITUDE != null)
        {
            url = "https://api.openweathermap.org/data/2.5/weather?lat=$LATITUDE&lon=$LONGITUDE&appid=$appid"
        }
        else
        {
            url = "https://api.openweathermap.org/data/2.5/weather?q=$cityName&appid=$appid"
        }
        val queue: RequestQueue = Volley.newRequestQueue(context)
        val jsonObjectRequest =
            JsonObjectRequest(
                Request.Method.GET, url, null,
                { response ->
                    try {
                        //FETCH DATA OF OBJECT MAIN
                        val jsonObjectMain = response.getJSONObject("main")
                        var temp = jsonObjectMain.getDouble("temp") - 273.15
                        temp = String.format("%.3f", temp).toDouble()
                        val tempMin = jsonObjectMain.getString("temp_min")
                        val tempMax = jsonObjectMain.getString("temp_max")
                        var feelsLike = jsonObjectMain.getDouble("feels_like") - 273.15
                        feelsLike = String.format("%.3f", feelsLike).toDouble()
                        val pressure = jsonObjectMain.getInt("pressure")
                        val humidity = jsonObjectMain.getInt("humidity")

                        //FETCH DATA OF WEATHER ARRAY
                        val jsonArrayWeather = response.getJSONArray("weather")
                        val jsonWeatherObject:JSONObject = jsonArrayWeather.get(0) as JSONObject
                        val weather = jsonWeatherObject.getString("main")
                        val weatherIcon = jsonWeatherObject.getString("icon")
                        val description = jsonWeatherObject.getString("description")

                        //FETCH WIND DATA
                        val jsonObjectWind = response.getJSONObject("wind")
                        val wind = jsonObjectWind.getString("speed")

                        //CLOUDS DATA
                        val jsonObjectClouds = response.getJSONObject("clouds")
                        val clouds = jsonObjectClouds.getString("all")

                        //SYS DATA
                        val jsonObjectSys = response.getJSONObject("sys")
                        val countryName = jsonObjectSys.getString("country")
                        val cityName = response.getString("name")

                        binding.llWeatherInfo.visibility = View.VISIBLE

                        binding.txtTemp?.text = "$temp°C"
                        binding.txtCity?.text = "$cityName,$countryName"
                        binding.txtDescription?.text = "${getCapSentence(description)}"
                        binding.txtHumidity?.text = "$humidity%"
                        binding.txtWind?.text = "$wind m/s"
                        binding.txtCloudiness?.text = "$clouds%"
                        binding.txtPressure?.text = "$pressure hPa"

                        var imageUrl = "https://openweathermap.org/img/wn/$weatherIcon.png"
                        Glide.with(context!!).load(imageUrl).into(binding.imgWeather!!)

                    } catch (e: JSONException) {
                        // if we do not extract data from json object properly.
                        // below line of code is use to handle json exception
                        e.printStackTrace()
                    }
                }, object : Response.ErrorListener {
                    // this is the error listener method which
                    // we will call if we get any error from API.
                    override fun onErrorResponse(error: VolleyError?) {
                        // below line is use to display a toast message along with our error.
                        //Toast.makeText(context, "Fail to get data..", Toast.LENGTH_SHORT).show()
                        binding.llWeatherInfo.visibility = View.GONE
                    }
                })
        queue.add(jsonObjectRequest)
    }

    /**
     * CALLBACK METHODS
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Handle the result of the permission request
        when (requestCode) {
            // Add your permission request code here
            1 -> {
                // Check if the permission is granted
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    getCurrentLocation()
                }
                else {
                }
            }
            // Add more cases for other permission request codes if needed
        }
    }
}