package com.example.ba_calander

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ba_calander.ui.theme.BacalanderTheme
import kotlin.properties.Delegates

enum class Screen {
    LoginView,
    CalendarView
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
//hash: 4239f59c794dff7889ab8d51602b5710

    @Composable
    fun LoginView(
        viewModel: MainViewModel,
        onButtonClicked: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val (text1, setText1) = remember { mutableStateOf("3004719") }
        val (text2, setText2) = remember { mutableStateOf("4239f59c794dff7889ab8d51602b5710") }
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

                viewModel.events.observeAsState().value?.let { events ->
                    viewModel.events.value = events
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
        modifier: Modifier = Modifier) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Calendar Ansicht")
                events.forEach() {
                    Text(it.title)
                }
                
                Button(onClick = { onLogoutClicked() }) {
                    Text("Logout")
                }
            }
        }
    }
