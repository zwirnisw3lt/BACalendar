package com.example.ba_calander

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat


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

    fun showCalendar(context: Context, preferences: SharedPreferences, checked: Boolean, text1: String, text2: String) {
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {

            Log.d("identifier", "Text1: $text1")
            Log.d("identifier", "Text2: $text2")

            if (checked) {
                with(preferences.edit()) {
                    putString("user", text1)
                    putString("hash", text2)
                    apply()
                }
            } else {
            }
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date = format.parse("2000-10-01 00:00:01") // Replace with your date string
            val timestamp1 = date.time / 1000
            val date2 = format.parse("2099-10-01 00:00:01") // Replace with your date string
            val timestamp2 = date2.time / 1000

            getPersonalCalendar(text1, text2, timestamp1, timestamp2, viewModelScope, _events)

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

    fun loadEvents(prefs: SharedPreferences) {
        val eventsJson = prefs.getString("events", null)
        val type = object : TypeToken<List<Event>>() {}.type
        val events = if (eventsJson != null) Gson().fromJson<List<Event>>(eventsJson, type) else listOf()
        _events.value = events
    }


}