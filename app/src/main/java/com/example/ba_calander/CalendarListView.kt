package com.example.ba_calander

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("SuspiciousIndentation")
@Composable
fun CalendarListView(
    viewModel: MainViewModel,
    context: Context,
    onLogoutClicked: () -> Unit,
    onSwitchViewClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loading by viewModel.loading.collectAsState()
    val events by viewModel.events.collectAsState()
    val loadingRefresh by viewModel.loadingRefresh.collectAsState()

    val prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)

    val groupedEvents = filterEvents(events).groupBy {
        Instant.ofEpochSecond(it.start.toLong()).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = loadingRefresh)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { viewModel.updateEvents(prefs, context) }
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
                                modifier = Modifier.weight(0.7f) //allocate 100% of the remaining width to the Text
                            )
                            IconButton(onClick = onSwitchViewClicked) {
                                Icon(
                                    imageVector = Icons.Filled.ViewDay,
                                    contentDescription = "Switch to Daily View",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(0.3f) // allocate 0% of the remaining width to the IconButton
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
}