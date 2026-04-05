package net.vaultcity.splitp2p

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

// Hilfsklasse für das JSON Payload
@Serializable
data class GroupJoinPayload(
    val id:   String,  //id
    val name: String,  //name
    val cur:  String,  //currency
    val key:  String,  //key
    val v:    Long     //version
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupScreen(
    // Signatur geändert, um das Payload-Objekt zu übergeben
    onGroupCreated: (GroupJoinPayload) -> Unit,
    onBack: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val currencies = listOf("EUR")
    var expanded by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf(currencies[0]) }

    // Zustand, ob der QR-Code angezeigt werden soll
    var showQrCode by remember { mutableStateOf(false) }
    // Der generierte JSON-String für den QR-Code
    var qrPayloadJson by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        // Titel angepasst
                        text = if (showQrCode) "Gruppeneinladung" else "Gruppe erstellen",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Zurück",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF4CAF50)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally // Zentriert den QR Code
        ) {
            if (!showQrCode) {
                // --- ZUSTAND 1: EINGABE (mit leichten Korrekturen) ---
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Gruppenname") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Währung") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        selectedCurrency = currency
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // "Erstellen" Knopf (Deaktiviert, wenn Name leer ist)
                    Button(
                        onClick = {
                            // Lazysodium kurz initialisieren
                            val sodium = SodiumAndroid()
                            val ls = LazySodiumAndroid(sodium)
                            // get key bytes
                            val keyBytes = ls.randomBytesBuf(32)
                            // Daten generieren
                            val payload = GroupJoinPayload(
                                id = UUID.randomUUID().toString(),
                                name = groupName,
                                cur = selectedCurrency,
                                // Simpler Hex-Key Platzhalter
                                key = keyBytes.toHexString(),
                                v = 1
                            )
                            // In JSON verwandeln
                            qrPayloadJson = Json.encodeToString(payload)

                            // Callback an MainActivity zum Speichern in Room
                            onGroupCreated(payload)

                            // Zustand wechseln
                            showQrCode = true
                        },
                        enabled = groupName.isNotBlank(), // Logik für Bedienbarkeit
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Erstellen & QR-Code zeigen")
                    }

                    // "Abbrechen" Knopf (Ausgegrauter Stil)
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Abbrechen")
                    }
                }
            } else {
                // --- ZUSTAND 2: QR-CODE ANZEIGE (Nutzt den Platz!) ---
                Text(
                    text = "Gruppe \"$groupName\" erstellt!",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF388E3C), // Dunkleres Grün
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Lasse diesen Code von einem Freund scannen, um ihn einzuladen.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.weight(0.5f))

                // --- QR-Code generieren (als Bitmap) ---
                // Wir nutzen remember, damit der Code nicht bei jedem Re-Draw neu generiert wird
                val qrCodeBitmap = remember(qrPayloadJson) {
                    val data: QrData = QrData.Text(qrPayloadJson)
                    val options = QrVectorOptions.Builder()
                        .setPadding(0.125f) // Ein wenig Rand
                        .build()
                    // 3. Drawable direkt über den Konstruktor erstellen (WICHTIG: QrCodeDrawable statt QrCodeDrawables)
                    val drawable = QrCodeDrawable(data, options)

                    // Drawable in ein Bitmap umwandeln (512x512 Pixel)
                    val size = 512
                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, size, size)
                    drawable.draw(canvas)

                    // Für Compose konvertieren
                    bitmap.asImageBitmap()
                }

                // Das QR-Code Bild anzeigen
                Image(
                    bitmap = qrCodeBitmap,
                    contentDescription = "QR-Code für Gruppenbeitritt",
                    modifier = Modifier
                        .size(280.dp) // Großer QR Code
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.weight(1f))

                // "Fertig" Button, um zurückzuspringen
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fertig")
                }
            }
        }
    }
}
