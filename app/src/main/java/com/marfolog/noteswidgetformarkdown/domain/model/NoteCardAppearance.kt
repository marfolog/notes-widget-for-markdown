package com.marfolog.noteswidgetformarkdown.domain.model

data class NoteCardAppearance(
    val size: NoteCardSize = NoteCardSize.Compact,
    val color: NoteCardColor = NoteCardColor.Default,
    val textSize: NoteCardTextSize = NoteCardTextSize.Default,
    val customColorHex: String? = null
)

enum class NoteCardSize(
    val storageValue: String,
    val label: String,
    val heightDp: Int,
    val previewMaxLines: Int
) {
    Compact("compact", "Compact", 96, 4),
    Medium("medium", "Medium", 156, 9),
    Large("large", "Large", 260, 18);

    companion object {
        fun fromStorage(value: String?): NoteCardSize {
            return entries.firstOrNull { it.storageValue == value } ?: Compact
        }
    }
}

enum class NoteCardColor(
    val storageValue: String,
    val label: String
) {
    Default("default", "Default"),
    Rose("rose", "Rose"),
    Red("red", "Red"),
    Amber("amber", "Amber"),
    Orange("orange", "Orange"),
    Mint("mint", "Mint"),
    Green("green", "Green"),
    Sky("sky", "Sky"),
    Blue("blue", "Blue"),
    Lavender("lavender", "Lavender"),
    Custom("custom", "Custom");

    companion object {
        fun fromStorage(value: String?): NoteCardColor {
            return entries.firstOrNull { it.storageValue == value } ?: Default
        }
    }
}

enum class NoteCardTextSize(
    val storageValue: String,
    val label: String,
    val titleSp: Int,
    val previewSp: Int
) {
    Default("default", "Default", 14, 12),
    Large("large", "Large", 16, 14),
    ExtraLarge("extra_large", "Extra", 18, 16);

    companion object {
        fun fromStorage(value: String?): NoteCardTextSize {
            return entries.firstOrNull { it.storageValue == value } ?: Default
        }
    }
}
