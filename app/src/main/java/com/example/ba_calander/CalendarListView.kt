package com.example.ba_calander

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Filter1
import androidx.compose.material.icons.filled.Filter2
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("SuspiciousIndentation")
@Composable
fun CalendarListView(
    viewModel: MainViewModel,
    context: Context,
    onLogoutClicked: () -> Unit,
    onSwitchViewClicked: () -> Unit,
    isNetworkAvailable: (Context) -> Boolean,
    modifier: Modifier = Modifier
) {
    val loading by viewModel.loading.collectAsState()
    val events by viewModel.events.collectAsState()
    val loadingRefresh by viewModel.loadingRefresh.collectAsState()

    val prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)

    // Load the selected group from shared preferences
    var selectedGroup by remember {
        mutableStateOf(prefs.getInt("selectedGroup", 1))
    }

    // Save the selected group in shared preferences when it changes
    fun saveSelectedGroup(group: Int) {
        prefs.edit().putInt("selectedGroup", group).apply()
    }

    // Get the current date
    val currentDate = ZonedDateTime.now(ZoneId.of("Europe/Berlin")).toLocalDate()

    // Filter events based on the selected group and current date
    val filteredEvents = events.filter { event ->
        val eventDate = Instant.ofEpochSecond(event.start.toLong()).atZone(ZoneId.of("Europe/Berlin")).toLocalDate()
        val groupPattern = "Gruppe \\d+".toRegex()
        val matchResult = groupPattern.find(event.title)
        if (matchResult != null) {
            event.title.contains("Gruppe $selectedGroup") && !eventDate.isBefore(currentDate)
        } else {
            !eventDate.isBefore(currentDate) // Show events that do not contain "Gruppe" and are not in the past
        }
    }

    // Group events by date
    val groupedEvents = filteredEvents.groupBy {
        Instant.ofEpochSecond(it.start.toLong()).atZone(ZoneId.of("Europe/Berlin")).toLocalDate()
    }

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = loadingRefresh)

    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        if (loading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator(
                    strokeWidth = 8.dp, // Adjust this to change the thickness of the circle
                    color = MaterialTheme.colorScheme.onSurface, // Change this to the color of your choice
                    modifier = Modifier
                        .size(220.dp) // Adjust this to change the size of the circle
                        .rotate(angle)
                )
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(200.dp) // Adjust this to change the size of the logo
                        .clip(CircleShape)
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "Kalender wird geladen!",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { 
                    if (isNetworkAvailable(context)) {
                        viewModel.updateEvents(prefs, context) 
                    } else {
                        Toast.makeText(context, "Aktualisieren fehlgeschlagen. Keine Internetverbindung!.", Toast.LENGTH_LONG).show()
                    }
                }
            ) {
                LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Listenansicht",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onSwitchViewClicked) {
                                Icon(
                                    imageVector = Icons.Filled.ViewDay,
                                    contentDescription = "Switch to Daily View",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = {
                                selectedGroup = if (selectedGroup == 1) 2 else 1
                                saveSelectedGroup(selectedGroup)
                            }) {
                                Icon(
                                    imageVector = if (selectedGroup == 1) Icons.Default.Filter1 else Icons.Default.Filter2,
                                    contentDescription = "Toggle Group",
                                    tint = if (MaterialTheme.colorScheme.background == Color.Black) Color.White else Color.Black
                                )
                            }
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
                                eventsForDate.toSet().forEachIndexed { index, event ->
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Spacer(modifier = Modifier.height((-8).dp))
                                        Text(text = event.title, fontSize = 20.sp)
                                        Row {
                                            val formatter = DateTimeFormatter.ofPattern("HH:mm")
                                            val startDateTime = ZonedDateTime.ofInstant(
                                                Instant.ofEpochSecond(event.start.toLong()),
                                                ZoneId.of("Europe/Berlin")
                                            )
                                            val startTime = startDateTime.toLocalTime().format(formatter)
                                            val endDateTime = ZonedDateTime.ofInstant(
                                                Instant.ofEpochSecond(event.end.toLong()),
                                                ZoneId.of("Europe/Berlin")
                                            )
                                            val endTime = endDateTime.toLocalTime().format(formatter)
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
}