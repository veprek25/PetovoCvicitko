package cz.veprek.zdravi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val GOOD = Color(0xFF2E9E5B)
private val BAD = Color(0xFFE08A1E)

@Composable
fun SleepScreen() {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var hours by rememberSaveable { mutableStateOf("") }
    var mins by rememberSaveable { mutableStateOf("") }
    var range by rememberSaveable { mutableIntStateOf(14) }

    val entries = Store.sleep.toList()
    val (current, best) = sleepStreaks(entries)
    val weekAgo = LocalDate.now().minusDays(6)
    val lastWeek = entries.filter { !it.date.isBefore(weekAgo) }
    val avg = if (lastWeek.isEmpty()) null else lastWeek.sumOf { it.minutes } / lastWeek.size

    val h = hours.toIntOrNull() ?: 0
    val m = mins.toIntOrNull() ?: 0
    val total = h * 60 + m
    val valid = (hours.isNotEmpty() || mins.isNotEmpty()) && m < 60 && total in 1..(24 * 60)
    val existing = Store.sleepFor(date)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Spánek", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // Streak + průměr
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("🔥 Streak", "$current ${daysWord(current)}", Modifier.weight(1f))
                StatCard("🏆 Rekord", "$best ${daysWord(best)}", Modifier.weight(1f))
                StatCard("Ø 7 dní", avg?.let { formatDuration(it) } ?: "–", Modifier.weight(1f))
            }
        }

        // Graf
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Graf", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        listOf(7, 14, 30).forEach { r ->
                            TextButton(onClick = { range = r }) {
                                Text(
                                    "$r d",
                                    fontWeight = if (r == range) FontWeight.Bold else FontWeight.Normal,
                                    color = if (r == range) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    SleepChart(entries, range)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Čárkovaná čára = 8 h · zelená ≥ 8 h · oranžová < 8 h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Zápis
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Zapsat spánek", style = MaterialTheme.typography.titleMedium)
                    DateSelector(date) { date = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = hours,
                            onValueChange = { hours = it.filter(Char::isDigit).take(2) },
                            label = { Text("Hodiny") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = mins,
                            onValueChange = { mins = it.filter(Char::isDigit).take(2) },
                            label = { Text("Minuty") },
                            singleLine = true,
                            isError = m >= 60,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (existing != null) {
                        Text(
                            "Pro tento den už je zapsáno ${formatDuration(existing.minutes)} – uložení ho přepíše.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = {
                            Store.setSleep(date, total)
                            hours = ""
                            mins = ""
                        },
                        enabled = valid,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Uložit") }
                }
            }
        }

        item {
            Text("Historie", style = MaterialTheme.typography.titleMedium)
        }
        if (entries.isEmpty()) {
            item {
                Text("Zatím žádné záznamy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(entries, key = { it.date.toString() }) { e ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // klik = načíst do formuláře k úpravě
                        date = e.date
                        hours = (e.minutes / 60).toString()
                        mins = (e.minutes % 60).toString()
                    }
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(formatDate(e.date), Modifier.weight(1f))
                Text(
                    formatDuration(e.minutes),
                    color = if (e.minutes >= SLEEP_GOAL_MIN) GOOD else BAD,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { Store.removeSleep(e.date) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Smazat")
                }
            }
        }
    }
}

private fun daysWord(n: Int): String = when {
    n == 1 -> "den"
    n in 2..4 -> "dny"
    else -> "dní"
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SleepChart(entries: List<SleepEntry>, days: Int) {
    val today = LocalDate.now()
    val dates = (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
    val byDate = entries.associate { it.date to it.minutes }
    val maxMin = maxOf(10 * 60, dates.maxOfOrNull { byDate[it] ?: 0 } ?: 0)
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(top = 8.dp)
    ) {
        val slot = size.width / dates.size
        val barW = slot * 0.6f
        val radius = CornerRadius(3.dp.toPx())
        dates.forEachIndexed { i, d ->
            val x = i * slot + (slot - barW) / 2f
            val v = byDate[d]
            if (v == null) {
                // nezapsaný den = malá šedá značka
                val hgt = 3.dp.toPx()
                drawRoundRect(emptyColor, Offset(x, size.height - hgt), Size(barW, hgt), radius)
            } else {
                val hgt = size.height * v / maxMin
                drawRoundRect(
                    color = if (v >= SLEEP_GOAL_MIN) GOOD else BAD,
                    topLeft = Offset(x, size.height - hgt),
                    size = Size(barW, hgt),
                    cornerRadius = radius,
                )
            }
        }
        val y = size.height - size.height * SLEEP_GOAL_MIN / maxMin
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
        )
    }
    Row(Modifier.fillMaxWidth()) {
        dates.forEachIndexed { i, d ->
            val show = days <= 14 || i % 5 == 0 || i == dates.lastIndex
            Text(
                text = if (show) d.dayOfMonth.toString() else "",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
