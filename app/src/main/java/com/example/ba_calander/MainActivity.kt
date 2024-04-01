package com.example.ba_calander

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Website.URL
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.ba_calander.ui.theme.BacalanderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BacalanderTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginView()
                }
            }
        }
    }
}
// TODO: HASH DELETE
//hash: hash

@Composable
fun LoginView(modifier: Modifier = Modifier) {
    val (text1, setText1) = remember { mutableStateOf("") }
    val (text2, setText2) = remember { mutableStateOf("") }
    val (checked, setChecked) = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text("Berufsakademie Kalernder")

            OutlinedTextField(
                value = text1,
                onValueChange = setText1,
                label = { Text("Matrikelnummer") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            OutlinedTextField(
                value = text2,
                onValueChange = setText2,
                label = { Text("Campus Dual Hash") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = setChecked,
                )
                Text("Daten Speichern")
            }

            Button(onClick = { showCalendar(context, prefs, checked, text1, text2) }) {
                Text("Kalender Anzeigen")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginViewPreview() {
    BacalanderTheme {
        LoginView()
    }
}

fun showCalendar(context: Context, preferences: SharedPreferences, checked: Boolean, text1: String, text2: String) {
    if (checked) {
        with (preferences.edit()) {
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

    GlobalScope.launch(Dispatchers.IO) {
        getPersonalCalendar(text1, text2, timestamp1, timestamp2)
    }
}

fun getPersonalCalendar (user: String, hash: String, start: Long, end: Long){
    val url = URL("https://selfservice.campus-dual.de/room/json?userid=${user}&hash=${hash}&start=${start}&end=${end}&_=1711960057052")
    println(url)
    val connection = url.openConnection() as HttpURLConnection

    connection.requestMethod = "GET"

    val responseCode = connection.responseCode

    if (responseCode == HttpURLConnection.HTTP_OK) {
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        // Parse JSON data here
    } else {
        throw IOException("Unexpected response code: $responseCode")
    }

    connection.disconnect()
}