package com.marfolog.noteswidgetformarkdown.util

/** A [KeyValueStore] in a map, for tests of logic that reads and writes preferences. */
internal class InMemoryKeyValueStore : KeyValueStore {
    val values = mutableMapOf<String, Any>()
    override fun contains(key: String) = key in values
    override fun getLong(key: String) = values[key] as Long?
    override fun putLong(key: String, value: Long) { values[key] = value }
    override fun getInt(key: String) = values[key] as Int?
    override fun putInt(key: String, value: Int) { values[key] = value }
    override fun getString(key: String) = values[key] as String?
    override fun putString(key: String, value: String) { values[key] = value }
    override fun getBoolean(key: String, default: Boolean) = values[key] as Boolean? ?: default
    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}
