package cz.veprek.zdravi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CS: Locale = Locale.forLanguageTag("cs-CZ")
private val longFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d. M. yyyy", CS)
private val shortFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d. M.", CS)

fun formatDate(d: LocalDate): String {
    val today = LocalDate.now()
    return when (d) {
        today -> "Dnes"
        today.minusDays(1) -> "Včera"
        else -> d.format(longFmt).replaceFirstChar { it.uppercase() }
    }
}

fun formatShort(d: LocalDate): String = d.format(shortFmt)

fun formatDuration(min: Int): String =
    if (min % 60 == 0) "${min / 60} h" else "${min / 60} h ${min % 60} min"

/** Přepínač dne: ‹ datum ›. Klik na datum skočí na dnešek. */
@Composable
fun DateSelector(date: LocalDate, onChange: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onChange(date.minusDays(1)) }) {
            Text("‹", fontSize = 28.sp)
        }
        Text(
            text = formatDate(date),
            modifier = Modifier
                .weight(1f)
                .clickable { onChange(today) }
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = { onChange(date.plusDays(1)) }, enabled = date.isBefore(today)) {
            Text("›", fontSize = 28.sp)
        }
    }
}
