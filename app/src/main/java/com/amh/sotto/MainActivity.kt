package com.amh.sotto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amh.sotto.data.Phrase
import com.amh.sotto.data.PhraseRepository
import com.amh.sotto.data.SharedPreferencesPhraseRepository
import com.amh.sotto.data.SharedPreferencesVoiceSettingsRepository
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.ui.main.MainViewModel
import com.amh.sotto.util.ChimePlayer
import com.amh.sotto.ui.main.VoiceSettingsDialog
import com.amh.sotto.util.LocaleHelper
import com.amh.sotto.util.TranslationHelper
import sh.calvin.reorderable.*
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady by mutableStateOf(false)
    private var availableVoices by mutableStateOf<List<Voice>>(emptyList())
    private var availableTtsLanguages by mutableStateOf<Set<Locale>>(emptySet())
    private var latestVoiceSettings by mutableStateOf(VoiceSettings())
    private var currentLanguage by mutableStateOf(LocaleHelper.LANG_SYSTEM)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        currentLanguage = LocaleHelper.getLanguage(this)
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
                        availableTtsLanguages = availableTtsLanguages,
                        currentLanguage = currentLanguage,
                        onLanguageChanged = { newLang ->
                            LocaleHelper.setLanguage(this@MainActivity, newLang)
                            currentLanguage = newLang
                            recreate()
                        },
                        onSpeak = { phrase ->
                            if (latestVoiceSettings.showLanguageSwitcher && !phrase.spokenText.isNullOrBlank()) {
                                speakUtterance(phrase.spokenText, phrase.spokenLanguage)
                            } else {
                                speakUtterance(phrase.text, phrase.language)
                            }
                        },
                        onSpeakText = { text, lang ->
                            speakUtterance(text, lang)
                        },
                        onAddPhrase = { viewModel.addPhrase(it) },
                        onEditPhrase = { old, new -> viewModel.editPhrase(old, new) },
                        onDeletePhrase = { viewModel.deletePhrase(it) },
                        onMovePhrase = { fromPhrase, toPhrase -> viewModel.movePhrase(fromPhrase, toPhrase) },
                        onUpdateVoiceSettings = {
                            latestVoiceSettings = it
                            viewModel.updateVoiceSettings(it)
                        },
                        onTestVoice = { testSettings ->
                            if (ttsReady) {
                                if (testSettings.playAttentionChime) {
                                    playAttentionChime()
                                }
                                val effectiveLang = LocaleHelper.getEffectiveLanguage(this@MainActivity)
                                val testLocale = LocaleHelper.getLocaleForLanguage(effectiveLang)
                                tts?.setLanguage(testLocale)
                                applyVoiceSettings(testSettings, testLocale)
                                val testPhrase = getString(R.string.test_voice_phrase)
                                if (testSettings.playAttentionChime) {
                                    tts?.playSilentUtterance(280, TextToSpeech.QUEUE_FLUSH, null)
                                    tts?.speak(testPhrase, TextToSpeech.QUEUE_ADD, null, null)
                                } else {
                                    tts?.speak(testPhrase, TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun speakUtterance(text: String, language: String? = null) {
        if (ttsReady && text.isNotBlank()) {
            if (latestVoiceSettings.playAttentionChime) {
                playAttentionChime()
            }
            val effectiveLang = LocaleHelper.getEffectiveLanguage(this)
            val targetLocale = LocaleHelper.resolvePhraseLocale(language ?: LocaleHelper.LANG_AUTO, text, effectiveLang)
            tts?.setLanguage(targetLocale)
            applyVoiceSettings(latestVoiceSettings, targetLocale)
            if (latestVoiceSettings.playAttentionChime) {
                tts?.playSilentUtterance(280, TextToSpeech.QUEUE_FLUSH, null)
                tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
            } else {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    private fun playAttentionChime() {
        ChimePlayer.play(this)
    }

    private fun applyVoiceSettings(settings: VoiceSettings, targetLocale: Locale? = null) {
        if (settings.voiceName != null) {
            val targetVoice = tts?.voices?.firstOrNull { it.name == settings.voiceName }
            if (targetVoice != null && (targetLocale == null || targetVoice.locale.language.equals(targetLocale.language, ignoreCase = true))) {
                tts?.setVoice(targetVoice)
            }
        }

        tts?.setSpeechRate(settings.speechRate)
        tts?.setPitch(settings.speechPitch)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val effectiveLang = LocaleHelper.getEffectiveLanguage(this)
            val initialLocale = LocaleHelper.getLocaleForLanguage(effectiveLang)
            val result = tts?.setLanguage(initialLocale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = true
                val installedLocales = tts?.voices?.filter { voice ->
                    !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                }?.map { it.locale }?.toSet()
                
                availableTtsLanguages = installedLocales ?: tts?.availableLanguages ?: emptySet()
                tts?.voices?.let { voices ->
                    val localVoices = voices.filter { !it.isNetworkConnectionRequired }
                    val matchingLang = localVoices.filter {
                        it.locale.language.equals(initialLocale.language, ignoreCase = true) ||
                        (initialLocale.language == "id" && it.locale.language.equals("in", ignoreCase = true))
                    }
                    val listToShow = if (matchingLang.isNotEmpty()) matchingLang else localVoices
                    availableVoices = listToShow.sortedWith(compareBy({ it.locale.displayCountry }, { it.name }))
                }
                applyVoiceSettings(latestVoiceSettings, initialLocale)
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        TranslationHelper.close()
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
    phrases: List<Phrase>,
    ttsReady: Boolean,
    voiceSettings: VoiceSettings,
    availableVoices: List<Voice>,
    availableTtsLanguages: Set<Locale> = emptySet(),
    currentLanguage: String,
    onLanguageChanged: (String) -> Unit,
    onSpeak: (Phrase) -> Unit,
    onSpeakText: (String, String?) -> Unit = { _, _ -> },
    onAddPhrase: (Phrase) -> Unit,
    onEditPhrase: (Phrase, Phrase) -> Unit,
    onDeletePhrase: (Phrase) -> Unit,
    onMovePhrase: (Phrase, Phrase) -> Unit,
    onUpdateVoiceSettings: (VoiceSettings) -> Unit,
    onTestVoice: (VoiceSettings) -> Unit
) {
    var expandedPhrase by remember { mutableStateOf<Phrase?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by rememberSaveable { mutableStateOf(false) }
    var phraseToEdit by remember { mutableStateOf<Phrase?>(null) }
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var activeSpeechTarget by rememberSaveable { mutableStateOf("primary") }

    val allCategoryKey = "ALL"
    var selectedCategory by rememberSaveable { mutableStateOf(allCategoryKey) }

    val categories = remember {
        listOf(
            allCategoryKey to R.string.category_all,
            Phrase.CATEGORY_EMERGENCY to R.string.category_emergency,
            Phrase.CATEGORY_NEEDS to R.string.category_needs,
            Phrase.CATEGORY_SOCIAL to R.string.category_social,
            Phrase.CATEGORY_GENERAL to R.string.category_general
        )
    }

    val filteredPhrases = remember(phrases, selectedCategory) {
        if (selectedCategory == allCategoryKey) {
            phrases
        } else if (selectedCategory == Phrase.CATEGORY_EMERGENCY) {
            phrases.filter { it.isEmergency }
        } else {
            phrases.filter { !it.isEmergency && it.category == selectedCategory }
        }
    }

    val lazyGridState = rememberLazyGridState()
    val state = sh.calvin.reorderable.rememberReorderableLazyGridState(
        lazyGridState = lazyGridState,
        onMove = { from, to ->
            val fromPhrase = filteredPhrases.getOrNull(from.index)
            val toPhrase = filteredPhrases.getOrNull(to.index)
            if (fromPhrase != null && toPhrase != null) {
                onMovePhrase(fromPhrase, toPhrase)
            }
        }
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        if (voiceSettings.showLanguageSwitcher) {
                            val context = LocalContext.current
                            val primarySelected = activeSpeechTarget == "primary"
                            val primaryLabel = LocaleHelper.getLocaleForLanguage(
                                LocaleHelper.getEffectiveLanguage(context)
                            ).language.uppercase(Locale.ROOT)
                            val secondaryLabel = voiceSettings.secondaryLanguage.uppercase(Locale.ROOT)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(modifier = Modifier.padding(2.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (primarySelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        modifier = Modifier.clickable { activeSpeechTarget = "primary" }
                                    ) {
                                        Text(
                                            text = primaryLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (primarySelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (primarySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (!primarySelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        modifier = Modifier.clickable { activeSpeechTarget = "secondary" }
                                    ) {
                                        Text(
                                            text = secondaryLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (!primarySelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (!primarySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (isEditMode) {
                            TextButton(onClick = { showVoiceDialog = true }) {
                                Text(stringResource(R.string.action_voice), color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        TextButton(onClick = { isEditMode = !isEditMode }) {
                            Text(
                                text = if (isEditMode) stringResource(R.string.action_done) else stringResource(R.string.action_edit_list),
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
                                    text = stringResource(R.string.status_initializing),
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

                // Category Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(categories) { (catKey, strRes) ->
                        val isSelected = selectedCategory == catKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = catKey },
                            label = {
                                Text(
                                    text = stringResource(strRes),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (catKey == Phrase.CATEGORY_EMERGENCY) Color(0xFF4E342E) else MaterialTheme.colorScheme.surfaceVariant,
                                selectedLabelColor = if (catKey == Phrase.CATEGORY_EMERGENCY) Color(0xFFFFCC80) else MaterialTheme.colorScheme.onSurface,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            ),
                            border = if (catKey == Phrase.CATEGORY_EMERGENCY && isSelected) {
                                BorderStroke(1.dp, Color(0xFFFFB74D))
                            } else null
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                var quickText by rememberSaveable { mutableStateOf("") }
                val context = LocalContext.current
                val voiceInputPrompt = stringResource(R.string.cd_voice_input)

                val quickSpeechLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        if (!matches.isNullOrEmpty()) {
                            quickText = if (quickText.isBlank()) matches[0] else "$quickText ${matches[0]}"
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quickText,
                        onValueChange = { quickText = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.hint_quick_speak),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (quickText.isNotBlank()) {
                                    IconButton(onClick = { quickText = "" }) {
                                        Text(
                                            "✕",
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                                        val langTag = LocaleHelper.getLocaleForLanguage(effectiveLang).toLanguageTag()
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, voiceInputPrompt)
                                        }
                                        try {
                                            quickSpeechLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            // Ignore if speech recognizer not present
                                        }
                                    }
                                ) {
                                    Text("🎤", fontSize = 18.sp)
                                }
                            }
                        },
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (quickText.isNotBlank()) {
                                    onSpeak(Phrase(quickText.trim(), LocaleHelper.LANG_AUTO))
                                }
                            }
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )

                    // Fullscreen Button
                    IconButton(
                        onClick = {
                            if (quickText.isNotBlank()) {
                                expandedPhrase = Phrase(quickText.trim(), LocaleHelper.LANG_AUTO)
                            }
                        },
                        enabled = quickText.isNotBlank()
                    ) {
                        Text(
                            "⛶",
                            fontSize = 22.sp,
                            color = if (quickText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Speak Button
                    FilledIconButton(
                        onClick = {
                            if (quickText.isNotBlank()) {
                                onSpeak(Phrase(quickText.trim(), LocaleHelper.LANG_AUTO))
                            }
                        },
                        enabled = quickText.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("🔊", fontSize = 18.sp)
                    }
                }
            }
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
            items(
                items = filteredPhrases,
                key = { it.text },
                span = { phrase ->
                    if (phrase.isEmergency) GridItemSpan(2) else GridItemSpan(1)
                }
            ) { phrase ->
                ReorderableItem(state, key = phrase.text) { isDragging ->
                    val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                    var cardModifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (phrase.isEmergency) {
                                Modifier.wrapContentHeight()
                            } else {
                                Modifier.aspectRatio(1f)
                            }
                        )
                        .clip(RoundedCornerShape(16.dp))
                        
                    cardModifier = if (isEditMode) {
                        cardModifier
                            .longPressDraggableHandle()
                            .clickable { phraseToEdit = phrase }
                    } else {
                        cardModifier.combinedClickable(
                            onClick = {
                                if (voiceSettings.showLanguageSwitcher) {
                                    if (activeSpeechTarget == "secondary" && !phrase.spokenText.isNullOrBlank()) {
                                        onSpeakText(phrase.spokenText, phrase.spokenLanguage ?: voiceSettings.secondaryLanguage)
                                    } else {
                                        onSpeakText(phrase.text, phrase.language)
                                    }
                                } else {
                                    onSpeak(phrase)
                                }
                            },
                            onLongClick = { expandedPhrase = phrase }
                        )
                    }

                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(defaultElevation = elevation.value),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEditMode) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else if (phrase.isEmergency) {
                                Color(0xFF251E14)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = if (phrase.isEmergency) BorderStroke(1.5.dp, Color(0xFFFFB74D)) else null
                    ) {
                        if (phrase.isEmergency) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = Color(0x33FFB74D),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "🚨 " + stringResource(R.string.emergency_badge),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFFB74D),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = phrase.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFFFFE0B2),
                                    lineHeight = 24.sp
                                )
                                if (voiceSettings.showLanguageSwitcher && !phrase.spokenText.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("🗣️", fontSize = 12.sp)
                                        Text(
                                            text = phrase.spokenText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFFFCC80).copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = phrase.text,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = if (voiceSettings.showLanguageSwitcher && !phrase.spokenText.isNullOrBlank()) 3 else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (voiceSettings.showLanguageSwitcher && !phrase.spokenText.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = "🗣️",
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = phrase.spokenText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val availableLanguages = remember(availableTtsLanguages) {
        LocaleHelper.getAvailableLanguages(availableTtsLanguages)
    }

    // Voice Settings Dialog
    if (showVoiceDialog) {
        VoiceSettingsDialog(
            currentSettings = voiceSettings,
            currentLanguage = currentLanguage,
            availableLanguages = availableLanguages,
            onLanguageChanged = onLanguageChanged,
            onSettingsChanged = onUpdateVoiceSettings,
            onTestVoice = onTestVoice,
            onDismiss = { showVoiceDialog = false }
        )
    }

    // Modal Dialog for viewing/speaking giant text
    expandedPhrase?.let { phrase ->
        val isEmergency = phrase.isEmergency
        val hasSpokenText = voiceSettings.showLanguageSwitcher && !phrase.spokenText.isNullOrBlank()
        Dialog(
            onDismissRequest = { expandedPhrase = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isEmergency) Color(0xFF1E1710) else MaterialTheme.colorScheme.background,
                border = if (isEmergency) BorderStroke(2.dp, Color(0xFFFFB74D)) else null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isEmergency) {
                            Surface(
                                color = Color(0x33FFB74D),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Text(
                                    text = "🚨 " + stringResource(R.string.emergency_badge),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB74D),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { expandedPhrase = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEmergency) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight(Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = phrase.text,
                            fontSize = if (isEmergency && phrase.text.length > 60) 36.sp else 48.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isEmergency && phrase.text.length > 60) 44.sp else 56.sp,
                            color = if (isEmergency) Color(0xFFFFE0B2) else MaterialTheme.colorScheme.onBackground
                        )
                        if (hasSpokenText) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🗣️", fontSize = 20.sp)
                                    Text(
                                        text = phrase.spokenText,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    if (hasSpokenText) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onSpeakText(phrase.text, phrase.language) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = if (isEmergency) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFB74D),
                                        contentColor = Color(0xFF1E1710)
                                    )
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                val context = LocalContext.current
                                val targetLocale = LocaleHelper.resolvePhraseLocale(phrase.language, phrase.text, LocaleHelper.getEffectiveLanguage(context))
                                val langLabel = targetLocale.getDisplayLanguage(targetLocale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(targetLocale) else it.toString() }
                                Text(
                                    text = stringResource(R.string.action_speak_primary, langLabel),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { onSpeakText(phrase.spokenText, phrase.spokenLanguage) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                val context = LocalContext.current
                                val spokenLang = phrase.spokenLanguage ?: LocaleHelper.LANG_AUTO
                                val targetLocale = LocaleHelper.resolvePhraseLocale(spokenLang, phrase.spokenText, LocaleHelper.getEffectiveLanguage(context))
                                val langLabel = targetLocale.getDisplayLanguage(targetLocale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(targetLocale) else it.toString() }
                                Text(
                                    text = stringResource(R.string.action_speak_secondary, langLabel),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { onSpeak(phrase) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            colors = if (isEmergency) {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB74D),
                                    contentColor = Color(0xFF1E1710)
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Text(
                                stringResource(R.string.action_speak_aloud),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Dialog with Voice Input and Category selection
    if (showAddDialog || phraseToEdit != null) {
        val context = LocalContext.current
        val voiceInputPrompt = stringResource(R.string.cd_voice_input)
        var textValue by remember(phraseToEdit, showAddDialog) { mutableStateOf(phraseToEdit?.text ?: "") }
        var spokenTextValue by remember(phraseToEdit, showAddDialog) { mutableStateOf(phraseToEdit?.spokenText ?: "") }
        var spokenLangValue by remember(phraseToEdit, showAddDialog) { mutableStateOf(phraseToEdit?.spokenLanguage ?: LocaleHelper.LANG_AUTO) }
        var isSpokenTextExpanded by remember(phraseToEdit, showAddDialog) { mutableStateOf(!phraseToEdit?.spokenText.isNullOrBlank()) }
        var isTranslating by remember { mutableStateOf(false) }
        var isModelDownloading by remember { mutableStateOf(false) }
        var showModelDownloadConfirmDialog by remember { mutableStateOf(false) }
        var translationError by remember { mutableStateOf<String?>(null) }
        var selectedCat by remember(phraseToEdit, showAddDialog) {
            mutableStateOf(
                if (phraseToEdit?.isEmergency == true) Phrase.CATEGORY_EMERGENCY
                else (phraseToEdit?.category ?: Phrase.CATEGORY_GENERAL)
            )
        }
        val isEditModeDialog = phraseToEdit != null

        fun executeTranslation(targetLang: String, sourceLang: String) {
            translationError = null
            TranslationHelper.translate(
                text = textValue.trim(),
                sourceLangCode = sourceLang,
                targetLangCode = targetLang,
                onProgress = { isTranslating = it },
                onSuccess = { translated ->
                    spokenTextValue = translated
                    spokenLangValue = targetLang
                    isModelDownloading = false
                },
                onError = {
                    isModelDownloading = false
                    translationError = context.getString(R.string.error_translation_failed)
                }
            )
        }

        if (showModelDownloadConfirmDialog) {
            val targetLangName = LocaleHelper.getLanguageDisplayName(voiceSettings.secondaryLanguage)
            AlertDialog(
                onDismissRequest = { showModelDownloadConfirmDialog = false },
                title = { Text(stringResource(R.string.dialog_download_model_title)) },
                text = {
                    Text(stringResource(R.string.dialog_download_model_message, targetLangName))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showModelDownloadConfirmDialog = false
                            isModelDownloading = true
                            val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                            val sourceLang = LocaleHelper.resolvePhraseLocale(LocaleHelper.LANG_AUTO, textValue, effectiveLang).language
                            val targetLang = voiceSettings.secondaryLanguage
                            executeTranslation(targetLang, sourceLang)
                        }
                    ) {
                        Text(stringResource(R.string.action_download_and_translate))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showModelDownloadConfirmDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        val speechLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!matches.isNullOrEmpty()) {
                    textValue = if (textValue.isBlank()) matches[0] else "$textValue ${matches[0]}"
                }
            }
        }

        val spokenSpeechLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!matches.isNullOrEmpty()) {
                    spokenTextValue = if (spokenTextValue.isBlank()) matches[0] else "$spokenTextValue ${matches[0]}"
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                phraseToEdit = null
            },
            title = {
                Text(
                    if (isEditModeDialog) {
                        stringResource(R.string.dialog_edit_phrase_title)
                    } else {
                        stringResource(R.string.dialog_add_phrase_title)
                    }
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Phrase Text Field with Voice Input (Microphone button)
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = { Text(stringResource(R.string.label_phrase_text)) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                                    val langTag = LocaleHelper.getLocaleForLanguage(effectiveLang).toLanguageTag()
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, voiceInputPrompt)
                                    }
                                    try {
                                        speechLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        // Ignore if speech recognizer not present
                                    }
                                }
                            ) {
                                Text("🎤", fontSize = 20.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2
                    )

                    if (voiceSettings.showLanguageSwitcher) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Expandable Alternate Spoken Text
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isSpokenTextExpanded = !isSpokenTextExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🗣️", fontSize = 14.sp)
                                Text(
                                    text = stringResource(R.string.label_spoken_text),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = if (isSpokenTextExpanded) "▲" else "▼",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (isSpokenTextExpanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.label_spoken_language) + ": ${LocaleHelper.getLanguageDisplayName(voiceSettings.secondaryLanguage)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (textValue.isNotBlank() && !isTranslating && !isModelDownloading) {
                                            translationError = null
                                            val effectiveLang = LocaleHelper.getEffectiveLanguage(context)
                                            val sourceLang = LocaleHelper.resolvePhraseLocale(LocaleHelper.LANG_AUTO, textValue, effectiveLang).language
                                            val targetLang = voiceSettings.secondaryLanguage
                                            TranslationHelper.isModelDownloaded(targetLang) { isDownloaded ->
                                                if (isDownloaded) {
                                                    executeTranslation(targetLang, sourceLang)
                                                } else {
                                                    showModelDownloadConfirmDialog = true
                                                }
                                            }
                                        }
                                    },
                                    enabled = textValue.isNotBlank() && !isTranslating && !isModelDownloading,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    if (isTranslating || isModelDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isModelDownloading) stringResource(R.string.status_downloading_model) else stringResource(R.string.status_translating),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    } else {
                                        Text(
                                            text = "🌐 " + stringResource(R.string.action_auto_translate),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }

                            if (translationError != null) {
                                Text(
                                    text = translationError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFEF5350),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            OutlinedTextField(
                                value = spokenTextValue,
                                onValueChange = { spokenTextValue = it },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.hint_spoken_text),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val targetLang = if (spokenLangValue != LocaleHelper.LANG_AUTO) spokenLangValue else voiceSettings.secondaryLanguage
                                            val langTag = LocaleHelper.getLocaleForLanguage(targetLang).toLanguageTag()
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, voiceInputPrompt)
                                            }
                                            try {
                                                spokenSpeechLauncher.launch(intent)
                                            } catch (e: Exception) {
                                                // Ignore if speech recognizer not present
                                            }
                                        }
                                    ) {
                                        Text("🎤", fontSize = 20.sp)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false,
                                minLines = 2
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Selection
                    Text(
                        text = stringResource(R.string.label_category),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val dialogCategories = listOf(
                        Phrase.CATEGORY_GENERAL to R.string.category_general,
                        Phrase.CATEGORY_NEEDS to R.string.category_needs,
                        Phrase.CATEGORY_SOCIAL to R.string.category_social,
                        Phrase.CATEGORY_EMERGENCY to R.string.category_emergency
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dialogCategories.forEach { (catKey, strRes) ->
                            val isSelected = selectedCat == catKey
                            val isEmergencyCat = catKey == Phrase.CATEGORY_EMERGENCY
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCat = catKey },
                                label = {
                                    Text(
                                        text = if (isEmergencyCat) "🚨 " + stringResource(strRes) else stringResource(strRes),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isEmergencyCat) Color(0xFF4E342E) else MaterialTheme.colorScheme.surfaceVariant,
                                    selectedLabelColor = if (isEmergencyCat) Color(0xFFFFCC80) else MaterialTheme.colorScheme.onSurface
                                ),
                                border = if (isEmergencyCat && isSelected) {
                                    BorderStroke(1.dp, Color(0xFFFFB74D))
                                } else null
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (textValue.isNotBlank()) {
                        val isEmergency = selectedCat == Phrase.CATEGORY_EMERGENCY
                        val newPhrase = Phrase(
                            text = textValue.trim(),
                            spokenText = if (voiceSettings.showLanguageSwitcher) spokenTextValue.trim().takeIf { it.isNotBlank() } else phraseToEdit?.spokenText,
                            spokenLanguage = if (voiceSettings.showLanguageSwitcher) spokenLangValue.takeIf { spokenTextValue.isNotBlank() } else phraseToEdit?.spokenLanguage,
                            language = LocaleHelper.LANG_AUTO,
                            isEmergency = isEmergency,
                            category = selectedCat
                        )
                        if (isEditModeDialog) {
                            onEditPhrase(phraseToEdit!!, newPhrase)
                        } else {
                            onAddPhrase(newPhrase)
                        }
                    }
                    showAddDialog = false
                    phraseToEdit = null
                }) {
                    Text(stringResource(R.string.action_save))
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
                            Text(stringResource(R.string.action_delete), color = Color(0xFFEF5350))
                        }
                    }
                    TextButton(onClick = {
                        showAddDialog = false
                        phraseToEdit = null
                    }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        )
    }
}
