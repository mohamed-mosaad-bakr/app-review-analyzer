package com.qadaa.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Everything the user entered; persisted on disk so it survives closing the app. */
data class QadaaData(
    val owed: Int = 0,
    val deadline: LocalDate? = null,
    val cityId: String? = null,
    val completed: Set<LocalDate> = emptySet(),
    val unavailable: Set<LocalDate> = emptySet(),
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "qadaa")

class QadaaStore(private val context: Context) {
    private object Keys {
        val OWED = intPreferencesKey("owed")
        val DEADLINE = stringPreferencesKey("deadline")
        val CITY = stringPreferencesKey("city")
        val COMPLETED = stringSetPreferencesKey("completed")
        val UNAVAILABLE = stringSetPreferencesKey("unavailable")
    }

    private fun Set<String>?.dates(): Set<LocalDate> =
        orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()

    val data: Flow<QadaaData> = context.dataStore.data.map { p ->
        QadaaData(
            owed = p[Keys.OWED] ?: 0,
            deadline = p[Keys.DEADLINE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            cityId = p[Keys.CITY],
            completed = p[Keys.COMPLETED].dates(),
            unavailable = p[Keys.UNAVAILABLE].dates(),
        )
    }

    suspend fun setOwed(v: Int) { context.dataStore.edit { it[Keys.OWED] = v } }
    suspend fun setDeadline(d: LocalDate) { context.dataStore.edit { it[Keys.DEADLINE] = d.toString() } }
    suspend fun setCity(id: String) { context.dataStore.edit { it[Keys.CITY] = id } }

    suspend fun toggleCompleted(d: LocalDate, done: Boolean) = editSet(Keys.COMPLETED, d, done)
    suspend fun toggleUnavailable(d: LocalDate, excluded: Boolean) = editSet(Keys.UNAVAILABLE, d, excluded)

    private suspend fun editSet(key: Preferences.Key<Set<String>>, d: LocalDate, add: Boolean) {
        context.dataStore.edit { p ->
            val cur = p[key].orEmpty()
            p[key] = if (add) cur + d.toString() else cur - d.toString()
        }
    }
}
