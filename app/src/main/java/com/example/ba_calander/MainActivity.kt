package com.example.ba_calander

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Website.URL
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.example.ba_calander.ui.theme.BacalanderTheme
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
    val (text1, setText1) = remember { mutableStateOf("number") }
    val (text2, setText2) = remember { mutableStateOf("hash") }
    val (checked, setChecked) = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text("Berufsakademie Kalender", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(16.dp))

            
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

fun getPersonalCalendar(user: String, hash: String, start: Long, end: Long) {
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

                val jsonObject = JSONObject(response)

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