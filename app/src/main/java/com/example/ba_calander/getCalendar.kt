package com.example.ba_calander

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

fun getPersonalCalendar(user: String, hash: String, start: Long, end: Long, viewModelScope: CoroutineScope, events: MutableState<List<Event>>) {
    viewModelScope.launch(Dispatchers.IO) {
        val maxAttempts = 3
        var attempt = 0

        while (attempt < maxAttempts) {
            try {
                val url = URL("https://selfservice.campus-dual.de/room/json?userid=${user}&hash=${hash}&start=${start}&end=${end}")
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

                    val mapper = jacksonObjectMapper()
                    val jsonObject = mapper.readTree(response)

                    val newEvents = events.value.toMutableList()
                    jsonObject.forEach {
                        val room = it.get("room").asText()
                        val start = it.get("start").asText()
                        val end = it.get("end").asText()
                        val title = it.get("title").asText()
                        val instructor = it.get("instructor").asText()
                        val allDay = it.get("allDay").asBoolean()

                        val event = Event(room, start, end, title, instructor, allDay)
                        newEvents.add(event)
                    }
                    events.value = newEvents

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