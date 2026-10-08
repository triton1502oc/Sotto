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
import android.provider.Settings
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.amh.sotto.ui.main.SottoApp
import com.amh.sotto.util.UsabilityTracker
import com.amh.sotto.util.ChimePlayer
import com.amh.sotto.ui.main.VoiceSettingsDialog
import com.amh.sotto.util.LocaleHelper
import com.amh.sotto.util.TranslationHelper
import com.amh.sotto.ui.conversation.TwoWayConversationDialog
import com.amh.sotto.data.WhyFinderRepository
import com.amh.sotto.data.SharedPreferencesWhyFinderRepository
import com.amh.sotto.ui.whyfinder.WhyLogDialog
import com.amh.sotto.ui.whyfinder.WhyTreeEditorDialog
import com.amh.sotto.ui.category.ManageCategoriesDialog
import com.amh.sotto.ui.category.getCategoryDisplayName
import com.amh.sotto.util.SpeechRecognitionHelper
import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import sh.calvin.reorderable.*
import android.widget.Toast
import com.amh.sotto.ui.main.ImportBackupDialog
import com.amh.sotto.ui.main.ExportBackupDialog
import com.amh.sotto.ui.main.PmfSurveyDialog
import com.amh.sotto.util.BackupData
import com.amh.sotto.util.ImportResult
import com.amh.sotto.util.PhraseBackupHelper
import com.amh.sotto.theme.SottoAppTheme
import com.amh.sotto.theme.SottoColors
import java.text.SimpleDateFormat
import java.util.Date
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
        UsabilityTracker.recordAppOpen(this)

        val phraseRepository = SharedPreferencesPhraseRepository(applicationContext)
        val voiceSettingsRepository = SharedPreferencesVoiceSettingsRepository(applicationContext)
        voiceSettingsRepository.recordActiveDay(UsabilityTracker.getTodayDate())
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
                    val categories by viewModel.categories.collectAsState()
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
                            val hasValidSpoken = latestVoiceSettings.showLanguageSwitcher &&
                                !phrase.spokenText.isNullOrBlank() &&
                                phrase.spokenText != LocaleHelper.LANG_AUTO
                            if (hasValidSpoken) {
                                speakUtterance(phrase.spokenText, phrase.spokenLanguage)
                            } else {
                                speakUtterance(phrase.text, phrase.language)
                            }
                        },
                        onSpeakText = { text, lang ->
                            speakUtterance(text, lang)
                        },
                        onAddPhrase = {
                            viewModel.addPhrase(it)
                            UsabilityTracker.recordActivation(this@MainActivity, "custom_phrase")
                        },
                        onEditPhrase = { old, new -> viewModel.editPhrase(old, new) },
                        onDeletePhrase = { viewModel.deletePhrase(it) },
                        onMovePhrase = { fromPhrase, toPhrase -> viewModel.movePhrase(fromPhrase, toPhrase) },
                        onUpdateVoiceSettings = {
                            latestVoiceSettings = it
                            viewModel.updateVoiceSettings(it)
                        },
                        categories = categories,
                        onAddCategory = {
                            val added = viewModel.addCategory(it)
                            if (added) UsabilityTracker.recordActivation(this@MainActivity, "custom_category")
                            added
                        },
                        onRenameCategory = { old, new -> viewModel.renameCategory(old, new) },
                        onDeleteCategory = { viewModel.deleteCategory(it) },
                        onReorderCategories = { from, to -> viewModel.reorderCategories(from, to) },
                        customCategories = categories,
                        onAddCustomCategory = {
                            val added = viewModel.addCategory(it)
                            if (added) UsabilityTracker.recordActivation(this@MainActivity, "custom_category")
                            added
                        },
                        onRenameCustomCategory = { old, new -> viewModel.renameCategory(old, new) },
                        onDeleteCustomCategory = { viewModel.deleteCategory(it) },
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
                        },
                        isTtsLanguageInstalled = { isTtsVoiceInstalled(it) },
                        onInstallTtsVoice = { openTtsInstallSettings() },
                        onExportBackup = { phrases, tree, log, repo -> viewModel.exportBackup(phrases, tree, log, repo) },
                        onImportBackup = { backup, replace, repo -> viewModel.importBackup(backup, replace, repo) },
                        shouldPromptMetrics = remember { viewModel.shouldPromptMetrics() },
                        onMetricsPromptAnswered = { viewModel.onMetricsPromptAnswered(it) },
                        shouldPromptRole = remember { viewModel.shouldPromptRole() },
                        onRolePromptAnswered = { viewModel.onRolePromptAnswered(it) },
                        shouldPromptPmfSurvey = remember { viewModel.shouldPromptPmfSurvey() },
                        onPmfSurveyAnswered = { score, benefit, role ->
                            viewModel.onPmfSurveyAnswered(score, benefit, role) { s, b, r ->
                                UsabilityTracker.recordSurvey(this@MainActivity, s, b, r)
                            }
                        },
                        onPmfSurveyDismissed = { viewModel.onPmfSurveyDismissed() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ttsReady) {
            val installedLocales = tts?.voices?.filter { voice ->
                !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }?.map { it.locale }?.toSet()
            availableTtsLanguages = installedLocales ?: tts?.availableLanguages ?: emptySet()
        }
    }

    private fun isTtsVoiceInstalled(langCode: String): Boolean {
        if (!ttsReady || tts == null) return false
        val targetLocale = LocaleHelper.getLocaleForLanguage(langCode)
        val targetLang = targetLocale.language.lowercase(Locale.ROOT)
        val hasMatchingInstalledVoice = availableTtsLanguages.any {
            val l = it.language.lowercase(Locale.ROOT)
            l == targetLang ||
            (targetLang == "id" && l == "in") ||
            (targetLang == "in" && l == "id")
        }
        if (hasMatchingInstalledVoice) return true

        val avail = tts?.isLanguageAvailable(targetLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        return avail >= TextToSpeech.LANG_AVAILABLE
    }

    private fun openTtsInstallSettings() {
        try {
            val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent("com.android.settings.TTS_SETTINGS"))
            } catch (_: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) {}
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
        if (targetLocale != null) {
            val isTargetVoiceMatching = settings.voiceName?.let { name ->
                val voice = tts?.voices?.firstOrNull { it.name == name }
                if (voice != null && (voice.locale.language.equals(targetLocale.language, ignoreCase = true) ||
                    (targetLocale.language == "id" && voice.locale.language.equals("in", ignoreCase = true)) ||
                    (targetLocale.language == "in" && voice.locale.language.equals("id", ignoreCase = true)))) {
                    tts?.setVoice(voice)
                    true
                } else false
            } ?: false

            if (!isTargetVoiceMatching) {
                val localeVoice = tts?.voices?.firstOrNull { voice ->
                    val langMatch = voice.locale.language.equals(targetLocale.language, ignoreCase = true) ||
                        (targetLocale.language == "id" && voice.locale.language.equals("in", ignoreCase = true)) ||
                        (targetLocale.language == "in" && voice.locale.language.equals("id", ignoreCase = true))
                    langMatch && !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                }
                if (localeVoice != null) {
                    tts?.setVoice(localeVoice)
                }
            }
        } else if (settings.voiceName != null) {
            tts?.voices?.firstOrNull { it.name == settings.voiceName }?.let { tts?.setVoice(it) }
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
