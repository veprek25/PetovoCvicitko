package cz.veprek.petovocvicitko

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.YearMonth

private val TRAINED = Color(0xFF2E9E5B)
private val MONTHS = listOf(
    "Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
    "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec",
)
private val WEEKDAYS = listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")

/**
 * Kalendář tréninků: zelené dny = něco zapsáno, šedé = nic.
 * Klik na den ukáže pod kalendářem seznam cviků z toho dne.
 */
@Composable
fun GymCalendarDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.from(initial)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }

    val byDate = Store.gym.toList().groupBy { it.date }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Hlavička: ‹ Měsíc rok ›
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹", fontSize = 26.sp) }
                    Text(
                        "${MONTHS[month.monthValue - 1]} ${month.year}",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(
                        onClick = { month = month.plusMonths(1) },
                        enabled = month.isBefore(YearMonth.from(today)),
                    ) { Text("›", fontSize = 26.sp) }
                }

                // Názvy dnů
                Row(Modifier.fillMaxWidth()) {
                    WEEKDAYS.forEach {
                        Text(
                            it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                // Mřížka dnů (týden začíná pondělím)
                val first = month.atDay(1)
                val offset = first.dayOfWeek.value - 1
                val cells: List<LocalDate?> =
                    List(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        for (i in 0 until 7) {
                            val day = week.getOrNull(i)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (day != null) {
                                    DayCell(
                                        day = day,
                                        trained = byDate.containsKey(day),
                                        isToday = day == today,
                                        isSelected = day == selected,
                                        isFuture = day.isAfter(today),
                                        onClick = { selected = if (selected == day) null else day },
                                    )
                                }
                            }
                        }
                    }
                }

                // Detail vybraného dne
                selected?.let { day ->
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(formatDate(day), style = MaterialTheme.typography.titleMedium)
                    Store.split[day.dayOfWeek.value]?.let {
                        Text(
                            "Plán: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    val entries = byDate[day].orEmpty().sortedBy { it.id }
                    if (entries.isEmpty()) {
                        Text(
                            "Tento den nic zapsáno.",
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(
                            Modifier.padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            entries.forEach { e ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        e.exercise,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(describe(e), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (!day.isAfter(today)) {
                        TextButton(onClick = { onOpenDay(day) }) { Text("Otevřít tento den v záznamech") }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Zavřít") }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    trained: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (trained) TRAINED else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (trained) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    var m: Modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .alpha(if (isFuture) 0.4f else 1f)
        .clip(CircleShape)
        .background(bg)
    if (isSelected) {
        m = m.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else if (isToday) {
        m = m.border(1.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
    }
    Box(
        m.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.dayOfMonth.toString(),
            color = fg,
            fontWeight = if (trained || isToday) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
