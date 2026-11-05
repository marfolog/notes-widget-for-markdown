package com.marfolog.noteswidgetformarkdown.debug

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.marfolog.noteswidgetformarkdown.presentation.setup.SetupActivity
import com.marfolog.noteswidgetformarkdown.presentation.widget.NotesWidget
import com.marfolog.noteswidgetformarkdown.presentation.widget.WidgetInventory
import com.marfolog.noteswidgetformarkdown.util.AfterNoteRating
import com.marfolog.noteswidgetformarkdown.util.AppClock
import com.marfolog.noteswidgetformarkdown.util.FeatureInterest
import com.marfolog.noteswidgetformarkdown.util.RateReminder
import com.marfolog.noteswidgetformarkdown.util.SettingsVisits
import com.marfolog.noteswidgetformarkdown.util.Telemetry
import com.marfolog.noteswidgetformarkdown.util.UsageEvent
import com.marfolog.noteswidgetformarkdown.util.WhatsNewReminder
import com.marfolog.noteswidgetformarkdown.util.prompt.PROMPTS
import com.marfolog.noteswidgetformarkdown.util.prompt.PromptContext
import com.marfolog.noteswidgetformarkdown.util.prompt.PromptFacts
import com.marfolog.noteswidgetformarkdown.util.prompt.PromptSurfaces
import com.marfolog.noteswidgetformarkdown.util.prompt.Surface
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

/**
 * Debug build only. Moves the clock the prompts go by, shows where every prompt stands and resets
 * them, so that weeks of use can be tried in minutes. Nothing here changes the time of a file or
 * sends anything: the clock is only read where prompts decide.
 */
object DebugTools {

    private const val TAG = "NWPrompts"

    /** The state of every prompt in the registry, as lines, also written to logcat under `NWPrompts`. */
    fun report(context: Context): List<String> {
        val now = AppClock.now()
        val history = PromptSurfaces.history(context)
        val facts = facts(context, now)
        val lines = mutableListOf(
            "clock ${DateFormat.getDateTimeInstance().format(Date(now))} (offset ${AppClock.offsetDays()} d)",
            "settings visits ${SettingsVisits.count(context)}, interest banner shown " +
                "${FeatureInterest.bannerShows(context, UsageEvent.FEATURE_MULTI_FOLDER_WIDGETS)}x"
        )
        PROMPTS.forEach { def ->
            val due = Surface.entries.filter { it in def.surfaces }.joinToString("/") { surface ->
                "${surface.name}=${def.isDue(PromptContext(now, surface, facts, history))}"
            }
            lines += "${def.id} [${def.kind}] due: $due shown: ${time(history.shownAt(def.id))} " +
                "answered: ${time(history.answeredAt(def.id))}"
        }
        lines += "last interrupting: ${time(history.lastInterruptingAt())}"
        lines.forEach { Log.i(TAG, it) }
        return lines
    }

    /** Best effort: what is known without a screen, so the cards of a visit are not in it. */
    private fun facts(context: Context, now: Long): PromptFacts {
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val folder = prefs.getString(SetupActivity.KEY_NOTES_URI, null) != null
        val guideDone = prefs.getBoolean(SetupActivity.KEY_GUIDE_DONE, false)
        return PromptFacts(
            canReport = Telemetry.ENABLED,
            reportingAsked = Telemetry.hasBeenAsked(context),
            notesFolderChosen = folder,
            widgetsPlaced = WidgetInventory.Kind.entries.sumOf { WidgetInventory.count(context, it) },
            guideSkipped = prefs.getBoolean(SetupActivity.KEY_GUIDE_SKIPPED, false),
            guideDone = guideDone && folder,
            setupComplete = guideDone && folder,
            whatsNewDue = WhatsNewReminder.shouldShow(context, guideDone && folder, null),
            whatsNewMandatory = WhatsNewReminder.needsButtonsChoice(context),
            ratingAsk = AfterNoteRating.askFor(context, saved = true, now),
            rateChipOpen = RateReminder.openWindow(context, now) != null,
            settingsVisits = SettingsVisits.count(context),
            interestAnswered = FeatureInterest.isAnswered(context, UsageEvent.FEATURE_MULTI_FOLDER_WIDGETS),
            interestBannerShows = FeatureInterest.bannerShows(context, UsageEvent.FEATURE_MULTI_FOLDER_WIDGETS)
        )
    }

    private fun time(millis: Long?): String =
        millis?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "never"

    /** Forgets what the prompts remember, the way a new install would have it. */
    fun resetPrompts(context: Context) {
        val prefs = context.getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val prefixes = listOf(
            "prompt.", "after_note_rating_", "rate_chip_", "feature_interest_", "whats_new_",
            "last_auto_prompt_at", "rating_last_request_at", "settings_visits", "telemetry_asked", "last_version_seen"
        )
        val editor = prefs.edit()
        prefs.all.keys.filter { key -> prefixes.any { key.startsWith(it) } }.forEach { editor.remove(it) }
        editor.apply()
        Log.i(TAG, "prompts reset")
    }

    @Composable
    fun Section() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var refresh by remember { mutableIntStateOf(0) }
        fun shift(change: () -> Unit) {
            change()
            refresh++
            scope.launch { NotesWidget.updateAll(context) }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Debug: time travel",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Moves only what prompts decide by. No file time changes and nothing is sent.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { shift { AppClock.addDays(context, 1) } }) { Text("+1 day", maxLines = 1) }
            OutlinedButton(onClick = { shift { AppClock.addDays(context, 7) } }) { Text("+7 days", maxLines = 1) }
            OutlinedButton(onClick = { shift { AppClock.reset(context) } }) { Text("Reset time", maxLines = 1) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { shift { resetPrompts(context) } }, modifier = Modifier.fillMaxWidth()) {
            Text("Reset prompts")
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Read again after every tap; the lines are the ones logged under NWPrompts.
        val lines = remember(refresh) { report(context) }
        Column(modifier = Modifier.padding(top = 4.dp)) {
            lines.forEach { line ->
                Text(text = line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
