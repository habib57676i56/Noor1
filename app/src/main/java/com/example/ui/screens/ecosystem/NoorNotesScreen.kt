package com.example.ui.screens.ecosystem

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.catalog.SurahCatalog
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.viewmodel.EcosystemViewModel

@Composable
fun NoorNotesScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current

    val collections by viewModel.noteCollections.collectAsStateWithLifecycle()
    val notes by viewModel.personalNotes.collectAsStateWithLifecycle()

    var selectedCollectionId by remember { mutableStateOf<Long?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddCollectionDialog by remember { mutableStateOf(false) }

    val filteredNotes = remember(notes, selectedCollectionId, favoritesOnly) {
        notes.filter { n ->
            val colOk = selectedCollectionId == null || n.collectionId == selectedCollectionId
            val favOk = !favoritesOnly || n.isFavorite
            colOk && favOk
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "ملاحظات القرآن الشخصية", "NOOR Personal Quran Notes"),
            subtitle = L10n.pick(lang, "دوّن خواطرك ومراجعك على الآيات — النص القرآني العثماني محمي للقراءة فقط", "Attach Personal Notes to Ayahs — Uthmani Text Remains Strictly Read-Only"),
            onBack = onBack,
            trailing = {
                IconButton(onClick = { showAddCollectionDialog = true }) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = "New Collection", tint = noorColors.goldAccent)
                }
            }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                LiquidGlassChip(
                    L10n.pick(lang, "الكل (${notes.size})", "All (${notes.size})"),
                    selectedCollectionId == null && !favoritesOnly,
                    { selectedCollectionId = null; favoritesOnly = false }
                )
            }
            item {
                LiquidGlassChip(
                    L10n.pick(lang, "المفضلة", "Favorites"),
                    favoritesOnly,
                    { favoritesOnly = !favoritesOnly }
                )
            }
            items(collections, key = { it.id }) { col ->
                LiquidGlassChip(
                    col.name,
                    selectedCollectionId == col.id,
                    { selectedCollectionId = if (selectedCollectionId == col.id) null else col.id }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Button(
                    onClick = { showAddNoteDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("add_quran_note_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(L10n.pick(lang, "إضافة ملاحظة على آية", "Attach Note to an Ayah"))
                }
            }

            if (filteredNotes.isEmpty()) {
                item {
                    LiquidGlassCard {
                        Text(L10n.pick(lang, "لا توجد ملاحظات بعد. يمكنك إضافة خواطر التدبر ومواضع الوقف والآيات المؤثرة في مجموعات منظمة.", "No personal notes yet. Organize reflections, favorite Ayahs, and reading references here."))
                    }
                }
            }

            items(filteredNotes, key = { it.id }) { note ->
                val surah = SurahCatalog.findSurah(note.surahNumber)
                val colName = collections.firstOrNull { it.id == note.collectionId }?.name
                LiquidGlassCard(goldBorder = note.isFavorite) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "سورة ${surah.nameAr} — آية ${note.ayahNumber}" + (if (colName != null) " · [$colName]" else ""),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = noorColors.goldAccent
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(note.noteText, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { viewModel.toggleNoteFavorite(note) }) {
                            Icon(
                                if (note.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (note.isFavorite) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.deletePersonalNote(note.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Note", tint = SemanticError)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { onOpenSurah(note.surahNumber) }) {
                        Icon(Icons.Rounded.MenuBook, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("الانتقال إلى سورة ${surah.nameAr}")
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        var surahText by remember { mutableStateOf("1") }
        var ayahText by remember { mutableStateOf("1") }
        var noteText by remember { mutableStateOf("") }
        var isFav by remember { mutableStateOf(false) }
        var chosenCol by remember { mutableStateOf(selectedCollectionId) }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("إضافة ملاحظة شخصية على آية") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = surahText, onValueChange = { surahText = it.filter(Char::isDigit) }, label = { Text("رقم السورة") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = ayahText, onValueChange = { ayahText = it.filter(Char::isDigit) }, label = { Text("رقم الآية") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text("ملاحظتك / فائدة التدبر") }, minLines = 3)
                    LiquidGlassChip("إضافة للمفضلة", isFav, { isFav = !isFav })
                    if (collections.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(collections) { c ->
                                LiquidGlassChip(c.name, chosenCol == c.id, { chosenCol = if (chosenCol == c.id) null else c.id })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.savePersonalNote(
                        surahNumber = surahText.toIntOrNull() ?: 1,
                        ayahNumber = ayahText.toIntOrNull() ?: 1,
                        noteText = noteText,
                        collectionId = chosenCol,
                        isFavorite = isFav
                    )
                    showAddNoteDialog = false
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showAddNoteDialog = false }) { Text("إلغاء") } }
        )
    }

    if (showAddCollectionDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCollectionDialog = false },
            title = { Text("إنشاء مجموعة ملاحظات") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المجموعة (مثلاً: آيات الصبر)") }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    viewModel.createNoteCollection(name)
                    showAddCollectionDialog = false
                }) { Text("إنشاء") }
            },
            dismissButton = { TextButton(onClick = { showAddCollectionDialog = false }) { Text("إلغاء") } }
        )
    }
}
