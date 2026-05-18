package com.marfolog.noteswidgetformarkdown.domain.model

data class NoteCardAppearance(
    val size: NoteCardSize = NoteCardSize.Compact,
    val color: NoteCardColor = NoteCardColor.Default
)

enum class NoteCardSize(
    val storageValue: String,
    val label: String,
    val heightDp: Int,
    val previewMaxLines: Int
) {
    Compact("compact", "Compact", 96, 4),
    Medium("medium", "Medium", 144, 8),
    Large("large", "Large", 216, 14);

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
    Amber("amber", "Amber"),
    Mint("mint", "Mint"),
    Sky("sky", "Sky"),
    Lavender("lavender", "Lavender");

    companion object {
        fun fromStorage(value: String?): NoteCardColor {
            return entries.firstOrNull { it.storageValue == value } ?: Default
        }
    }
}
