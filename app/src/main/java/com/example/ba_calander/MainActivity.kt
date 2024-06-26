package com.example.ba_calander

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ba_calander.MainViewModel.Companion.REQUEST_CODE_SAVE_FILE
import com.example.ba_calander.ui.theme.BacalanderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.observeOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

enum class Screen {
    LoginView,
    CalendarListView,
    DailyCalendarView
}

//TODO: usage from back (arrow) button

fun filterEvents(events: List<Event>): List<Event> {
    val currentDateTime = LocalDateTime.now(ZoneOffset.UTC)
    val currentDate = currentDateTime.toLocalDate()

    return events.filter { event ->
        val startDateTime = Instant.ofEpochSecond(event.start.toLong())
        val zoneId = ZoneId.of("Europe/Berlin")
        val zonedDateTime = ZonedDateTime.ofInstant(startDateTime, zoneId)
        val startDate = zonedDateTime.toLocalDate()

        !startDate.isBefore(currentDate)
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp(viewModel, this@MainActivity)
        }
        // TODO: Change Status Bar Color
    }

    // TODO: find a better way to handle this
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SAVE_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                viewModel.saveIcsFileContent(this, uri)
            }
        }
    }
}

fun loadMarkdownContent(context: Context, filename: String): String {
    val inputStream = context.assets.open("$filename.md")
    return inputStream.bufferedReader().use { it.readText() }
}

@Composable
fun MyApp(viewModel: MainViewModel, mainActivity: MainActivity) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    val user = prefs.getString("user", null)
    val hash = prefs.getString("hash", null)
    var currentScreen by remember { mutableStateOf(if (user != null && hash != null) Screen.CalendarListView else Screen.LoginView) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BacalanderTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = currentScreen != Screen.LoginView, // Disable gestures when the login screen is shown
            drawerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom // Align items to the bottom
                ) {
                    Spacer(modifier = Modifier.weight(1f)) // Push items to the bottom
                    Button(onClick = {
                        viewModel.downloadAndSaveAsIcs(viewModel.events.value, context)
                        scope.launch {
                            drawerState.close() // Close the drawer when the button is clicked
                        }
                    }) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Download Icon"
                        ) // Add an icon
                        Text("Download als .ics Datei")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        prefs.edit().clear().apply()
                        currentScreen = Screen.LoginView
                        scope.launch {
                            drawerState.close() // Close the drawer when the button is clicked
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout Icon"
                        ) // Add an icon
                        Text("Ausloggen")
                    }
                }
            },
            content = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            viewModel.loadEvents(prefs)
                            if (user != null && hash != null) {
                                viewModel.updateEvents(prefs, context)
                            }
                        }
                    }

                    when (currentScreen) {
                        Screen.LoginView -> LoginView(
                            viewModel,
                            { currentScreen = Screen.CalendarListView },
                            { currentScreen = Screen.DailyCalendarView },
                            mainActivity::isNetworkAvailable)

                        Screen.CalendarListView -> CalendarListView(
                            viewModel,
                            context,
                            { currentScreen = Screen.LoginView },
                            { currentScreen = Screen.DailyCalendarView },
                            mainActivity::isNetworkAvailable)

                        Screen.DailyCalendarView -> DailyCalendarView(
                            viewModel,
                            { currentScreen = Screen.LoginView },
                            { currentScreen = Screen.CalendarListView })
                    }
                }
            }
        )
    }
}

// TODO: Optimize (I  Skipped 47 frames!  The application may be doing too much work on its main thread.)