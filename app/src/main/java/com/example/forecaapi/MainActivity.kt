package com.example.forecaapi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telecom.Call
import android.view.View
import android.widget.Toast
import com.example.forecaapi.model.ForecaAuthRequest
import com.example.forecaapi.model.ForecaAuthResponse
import com.example.forecaapi.model.LocationsResponse
import retrofit2.http.*
import com.example.forecaapi.model.ForecastResponse as ForecastResponse

interface ForecaApi {

    @POST("/authorize/token?expire_hours=-1")
    fun authenticate(@Body request: ForecaAuthRequest): Call<ForecaAuthResponse>

    @GET("/api/v1/location/search/{query}")
    fun getLocations(
        @Header("Authorization") token: String,
        @Path("query") query: String
    ): Call<LocationsResponse>

    @GET("/api/v1/current/{location}")
    fun getForecast(
        @Header("Authorization") token: String,
        @Path("location") locationId: Int
    ): Call<ForecastResponse>
}


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        private fun showMessage(text: String, additionalMessage: String) {
            if (text.isNotEmpty()) {
                val placeholderMessage = null
                placeholderMessage.visibility = View.VISIBLE
                locations.clear()
                adapter.notifyDataSetChanged()
                placeholderMessage.text = text
                if (additionalMessage.isNotEmpty()) {
                    Toast.makeText(applicationContext, additionalMessage, Toast.LENGTH_LONG)
                        .show()
                }
            } else {
                placeholderMessage.visibility = View.GONE
            }
        }

        searchButton.setOnClickListener {
            if (queryInput.text.isNotEmpty()) {
                if (token.isEmpty()) {
                    authenticate()
                } else {
                    search()
                }
            }
        }

        private fun authenticate() {
            forecaService.authenticate(ForecaAuthRequest("USER", "PASSWORD"))
                .enqueue(object : Callback<ForecaAuthResponse> {
                    override fun onResponse(
                        call: Call<ForecaAuthResponse>,
                        response: Response<ForecaAuthResponse>
                    ) {
                        if (response.code() == 200) {
                            token = response.body()?.token.toString()
                            search()
                        } else {
                            showMessage(
                                getString(R.string.something_went_wrong),
                                response.code().toString()
                            )
                        }
                    }

                    override fun onFailure(call: Call<ForecaAuthResponse>, t: Throwable) {
                        showMessage(getString(R.string.something_went_wrong), t.message.toString())
                    }

                })
        }

        private fun search() {
            forecaService.getLocations("Bearer $token", queryInput.text.toString())
                .enqueue(object : Callback<LocationsResponse> {
                    override fun onResponse(
                        call: Call<LocationsResponse>,
                        response: Response<LocationsResponse>
                    ) {
                        when (response.code()) {
                            200 -> {
                                if (response.body()?.locations?.isNotEmpty() == true) {
                                    locations.clear()
                                    locations.addAll(response.body()?.locations!!)
                                    adapter.notifyDataSetChanged()
                                    showMessage("", "")
                                } else {
                                    showMessage(getString(R.string.nothing_found), "")
                                }

                            }
                            401 -> authenticate()
                            else -> showMessage(
                                getString(R.string.something_went_wrong),
                                response.code().toString()
                            )
                        }

                    }

                    override fun onFailure(call: Call<LocationsResponse>, t: Throwable) {
                        showMessage(getString(R.string.something_went_wrong), t.message.toString())
                    }

                })
        }

        private fun showWeather(location: ForecastLocation) {
            forecaService.getForecast("Bearer $token", location.id)
                .enqueue(object : Callback<ForecastResponse> {
                    override fun onResponse(
                        call: Call<ForecastResponse>,
                        response: Response<ForecastResponse>
                    ) {
                        if (response.body()?.current != null) {
                            val message =
                                "${location.name} t: ${response.body()?.current?.temperature}\n(Ощущается как ${response.body()?.current?.feelsLikeTemp})"
                            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                        Toast.makeText(applicationContext, t.message, Toast.LENGTH_LONG).show()
                    }

                })
        }

    }


}