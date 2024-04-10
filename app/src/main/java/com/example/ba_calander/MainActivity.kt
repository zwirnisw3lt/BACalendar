package com.example.ba_calander

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ba_calander.ui.theme.BacalanderTheme
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

enum class Screen {
    LoginView,
    CalendarListView,
    DailyCalendarView,
    CalendarView
}

fun filterEvents(events: List<Event>): List<Event> {
    val currentDateTime = LocalDateTime.now(ZoneOffset.UTC)
    val currentDate = currentDateTime.toLocalDate()

    return events.filter { event ->
        val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(event.start.toLong()), ZoneOffset.UTC)
        val startDate = startDateTime.toLocalDate()

        !startDate.isBefore(currentDate)
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp(viewModel)
        }
    }
}

@Composable
fun MyApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    val user = prefs.getString("user", null)
    val hash = prefs.getString("hash", null)
    var currentScreen by remember { mutableStateOf(if (user != null && hash != null) Screen.CalendarListView else Screen.LoginView) }

    BacalanderTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LaunchedEffect(Unit) {
                viewModel.loadEvents(prefs)
            }
            when (currentScreen) {
                Screen.LoginView -> LoginView(viewModel, { currentScreen = Screen.CalendarListView })
                Screen.CalendarListView -> CalendarListView(viewModel, context, { currentScreen = Screen.LoginView })
                Screen.DailyCalendarView -> DailyCalendarView(viewModel.events.value, { currentScreen = Screen.CalendarView })
                Screen.CalendarView -> CalendarView(viewModel.events.value, { currentScreen = Screen.CalendarListView })
            }
        }
    }
}

// TODO: HASH DELETE
//hash: hash

    @Composable
    fun LoginView(
        viewModel: MainViewModel,
        onButtonClicked: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val (text1, setText1) = remember { mutableStateOf("number") }
        val (text2, setText2) = remember { mutableStateOf("hash") }
        val (checked, setChecked) = remember { mutableStateOf(false) }
        val context = LocalContext.current
        val prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)

        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    "Berufsakademie Kalender",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(16.dp)
                )


                OutlinedTextField(
                    value = text1,
                    onValueChange = setText1,
                    label = { Text("Matrikelnummer") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    maxLines = 1
                )

                OutlinedTextField(
                    value = text2,
                    onValueChange = setText2,
                    label = { Text("Campus Dual Hash") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    maxLines = 1
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = setChecked,
                    )
                    Text("Daten Speichern")
                }

                Button(onClick = {
                    viewModel.viewModelScope.launch {
                        viewModel.showCalendar(context, prefs, checked, text1, text2)
                        val eventsJson = Gson().toJson(viewModel.events.value)
                        prefs.edit().putString("events", eventsJson).commit()
                        withContext(Dispatchers.Main) {
                            onButtonClicked()
                        }
                    }
                }) {
                    Text("Kalender Anzeigen")
                }
            }
        }
    }

@SuppressLint("SuspiciousIndentation")
@Composable
fun CalendarListView(
    viewModel: MainViewModel,
    context: Context,
    onLogoutClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loading by viewModel.loading.collectAsState()
    val events by viewModel.events.collectAsState()

    val groupedEvents = filterEvents(events).groupBy {
        Instant.ofEpochSecond(it.start.toLong()).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else {
          val prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)

            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                item {
                    Text("Calendar Ansicht", fontSize = 30.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                item {
                    Button(onClick = {
                        prefs.edit().clear().apply()
                        onLogoutClicked()
                    }) {
                        Text("Logout")
                    }
                }
                items(groupedEvents.keys.toList()) { date ->
                    val eventsForDate = groupedEvents[date] ?: listOf()
                    OutlinedCard(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.GERMAN)),
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                            eventsForDate.toSet().forEachIndexed() { index, event ->
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Spacer(modifier = Modifier.height((-8).dp))
                                    Text(text = event.title, fontSize = 20.sp)
                                    Row {
                                        val formatter = DateTimeFormatter.ofPattern("HH:mm")
                                        val startTime = LocalTime.ofInstant(
                                            Instant.ofEpochSecond(event.start.toLong()),
                                            ZoneId.systemDefault()
                                        ).format(formatter)
                                        val endTime = LocalTime.ofInstant(
                                            Instant.ofEpochSecond(event.end.toLong()),
                                            ZoneId.systemDefault()
                                        ).format(formatter)
                                        Text("Start: $startTime")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Ende: $endTime")
                                    }
                                    Row {
                                        Text("Raum: ${event.room}")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Dozent: ${event.instructor}")
                                    }
                                    if (index < eventsForDate.size - 1) {
                                        Divider(
                                            color = MaterialTheme.colorScheme.primary,
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun DailyCalendarView(
    events: List<Event>,
    onLogoutClicked: () -> Unit,
    modifier: Modifier = Modifier
){
}

@Composable
fun CalendarView(
    events: List<Event>,
    onLogoutClicked: () -> Unit,
    modifier: Modifier = Modifier
){
}
