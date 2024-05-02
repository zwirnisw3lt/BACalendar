package com.example.ba_calander

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.example.ba_calander.MainViewModel.Companion.REQUEST_CODE_SAVE_FILE
import com.example.ba_calander.ui.theme.BacalanderTheme
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.gson.Gson
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Screen {
    LoginView,
    CalendarListView,
    DailyCalendarView
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

fun loadMarkdownContent(context: Context): String {
    val inputStream = context.assets.open("hash_help.md")
    return inputStream.bufferedReader().use { it.readText() }
}

@Composable
fun MyApp(viewModel: MainViewModel) {
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
            drawerContent = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom // Align items to the bottom
                ) {
                    Spacer(modifier = Modifier.weight(1f)) // Push items to the bottom
                    Button(onClick = {
                        prefs.edit().clear().apply()
                        currentScreen = Screen.LoginView
                        scope.launch {
                            drawerState.close() // Close the drawer when the button is clicked
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout Icon") // Add an icon
                        Text("Logout")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        viewModel.downloadAndSaveAsIcs(viewModel.events.value, context)
                        scope.launch {
                            drawerState.close() // Close the drawer when the button is clicked
                        }
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = "Download Icon") // Add an icon
                        Text("Download as .ics")
                    }
                }
            },
            content = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LaunchedEffect(Unit) {
                        viewModel.loadEvents(prefs)
                        if (user != null && hash != null) {
                            viewModel.updateEvents(prefs, context)
                        }
                    }
                    when (currentScreen) {
                        Screen.LoginView -> LoginView(viewModel, { currentScreen = Screen.CalendarListView }, { currentScreen = Screen.DailyCalendarView })
                        Screen.CalendarListView -> CalendarListView(viewModel, context, { currentScreen = Screen.LoginView }, { currentScreen = Screen.DailyCalendarView })
                        Screen.DailyCalendarView -> DailyCalendarView(viewModel, { currentScreen = Screen.LoginView }, { currentScreen = Screen.CalendarListView })
                    }
                }
            }
        )
    }
}

// TODO: HASH DELETE
//hash: 4239f59c794dff7889ab8d51602b5710

// TODO: Optimize (I  Skipped 47 frames!  The application may be doing too much work on its main thread.)

@Composable
fun MarkdownText(markdownContent: String) {
    val context = LocalContext.current
    val markwon = remember {
        Markwon.builder(context).build()
    }
    val markdown = remember(markdownContent) {
        markwon.toMarkdown(markdownContent)
    }

    val textcolor = MaterialTheme.colorScheme.onSurface.toArgb()
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
                setTextColor(textcolor)
            }
        },
        update = { view ->
            markwon.setParsedMarkdown(view, markdown)
        }
    )
}


@Composable
fun CurrentTimeIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(2.dp)
            .fillMaxWidth()
            .background(Color.Red)
    )
}



