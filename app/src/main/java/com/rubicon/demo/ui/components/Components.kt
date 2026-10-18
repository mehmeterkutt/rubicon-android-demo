package com.rubicon.demo.ui.components

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rubicon.analytics.LogLevel
import com.rubicon.demo.sdk.Calls
import com.rubicon.demo.sdk.FeedEntry
import com.rubicon.demo.sdk.LogLine
import com.rubicon.demo.ui.theme.Danger
import com.rubicon.demo.ui.theme.Mono
import com.rubicon.demo.ui.theme.Success
import com.rubicon.demo.ui.theme.Warning
import kotlinx.coroutines.launch

/** A titled card with an optional explanation: every screen is a column of these. */
@Composable
fun SectionCard(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

/** Wrapping rows of buttons or chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Flow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun KeyValueLine(
    key: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, style = Mono, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** A short status label with a colored dot. */
@Composable
fun StatusPill(
    text: String,
    color: Color,
) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

/** What a button is expected to show, in small print under it. */
@Composable
fun Expect(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A command or URL in monospace with a copy button. */
@Composable
fun CodeLine(text: String) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
            Text(text, style = Mono, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
            IconButton(onClick = {
                scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Rubicon", text))) }
                Calls.show("Copied")
            }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    description: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Chips for one value of an enum or a list. */
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Flow {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}

/** Chips for a set of values. */
@Composable
fun <T> MultiChips(
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onChange: (Set<T>) -> Unit,
) {
    Flow {
        options.forEach { option ->
            val on = option in selected
            FilterChip(selected = on, onClick = { onChange(if (on) selected - option else selected + option) }, label = { Text(label(option)) })
        }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        supportingText = supporting?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
    )
}

fun levelColor(level: LogLevel): Color =
    when (level) {
        LogLevel.ERROR -> Danger
        LogLevel.WARN -> Warning
        LogLevel.INFO -> Success
        else -> Color(0xFF7B8798)
    }

@Composable
fun LogRow(line: LogLine) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 5.dp).size(8.dp).background(levelColor(line.level), CircleShape))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(line.message, style = MaterialTheme.typography.bodySmall)
            if (line.throwable != null) Text(line.throwable, style = Mono, color = Danger)
            Text("${line.time}  ${line.level.name.lowercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FeedRow(entry: FeedEntry) {
    val color =
        when (entry.kind) {
            FeedEntry.Kind.QUEUED -> MaterialTheme.colorScheme.primary
            FeedEntry.Kind.SENT -> Success
            FeedEntry.Kind.DROPPED -> Danger
        }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        StatusPill(entry.kind.name.lowercase(), color)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.titleSmall)
            Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${entry.time}  ${entry.messageIds.firstOrNull()?.takeLast(12) ?: ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
