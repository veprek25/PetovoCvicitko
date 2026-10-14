package cz.veprek.petovocvicitko

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    // Export: systémový dialog "Uložit jako" (Downloads, Drive, …)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        status = try {
            context.contentResolver.openOutputStream(uri, "wt")!!.use {
                it.write(Store.exportJson().toByteArray())
            }
            "Záloha uložena."
        } catch (e: Exception) {
            "Export se nepovedl: ${e.message}"
        }
    }

    // Import: výběr souboru, pak potvrzení (přepíše současná data)
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            pendingImport = context.contentResolver.openInputStream(uri)!!.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            status = "Soubor nejde přečíst: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Záloha dat", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Uloží spánek, tréninky i split do jednoho .json souboru. " +
                        "Import ze zálohy nahradí všechna současná data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { exportLauncher.launch("petovocvicitko-zaloha-${LocalDate.now()}.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Exportovat data") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Importovat data") }
                status?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Importovat zálohu?") },
            text = { Text("Všechna současná data v appce budou nahrazena obsahem zálohy.") },
            confirmButton = {
                TextButton(onClick = {
                    status = if (Store.importJson(text)) "Data obnovena ze zálohy."
                    else "Soubor není platná záloha PetovoCvicitko."
                    pendingImport = null
                }) { Text("Importovat") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Zrušit") }
            },
        )
    }
}
