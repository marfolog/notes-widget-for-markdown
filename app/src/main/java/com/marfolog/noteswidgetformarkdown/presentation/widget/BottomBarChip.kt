package com.marfolog.noteswidgetformarkdown.presentation.widget

/**
 * The one chip in the right corner of the widget's bottom bar. There is room for one, so the rate
 * chip gives way whenever sync needs attention, and otherwise takes the sync chip's place. It then
 * takes turns with what would have been there: the healthy sync label, so that reassurance does
 * not disappear for two days, or a short line saying how little it asks.
 */
internal sealed interface BottomBarChip {
    data object None : BottomBarChip
    data class Sync(val label: GitSyncLabel.Label) : BottomBarChip
    data class Rate(val alternate: RateAlternate) : BottomBarChip
}

/** What the rate chip cross-fades with. */
internal sealed interface RateAlternate {
    data class SyncLabel(val label: GitSyncLabel.Label) : RateAlternate
    data object Hint : RateAlternate
}

/** The sync label keeps its usual first place; a hint only makes sense after the chip itself. */
internal val RateAlternate.startsWithRate: Boolean
    get() = this is RateAlternate.Hint

/** [syncLabel] is null when the sync chip is switched off in settings. */
internal fun bottomBarChip(rateWindowOpen: Boolean, syncLabel: GitSyncLabel.Label?): BottomBarChip {
    val severity = syncLabel?.severity
    return when {
        syncLabel != null && (!rateWindowOpen || severity == GitSyncLabel.Severity.Stale ||
            severity == GitSyncLabel.Severity.Problem) -> BottomBarChip.Sync(syncLabel)
        !rateWindowOpen -> BottomBarChip.None
        syncLabel != null && severity == GitSyncLabel.Severity.Ok ->
            BottomBarChip.Rate(RateAlternate.SyncLabel(syncLabel))
        else -> BottomBarChip.Rate(RateAlternate.Hint)
    }
}

/** With the add buttons switched off and no chip to show, the bar gives all its room to the notes. */
internal fun showsBottomBar(addButtons: Boolean, chip: BottomBarChip): Boolean =
    addButtons || chip != BottomBarChip.None
