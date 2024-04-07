package com.example.ba_calander

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
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
import com.example.ba_calander.ui.theme.BacalanderTheme
import androidx.lifecycle.LiveData
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class Screen {
    LoginView,
    CalendarView
}

fun filterEvents(events: List<Event>): List<Event> {
    val currentUnixTime = Instant.now().epochSecond
    return events.filter { event ->
        val startUnixTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(event.start.toLong()), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC)
        val endUnixTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(event.end.toLong()), ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC)
        startUnixTime > currentUnixTime && endUnixTime > currentUnixTime
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
    var currentScreen by remember { mutableStateOf(Screen.LoginView) }

    BacalanderTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                Screen.LoginView -> LoginView(viewModel, { currentScreen = Screen.CalendarView })
                Screen.CalendarView -> CalendarView(viewModel.events.value, { currentScreen = Screen.LoginView })
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
                    viewModel.showCalendar(context, prefs, checked, text1, text2)
                    onButtonClicked()
                }) {
                    Text("Kalender Anzeigen")
                }
            }
        }
    }

@Composable
fun CalendarView(
    events: List<Event>,
    onLogoutClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Text("Calendar Ansicht")
            }
            item {
                Button(onClick = { onLogoutClicked() }) {
                    Text("Logout")
                }
            }
            items(filterEvents(events)) { event ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(text = event.title, fontSize = 20.sp)
                    Row {
                        val formatter = DateTimeFormatter.ofPattern("HH:mm")
                        val startTime = LocalTime.ofInstant(Instant.ofEpochSecond(event.start.toLong()), ZoneId.systemDefault()).format(formatter)
                        val endTime = LocalTime.ofInstant(Instant.ofEpochSecond(event.end.toLong()), ZoneId.systemDefault()).format(formatter)
                        Text("Start: $startTime")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("End: $endTime")
                    }
                    Row {
                        Text("Room: ${event.room}")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Instructor: ${event.instructor}")
                    }
                    HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                }
            }
        }
    }
}
