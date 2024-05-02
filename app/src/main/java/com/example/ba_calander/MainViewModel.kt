package com.example.ba_calander

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager


// Data class to store the events 
data class Event(
    val room: String,
    val start: String,
    val end: String,
    val title: String,
    val instructor: String,
    val allDay: Boolean
)

class MainViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<Event>>(listOf())
    val events: StateFlow<List<Event>> = _events

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    val loadingRefresh = MutableStateFlow(false)

    // Function to fetch the personal calendar from the Campus Dual API using the user and hash
    fun getPersonalCalendar(user: String, hash: String) {
        val maxAttempts = 3
        var attempt = 0

        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = format.parse("2000-10-01 00:00:01")
        val start = date.time / 1000
        val date2 = format.parse("2099-10-01 00:00:01")
        val end = date2.time / 1000

        while (attempt < maxAttempts) {
            try {
                val url =
                    URL("https://selfservice.campus-dual.de/room/json?userid=${user}&hash=${hash}&start=${start}&end=${end}")
                println(url)
                // Create a TrustManager that trusts the server's certificate
                val trustManager = object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

                    override fun checkClientTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {
                    }

                    override fun checkServerTrusted(
                        chain: Array<out X509Certificate>?,
                        authType: String?
                    ) {
                    }
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

                    val fetchedEvents = mutableListOf<Event>()
                    jsonObject.forEach {
                        val room = it.get("room").asText()
                        val start = it.get("start").asText()
                        val end = it.get("end").asText()
                        val title = it.get("title").asText()
                        val instructor = it.get("instructor").asText()
                        val allDay = it.get("allDay").asBoolean()

                        val event = Event(room, start, end, title, instructor, allDay)
                        fetchedEvents.add(event)
                    }

                    _events.value = fetchedEvents
                    break // Break the loop if the response is as expected

                } else {
                    Log.e("identifier", "Unexpected response code: $responseCode")
                    attempt++ // Increment the attempt variable if the response is not as expected
                    if (attempt == maxAttempts) {
                        Log.e("identifier", "Failed to connect after $maxAttempts attempts")
                    }
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

    // Function to show the calendar and save the user and hash to SharedPreferences
    fun showCalendar(
        context: Context,
        preferences: SharedPreferences,
        checked: Boolean,
        text1: String,
        text2: String
    ) {
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {

            if (checked) {
                with(preferences.edit()) {
                    putString("user", text1)
                    putString("hash", text2)
                    apply()
                }
            }

            getPersonalCalendar(text1, text2)

            // Convert the events to a JSON string
            val mapper = jacksonObjectMapper()
            val eventsJson = mapper.writeValueAsString(_events.value)

            // Save the JSON string to SharedPreferences
            with(preferences.edit()) {
                putString("events", eventsJson)
                commit()
            }

            _loading.value = false
        }
    }

    // Function to load the events from SharedPreferences
    fun loadEvents(prefs: SharedPreferences) {
        val eventsJson = prefs.getString("events", null)
        val type = object : TypeToken<List<Event>>() {}.type
        val events =
            if (eventsJson != null) Gson().fromJson<List<Event>>(eventsJson, type) else listOf()
        _events.value = events
    }

    // Function to update the events
    fun updateEvents(pref: SharedPreferences, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            loadingRefresh.value = true
            getPersonalCalendar(pref.getString("user", "")!!, pref.getString("hash", "")!!)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Data has been updated", Toast.LENGTH_SHORT).show()
            }
            loadingRefresh.value = false
        }
    }

    // Function to download the events as an ICS file
    fun downloadAndSaveAsIcs(events: List<Event>, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val icsFileContent = StringBuilder().apply {
                append("BEGIN:VCALENDAR\n")
                append("VERSION:2.0\n")
                append("PRODID:-//hacksw/handcal//NONSGML v1.0//EN\n")

                val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.GERMANY)
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")

                for (event in events) {
                    append("BEGIN:VEVENT\n")
                    append("DTSTART:${dateFormat.format(Date(event.start.toLong() * 1000))}\n") // Convert to milliseconds
                    append("DTEND:${dateFormat.format(Date(event.end.toLong() * 1000))}\n") // Convert to milliseconds
                    append("UID:${event.title}\n")
                    append("SUMMARY:${event.title + " | " + event.instructor}\n")
                    append("INSTRUCTOR:${event.instructor}\n")
                    append("LOCATION:${event.room}\n")
                    append("ALLDAY:${event.allDay}\n")
                    append("END:VEVENT\n")
                }

                append("END:VCALENDAR\n")
            }.toString()

            withContext(Dispatchers.Main) {
                // Create an intent to open a file chooser
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/calendar"
                    putExtra(Intent.EXTRA_TITLE, "ba-calendar.ics")
                }

                // Start the file chooser activity
                (context as Activity).startActivityForResult(intent, REQUEST_CODE_SAVE_FILE)

                // Save the ICS file content to a temporary variable
                _tempIcsFileContent = icsFileContent
            }
        }
    }

    // Temporary variable to hold the ICS file content
    private var _tempIcsFileContent: String? = null

    // Request code for the "save file" activity
    companion object {
        const val REQUEST_CODE_SAVE_FILE = 1
    }

    fun saveIcsFileContent(context: Context, uri: Uri) {
        _tempIcsFileContent?.let { content ->
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write(content)
                }
            }
        }
    }
}