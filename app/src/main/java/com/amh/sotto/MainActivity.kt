package com.amh.sotto

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amh.sotto.data.PhraseRepository
import com.amh.sotto.data.SharedPreferencesPhraseRepository
import com.amh.sotto.data.SharedPreferencesVoiceSettingsRepository
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.ui.main.MainViewModel
import com.amh.sotto.ui.main.VoiceSettingsDialog
import sh.calvin.reorderable.*
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady by mutableStateOf(false)
    private var availableVoices by mutableStateOf<List<Voice>>(emptyList())
    private var latestVoiceSettings by mutableStateOf(VoiceSettings())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tts = TextToSpeech(this, this)

        val phraseRepository = SharedPreferencesPhraseRepository(applicationContext)
        val voiceSettingsRepository = SharedPreferencesVoiceSettingsRepository(applicationContext)
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(phraseRepository, voiceSettingsRepository) as T
            }
        })[MainViewModel::class.java]

        setContent {
            SottoAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val phrases by viewModel.phrases.collectAsState()
                    val voiceSettings by viewModel.voiceSettings.collectAsState()

                    LaunchedEffect(voiceSettings, ttsReady) {
                        latestVoiceSettings = voiceSettings
                        if (ttsReady) {
                            applyVoiceSettings(voiceSettings)
                        }
                    }

                    SottoApp(
                        phrases = phrases,
                        ttsReady = ttsReady,
                        voiceSettings = voiceSettings,
                        availableVoices = availableVoices,
                        onSpeak = { text ->
                            if (ttsReady) {
                                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        },
                        onAddPhrase = { viewModel.addPhrase(it) },
                        onEditPhrase = { old, new -> viewModel.editPhrase(old, new) },
                        onDeletePhrase = { viewModel.deletePhrase(it) },
                        onMovePhrase = { from, to -> viewModel.movePhrase(from, to) },
                        onUpdateVoiceSettings = { viewModel.updateVoiceSettings(it) },
                        onTestVoice = {
                            if (ttsReady) {
                                tts?.speak("Hello, this is my speaking voice.", TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun applyVoiceSettings(settings: VoiceSettings) {
        tts?.setSpeechRate(settings.speechRate)
        tts?.setPitch(settings.speechPitch)
        if (settings.voiceName != null) {
            val targetVoice = tts?.voices?.firstOrNull { it.name == settings.voiceName }
            if (targetVoice != null) {
                tts?.setVoice(targetVoice)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = true
                tts?.voices?.let { voices ->
                    val currentLang = tts?.voice?.locale?.language ?: Locale.getDefault().language
                    val localVoices = voices.filter { !it.isNetworkConnectionRequired }
                    val matchingLang = localVoices.filter { it.locale.language.equals(currentLang, ignoreCase = true) }
                    val listToShow = if (matchingLang.isNotEmpty()) matchingLang else localVoices
                    availableVoices = listToShow.sortedWith(compareBy({ it.locale.displayCountry }, { it.name }))
                }
                applyVoiceSettings(latestVoiceSettings)
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun SottoAppTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFFE0E0E0),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onPrimary = Color(0xFF121212),
        onBackground = Color(0xFFE0E0E0),
        onSurface = Color(0xFFE0E0E0)
    )
    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SottoApp(
    phrases: List<String>,
    ttsReady: Boolean,
    voiceSettings: VoiceSettings,
    availableVoices: List<Voice>,
    onSpeak: (String) -> Unit,
    onAddPhrase: (String) -> Unit,
    onEditPhrase: (String, String) -> Unit,
    onDeletePhrase: (String) -> Unit,
    onMovePhrase: (Int, Int) -> Unit,
    onUpdateVoiceSettings: (VoiceSettings) -> Unit,
    onTestVoice: () -> Unit
) {
    var expandedPhrase by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var phraseToEdit by remember { mutableStateOf<String?>(null) }
    var isEditMode by remember { mutableStateOf(false) }

    val lazyGridState = rememberLazyGridState()
    val state = sh.calvin.reorderable.rememberReorderableLazyGridState(
        lazyGridState = lazyGridState,
        onMove = { from, to ->
            onMovePhrase(from.index, to.index)
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sotto") },
                actions = {
                    if (isEditMode) {
                        TextButton(onClick = { showVoiceDialog = true }) {
                            Text("Voice", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    TextButton(onClick = { isEditMode = !isEditMode }) {
                        Text(
                            text = if (isEditMode) "Done" else "Edit List",
                            color = if (isEditMode) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!ttsReady) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFFFC107),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Initializing",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (isEditMode) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = lazyGridState,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(phrases, key = { it }) { phrase ->
                ReorderableItem(state, key = phrase) { isDragging ->
                    val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                    var cardModifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        
                    cardModifier = if (isEditMode) {
                        cardModifier
                            .longPressDraggableHandle()
                            .clickable { phraseToEdit = phrase }
                    } else {
                        cardModifier.combinedClickable(
                            onClick = { onSpeak(phrase) },
                            onLongClick = { expandedPhrase = phrase }
                        )
                    }

                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(defaultElevation = elevation.value),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEditMode) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = phrase,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Voice Settings Dialog
    if (showVoiceDialog) {
        VoiceSettingsDialog(
            currentSettings = voiceSettings,
            availableVoices = availableVoices,
            onSettingsChanged = onUpdateVoiceSettings,
            onTestVoice = onTestVoice,
            onDismiss = { showVoiceDialog = false }
        )
    }

    // Modal Dialog for viewing/speaking giant text
    expandedPhrase?.let { phrase ->
        Dialog(
            onDismissRequest = { expandedPhrase = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = phrase,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 56.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f).wrapContentHeight(Alignment.CenterVertically)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { expandedPhrase = null },
                            modifier = Modifier.weight(1f).height(64.dp)
                        ) {
                            Text("Dismiss", fontSize = 24.sp)
                        }
                        Button(
                            onClick = { onSpeak(phrase) },
                            modifier = Modifier.weight(1f).height(64.dp)
                        ) {
                            Text("Speak Aloud", fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog || phraseToEdit != null) {
        var textValue by remember { mutableStateOf(phraseToEdit ?: "") }
        val isEditModeDialog = phraseToEdit != null

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                phraseToEdit = null
            },
            title = { Text(if (isEditModeDialog) "Edit Phrase" else "Add Phrase") },
            text = {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Phrase text") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (textValue.isNotBlank()) {
                        if (isEditModeDialog) {
                            onEditPhrase(phraseToEdit!!, textValue.trim())
                        } else {
                            onAddPhrase(textValue.trim())
                        }
                    }
                    showAddDialog = false
                    phraseToEdit = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isEditModeDialog) {
                        TextButton(onClick = {
                            onDeletePhrase(phraseToEdit!!)
                            showAddDialog = false
                            phraseToEdit = null
                        }) {
                            Text("Delete", color = Color(0xFFEF5350))
                        }
                    }
                    TextButton(onClick = {
                        showAddDialog = false
                        phraseToEdit = null
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
