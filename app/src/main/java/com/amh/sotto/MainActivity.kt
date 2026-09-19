package com.amh.sotto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.amh.sotto.data.VoiceGender
import com.amh.sotto.data.VoiceSettings
import com.amh.sotto.ui.main.MainViewModel
import com.amh.sotto.ui.main.VoiceSettingsDialog
import com.amh.sotto.util.LocaleHelper
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
                            if (ttsReady) {
                                val effectiveLang = LocaleHelper.getEffectiveLanguage(this@MainActivity)
                                val targetLocale = LocaleHelper.resolvePhraseLocale(phrase.language, phrase.text, effectiveLang)
                                tts?.setLanguage(targetLocale)
                                applyVoiceSettings(latestVoiceSettings, targetLocale)
                                tts?.speak(phrase.text, TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        },
                        onAddPhrase = { viewModel.addPhrase(it) },
                        onEditPhrase = { old, new -> viewModel.editPhrase(old, new) },
                        onDeletePhrase = { viewModel.deletePhrase(it) },
                        onMovePhrase = { from, to -> viewModel.movePhrase(from, to) },
                        onUpdateVoiceSettings = {
                            latestVoiceSettings = it
                            viewModel.updateVoiceSettings(it)
                        },
                        onTestVoice = { testSettings ->
                            if (ttsReady) {
                                val effectiveLang = LocaleHelper.getEffectiveLanguage(this@MainActivity)
                                val testLocale = LocaleHelper.getLocaleForLanguage(effectiveLang)
                                tts?.setLanguage(testLocale)
                                applyVoiceSettings(testSettings, testLocale)
                                val testPhrase = getString(R.string.test_voice_phrase)
                                tts?.speak(testPhrase, TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun findVoiceForGender(gender: VoiceGender, targetLocale: Locale?): Pair<Voice?, Boolean> {
        if (gender == VoiceGender.DEFAULT) return Pair(null, false)
        val voices = tts?.voices ?: return Pair(null, false)

        val candidateVoices = if (targetLocale != null) {
            val matchingLang = voices.filter { voice ->
                val voiceLang = voice.locale.language
                val targetLang = targetLocale.language
                voiceLang.equals(targetLang, ignoreCase = true) ||
                (targetLang == "id" && voiceLang.equals("in", ignoreCase = true)) ||
                (targetLang == "in" && voiceLang.equals("id", ignoreCase = true))
            }
            if (matchingLang.isNotEmpty()) matchingLang else voices.toList()
        } else {
            voices.toList()
        }

        val installedVoices = candidateVoices.filter {
            !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }
        val pool = if (installedVoices.isNotEmpty()) installedVoices else candidateVoices

        val sortedPool = pool.sortedWith(
            compareBy<Voice>(
                { it.isNetworkConnectionRequired },
                { if (targetLocale != null && it.locale.country.equals(targetLocale.country, ignoreCase = true)) 0 else 1 }
            )
        )

        val maleCodes = listOf("sfg", "iob", "iol", "fis", "gda", "dft", "eed", "ccc", "ald", "gcl", "omj", "baf", "zha", "wls", "olb")
        val femaleCodes = listOf("tpf", "tpc", "iom", "rjs", "aub", "dfz", "izg", "efa", "lga", "apa", "bmd", "sfd")

        fun isMale(voice: Voice): Boolean {
            val name = voice.name
            if (name.contains("female", ignoreCase = true)) return false
            if (name.contains("male", ignoreCase = true)) return true
            if (name.contains("_m0", ignoreCase = true) || name.contains("-m0", ignoreCase = true) || name.contains("m00", ignoreCase = true) || name.contains("m01", ignoreCase = true)) return true
            if (maleCodes.any { code -> name.contains("-x-$code", ignoreCase = true) || name.contains("#$code", ignoreCase = true) }) return true
            if (voice.features.any { (it.contains("gender:male", ignoreCase = true) || it.equals("male", ignoreCase = true)) && !it.contains("female", ignoreCase = true) }) return true
            return false
        }

        fun isFemale(voice: Voice): Boolean {
            val name = voice.name
            if (name.contains("female", ignoreCase = true)) return true
            if (name.contains("_f0", ignoreCase = true) || name.contains("-f0", ignoreCase = true) || name.contains("f00", ignoreCase = true) || name.contains("f01", ignoreCase = true)) return true
            if (femaleCodes.any { code -> name.contains("-x-$code", ignoreCase = true) || name.contains("#$code", ignoreCase = true) }) return true
            if (voice.features.any { it.contains("gender:female", ignoreCase = true) || it.equals("female", ignoreCase = true) }) return true
            return false
        }

        return when (gender) {
            VoiceGender.FEMALE -> {
                val explicit = sortedPool.firstOrNull { isFemale(it) }
                if (explicit != null) {
                    Pair(explicit, true)
                } else if (sortedPool.isNotEmpty()) {
                    Pair(sortedPool.first(), false)
                } else {
                    Pair(null, false)
                }
            }
            VoiceGender.MALE -> {
                val explicit = sortedPool.firstOrNull { isMale(it) }
                if (explicit != null) {
                    Pair(explicit, true)
                } else if (sortedPool.size >= 2) {
                    Pair(sortedPool[1], false)
                } else {
                    Pair(null, false)
                }
            }
            VoiceGender.DEFAULT -> Pair(null, false)
        }
    }

    private fun applyVoiceSettings(settings: VoiceSettings, targetLocale: Locale? = null) {
        val (genderVoice, isExplicitVoice) = findVoiceForGender(settings.voiceGender, targetLocale)
        if (genderVoice != null) {
            tts?.setVoice(genderVoice)
        } else if (settings.voiceName != null) {
            val targetVoice = tts?.voices?.firstOrNull { it.name == settings.voiceName }
            if (targetVoice != null && (targetLocale == null || targetVoice.locale.language.equals(targetLocale.language, ignoreCase = true))) {
                tts?.setVoice(targetVoice)
            }
        } else {
            tts?.defaultVoice?.let { tts?.setVoice(it) }
        }

        val effectivePitch = when (settings.voiceGender) {
            VoiceGender.MALE -> {
                if (isExplicitVoice) {
                    settings.speechPitch
                } else {
                    (settings.speechPitch * 0.78f).coerceIn(0.5f, 2.0f)
                }
            }
            VoiceGender.FEMALE -> {
                if (isExplicitVoice) {
                    settings.speechPitch
                } else {
                    (settings.speechPitch * 1.15f).coerceIn(0.5f, 2.0f)
                }
            }
            VoiceGender.DEFAULT -> settings.speechPitch
        }

        tts?.setSpeechRate(settings.speechRate)
        tts?.setPitch(effectivePitch)
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
    onAddPhrase: (Phrase) -> Unit,
    onEditPhrase: (Phrase, Phrase) -> Unit,
    onDeletePhrase: (Phrase) -> Unit,
    onMovePhrase: (Int, Int) -> Unit,
    onUpdateVoiceSettings: (VoiceSettings) -> Unit,
    onTestVoice: (VoiceSettings) -> Unit
) {
    var expandedPhrase by remember { mutableStateOf<Phrase?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by rememberSaveable { mutableStateOf(false) }
    var phraseToEdit by remember { mutableStateOf<Phrase?>(null) }
    var isEditMode by rememberSaveable { mutableStateOf(false) }

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
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
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
            items(phrases, key = { it.text }) { phrase ->
                ReorderableItem(state, key = phrase.text) { isDragging ->
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
                                text = phrase.text,
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
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { expandedPhrase = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = "✕",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Text(
                        text = phrase.text,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 56.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight(Alignment.CenterVertically)
                    )

                    Button(
                        onClick = { onSpeak(phrase) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Text(stringResource(R.string.action_speak_aloud), fontSize = 24.sp)
                    }
                }
            }
        }
    }

    // Add/Edit Dialog with Voice Input
    if (showAddDialog || phraseToEdit != null) {
        val context = LocalContext.current
        var textValue by remember { mutableStateOf(phraseToEdit?.text ?: "") }
        val isEditModeDialog = phraseToEdit != null

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
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.cd_voice_input))
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
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (textValue.isNotBlank()) {
                        val newPhrase = Phrase(textValue.trim(), LocaleHelper.LANG_AUTO)
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
