package com.example.ba_calander

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableStateOf
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
    val events = mutableStateOf<List<Event>>(listOf())
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

            getPersonalCalendar(text1, text2, timestamp1, timestamp2, viewModelScope, events)
        }
    }


}