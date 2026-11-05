package com.marfolog.noteswidgetformarkdown.presentation.setup

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlin.random.Random
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.marfolog.noteswidgetformarkdown.BuildConfig
import com.marfolog.noteswidgetformarkdown.R
import com.marfolog.noteswidgetformarkdown.data.preferences.ExtraNotesStore
import com.marfolog.noteswidgetformarkdown.data.preferences.NoteCardSettingsStore
import com.marfolog.noteswidgetformarkdown.data.repository.GitSyncStatusReader
import com.marfolog.noteswidgetformarkdown.data.repository.fileNameFromDocumentUri
import com.marfolog.noteswidgetformarkdown.data.repository.isAlreadyAFolderNote
import com.marfolog.noteswidgetformarkdown.data.repository.isMarkdownFileName
import com.marfolog.noteswidgetformarkdown.data.repository.ObsidianVaultLocator
import java.text.DateFormat
import java.util.Date
import com.marfolog.noteswidgetformarkdown.domain.model.GitSyncStatus
import com.marfolog.noteswidgetformarkdown.domain.model.FastNoteStyle
import com.marfolog.noteswidgetformarkdown.domain.model.CardAppearances
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardAppearance
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardColor
import com.marfolog.noteswidgetformarkdown.domain.model.WidgetColor
import com.marfolog.noteswidgetformarkdown.domain.model.WidgetColorChoice
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardSize
import com.marfolog.noteswidgetformarkdown.domain.model.NoteCardTextSize
import com.marfolog.noteswidgetformarkdown.domain.model.NoteSummary
import com.marfolog.noteswidgetformarkdown.domain.usecase.DeleteNoteUseCase
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetExtraNotesUseCase
import com.marfolog.noteswidgetformarkdown.domain.usecase.GetNotesUseCase
import com.marfolog.noteswidgetformarkdown.presentation.widget.GitSyncLabel
import com.marfolog.noteswidgetformarkdown.presentation.widget.text
import com.marfolog.noteswidgetformarkdown.presentation.consent.ReportingConsentDialog
import com.marfolog.noteswidgetformarkdown.presentation.onboarding.OnboardingScreen
import com.marfolog.noteswidgetformarkdown.presentation.onboarding.OnboardingSession
import com.marfolog.noteswidgetformarkdown.presentation.widget.AddButtons
import com.marfolog.noteswidgetformarkdown.presentation.widget.CardDefaults
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget
import com.marfolog.noteswidgetformarkdown.presentation.widget.OpenWith
import com.marfolog.noteswidgetformarkdown.presentation.widget.WidgetBackground
import com.marfolog.noteswidgetformarkdown.presentation.widget.WidgetInventory
import com.marfolog.noteswidgetformarkdown.util.AppLanguage
import com.marfolog.noteswidgetformarkdown.util.AppLog
import com.marfolog.noteswidgetformarkdown.util.AutoPrompt
import com.marfolog.noteswidgetformarkdown.util.ConsentPrompt
import com.marfolog.noteswidgetformarkdown.util.PromptBudget
import com.marfolog.noteswidgetformarkdown.util.ProjectLinks
import com.marfolog.noteswidgetformarkdown.util.RateReminder
import com.marfolog.noteswidgetformarkdown.util.RatingPrompt
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import com.marfolog.noteswidgetformarkdown.util.UsageProperties
import com.marfolog.noteswidgetformarkdown.util.WhatsNewReminder
import com.marfolog.noteswidgetformarkdown.ui.theme.NoteCardPalette
import com.marfolog.noteswidgetformarkdown.ui.theme.WidgetPalette
import com.marfolog.noteswidgetformarkdown.ui.theme.NotesWidgetForMarkdownTheme
import com.marfolog.noteswidgetformarkdown.ui.theme.ThemePreference
import sh.calvin.reorderable.ReorderableColumn
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get

class SetupActivity : ComponentActivity() {

