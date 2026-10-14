package cz.veprek.petovocvicitko

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GymScreen() {
    var sub by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            if (sub == 0) GymRecords() else SplitScreen()
        }
        // přepínač nad spodní lištou; při otevřené klávesnici schovaný, ať nezabírá místo
        if (!WindowInsets.isImeVisible) {
            SubNav(listOf("Záznamy", "Split"), sub) { sub = it }
        }
    }
}

/** Menší vycentrovaný přepínač ve stylu spodní navigace (pilulka pod vybranou položkou). */
@Composable
private fun SubNav(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items.forEachIndexed { i, label ->
                    val isSel = i == selected
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSel) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent
                            )
                            .clickable { onSelect(i) }
                            .padding(horizontal = 22.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSel) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GymRecords() {
    var date by remember { mutableStateOf(LocalDate.now()) }
    var name by rememberSaveable { mutableStateOf("") }
    var sets by rememberSaveable { mutableStateOf("") }
    var kg by rememberSaveable { mutableStateOf("") }
    var nameFocused by remember { mutableStateOf(false) }

    val typed = name.trim()
    val suggestions = if (!nameFocused) emptyList() else
        Store.exerciseNames()
            .filter { it.contains(typed, ignoreCase = true) && !it.equals(typed, ignoreCase = true) }
            .take(8)
    val last = if (typed.isEmpty()) null else Store.lastBefore(typed, date)

    val allEntries = Store.gym.toList()
    val dayEntries = allEntries.filter { it.date == date }.sortedBy { it.id }
    val recentDays = allEntries.map { it.date }.distinct().sortedDescending().filter { it != date }.take(8)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DateSelector(date) { date = it } }

        // co je podle splitu na zvolený den
        Store.split[date.dayOfWeek.value]?.let { plan ->
            item {
                Text(
                    "Plán: $plan",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Formulář: 1 nahoře (cvik), 2 dole (série×opakování | kg)
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Cvik") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { nameFocused = it.isFocused },
                    )

                    if (suggestions.isNotEmpty()) {
                        Card {
                            Column {
                                suggestions.forEachIndexed { i, s ->
                                    if (i > 0) HorizontalDivider()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { name = s },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            s,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                        )
                                        IconButton(onClick = { Store.hideSuggestion(s) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Odebrat z nabídky")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (last != null) {
                        Text(
                            "Naposledy (${formatShort(last.date)}): ${describe(last)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = sets,
                            onValueChange = { sets = it },
                            label = { Text("Série × opak.") },
                            placeholder = { Text("3x15") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = kg,
                            onValueChange = { v -> kg = v.filter { it.isDigit() || it == ',' || it == '.' }.take(6) },
                            label = { Text("kg") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Button(
                        onClick = {
                            Store.addGym(date, typed, sets.trim(), kg.trim().replace('.', ','))
                            name = ""
                            sets = ""
                            kg = ""
                        },
                        enabled = typed.isNotEmpty() && sets.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Přidat") }
                }
            }
        }

        item {
            Text("Záznamy – ${formatDate(date).lowercase()}", style = MaterialTheme.typography.titleMedium)
        }
        if (dayEntries.isEmpty()) {
            item {
                Text("Tento den nic zapsáno.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(dayEntries, key = { it.id }) { e ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.exercise, style = MaterialTheme.typography.titleMedium)
                        Text(describe(e), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { Store.removeGym(e.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Smazat")
                    }
                }
            }
        }

        if (recentDays.isNotEmpty()) {
            item {
                Text("Předchozí tréninky", style = MaterialTheme.typography.titleMedium)
            }
            items(recentDays, key = { "d" + it.toString() }) { d ->
                val count = allEntries.count { it.date == d }
                TextButton(onClick = { date = d }) {
                    Text("${formatDate(d)} · $count ${exWord(count)}")
                }
            }
        }
    }
}

private fun describe(e: GymEntry): String =
    if (e.kg.isBlank()) e.setsReps else "${e.setsReps} · ${e.kg} kg"

private fun exWord(n: Int): String = when {
    n == 1 -> "cvik"
    n in 2..4 -> "cviky"
    else -> "cviků"
}
