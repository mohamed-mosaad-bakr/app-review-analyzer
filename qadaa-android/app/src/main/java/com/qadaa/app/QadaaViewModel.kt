package com.qadaa.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qadaa.app.data.QadaaData
import com.qadaa.app.data.QadaaStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class QadaaViewModel(app: Application) : AndroidViewModel(app) {
    private val store = QadaaStore(app)

    /** null until the saved data has been read from disk. */
    val data: StateFlow<QadaaData?> = store.data.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setOwed(v: Int) = viewModelScope.launch { store.setOwed(v) }
    fun setDeadline(d: LocalDate) = viewModelScope.launch { store.setDeadline(d) }
    fun setCity(id: String) = viewModelScope.launch { store.setCity(id) }
    fun markFasted(d: LocalDate, done: Boolean = true) = viewModelScope.launch {
        store.toggleCompleted(d, done)
        if (done) store.toggleUnavailable(d, false)
    }
    fun exclude(d: LocalDate, excluded: Boolean = true) = viewModelScope.launch { store.toggleUnavailable(d, excluded) }
}
