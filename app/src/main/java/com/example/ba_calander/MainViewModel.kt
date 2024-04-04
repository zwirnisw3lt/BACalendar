package com.example.ba_calander

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

data class Event(
    val room: String,
    val start: String,
    val end: String,
    val title: String,
    val instructor: String,
    val allDay: Boolean
)

class MainViewModel : ViewModel() {
    // Define your data and functions here
    fun showCalendar(context: Context, preferences: SharedPreferences, checked: Boolean, text1: String, text2: String) {
        viewModelScope.launch(Dispatchers.IO) {

            Log.d("identifier", "Text1: $text1")
            Log.d("identifier", "Text2: $text2")

            if (checked) {
                with(preferences.edit()) {
                    putString("text1", text1)
                    putString("text2", text2)
                    apply()
                }
            } else {
            }
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date = format.parse("2022-10-01 00:00:01") // Replace with your date string
            val timestamp1 = date.time / 1000
            val date2 = format.parse("2025-10-01 00:00:01") // Replace with your date string
            val timestamp2 = date2.time / 1000

            getPersonalCalendar(text1, text2, timestamp1, timestamp2)
        }
    }

    fun getPersonalCalendar(user: String, hash: String, start: Long, end: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val maxAttempts = 3
            var attempt = 0

            while (attempt < maxAttempts) {
                try {
                    val url = URL("https://selfservice.campus-dual.de/room/json?userid=${user}&hash=${hash}&start=${start}&end=${end}&_=1711960057052")
                    println(url)
                    // Create a TrustManager that trusts the server's certificate
                    val trustManager = object : X509TrustManager {
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    }

                    // Create an SSLSocketFactory that uses our TrustManager
                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(null, arrayOf(trustManager), null)
                    val sslSocketFactory = sslContext.socketFactory

                    // Create an HttpsURLConnection using our SSLSocketFactory
                    val connection = url.openConnection() as HttpsURLConnection
                    connection.sslSocketFactory = sslSocketFactory

                    val responseCode = connection.responseCode
                    val responseMessage = connection.responseMessage

                    println("Response Code: $responseCode")
                    println("Response Message: $responseMessage")

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }

                        println("Response: $response")

                        val mapper = jacksonObjectMapper()
                        val jsonObject = mapper.readTree(response)
                        val events = mutableListOf<Event>()

                        jsonObject.forEach {
                            val room = it.get("room").asText()
                            val start = it.get("start").asText()
                            val end = it.get("end").asText()
                            val title = it.get("title").asText()
                            val instructor = it.get("instructor").asText()
                            val allDay = it.get("allDay").asBoolean()

                            val event = Event(room, start, end, title, instructor, allDay)
                            events.add(event)
                        }

                        break

                    } else {
                        throw IOException("Unexpected response code: $responseCode")
                    }

                    connection.disconnect()
                } catch (e: SocketTimeoutException) {
                    Log.e("identifier", "SocketTimeoutException: ${e.message}")
                    attempt++
                    if (attempt == maxAttempts) {
                        Log.e("identifier", "Failed to connect after $maxAttempts attempts")
                    }
                } catch (e: IOException) {
                    Log.e("identifier", "IOException: ${e.message}")
                    break
                }
            }
        }
    }
}