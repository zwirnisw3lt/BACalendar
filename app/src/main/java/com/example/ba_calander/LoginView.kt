package com.example.ba_calander

import android.annotation.TargetApi
import android.content.Context
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginView(
    viewModel: MainViewModel,
    onButtonClicked: () -> Unit,
    onSwitchViewClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (text1, setText1) = remember { mutableStateOf("") }
    val (text2, setText2) = remember { mutableStateOf("") }
    val (checked, setChecked) = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    var showDialog by remember { mutableStateOf(false) }
    var markdownContent by remember { mutableStateOf("") }
    val (showWebView, setShowWebView) = remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            OutlinedCard(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(16.dp),

                ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Berufsakademie Kalender",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(0.9f) // allocate 90% of the width to the Text
                        )
                        IconButton(
                            onClick = {
                                markdownContent = loadMarkdownContent(context, "hash_help")
                                showDialog = true
                            },
                            modifier = Modifier.weight(0.1f) // allocate 10% of the width to the IconButton
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Help Icon",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = text1,
                        onValueChange = setText1,
                        label = { Text("Matrikelnummer") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = text2,
                        onValueChange = setText2,
                        label = { Text("Campus Dual Hash") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = setChecked,
                        )
                        Text("Daten Speichern")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.viewModelScope.launch {
                                viewModel.showCalendar(context, prefs, checked, text1, text2)
                                val eventsJson = Gson().toJson(viewModel.events.value)
                                prefs.edit().putString("events", eventsJson).commit()
                                withContext(Dispatchers.Main) {
                                    onButtonClicked()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = "Calendar Icon",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Kalender Anzeigen")
                    }


                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = { showDialog = false },
                            text = { MarkdownText(markdownContent) },
                            confirmButton = {
                                Button(onClick = { showDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }
                }
            }

            OutlinedCard(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Anmelden über Campusdual",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(0.9f) // allocate 90% of the width to the Text
                        )
                        IconButton(
                            onClick = {
                                markdownContent = loadMarkdownContent(context, "webview_info")
                                showDialog = true
                            },
                            modifier = Modifier.weight(0.1f) // allocate 10% of the width to the IconButton
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Help Icon",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = {
                        setShowWebView(true)
                    }) {
                        Icon(
                            Icons.Filled.Web,
                            contentDescription = "WebView Icon",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp)) // Add some space between the icon and the text
                        Text("Open WebView")
                    }
                }
            }
        }

        if (showWebView) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            visibility = View.VISIBLE // Make the WebView visible
                            clearCache(true)
                            clearHistory()
                            clearFormData()

                            // Clear all the cookies
                            val cookieManager = android.webkit.CookieManager.getInstance()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                cookieManager.removeAllCookies(null)
                                cookieManager.flush()
                            } else {
                                cookieManager.removeAllCookie()
                            }

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                                    handler.proceed() // Ignore SSL certificate errors
                                }

                                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                                    // Check the URL of the request
                                    if (request.url.toString().contains("https://selfservice.campus-dual.de/room/json")) {
                                        // The request is for the URL you're interested in
                                        // Parse the URL
                                        val uri = Uri.parse(request.url.toString())
                                        // Get the userid and hash from the URL
                                        val userid = uri.getQueryParameter("userid") ?: ""
                                        val hash = uri.getQueryParameter("hash") ?: ""
                                        // Check if the userid or hash is null
                                        if (userid == "" || hash == "") {
                                            // The userid or hash is null
                                            // Show an error message
                                            Toast.makeText(context, "Error: Please try again.", Toast.LENGTH_LONG).show()
                                        } else {
                                            // The userid and hash are not null
                                            // Save the userid and hash to the shared preferences
                                            prefs.edit().putString("userid", userid).putString("hash", hash).apply()
                                            // Call the loadCalendar function with the userid and hash
                                            val checked = true
                                            viewModel.viewModelScope.launch {
                                                viewModel.showCalendar(context, prefs, checked ,userid, hash)
                                                withContext(Dispatchers.Main) {
                                                    onButtonClicked()
                                                }
                                            }
                                            // Close the WebView
                                            setShowWebView(false)
                                        }
                                    }
                                    // Return null to let the WebView handle the request
                                    return null
                                }
                            }
                            loadUrl("https://selfservice.campus-dual.de/index/login") // Open Campusdual login site
                        }
                    },
                    update = { webView ->
                        if (!showWebView) {
                            webView.destroy()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Button(
                    onClick = { setShowWebView(false) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close Icon",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp)) // Add some space between the icon and the text
                    Text("Quit")
                }
            }
        }
    }
}