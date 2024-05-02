package com.example.ba_calander

import android.content.Context
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
    val (text1, setText1) = remember { mutableStateOf("3004719") }
    val (text2, setText2) = remember { mutableStateOf("4239f59c794dff7889ab8d51602b5710") }
    val (checked, setChecked) = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    var showDialog by remember { mutableStateOf(false) }
    var markdownContent by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        OutlinedCard(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),

            ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
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
                            markdownContent = loadMarkdownContent(context)
                            showDialog = true
                        },
                        modifier = Modifier.weight(0.1f) // allocate 10% of the width to the IconButton
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help Icon", tint = MaterialTheme.colorScheme.onSurface)
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
    }
}
