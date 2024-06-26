package com.example.ba_calander

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun EventItem(event: Event, hourHeight: Dp, earliestStartTime: LocalTime) {
    val startMinutes = event.start.toLongOrNull()?.let { time ->
        Instant.ofEpochMilli(time * 1000)
            .atZone(ZoneId.of("Europe/Berlin"))
            .toLocalTime()
            .let { it.hour * 60 + it.minute }
    } ?: 0

    val endMinutes = event.end.toLongOrNull()?.let { time ->
        Instant.ofEpochMilli(time * 1000)
            .atZone(ZoneId.of("Europe/Berlin"))
            .toLocalTime()
            .let { it.hour * 60 + it.minute }
    } ?: 0

    val earliestStartMinutes = earliestStartTime.hour * 60 + earliestStartTime.minute

    val topOffset = ((startMinutes - earliestStartMinutes) / 60.0) * hourHeight.value
    val eventHeight = ((endMinutes - startMinutes) / 60.0) * hourHeight.value

    val borderColor = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Black
    }
    
    Box(
        modifier = Modifier
            .offset(y = topOffset.dp)
            .height(eventHeight.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clip(RoundedCornerShape(10.dp)) // Use RoundedCornerShape to make the corners more rounded
            .border(4.dp, borderColor), // Set the border color based on the theme
        contentAlignment = Alignment.Center
    ) {
        Column {
            Text(event.title)
            Text(event.room)
            Text(event.instructor)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyCalendarView(
    viewModel: MainViewModel,
    onLogoutClicked: () -> Unit,
    onSwitchViewClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val events by viewModel.events.collectAsState()

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    // Add a state variable to trigger a recomposition every minute
    var triggerRecomposition by remember { mutableStateOf(0) }

    // Launch an effect that triggers a recomposition every minute
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000L) // Delay for 60 seconds (1 minute)
            triggerRecomposition++ // Increment the state variable to trigger a recomposition
        }
    }


    // Add a state variable to hold the loading status
    var isLoading by remember { mutableStateOf(true) }

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

            // Calculate the minimum and maximum times
            var earliestStartTime: LocalTime = LocalTime.MAX
            var latestEndTime: LocalTime = LocalTime.MIN

            for (event in events) {
                event.start.toLongOrNull()?.let { time ->
                    val localTime = Instant.ofEpochMilli(time * 1000)
                        .atZone(ZoneId.of("Europe/Berlin"))
                        .toLocalTime()
                    if (localTime.isBefore(earliestStartTime)) {
                        earliestStartTime = localTime
                    }
                }

                event.end.toLongOrNull()?.let { time ->
                    val localTime = Instant.ofEpochMilli(time * 1000)
                        .atZone(ZoneId.of("Europe/Berlin"))
                        .toLocalTime()
                    if (localTime.isAfter(latestEndTime)) {
                        latestEndTime = localTime
                    }
                }
            }

            // Round up the latestEndTime to the next hour
            latestEndTime = if (latestEndTime.minute > 0) latestEndTime.plusHours(1).withMinute(0) else latestEndTime

            // Create a list of times from the minimum time to the maximum time with an interval of 1 hour
            val times = mutableListOf<LocalTime>()
            var currentTime = earliestStartTime
            while (!currentTime.isAfter(latestEndTime)) {
                times.add(currentTime)
                currentTime = currentTime.plusHours(1)
            }

            times.sort()

            // Calculate the height of each hour block
            val hourHeight = if (times.isNotEmpty()) screenHeight / times.size else 60.dp // Default height if times list is empty

            // Determine the number of days to display in the pager. This could be the number of unique days in your events list.
            val numDays = events.map { event ->
                Instant.ofEpochMilli(event.start.toLong() * 1000)
                    .atZone(ZoneId.of("Europe/Berlin"))
                    .toLocalDate()
            }.distinct().count()

            val pagerState = rememberPagerState(pageCount = { numDays })

            // Calculate the date for the current page
            val startDate = LocalDate.now()
            val currentDate = startDate.plusDays(pagerState.currentPage.toLong())
            val dayOfWeek = currentDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val date = currentDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$dayOfWeek, $date",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                // Display the time scale on the left
                Column(
                    modifier = Modifier
                        .width(60.dp)
                        .padding(end = 8.dp)
                ) {
                    times.forEach { time ->
                        Box(
                            modifier = Modifier
                                .height(hourHeight) // Set the height of each hour block
                                .offset(y = -(hourHeight / 5)) // Move the time label up by half of the hourHeight
                        ) {
                            Text(
                                text = formatter.format(time),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                // Define dates and eventsByDate outside the LaunchedEffect block
                var dates by remember { mutableStateOf(emptyList<LocalDate>()) }
                var eventsByDate by remember { mutableStateOf(emptyList<List<Event>>()) }

                // Launch a coroutine to load the data in the background
                LaunchedEffect(Unit) {
                    // Load the data
                    val (loadedDates, loadedEventsByDate) = viewModel.calculateDatesAndEventsByDate(
                        startDate,
                        numDays
                    )

                    // Update dates and eventsByDate
                    dates = loadedDates
                    eventsByDate = loadedEventsByDate

                    // Set the loading status to false
                    isLoading = false
                }


                if (isLoading) {
                    // Show a loading spinner
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    )
                    {
                        CircularProgressIndicator()
                    }
                } else {

                    HorizontalPager(
                        state = pagerState,
                    ) { page ->
                        // Get the date and events for the current page
                        val date = dates[page]
                        val todayEvents = eventsByDate[page]

                        // Display the events for the current date
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 12.dp)
                        ) {
                            todayEvents.forEach { event ->
                                EventItem(event, hourHeight, earliestStartTime)
                            }

                            // Only show the current timeline on the current day
                            if (date == LocalDate.now()) {
                                // Calculate the total minutes between the earliest start time and the latest end time
                                val totalMinutes =
                                    if (earliestStartTime != null && latestEndTime != null) {
                                        val startCalendar =
                                            Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))
                                                .apply {
                                                    time =
                                                        SimpleDateFormat("HH:mm").parse(
                                                            earliestStartTime.toString()
                                                        )
                                                }
                                        val endCalendar =
                                            Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))
                                                .apply {
                                                    time = SimpleDateFormat("HH:mm").parse(
                                                        latestEndTime.toString()
                                                    )
                                                }
                                        val hours =
                                            endCalendar.get(Calendar.HOUR_OF_DAY) - startCalendar.get(
                                                Calendar.HOUR_OF_DAY
                                            )
                                        hours * 60
                                    } else {
                                        24 * 60 // Total minutes in a day
                                    }

                                // Calculate the current time in minutes
                                val currentTime =
                                    Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))
                                val minutes =
                                    currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(
                                        Calendar.MINUTE
                                    )

                                // Calculate the position of the current time line
                                val position =
                                    if (earliestStartTime != null && latestEndTime != null) {
                                        val startMinutes =
                                            SimpleDateFormat("HH:mm").parse(earliestStartTime.toString())
                                                .let {
                                                    val calendar =
                                                        Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))
                                                    calendar.time = it
                                                    calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(
                                                        Calendar.MINUTE
                                                    )
                                                }
                                        // Use the state variable in the calculation to ensure that it is recalculated every minute
                                        (minutes - startMinutes + triggerRecomposition).toDouble() / totalMinutes
                                    } else {
                                        // Use the state variable in the calculation to ensure that it is recalculated every minute
                                        (minutes + triggerRecomposition).toDouble() / (24 * 60)
                                    }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .align(Alignment.TopStart)
                                        .offset(y = (position * screenHeight.value).dp)
                                        .background(Color.Red)
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }