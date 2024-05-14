package com.example.ba_calander

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyCalendarView(
    viewModel: MainViewModel,
    onLogoutClicked: () -> Unit,
    onSwitchViewClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val events by viewModel.events.collectAsState()

    val hours = listOf(
        "7:45 - 9:15",
        "Pause",
        "9:45 - 11:15",
        "Pause",
        "11:45 - 13:15",
        "Pause",
        "13:45 - 15:15",
        "Pause",
        "15:30 - 17:00",
        "Pause",
        "17:15 - 18:45"
    )

    val eventsByDate = events.groupBy {
        Instant.ofEpochSecond(it.start.toLong()).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Tagesansicht",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.7f) // allocate 100% of the remaining width to the Text
                )
                IconButton(onClick = onSwitchViewClicked) {
                    Icon(
                        imageVector = Icons.Filled.ViewAgenda,
                        contentDescription = "Switch to List View",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(0.3f) // allocate 0% of the remaining width to the IconButton
                    )
                }
            }

            val sortedDates = eventsByDate.keys.sorted()
            val todayIndex = sortedDates.indexOf(LocalDate.now())

            val pagerState =
                rememberPagerState(pageCount = { eventsByDate.keys.size }, initialPage = todayIndex)

            HorizontalPager(
                state = pagerState,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) { page ->
                val date = eventsByDate.keys.sorted()[page]
                val eventsForDate = eventsByDate[date] ?: emptyList()

                Column(modifier = modifier) {
                    val germanFormatter =
                        DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.GERMAN)
                    val germanDate = date.format(germanFormatter)

                    Text(
                        text = germanDate,
                        modifier = Modifier.padding(8.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val hoursWithEvents = mutableListOf<String>()
                    hours.forEachIndexed { index, hour ->
                        if (hour == "Pause" && index != hours.lastIndex - 1) {
                            hoursWithEvents.add(hour)
                        } else if (hour != "Pause") {
                            hoursWithEvents.add(hour)
                        }
                    }

                    hoursWithEvents.forEachIndexed { index, hour ->
                        if (hour == "Pause") {
                            // Check if the current hour is not the last in the list
                            if (index != hours.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Gray)
                                        .height(20.dp)
                                ) {
                                    Text(text = "Pause", textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            // Display the events for the current hour
                            val eventsForHour = eventsForDate.filter { event ->
                                val eventStart = Instant.ofEpochSecond(event.start.toLong())
                                    .atZone(ZoneId.of("Europe/Berlin"))
                                val eventEnd = Instant.ofEpochSecond(event.end.toLong())
                                    .atZone(ZoneId.of("Europe/Berlin"))

                                val hourParts = hour.split(" - ")
                                val hourStartStr =
                                    if (hourParts[0].length == 4) "0${hourParts[0]}" else hourParts[0]
                                val hourEndStr =
                                    if (hourParts[1].length == 4) "0${hourParts[1]}" else hourParts[1]

                                val hourStart = ZonedDateTime.of(
                                    date,
                                    LocalTime.parse(hourStartStr),
                                    ZoneId.of("Europe/Berlin")
                                )
                                val hourEnd = ZonedDateTime.of(
                                    date,
                                    LocalTime.parse(hourEndStr),
                                    ZoneId.of("Europe/Berlin")
                                )

                                (eventStart.isBefore(hourEnd) || eventStart.equals(hourStart)) && (eventEnd.isAfter(
                                    hourStart
                                ) || eventEnd.equals(hourStart))
                            }

                            // Calculate the number of events for the current hour
                            val numEvents = eventsForHour.size

                            // Calculate the height of the row based on the number of events
                            val rowHeight = 60.dp * numEvents

                            Row(modifier = Modifier.height(rowHeight)) {
                                val hourParts = hour.split(" - ")
                                Text(
                                    text = "${hourParts[0]}-\n${hourParts[1]}",
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .weight(1f) // This will take 1 fraction of available space
                                )

                                Column(modifier = Modifier.weight(3f)) { // This will take 3 fractions of available space
                                    eventsForHour.forEach { event ->
                                        val color = adjustColor(Color(event.title.hashCode()))

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(color)
                                                .padding(8.dp)
                                        ) {
                                            Text(text = event.title)
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

fun adjustColor(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[1] = max(hsv[1], 0.2f) // Ensure minimum saturation
    hsv[2] = max(hsv[2], 0.2f) // Ensure minimum brightness
    return Color(android.graphics.Color.HSVToColor(hsv))
}