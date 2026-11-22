package com.example.ui.screens.ecosystem

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.db.LibraryBookEntity
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassChip
import com.example.ui.components.NoorScreenHeader
import com.example.ui.components.NoorSectionHeader
import com.example.ui.components.WarmIvoryParchmentCard
import com.example.ui.i18n.L10n
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryInk
import com.example.ui.theme.LocalNoorColors
import com.example.ui.theme.SemanticError
import com.example.viewmodel.EcosystemViewModel

@Composable
fun IslamicLibraryScreen(
    viewModel: EcosystemViewModel,
    onBack: () -> Unit,
    initialCategory: String = "ALL"
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    val context = LocalContext.current

    val books by viewModel.libraryBooks.collectAsStateWithLifecycle()
    val allNotes by viewModel.libraryNotes.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var searchQuery by remember { mutableStateOf("") }
    var readingBook by remember { mutableStateOf<LibraryBookEntity?>(null) }

    // SAF zero-permission file picker for authorized TXT/JSON Islamic documents
    val importDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (text.isNotBlank()) {
                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "كتاب مستورد"
                    viewModel.addOrImportLibraryBook(
                        category = if (selectedCategory == "ALL") "LESSONS" else selectedCategory,
                        titleAr = fileName.substringBeforeLast('.'),
                        authorAr = "وثيقة محلية مستوردة",
                        summaryAr = text.take(140),
                        contentText = text,
                        sourceRef = "ملف محلي مصرح به ($fileName)",
                        isImported = true
                    )
                }
            }
        }
    }

    if (readingBook != null) {
        BackHandler { readingBook = null }
        val currentBook = books.firstOrNull { it.id == readingBook?.id } ?: readingBook!!
        BookReaderView(
            book = currentBook,
            notes = allNotes.filter { it.bookId == currentBook.id },
            onBack = { readingBook = null },
            onToggleBookmark = { viewModel.toggleBookBookmark(currentBook.id, !currentBook.isBookmarked) },
            onMarkProgress = { offset -> viewModel.updateBookReadProgress(currentBook.id, offset) },
            onAddNote = { text -> viewModel.addLibraryNote(currentBook.id, text) },
            onDeleteNote = { id -> viewModel.deleteLibraryNote(id) }
        )
        return
    }

    val categories = listOf(
        "ALL" to L10n.pick(lang, "الكل", "All"),
        "TAFSEER" to L10n.pick(lang, "التفسير", "Tafseer"),
        "HADITH" to L10n.pick(lang, "الحديث", "Hadith"),
        "FIQH" to L10n.pick(lang, "الفقه", "Fiqh"),
        "AQEEDAH" to L10n.pick(lang, "العقيدة", "Aqeedah"),
        "SEERAH" to L10n.pick(lang, "السيرة", "Seerah"),
        "PROPHETS" to L10n.pick(lang, "قصص الأنبياء", "Prophets"),
        "COMPANIONS" to L10n.pick(lang, "الصحابة", "Companions"),
        "LESSONS" to L10n.pick(lang, "دروس إسلامية", "Lessons")
    )

    val filteredBooks = remember(books, selectedCategory, searchQuery) {
        val q = searchQuery.trim().lowercase()
        books.filter { b ->
            val catOk = selectedCategory == "ALL" || b.category.equals(selectedCategory, ignoreCase = true)
            val queryOk = q.isEmpty() ||
                b.titleAr.lowercase().contains(q) ||
                b.titleEn.lowercase().contains(q) ||
                b.authorAr.lowercase().contains(q) ||
                b.contentText.lowercase().contains(q)
            catOk && queryOk
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, "مكتبة نور الإسلامية", "NOOR Islamic Library"),
            subtitle = L10n.pick(lang, "تفسير، حديث، فقه، عقيدة، سيرة ودروس تعمل ١٠٠٪ بدون إنترنت", "100% Offline Searchable Islamic Library"),
            onBack = onBack,
            trailing = {
                IconButton(
                    onClick = { importDocLauncher.launch(arrayOf("text/plain", "application/json", "*/*")) },
                    modifier = Modifier.testTag("library_import_btn")
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = "Import Local Book", tint = noorColors.goldAccent)
                }
            }
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text(L10n.pick(lang, "ابحث بالعنوان أو المؤلف أو داخل النصوص...", "Search title, author, or inside text...")) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("library_search_input")
        )

        Spacer(Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { (key, label) ->
                LiquidGlassChip(label, selectedCategory == key, { selectedCategory = key })
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredBooks, key = { it.id }) { book ->
                val progress = if (book.totalLength > 0) {
                    (book.lastReadCharOffset.toFloat() / book.totalLength).coerceIn(0f, 1f)
                } else 0f
                LiquidGlassCard(onClick = { readingBook = book }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = L10n.pick(lang, book.titleAr, book.titleEn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${L10n.pick(lang, book.authorAr, book.authorEn)} · ${book.category}",
                                style = MaterialTheme.typography.labelMedium,
                                color = noorColors.goldAccent
                            )
                            if (book.summaryAr.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(book.summaryAr, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                        }
                        IconButton(onClick = { viewModel.toggleBookBookmark(book.id, !book.isBookmarked) }) {
                            Icon(
                                if (book.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = noorColors.goldAccent
                            )
                        }
                        if (book.isImported) {
                            IconButton(onClick = { viewModel.deleteLibraryBook(book.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = SemanticError)
                            }
                        }
                    }
                    if (progress > 0f) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = GoldPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun BookReaderView(
    book: LibraryBookEntity,
    notes: List<com.example.data.local.db.LibraryNoteEntity>,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onMarkProgress: (Int) -> Unit,
    onAddNote: (String) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    val lang = LocalAppLanguage.current
    val noorColors = LocalNoorColors.current
    var noteInput by remember { mutableStateOf("") }
    var inTextQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        NoorScreenHeader(
            title = L10n.pick(lang, book.titleAr, book.titleEn),
            subtitle = "${book.authorAr} · ${book.sourceReference}",
            onBack = onBack,
            trailing = {
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        if (book.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = noorColors.goldAccent
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = inTextQuery,
                onValueChange = { inTextQuery = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("ابحث داخل هذا الكتاب...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val paragraphs = remember(book.contentText, inTextQuery) {
                val all = book.contentText.split(Regex("\\r?\\n\\s*\\r?\\n")).filter { it.isNotBlank() }
                if (inTextQuery.isBlank()) all else all.filter { it.contains(inTextQuery, ignoreCase = true) }
            }

            WarmIvoryParchmentCard {
                paragraphs.forEach { para ->
                    Text(
                        text = para,
                        fontFamily = AmiriFontFamily,
                        fontSize = 19.sp,
                        lineHeight = 32.sp,
                        color = IvoryInk
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onMarkProgress((book.totalLength / 2).coerceAtLeast(1)) },
                    modifier = Modifier.weight(1f)
                ) { Text("حفظ تقدم ٥٠٪") }
                Button(
                    onClick = { onMarkProgress(book.totalLength.coerceAtLeast(1)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                ) { Text("إتمام القراءة ١٠٠٪") }
            }

            NoorSectionHeader(title = "ملاحظاتي على الكتاب (${notes.size})")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = { Text("أضف فائدة أو ملاحظة شخصية...") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (noteInput.isNotBlank()) {
                            onAddNote(noteInput)
                            noteInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = EmeraldDeep)
                ) { Text("حفظ") }
            }

            notes.forEach { n ->
                LiquidGlassCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(n.noteText, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { onDeleteNote(n.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Note", tint = SemanticError)
                        }
                    }
                }
            }
        }
    }
}