    private lateinit var onboarding: OnboardingSession

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemePreference.load(this)
        onboarding = OnboardingSession(applicationContext)
        if (savedInstanceState == null) {
            Telemetry.event(
                UsageEvent.SETTINGS_OPENED,
                UsageEvent.PARAM_FROM to UsageEvent.openedFrom(intent.getStringExtra(EXTRA_OPENED_FROM))
            )
        }
        setContent {
            NotesWidgetForMarkdownTheme(
                darkTheme = when (ThemePreference.mode) {
                    ThemePreference.Mode.Light -> false
                    ThemePreference.Mode.Dark -> true
                    ThemePreference.Mode.System -> isSystemInDarkTheme()
                }
            ) {
                if (onboarding.step != null) {
                    OnboardingScreen(session = onboarding, onLeave = ::finish)
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(stringResource(R.string.settings_title))
                                        // Verze primo v hlavicce: pri hlaseni chyby ji uzivatel
                                        // nemusi hledat v systemovem nastaveni.
                                        Text(
                                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        SetupScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        UsageProperties.publish(this)
    }

    companion object {
        internal const val AREA = "Setup"
        const val PREFS_NAME = "app_prefs"
        const val KEY_GUIDE_DONE = "guide_done"
        const val KEY_GUIDE_SKIPPED = "guide_skipped"
        const val KEY_OPEN_WITH = "open_with"
        const val KEY_SYNC_MODE = "sync_mode"
        const val KEY_STALE_HOURS = "sync_stale_hours"
        const val KEY_VAULT_URI = "vault_uri"
        const val KEY_NOTES_URI = "notes_uri"
        const val KEY_VAULT_NAME = "vault_name"
        const val KEY_NOTE_FOLDER_PATH = "note_folder_path"
        const val KEY_FAST_NOTE_TARGET_URI = "fast_note_target_uri"
        const val KEY_GIT_ROOT_URI = "git_root_uri"
        const val KEY_FAST_NOTE_STYLE = "fast_note_style"
        const val KEY_WIDGET_BACKGROUND = "widget_background_percent"
        const val KEY_WIDGET_COLOR = "widget_color"
        const val KEY_WIDGET_COLOR_HEX = "widget_color_hex"
        const val KEY_SHOW_ADD_BUTTONS = "show_add_buttons"
        const val KEY_DEFAULT_CARD_SIZE = "default_card_size"
        const val KEY_DEFAULT_CARD_COLOR = "default_card_color"
        const val KEY_DEFAULT_CARD_TEXT_SIZE = "default_card_text_size"
        const val KEY_DEFAULT_CARD_COLOR_HEX = "default_card_color_hex"

        /** One of the `UsageEvent.FROM_*` values; none means the launcher. */
        const val EXTRA_OPENED_FROM = "opened_from"

        fun intent(context: Context, openedFrom: String): Intent =
            Intent(context, SetupActivity::class.java)
                .putExtra(EXTRA_OPENED_FROM, openedFrom)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        /** Users who sync by other means (Syncthing, Obsidian Sync…) can hide the chip. */
        const val KEY_SHOW_GIT_STATUS = "show_git_status"
    }
}

@Composable
private fun SetupScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()
    val cardSettingsStore = remember { NoteCardSettingsStore(context.applicationContext) }
    val notesLoadFailed = stringResource(R.string.notes_load_failed)
    val deleteNoteFailed = stringResource(R.string.delete_note_failed)

    var vaultUri by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_VAULT_URI, null)) }
    var notesUri by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_NOTES_URI, null)) }
    var vaultName by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_VAULT_NAME, null)) }
    var derivedPath by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_NOTE_FOLDER_PATH, null) ?: "") }
    var fastNoteTargetUri by rememberSaveable { mutableStateOf(prefs.getString(SetupActivity.KEY_FAST_NOTE_TARGET_URI, null)) }
    var notesForSettings by remember { mutableStateOf<List<NoteSummary>>(emptyList()) }
    var cardSettings by remember { mutableStateOf(cardSettingsStore.getAll()) }
    var cardDefault by remember { mutableStateOf(CardDefaults.get(context)) }
    val cardAppearances = CardAppearances(cardSettings, cardDefault)
    var notesLoadError by rememberSaveable { mutableStateOf<String?>(null) }
    val extraNotesStore = remember { ExtraNotesStore(context.applicationContext) }
    var extraNoteUris by remember { mutableStateOf(extraNotesStore.getAll()) }
    var extraNoteEntries by remember { mutableStateOf<List<ExtraNoteEntry>>(emptyList()) }
    var extraNoteToRemove by remember { mutableStateOf<ExtraNoteEntry?>(null) }

    LaunchedEffect(extraNoteUris) {
        val getExtraNotesUseCase = get<GetExtraNotesUseCase>(GetExtraNotesUseCase::class.java)
        extraNoteEntries = getExtraNotesUseCase(extraNoteUris).map { extra ->
            ExtraNoteEntry(
                fileUri = extra.fileUri,
                // A file that cannot be opened any more has no name to ask for, so it comes
                // from the uri — still enough to recognise which one to remove.
                name = extra.note?.fileName ?: fileNameFromDocumentUri(extra.fileUri),
                isAvailable = extra.isAvailable
            )
        }
    }

    LaunchedEffect(notesUri, extraNoteUris) {
        val currentNotesUri = notesUri
        if (currentNotesUri.isNullOrEmpty()) {
            notesForSettings = emptyList()
            notesLoadError = null
            return@LaunchedEffect
        }

        runCatching {
            val getNotesUseCase = get<GetNotesUseCase>(GetNotesUseCase::class.java)
            cardSettingsStore.applyOrder(
                getNotesUseCase(currentNotesUri, extraNoteUris).firstOrNull().orEmpty()
            )
        }.onSuccess { notes ->
            notesForSettings = notes
            cardSettings = cardSettingsStore.getAll()
            notesLoadError = null
        }.onFailure { error ->
            AppLog.w(SetupActivity.AREA, "Could not list notes in $currentNotesUri", error)
            notesForSettings = emptyList()
            notesLoadError = error.message ?: notesLoadFailed
        }
    }

    var expandedNoteUri by rememberSaveable { mutableStateOf<String?>(null) }
    var noteToDelete by remember { mutableStateOf<NoteSummary?>(null) }
    var gitStatus by remember { mutableStateOf<GitSyncStatus>(GitSyncStatus.NotTracked) }
    var telemetryEnabled by rememberSaveable { mutableStateOf(Telemetry.isEnabled(context)) }
    // Asked once, and only where there is something to ask about. Until it is answered the Play
    // build sends nothing, so this is a real question rather than a notice.
    var askAboutReporting by rememberSaveable { mutableStateOf(!Telemetry.hasBeenAsked(context)) }
    // Whether the app could work the vault out on its own. When it can, the root folder never
    // has to be picked, which is the difference between one tap and two.
    var detectedVaultName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(notesUri, vaultUri) {
        val vault = ObsidianVaultLocator(context.applicationContext).locate()
        detectedVaultName = vault
            ?.takeIf { ObsidianVaultLocator.relativePath(it.documentId, notesUri) != null }
            ?.name
    }
    var showGitStatus by rememberSaveable {
        mutableStateOf(prefs.getBoolean(SetupActivity.KEY_SHOW_GIT_STATUS, true))
    }
    // Stara volba "zobrazovat cip" se prevede na rezim, at se po aktualizaci nikomu nic nezmeni.
    var openWith by remember { mutableStateOf(OpenWith.current(context)) }
    var syncMode by rememberSaveable {
        mutableStateOf(
            prefs.getString(SetupActivity.KEY_SYNC_MODE, null)
                ?: if (showGitStatus) "git" else "none"
        )
    }
    var staleHours by rememberSaveable {
        mutableIntStateOf(
            prefs.getInt(SetupActivity.KEY_STALE_HOURS, GitSyncLabel.DEFAULT_STALE_HOURS)
        )
    }
    var gitReloadTrigger by remember { mutableIntStateOf(0) }
    val rateChipSettingAvailable = remember {
        RateReminder.isSettingAvailable(context, System.currentTimeMillis())
    }
    var rateChipEnabled by rememberSaveable { mutableStateOf(RateReminder.isEnabled(context)) }
    val languageName = remember { currentLanguageName(context) }
    var fastNoteStyle by rememberSaveable {
        mutableStateOf(FastNoteStyle.from(prefs.getString(SetupActivity.KEY_FAST_NOTE_STYLE, null)))
    }

    var widgetBackgroundPercent by rememberSaveable { mutableIntStateOf(WidgetBackground.percent(context)) }
    val savedWidgetColor = remember { WidgetBackground.color(context) }
    var widgetColor by rememberSaveable { mutableStateOf(savedWidgetColor.color) }
    var widgetColorHex by rememberSaveable { mutableStateOf(savedWidgetColor.customHex) }

    var showAddButtons by rememberSaveable { mutableStateOf(AddButtons.isShown(context)) }

    fun changeCard(note: NoteSummary, change: (NoteCardAppearance) -> NoteCardAppearance) {
        cardSettingsStore.save(note.fileUri, change(cardAppearances.of(note.fileUri)))
        cardSettings = cardSettingsStore.getAll()
        scope.launch { NotesWidget.updateAll(context) }
    }

    fun changeCardDefault(updated: NoteCardAppearance) {
        cardDefault = updated
        CardDefaults.save(context, updated)
        scope.launch { NotesWidget.updateAll(context) }
    }

    fun changeWidgetColor(choice: WidgetColorChoice) {
        val before = WidgetColorChoice(widgetColor, widgetColorHex)
        widgetColor = choice.color
        widgetColorHex = choice.customHex
        WidgetBackground.saveColor(context, choice)
        if (WidgetPalette.changesWidget(before, choice)) {
            scope.launch { NotesWidget.updateAll(context) }
        }
    }

    LaunchedEffect(vaultUri, notesUri, gitReloadTrigger) {
        gitStatus = GitSyncStatusReader(context.applicationContext).read(vaultUri)
    }

    /**
     * SAF cannot walk above a granted tree, so when `.git` sits one level up we have to ask
     * for that folder explicitly. We at least open the picker directly in the parent folder.
     */
    val gitPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            AppLog.i(SetupActivity.AREA, "Git folder granted: $uri")
            prefs.edit().putString(SetupActivity.KEY_GIT_ROOT_URI, uri.toString()).apply()
            Telemetry.event(UsageEvent.GIT_ROOT_SET)
            gitReloadTrigger++
            scope.launch { NotesWidget.updateAll(context) }
        }
    }

    fun parentFolderHint(): Uri? = runCatching {
        val childTreeUri = Uri.parse(notesUri ?: vaultUri ?: return@runCatching null)
        val parentDocId = DocumentsContract.getTreeDocumentId(childTreeUri)
            .substringBeforeLast('/')
            .takeIf { it.contains(':') }
            ?: return@runCatching null
        DocumentsContract.buildDocumentUri(childTreeUri.authority, parentDocId)
    }.getOrNull()

    val vaultPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val oldUriString = prefs.getString(SetupActivity.KEY_VAULT_URI, null)
            if (oldUriString != null) {
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(oldUriString),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }

            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            val name = DocumentFile.fromTreeUri(context, uri)?.name
            val uriStr = uri.toString()
            AppLog.i(SetupActivity.AREA, "Vault root granted: $uriStr")

            prefs.edit()
                .putString(SetupActivity.KEY_VAULT_URI, uriStr)
                .putString(SetupActivity.KEY_VAULT_NAME, name)
                .apply()

            vaultUri = uriStr
            vaultName = name
            Telemetry.event(UsageEvent.VAULT_ROOT_SET)

            // Recompute relative path if notes folder is already selected
            val currentNotesUri = notesUri
            if (currentNotesUri != null) {
                val path = NotesFolderSelection.relativePath(uriStr, currentNotesUri)
                derivedPath = path
                prefs.edit().putString(SetupActivity.KEY_NOTE_FOLDER_PATH, path).apply()
            }
        }
    }

    val notesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            NotesFolderSelection.select(context, uri, scope)?.let { derivedPath = it }
            notesUri = uri.toString()
        }
    }

    // One file per pick. The type list is wide on purpose — providers disagree on what a .md file
    // is — and the extension check below is what actually decides.
    val extraNotePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = runCatching { DocumentFile.fromSingleUri(context, uri)?.name }.getOrNull()
        if (!isMarkdownFileName(name)) {
            Toast.makeText(context, R.string.extra_notes_not_markdown, Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        // Checked before any access is kept: a refused file must not leave a grant behind.
        val folderNoteUris = notesForSettings.filterNot { it.isExtra }.map { it.fileUri }
        if (isAlreadyAFolderNote(uri.toString(), folderNoteUris)) {
            Toast.makeText(context, R.string.extra_notes_in_folder, Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        extraNotesStore.add(uri.toString())
            .onSuccess { added ->
                if (added) {
                    Telemetry.event(UsageEvent.EXTRA_FILE_ADDED)
                    extraNoteUris = extraNotesStore.getAll()
                    scope.launch { NotesWidget.updateAll(context) }
                } else {
                    Toast.makeText(context, R.string.extra_notes_already_added, Toast.LENGTH_SHORT).show()
                }
            }
            .onFailure { error ->
                AppLog.w(SetupActivity.AREA, "Could not keep access to an extra note", error)
                Toast.makeText(context, R.string.extra_notes_add_failed, Toast.LENGTH_SHORT).show()
            }
    }

    val vaultDisplayName = vaultName
    // Nazev musi byt neprazdny vzdy, kdyz je slozka vybrana — zbytek nastaveni se podle nej
    // zobrazuje. Kdyz na ni prijdeme o opravneni, DocumentFile.name vrati null, tak vezmeme
    // aspon posledni kus cesty.
    val notesDisplayName = notesUri?.let { uriString ->
        runCatching {
            DocumentFile.fromTreeUri(context, Uri.parse(uriString))?.name
        }.getOrNull() ?: Uri.decode(uriString.substringAfterLast('/')).substringAfterLast('/')
    }

    // The first-launch guide sets this when it ends. Someone who set the app up before there was a
    // guide only has a folder, and counts as set up the same.
    var guideDone by rememberSaveable {
        mutableStateOf(prefs.getBoolean(SetupActivity.KEY_GUIDE_DONE, false))
    }
    LaunchedEffect(Unit) {
        if (!guideDone && notesUri != null) {
            prefs.edit().putBoolean(SetupActivity.KEY_GUIDE_DONE, true).apply()
            guideDone = true
        }
    }

    val onSave: () -> Unit = {
        Toast.makeText(context, R.string.widget_updated_toast, Toast.LENGTH_SHORT).show()

        scope.launch {
            NotesWidget.updateAll(context)
            (context as? Activity)?.finish()
        }
    }

    var showFeedback by rememberSaveable { mutableStateOf(false) }
    val openFeedback: () -> Unit = {
        showFeedback = true
        Telemetry.event(UsageEvent.FEEDBACK_DIALOG_SHOWN)
    }
    var showWhatsNew by rememberSaveable { mutableStateOf(false) }
    // Saved, so a rotation neither asks again nor brings a second dialog.
    var autoPrompt by rememberSaveable { mutableStateOf<AutoPrompt?>(null) }
    // Checked on every return to this screen until something shows: adding the widget happens on
    // the home screen while settings stay open underneath.
    var resumes by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumes++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(resumes) {
        if (resumes == 0 || autoPrompt != null) return@LaunchedEffect
        val now = System.currentTimeMillis()
        val setupComplete = notesUri != null && guideDone
        val openedFrom = (context as? Activity)?.intent?.getStringExtra(SetupActivity.EXTRA_OPENED_FROM)
        if (PromptBudget.isQuietVisit(openedFrom)) return@LaunchedEffect
        val due = buildSet {
            val consentDue = ConsentPrompt.isDue(
                canReport = Telemetry.ENABLED,
                alreadyAsked = !askAboutReporting,
                notesFolderChosen = notesUri != null,
                widgetsPlaced = WidgetInventory.Kind.entries.sumOf { WidgetInventory.count(context, it) },
                guideSkipped = prefs.getBoolean(SetupActivity.KEY_GUIDE_SKIPPED, false)
            )
            if (consentDue) add(AutoPrompt.Consent)
            if (WhatsNewReminder.shouldShow(context, setupComplete, openedFrom)) add(AutoPrompt.WhatsNew)
        }
        val mandatory = if (AutoPrompt.WhatsNew in due && WhatsNewReminder.needsButtonsChoice(context)) {
            setOf(AutoPrompt.WhatsNew)
        } else {
            emptySet()
        }
        val chosen = PromptBudget.pick(context, due, now, promptedThisVisit = autoPrompt != null, exemptFromGap = mandatory)
            ?: return@LaunchedEffect
        autoPrompt = chosen
        PromptBudget.markShown(context, now)
        when (chosen) {
            AutoPrompt.Consent -> Unit
            AutoPrompt.WhatsNew -> {
                showWhatsNew = true
                // A dialog that opens with a question is marked as seen once it is answered, by the dialog.
                if (!WhatsNewReminder.needsButtonsChoice(context)) {
                    WhatsNewReminder.markShown(context, now)
                    // The rate chip keeps its distance from this dialog, so redraw it with the new date.
                    scope.launch { NotesWidget.updateAll(context) }
                }
            }
        }
    }
    if (showWhatsNew) {
        WhatsNewDialogHost(onButtonsChosen = { showAddButtons = it }, onClose = { showWhatsNew = false })
    }
    if (showFeedback) {
        FeedbackDialogHost(onClose = { showFeedback = false })
    }

    if (askAboutReporting && Telemetry.ENABLED && autoPrompt == AutoPrompt.Consent) {
        ReportingConsentDialog { agreed ->
            telemetryEnabled = agreed
            askAboutReporting = false
        }
    }

    SetupScreenContent(
        vaultName = vaultDisplayName,
        onSelectVault = { vaultPicker.launch(null) },
        notesFolderName = notesDisplayName,
        notesFolderPath = notesUri?.let { humanFolderPath(it) },
        onSelectNotesFolder = { notesPicker.launch(null) },
        extraNotes = extraNoteEntries,
        onAddExtraNote = { extraNotePicker.launch(EXTRA_NOTE_MIME_TYPES) },
        onRemoveExtraNote = { entry -> extraNoteToRemove = entry },
        derivedPath = derivedPath,
        notes = notesForSettings,
        cardAppearances = cardAppearances,
        notesLoadError = notesLoadError,
        fastNoteTargetUri = fastNoteTargetUri,
        onFastNoteTargetSelected = { note ->
            fastNoteTargetUri = note.fileUri
            prefs.edit()
                .putString(SetupActivity.KEY_FAST_NOTE_TARGET_URI, note.fileUri)
                .commit()
            Telemetry.event(UsageEvent.FAST_TARGET_SET)
        },
        fastNoteStyle = fastNoteStyle,
        onFastNoteStyleChanged = { style ->
            fastNoteStyle = style
            prefs.edit().putString(SetupActivity.KEY_FAST_NOTE_STYLE, style.storageValue).apply()
            logSettingChanged(UsageEvent.SETTING_FAST_NOTE_STYLE, style.storageValue)
        },
        widgetBackgroundPercent = widgetBackgroundPercent,
        onWidgetBackgroundChanged = { percent ->
            if (percent != widgetBackgroundPercent) {
                widgetBackgroundPercent = percent
                WidgetBackground.save(context, percent)
                logSettingChanged(UsageEvent.SETTING_WIDGET_BACKGROUND, percent.toString())
                scope.launch { NotesWidget.updateAll(context) }
            }
        },
        widgetColor = WidgetColorChoice(widgetColor, widgetColorHex),
        onWidgetColorSelected = { color ->
            if (color != widgetColor) {
                changeWidgetColor(WidgetColorChoice(color, widgetColorHex))
                logSettingChanged(UsageEvent.SETTING_WIDGET_COLOR, color.storageValue)
            }
        },
        onWidgetCustomColorChanged = { hex -> changeWidgetColor(WidgetColorChoice(WidgetColor.Custom, hex)) },
        onCardSizeSelected = { note, size ->
            changeCard(note) { it.copy(size = size) }
            Telemetry.event(UsageEvent.CARD_HEIGHT_CHANGED)
        },
        onCardColorSelected = { note, color ->
            changeCard(note) { it.copy(color = color) }
            Telemetry.event(UsageEvent.CARD_COLOR_CHANGED)
        },
        onCardTextSizeSelected = { note, textSize ->
            changeCard(note) { it.copy(textSize = textSize) }
        },
        onCardCustomColorChanged = { note, colorHex ->
            changeCard(note) { it.copy(color = NoteCardColor.Custom, customColorHex = colorHex) }
        },
        onCardResetToDefault = { note ->
            cardSettingsStore.forgetAppearance(note.fileUri)
            cardSettings = cardSettingsStore.getAll()
            scope.launch { NotesWidget.updateAll(context) }
        },
        onDefaultCardSizeSelected = { size ->
            if (size != cardDefault.size) {
                changeCardDefault(cardDefault.copy(size = size))
                logSettingChanged(UsageEvent.SETTING_DEFAULT_CARD_SIZE, size.storageValue)
            }
        },
        onDefaultCardTextSizeSelected = { textSize ->
            if (textSize != cardDefault.textSize) {
                changeCardDefault(cardDefault.copy(textSize = textSize))
                logSettingChanged(UsageEvent.SETTING_DEFAULT_CARD_TEXT_SIZE, textSize.storageValue)
            }
        },
        onDefaultCardColorSelected = { color ->
            if (color != cardDefault.color) {
                changeCardDefault(cardDefault.copy(color = color))
                logSettingChanged(UsageEvent.SETTING_DEFAULT_CARD_COLOR, color.storageValue)
            }
        },
        onDefaultCardCustomColorChanged = { hex ->
            changeCardDefault(cardDefault.copy(color = NoteCardColor.Custom, customColorHex = hex))
        },
        showAddButtons = showAddButtons,
        onShowAddButtonsChanged = { shown ->
            showAddButtons = shown
            AddButtons.setShown(context, shown)
            Telemetry.event(UsageEvent.SETTING_CHANGED, *UsageEvent.addButtonsChanged(shown, UsageEvent.FROM_SETTINGS))
            scope.launch { NotesWidget.updateAll(context) }
        },
        onReorder = { from, to ->
            if (from in notesForSettings.indices && to in notesForSettings.indices) {
                notesForSettings = notesForSettings.toMutableList().also { notes ->
                    notes.add(to, notes.removeAt(from))
                }
                cardSettingsStore.saveOrder(notesForSettings.map { it.fileUri })
                Telemetry.event(UsageEvent.CARD_REORDERED)
                scope.launch { NotesWidget.updateAll(context) }
            }
        },
        expandedNoteUri = expandedNoteUri,
        onToggleExpanded = { note ->
            expandedNoteUri = if (expandedNoteUri == note.fileUri) null else note.fileUri
        },
        onDeleteRequested = { note ->
            // A swipe on an extra note only takes it off the widget, so it gets that dialog —
            // never the delete one.
            if (note.isExtra) {
                extraNoteToRemove = ExtraNoteEntry(note.fileUri, note.fileName, isAvailable = true)
            } else {
                noteToDelete = note
            }
        },
        openWith = openWith,
        onOpenWithChanged = { mode ->
            openWith = mode
            prefs.edit().putString(SetupActivity.KEY_OPEN_WITH, mode.storageValue).apply()
            logSettingChanged(UsageEvent.SETTING_OPEN_WITH, mode.storageValue)
            scope.launch { NotesWidget.updateAll(context) }
        },
        syncMode = syncMode,
        onSyncModeChanged = { mode ->
            syncMode = mode
            prefs.edit().putString(SetupActivity.KEY_SYNC_MODE, mode).apply()
            logSettingChanged(UsageEvent.SETTING_SYNC_MODE, mode)
            scope.launch { NotesWidget.updateAll(context) }
        },
        staleHours = staleHours,
        onStaleHoursChanged = { hours ->
            staleHours = hours
            prefs.edit().putInt(SetupActivity.KEY_STALE_HOURS, hours).apply()
            logSettingChanged(UsageEvent.SETTING_STALE_HOURS, hours.toString())
            scope.launch { NotesWidget.updateAll(context) }
        },
        gitStatusText = gitStatusText(context, gitStatus, staleHours),
        syncWarning = if (syncMode == "git") syncWarningUi(context, gitStatus, staleHours) else null,
        gitIsTracked = gitStatus is GitSyncStatus.Tracked,
        gitIsBroken = gitStatus is GitSyncStatus.Unavailable,
        onSelectGitFolder = { gitPicker.launch(parentFolderHint()) },
        detectedVaultName = detectedVaultName,
        telemetryAvailable = Telemetry.ENABLED,
        telemetryEnabled = telemetryEnabled,
        onTelemetryChanged = { enabled ->
            telemetryEnabled = enabled
            // Logged while reporting is still on: switching it off is the one change that would
            // otherwise never arrive.
            if (!enabled) logSettingChanged(UsageEvent.SETTING_TELEMETRY, UsageEvent.onOff(false))
            Telemetry.setEnabled(context, enabled)
            Telemetry.markAsked(context)
            askAboutReporting = false
            if (enabled) logSettingChanged(UsageEvent.SETTING_TELEMETRY, UsageEvent.onOff(true))
            AppLog.i(SetupActivity.AREA, "Reporting switched ${if (enabled) "on" else "off"}")
        },
        showGitStatus = showGitStatus,
        onShowGitStatusChanged = { enabled ->
            showGitStatus = enabled
            prefs.edit().putBoolean(SetupActivity.KEY_SHOW_GIT_STATUS, enabled).apply()
            scope.launch { NotesWidget.updateAll(context) }
        },
        onSendFeedback = openFeedback,
        languageName = languageName,
        onOpenLanguage = {
            Telemetry.event(UsageEvent.LANGUAGE_OPENED)
            openAppLanguageSettings(context)
        },
        rateChipSettingAvailable = rateChipSettingAvailable,
        rateChipEnabled = rateChipEnabled,
        onRateChipChanged = { enabled ->
            rateChipEnabled = enabled
            RateReminder.setEnabled(context, enabled)
            logSettingChanged(UsageEvent.SETTING_RATE_CHIP, UsageEvent.onOff(enabled))
            scope.launch { NotesWidget.updateAll(context) }
        },
        canSave = notesUri != null,
        onSave = onSave,
        modifier = modifier
    )

    // Deleting is permanent — SAF has no trash, and recreating the file would give it a new
    // URI, orphaning its appearance settings. So: confirm, then delete for real.
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(stringResource(R.string.delete_note_title, note.fileName)) },
            text = { Text(stringResource(R.string.delete_note_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        noteToDelete = null
                        scope.launch {
                            val deleteNoteUseCase = get<DeleteNoteUseCase>(DeleteNoteUseCase::class.java)
                            AppLog.i(SetupActivity.AREA, "Deleting a note file on user request")
                            deleteNoteUseCase(note)
                                .onSuccess { deleted ->
                                    if (deleted) {
                                        cardSettingsStore.forget(note.fileUri)
                                        if (fastNoteTargetUri == note.fileUri) {
                                            fastNoteTargetUri = null
                                            prefs.edit()
                                                .remove(SetupActivity.KEY_FAST_NOTE_TARGET_URI)
                                                .apply()
                                        }
                                        notesForSettings = notesForSettings.filterNot {
                                            it.fileUri == note.fileUri
                                        }
                                        cardSettings = cardSettingsStore.getAll()
                                        if (expandedNoteUri == note.fileUri) {
                                            expandedNoteUri = null
                                        }
                                        NotesWidget.updateAll(context)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Could not delete ${note.fileName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .onFailure { error ->
                                    AppLog.e(SetupActivity.AREA, "Delete failed", error)
                                    Toast.makeText(
                                        context,
                                        error.message ?: deleteNoteFailed,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { noteToDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    // Removing an extra note forgets it and gives its access back. The file is not touched.
    extraNoteToRemove?.let { entry ->
        RemoveExtraNoteDialog(
            name = entry.name,
            onDismiss = { extraNoteToRemove = null },
            onConfirm = {
                extraNoteToRemove = null
                extraNotesStore.remove(entry.fileUri)
                Telemetry.event(UsageEvent.EXTRA_FILE_REMOVED)
                cardSettingsStore.forget(entry.fileUri)
                extraNoteUris = extraNotesStore.getAll()
                notesForSettings = notesForSettings.filterNot { it.isExtra && it.fileUri == entry.fileUri }
                cardSettings = cardSettingsStore.getAll()
                if (expandedNoteUri == entry.fileUri) {
                    expandedNoteUri = null
                }
                scope.launch { NotesWidget.updateAll(context) }
            }
        )
    }
}

private fun logSettingChanged(setting: String, value: String) {
    Telemetry.event(UsageEvent.SETTING_CHANGED, UsageEvent.PARAM_SETTING to setting, UsageEvent.PARAM_VALUE to value)
}

/** What the single-file picker offers. See the picker for why the list is this wide. */
private val EXTRA_NOTE_MIME_TYPES = arrayOf(
    "text/markdown",
    "text/x-markdown",
    "text/plain",
    "application/octet-stream"
)

/**
 * Ctelna cesta ke slozce ze SAF tree URI. Z "primary:Documents/vault/Quick" udela
 * "Internal storage/Documents/vault/Quick". Kdyz se to nepovede, vrati null a zobrazi se
 * aspon nazev slozky.
 */
private fun humanFolderPath(treeUri: String): String? = runCatching {
    val docId = DocumentsContract.getTreeDocumentId(Uri.parse(treeUri))
    val volume = docId.substringBefore(':')
    val path = docId.substringAfter(':', "")
    val root = if (volume.equals("primary", ignoreCase = true)) "Internal storage" else volume
    if (path.isEmpty()) root else "$root/$path"
}.getOrNull()

private fun gitStatusText(context: Context, status: GitSyncStatus, staleHours: Int): String = when (status) {
    is GitSyncStatus.NotTracked -> context.getString(R.string.git_status_not_tracked)
    is GitSyncStatus.Unavailable ->
        context.getString(R.string.git_status_unreadable, status.reason)
    is GitSyncStatus.FileActivity -> context.getString(
        R.string.git_status_file_activity,
        SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
            .format(Date(status.lastChangeAtMillis))
    )
    is GitSyncStatus.Tracked -> buildString {
        // This is when HEAD last moved — a pull, a commit, a checkout. Not "when the note
        // was written", and not "when the client last ran".
        append(context.getString(R.string.git_status_last_change))
        status.lastAction?.let { append(" ($it)") }
        append(": ")
        append(
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(status.lastChangeAtMillis))
        )
        status.branch?.let { append(context.getString(R.string.git_status_branch, it)) }
        status.lastRemoteContactAtMillis?.let { fetchedAt ->
            append("\n")
            append(context.getString(R.string.git_status_last_remote_contact))
            append(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(fetchedAt))
            )
        }
        // Two different questions: "when did anything arrive" vs "is the client even running".
        status.lastClientActivityAtMillis?.let { activeAt ->
            append("\n")
            append(context.getString(R.string.git_status_client_active))
            append(
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(activeAt))
            )
        }
    }
}

/** Plain-language sync warning already resolved to text, so the composable only draws it. */
internal data class SyncWarningUi(val text: String, val isProblem: Boolean)

private fun syncWarningUi(context: Context, status: GitSyncStatus, staleHours: Int): SyncWarningUi? {
    if (status !is GitSyncStatus.Tracked) return null
    val label = GitSyncLabel.format(status, System.currentTimeMillis(), staleHours)
    return GitSyncLabel.warningOf(label)?.let { SyncWarningUi(it.detail.text(context), it.isProblem) }
}

@Composable
private fun SyncWarningCard(warning: SyncWarningUi, modifier: Modifier = Modifier) {
    val container = if (warning.isProblem) MaterialTheme.colorScheme.errorContainer else Color(0xFFFFE8B0)
    val content = if (warning.isProblem) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF4A3300)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = if (warning.isProblem) MaterialTheme.colorScheme.error else Color(0xFFB37800)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = warning.text, style = MaterialTheme.typography.bodyMedium, color = content)
    }
}

@Composable
internal fun SetupScreenContent(
    vaultName: String?,
    onSelectVault: () -> Unit,
    notesFolderName: String?,
    notesFolderPath: String? = null,
    onSelectNotesFolder: () -> Unit,
    extraNotes: List<ExtraNoteEntry> = emptyList(),
    onAddExtraNote: () -> Unit = {},
    onRemoveExtraNote: (ExtraNoteEntry) -> Unit = {},
    derivedPath: String,
    notes: List<NoteSummary> = emptyList(),
    cardAppearances: CardAppearances = CardAppearances(),
    notesLoadError: String? = null,
    fastNoteTargetUri: String? = null,
    onFastNoteTargetSelected: (NoteSummary) -> Unit = {},
    fastNoteStyle: FastNoteStyle = FastNoteStyle.Bullet,
    onFastNoteStyleChanged: (FastNoteStyle) -> Unit = {},
    widgetBackgroundPercent: Int = WidgetBackground.DEFAULT_PERCENT,
    onWidgetBackgroundChanged: (Int) -> Unit = {},
    widgetColor: WidgetColorChoice = WidgetColorChoice(),
    onWidgetColorSelected: (WidgetColor) -> Unit = {},
    onWidgetCustomColorChanged: (String) -> Unit = {},
    onCardSizeSelected: (NoteSummary, NoteCardSize) -> Unit = { _, _ -> },
    onCardColorSelected: (NoteSummary, NoteCardColor) -> Unit = { _, _ -> },
    onCardTextSizeSelected: (NoteSummary, NoteCardTextSize) -> Unit = { _, _ -> },
    onCardCustomColorChanged: (NoteSummary, String) -> Unit = { _, _ -> },
    onCardResetToDefault: (NoteSummary) -> Unit = {},
    onDefaultCardSizeSelected: (NoteCardSize) -> Unit = {},
    onDefaultCardTextSizeSelected: (NoteCardTextSize) -> Unit = {},
    onDefaultCardColorSelected: (NoteCardColor) -> Unit = {},
    onDefaultCardCustomColorChanged: (String) -> Unit = {},
    showAddButtons: Boolean = true,
    onShowAddButtonsChanged: (Boolean) -> Unit = {},
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    expandedNoteUri: String? = null,
    onToggleExpanded: (NoteSummary) -> Unit = {},
    onDeleteRequested: (NoteSummary) -> Unit = {},
    gitStatusText: String = "",
    syncWarning: SyncWarningUi? = null,
    gitIsTracked: Boolean = false,
    // Cervene se hlasi jen nalezeny, ale necitelny .git. Zadny git neni chyba — spousta lidi
    // synchronizuje jinak.
    gitIsBroken: Boolean = false,
    onSelectGitFolder: () -> Unit = {},
    showGitStatus: Boolean = true,
    onShowGitStatusChanged: (Boolean) -> Unit = {},
    openWith: OpenWith = OpenWith.Obsidian,
    onOpenWithChanged: (OpenWith) -> Unit = {},
    syncMode: String = "git",
    onSyncModeChanged: (String) -> Unit = {},
    staleHours: Int = GitSyncLabel.DEFAULT_STALE_HOURS,
    onStaleHoursChanged: (Int) -> Unit = {},
    detectedVaultName: String? = null,
    telemetryAvailable: Boolean = false,
    telemetryEnabled: Boolean = false,
    onTelemetryChanged: (Boolean) -> Unit = {},
    languageName: String? = null,
    onOpenLanguage: () -> Unit = {},
    rateChipSettingAvailable: Boolean = true,
    rateChipEnabled: Boolean = true,
    onRateChipChanged: (Boolean) -> Unit = {},
    onSendFeedback: () -> Unit = {},
    canSave: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        WatermarkLogo()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {

        // Kdo klepne na zluty nebo cerveny cip, musi duvod videt hned, ne az dole u stavu gitu.
        syncWarning?.let {
            SyncWarningCard(it)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section 1: the only folder anyone has to pick
        Text(
            text = if (notesFolderName == null) stringResource(R.string.notes_folder_title_first) else stringResource(R.string.notes_folder_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.notes_folder_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (notesFolderName != null) {
            // Samotny nazev slozky rikal min nez cesta, ktera ho obsahuje taky.
            Text(
                text = notesFolderPath ?: notesFolderName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.notes_folder_plus_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (notesFolderName == null) {
            Button(
                onClick = onSelectNotesFolder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.notes_folder_select))
            }
        } else {
            OutlinedButton(onClick = onSelectNotesFolder, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.notes_folder_change))
            }
        }

        // Vsechno dalsi ma smysl az nad vybranou slozkou. Dokud neni, byla by to jen rada
        // zasedlych tlacitek a odstavcu o necem, co jeste neexistuje.
        if (notesFolderName != null) {

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        WidgetButtonsSection(shown = showAddButtons, onChanged = onShowAddButtonsChanged)

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.fast_note_target_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.fast_note_target_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        FastNoteTargetSection(
            // Fast notes go into the folder; an extra note is not a target.
            notes = notes.filterNot { it.isExtra },
            notesLoadError = notesLoadError,
            selectedNoteUri = fastNoteTargetUri,
            onTargetSelected = onFastNoteTargetSelected,
            folderSelected = notesFolderName != null
        )
        Spacer(modifier = Modifier.height(12.dp))
        FastNoteStyleChooser(selected = fastNoteStyle, onSelected = onFastNoteStyleChanged)

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        ExtraNotesSection(
            entries = extraNotes,
            onAddFile = onAddExtraNote,
            onRemove = onRemoveExtraNote
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.open_with_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.open_with_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            OPEN_WITH_CHOICES.forEach { (mode, labelRes) ->
                FilterChip(
                    selected = openWith == mode,
                    onClick = { onOpenWithChanged(mode) },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                when (openWith) {
                    OpenWith.Obsidian -> R.string.open_with_hint_obsidian
                    OpenWith.DefaultApp -> R.string.open_with_hint_default
                    OpenWith.AskEveryTime -> R.string.open_with_hint_ask
                }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: only needed when the vault could not be worked out
        if (detectedVaultName != null) {
            Text(
                text = stringResource(R.string.vault_detected, detectedVaultName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        } else {
        Text(
            text = stringResource(R.string.vault_root_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.vault_root_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (vaultName != null) {
            Text(
                text = vaultName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = onSelectVault,
            enabled = notesFolderName != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (vaultName != null) stringResource(R.string.vault_root_change) else stringResource(R.string.vault_root_select))
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        }

        // Only the Play build has anything to switch off; the foss build never reports.
        if (telemetryAvailable) {
            Text(
                text = stringResource(R.string.reporting_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = telemetryEnabled, role = Role.Switch, onValueChange = onTelemetryChanged),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.reporting_switch),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = telemetryEnabled, onCheckedChange = null)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.reporting_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section 3: Git sync status
        Text(
            text = stringResource(R.string.sync_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.sync_section_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.sync_question),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SYNC_MODES.forEach { (mode, labelRes) ->
                FilterChip(
                    selected = syncMode == mode,
                    onClick = { onSyncModeChanged(mode) },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when (syncMode) {
                "git" -> stringResource(R.string.sync_desc_git)
                "other" -> stringResource(R.string.sync_desc_other)
                else -> stringResource(R.string.sync_no_chip)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (syncMode != "none") {
            Spacer(modifier = Modifier.height(12.dp))
            // "Nothing new" znamena v kazdem rezimu neco jineho, tak se to rovnou pojmenuje.
            Text(
                text = if (syncMode == "git") {
                    stringResource(R.string.sync_warn_git)
                } else {
                    stringResource(R.string.sync_warn_files)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (syncMode == "git") {
                    stringResource(R.string.sync_warn_git_hint)
                } else {
                    stringResource(R.string.sync_warn_files_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                STALE_CHOICES.forEach { (hours, labelRes) ->
                    FilterChip(
                        selected = staleHours == hours,
                        onClick = { onStaleHoursChanged(hours) },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (syncMode != "none") {
        Text(
            text = if (notesFolderName == null) {
                stringResource(R.string.sync_not_checked_yet)
            } else {
                gitStatusText
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (gitIsBroken) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        syncWarning?.let {
            Spacer(modifier = Modifier.height(12.dp))
            SyncWarningCard(it)
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (syncMode == "git") {
        OutlinedButton(
            onClick = onSelectGitFolder,
            enabled = notesFolderName != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (gitIsTracked) stringResource(R.string.git_folder_change) else stringResource(R.string.git_folder_select))
        }
        }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        WidgetBackgroundSetting(
            percent = widgetBackgroundPercent,
            onChanged = onWidgetBackgroundChanged,
            color = widgetColor,
            onColorSelected = onWidgetColorSelected,
            onCustomColorChanged = onWidgetCustomColorChanged
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.cards_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.cards_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        DefaultCardSetting(
            appearance = cardAppearances.default,
            onSizeSelected = onDefaultCardSizeSelected,
            onTextSizeSelected = onDefaultCardTextSizeSelected,
            onColorSelected = onDefaultCardColorSelected,
            onCustomColorChanged = onDefaultCardCustomColorChanged
        )

        Spacer(modifier = Modifier.height(16.dp))

        CardSettingsSection(
            notes = notes,
            cardAppearances = cardAppearances,
            notesLoadError = notesLoadError,
            onCardSizeSelected = onCardSizeSelected,
            onCardColorSelected = onCardColorSelected,
            onCardTextSizeSelected = onCardTextSizeSelected,
            onCardCustomColorChanged = onCardCustomColorChanged,
            onCardResetToDefault = onCardResetToDefault,
            onReorder = onReorder,
            expandedNoteUri = expandedNoteUri,
            onToggleExpanded = onToggleExpanded,
            onDeleteRequested = onDeleteRequested
        )

        Spacer(modifier = Modifier.height(32.dp))

        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.appearance_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.appearance_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        val appearanceContext = LocalContext.current
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePreference.Mode.entries.forEach { mode ->
                FilterChip(
                    selected = ThemePreference.mode == mode,
                    onClick = {
                        ThemePreference.set(appearanceContext, mode)
                        logSettingChanged(UsageEvent.SETTING_THEME, mode.name.lowercase(Locale.ROOT))
                    },
                    label = { Text(stringResource(themeModeLabel(mode))) }
                )
            }
        }
        if (languageName != null) {
            Spacer(modifier = Modifier.height(12.dp))
            LanguageSetting(languageName = languageName, onClick = onOpenLanguage)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        if (rateChipSettingAvailable) {
            RateChipSetting(enabled = rateChipEnabled, onChanged = onRateChipChanged)

            Spacer(modifier = Modifier.height(24.dp))
        }

        AboutSection(onSendFeedback = onSendFeedback)

        Spacer(modifier = Modifier.height(32.dp))

        // Save button
        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_done))
        }

        Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FastNoteTargetSection(
    notes: List<NoteSummary>,
    notesLoadError: String?,
    selectedNoteUri: String?,
    onTargetSelected: (NoteSummary) -> Unit,
    folderSelected: Boolean = false
) {
    when {
        notesLoadError != null -> {
            Text(
                text = notesLoadError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        notes.isEmpty() -> {
            // Prazdna slozka a nevybrana slozka jsou dve ruzne situace a uzivatel v kazde dela
            // neco jineho.
            Text(
                text = if (folderSelected) {
                    stringResource(R.string.fast_note_target_folder_empty)
                } else {
                    stringResource(R.string.fast_note_target_none_yet)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            val selectedExists = selectedNoteUri == null || notes.any { it.fileUri == selectedNoteUri }
            if (!selectedExists) {
                Text(
                    text = stringResource(R.string.fast_note_target_gone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                notes.forEach { note ->
                    FilterChip(
                        selected = note.fileUri == selectedNoteUri,
                        onClick = { onTargetSelected(note) },
                        label = { Text(note.title) }
                    )
                }
            }
        }
    }
}

/** Reports only where the slider was let go, so one drag is one change rather than dozens. */
@Composable
private fun WidgetBackgroundSetting(
    percent: Int,
    onChanged: (Int) -> Unit,
    color: WidgetColorChoice,
    onColorSelected: (WidgetColor) -> Unit,
    onCustomColorChanged: (String) -> Unit
) {
    var position by remember(percent) { mutableFloatStateOf(percent.toFloat()) }
    Text(
        text = stringResource(R.string.widget_background_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.card_color),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        WidgetColor.entries.forEach { option ->
            ColorChip(
                selected = color.color == option,
                label = stringResource(widgetColorLabel(option)),
                swatch = widgetColorPreview(color.copy(color = option)),
                onClick = { onColorSelected(option) }
            )
        }
    }
    if (color.color == WidgetColor.Custom) {
        Spacer(modifier = Modifier.height(8.dp))
        CustomColorField(value = color.customHex.orEmpty(), onValueChange = onCustomColorChanged)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.widget_color_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.widget_background_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            value = position,
            onValueChange = { position = it },
            onValueChangeFinished = { onChanged(WidgetBackground.fromSlider(position)) },
            valueRange = 0f..100f,
            steps = WidgetBackground.SLIDER_STEPS,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.widget_background_value, WidgetBackground.fromSlider(position)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Only on Android 13 and newer, where system settings have a language page for each app. */
private fun currentLanguageName(context: Context): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val appLocales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
    val shown = AppLanguage.shown(
        appLocales = appLocales?.toList().orEmpty(),
        systemLocales = Resources.getSystem().configuration.locales.toList()
    )
    return AppLanguage.displayName(shown)
}

private fun LocaleList.toList(): List<Locale> = (0 until size()).map { get(it) }

private fun openAppLanguageSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
    runCatching { context.startActivity(intent) }
        .onFailure { AppLog.w(SetupActivity.AREA, "No app language page in system settings", it) }
}

@Composable
private fun LanguageSetting(languageName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.language_setting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = languageName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RateChipSetting(enabled: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onChanged),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.rate_chip_setting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = enabled, onCheckedChange = null)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.rate_chip_setting_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CardSettingsSection(
    notes: List<NoteSummary>,
    cardAppearances: CardAppearances,
    notesLoadError: String?,
    onCardSizeSelected: (NoteSummary, NoteCardSize) -> Unit,
    onCardColorSelected: (NoteSummary, NoteCardColor) -> Unit,
    onCardTextSizeSelected: (NoteSummary, NoteCardTextSize) -> Unit,
    onCardCustomColorChanged: (NoteSummary, String) -> Unit,
    onCardResetToDefault: (NoteSummary) -> Unit,
    onReorder: (Int, Int) -> Unit,
    expandedNoteUri: String?,
    onToggleExpanded: (NoteSummary) -> Unit,
    onDeleteRequested: (NoteSummary) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var hintPending by rememberSaveable { mutableStateOf(!prefs.getBoolean(KEY_SWIPE_HINT_SHOWN, false)) }
    val hintOffset = remember { Animatable(0f) }

    when {
        notesLoadError != null -> {
            Text(
                text = notesLoadError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        notes.isEmpty() -> {
            Text(
                text = stringResource(R.string.cards_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            // Prvni radek trikrat popojede do strany, at je gesto videt. Pak uz nikdy.
            LaunchedEffect(hintPending, notes.isEmpty()) {
                if (!hintPending || notes.isEmpty()) return@LaunchedEffect
                delay(600)
                repeat(3) {
                    hintOffset.animateTo(-64f, tween(260))
                    hintOffset.animateTo(0f, tween(260))
                    delay(120)
                }
                prefs.edit().putBoolean(KEY_SWIPE_HINT_SHOWN, true).apply()
                hintPending = false
            }
            ReorderableColumn(
                list = notes,
                onSettle = onReorder,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { index, note, isDragging ->
                val appearance = cardAppearances.of(note.fileUri)
                // ReorderableColumn has no notion of item identity — it is a plain Column
                // walking `list` by index, so remembered state (the swipe-to-dismiss state
                // below included) stays tied to a *position*. When the list shrinks or
                // reorders, Compose reuses that position's composition for whatever note now
                // sits there, and a swipe state from the note that used to be there can carry
                // over — a swipe recorded against note 14 lands on note 16. Keying on the
                // file uri forces Compose to throw away and rebuild that state whenever the
                // note occupying this slot actually changes.
                key(note.fileUri) {
                ReorderableItem {
                SwipeToDelete(
                    onDelete = { onDeleteRequested(note) },
                    hintOffset = if (index == 0) hintOffset.value else 0f,
                    removesFromWidgetOnly = note.isExtra
                ) {
                NoteCardSettingsRow(
                    note = note,
                    appearance = appearance,
                    hasOwnAppearance = cardAppearances.hasOwn(note.fileUri),
                    expanded = expandedNoteUri == note.fileUri,
                    isDragging = isDragging,
                    dragHandleModifier = Modifier.draggableHandle(),
                    onToggleExpanded = { onToggleExpanded(note) },
                    onSizeSelected = { size -> onCardSizeSelected(note, size) },
                    onColorSelected = { color -> onCardColorSelected(note, color) },
                    onTextSizeSelected = { textSize -> onCardTextSizeSelected(note, textSize) },
                    onCustomColorChanged = { colorHex -> onCardCustomColorChanged(note, colorHex) },
                    onResetToDefault = { onCardResetToDefault(note) }
                )
                }
                }
                }
            }
        }
    }
}

@Composable
private fun NoteCardSettingsRow(
    note: NoteSummary,
    appearance: NoteCardAppearance,
    hasOwnAppearance: Boolean,
    expanded: Boolean,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onToggleExpanded: () -> Unit,
    onSizeSelected: (NoteCardSize) -> Unit,
    onColorSelected: (NoteCardColor) -> Unit,
    onTextSizeSelected: (NoteCardTextSize) -> Unit,
    onCustomColorChanged: (String) -> Unit,
    onResetToDefault: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        // Lift the card while it is being dragged so it reads as picked up.
        tonalElevation = if (isDragging) 8.dp else 0.dp,
        shadowElevation = if (isDragging) 8.dp else 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(cardColorPreview(appearance.color), RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggleExpanded)
                )
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.card_drag_hint),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragHandleModifier.padding(8.dp)
                )
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (expanded) stringResource(R.string.card_collapse) else stringResource(R.string.card_expand)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                CardAppearanceChooser(
                    appearance = appearance,
                    onSizeSelected = onSizeSelected,
                    onTextSizeSelected = onTextSizeSelected,
                    onColorSelected = onColorSelected,
                    onCustomColorChanged = onCustomColorChanged
                )
                if (hasOwnAppearance) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = onResetToDefault) {
                        Text(stringResource(R.string.card_use_default))
                    }
                }
            }
        }
    }
}

/** Size, text size and colour of one card, or of every card that has no look of its own. */
@Composable
private fun CardAppearanceChooser(
    appearance: NoteCardAppearance,
    onSizeSelected: (NoteCardSize) -> Unit,
    onTextSizeSelected: (NoteCardTextSize) -> Unit,
    onColorSelected: (NoteCardColor) -> Unit,
    onCustomColorChanged: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        NoteCardSize.entries.forEach { size ->
            FilterChip(
                selected = appearance.size == size,
                onClick = { onSizeSelected(size) },
                label = { Text(size.label) }
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.card_text_size),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        NoteCardTextSize.entries.forEach { textSize ->
            FilterChip(
                selected = appearance.textSize == textSize,
                onClick = { onTextSizeSelected(textSize) },
                label = { Text(textSize.label) }
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.card_color),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        NoteCardColor.entries.forEach { color ->
            ColorChip(
                selected = appearance.color == color,
                label = color.label,
                swatch = cardColorPreview(color),
                onClick = { onColorSelected(color) }
            )
        }
    }
    if (appearance.color == NoteCardColor.Custom) {
        Spacer(modifier = Modifier.height(8.dp))
        CustomColorField(value = appearance.customColorHex.orEmpty(), onValueChange = onCustomColorChanged)
    }
}

@Composable
private fun DefaultCardSetting(
    appearance: NoteCardAppearance,
    onSizeSelected: (NoteCardSize) -> Unit,
    onTextSizeSelected: (NoteCardTextSize) -> Unit,
    onColorSelected: (NoteCardColor) -> Unit,
    onCustomColorChanged: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.card_default_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.card_default_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    CardAppearanceChooser(
        appearance = appearance,
        onSizeSelected = onSizeSelected,
        onTextSizeSelected = onTextSizeSelected,
        onColorSelected = onColorSelected,
        onCustomColorChanged = onCustomColorChanged
    )
}

@Composable
private fun ColorChip(selected: Boolean, label: String, swatch: Color, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(swatch, RoundedCornerShape(3.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(label)
            }
        }
    )
}

@Composable
private fun CustomColorField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.card_custom_color)) },
        placeholder = { Text("#D9E8C8") },
        singleLine = true
    )
}

private val PREVIEW_FALLBACK = Color(0xFFE7E0EC)

private fun cardColorPreview(color: NoteCardColor): Color =
    NoteCardPalette.background(color) ?: PREVIEW_FALLBACK

/** System has no one colour to show, so its swatch is the app's own surface. */
@Composable
private fun widgetColorPreview(choice: WidgetColorChoice): Color = when (choice.color) {
    WidgetColor.System -> MaterialTheme.colorScheme.surface
    else -> WidgetPalette.background(choice) ?: PREVIEW_FALLBACK
}

@StringRes
private fun widgetColorLabel(color: WidgetColor): Int = when (color) {
    WidgetColor.System -> R.string.widget_color_system
    WidgetColor.Rose -> R.string.widget_color_rose
    WidgetColor.Red -> R.string.widget_color_red
    WidgetColor.Amber -> R.string.widget_color_amber
    WidgetColor.Orange -> R.string.widget_color_orange
    WidgetColor.Mint -> R.string.widget_color_mint
    WidgetColor.Green -> R.string.widget_color_green
    WidgetColor.Sky -> R.string.widget_color_sky
    WidgetColor.Blue -> R.string.widget_color_blue
    WidgetColor.Lavender -> R.string.widget_color_lavender
    WidgetColor.Graphite -> R.string.widget_color_graphite
    WidgetColor.Forest -> R.string.widget_color_forest
    WidgetColor.Custom -> R.string.widget_color_custom
}

private const val KEY_SWIPE_HINT_SHOWN = "card_swipe_hint_shown"

/**
 * Prejeti prstem po karte na kteroukoli stranu vyvola smazani. Potvrzeni resi dialog vys,
 * takze se karta vzdy vrati zpatky — nic nemizi pod rukou.
 *
 * [hintOffset] je jen posun pro uvodni napovedu; gesto samo bezi pres stav dismiss boxu.
 *
 * [removesFromWidgetOnly] is for extra notes: the swipe then shows a "remove" icon instead of
 * the trash can, because nothing gets deleted.
 */
@Composable
internal fun SwipeToDelete(
    onDelete: () -> Unit,
    hintOffset: Float,
    removesFromWidgetOnly: Boolean = false,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onDelete()
            false
        }
    )
    Box(modifier = Modifier.offset { IntOffset(hintOffset.toInt(), 0) }) {
        SwipeToDismissBox(
            state = state,
            backgroundContent = { DeleteSwipeBackground(state.dismissDirection, removesFromWidgetOnly) },
            content = { content() }
        )
    }
}

@Composable
private fun DeleteSwipeBackground(direction: SwipeToDismissBoxValue, removesFromWidgetOnly: Boolean) {
    val alignment = if (direction == SwipeToDismissBoxValue.EndToStart) {
        Alignment.CenterEnd
    } else {
        Alignment.CenterStart
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            // Red either way, so the swipe reads as "this goes away". The icon and the dialog
            // are what tell removing from the widget apart from deleting the file.
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        if (removesFromWidgetOnly) {
            Icon(
                imageVector = Icons.Default.RemoveCircleOutline,
                contentDescription = stringResource(R.string.card_remove_from_widget),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        } else {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.card_delete_file),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun BoxScope.WatermarkLogo() {
    // Logo je svetle, takze na svetlem pozadi by bylo bila na bile — tam se prebarvi na tmavou
    // navy z ikony. Tma se pozna z pouziteho schematu, ne ze systemoveho nastaveni.
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val horizontalBias = rememberSaveable { Random.nextFloat() * 2f - 1f }
    val verticalBias = rememberSaveable { Random.nextFloat() * 2f - 1f }
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = null,
        colorFilter = if (dark) null else ColorFilter.tint(androidx.compose.ui.graphics.Color(0xFF061B2A)),
        modifier = Modifier
            .align(BiasAlignment(horizontalBias, verticalBias))
            .size(260.dp)
            .alpha(if (dark) 0.14f else 0.10f)
    )
}

/**
 * Odkazy ven a zpetna vazba. Otevirat je muze prohlizec, takze to funguje i ve foss variante,
 * ktera sama opravneni k internetu nema.
 */
@Composable
private fun AboutSection(onSendFeedback: () -> Unit) {
    val context = LocalContext.current
    fun clicked(button: String) = Telemetry.event(UsageEvent.ABOUT_CLICKED, UsageEvent.PARAM_BUTTON to button)

    Text(
        text = stringResource(R.string.about_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.about_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            AboutRow(Icons.Outlined.BugReport, R.string.about_report) {
                clicked(UsageEvent.BUTTON_REPORT_BUG)
                ProjectLinks.open(context, ProjectLinks.ISSUES_URL)
            }
            AboutRow(Icons.Outlined.RateReview, R.string.about_feedback) {
                clicked(UsageEvent.BUTTON_FEEDBACK)
                onSendFeedback()
            }
            AboutRow(Icons.Outlined.StarOutline, RatingPrompt.ABOUT_RATE_RES) {
                clicked(UsageEvent.BUTTON_RATE)
                RateReminder.markRateTapped(context)
                RatingPrompt.openRatePage(context)
            }
            AboutRow(Icons.Outlined.ChatBubbleOutline, R.string.about_say_hi) {
                clicked(UsageEvent.BUTTON_SAY_HI)
                ProjectLinks.open(context, ProjectLinks.DISCUSSIONS_URL)
            }
            AboutRow(Icons.Outlined.Code, R.string.about_source) {
                clicked(UsageEvent.BUTTON_SOURCE)
                ProjectLinks.open(context, ProjectLinks.REPO_URL)
            }
            AboutRow(Icons.Outlined.PrivacyTip, R.string.about_privacy) {
                clicked(UsageEvent.BUTTON_PRIVACY)
                ProjectLinks.open(context, ProjectLinks.PRIVACY_URL)
            }
        }
    }
}

@Composable
private fun AboutRow(icon: ImageVector, @StringRes labelRes: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Poradi je zamerne: git je nejpresnejsi, "nic" je posledni moznost. */
private val SYNC_MODES = listOf(
    "git" to R.string.sync_mode_git,
    "other" to R.string.sync_mode_other,
    "none" to R.string.sync_mode_none
)

private val STALE_CHOICES = listOf(
    6 to R.string.stale_6h,
    24 to R.string.stale_1d,
    72 to R.string.stale_3d,
    168 to R.string.stale_1w
)

@StringRes
private fun themeModeLabel(mode: ThemePreference.Mode): Int = when (mode) {
    ThemePreference.Mode.System -> R.string.theme_system
    ThemePreference.Mode.Light -> R.string.theme_light
    ThemePreference.Mode.Dark -> R.string.theme_dark
}

private val OPEN_WITH_CHOICES = listOf(
    OpenWith.Obsidian to R.string.open_with_obsidian,
    OpenWith.DefaultApp to R.string.open_with_default,
    OpenWith.AskEveryTime to R.string.open_with_ask
)
