package com.marfolog.noteswidgetformarkdown.domain.model

data class NoteSummary(
    val id: String,
    val title: String,
    val preview: String,
    val lastModified: Long,
    val fileUri: String,
    val fileName: String
)
