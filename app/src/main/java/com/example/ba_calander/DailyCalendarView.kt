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

            println("Events by date: $eventsByDate")

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

                println("Events for date $date: $eventsForDate")


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

                    hours.forEach { hour ->
                        if (hour == "Pause") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Gray)
                                    .height(20.dp)
                            ) {
                                Text(text = "Pause", textAlign = TextAlign.Center)
                            }
                        } else {
                            Row(modifier = Modifier.height(60.dp)) {
                                Text(text = hour, modifier = Modifier.padding(start = 8.dp))

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

                                println("Events for hour $hour: $eventsForHour") // Add this line

                                Column(modifier = Modifier.weight(1f)) {
                                    eventsForHour.forEach { event ->
                                        // Generate a color based on the hash code of the title
                                        val color = Color(event.title.hashCode())

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(color)
                                                .padding(8.dp)
                                                .weight(3f)
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