package com.marfolog.noteswidgetformarkdown.presentation.widget

/**
 * What a new note opens, in the order the activities are started; the last one ends up on top. After
 * a "Rate" the store goes over the editor, so Back from the store lands in the editor instead of the
 * editor being started from behind the store and the rating page being lost.
 */
internal fun <T> newNoteLaunchOrder(rated: Boolean, editor: T, ratePage: T): List<T> =
    if (rated) listOf(editor, ratePage) else listOf(editor)
