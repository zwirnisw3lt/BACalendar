package com.example.ba_calander

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

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