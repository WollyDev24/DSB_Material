package dev.wolly.dsbmaterial.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.wolly.dsbmaterial.api.DSBMobileAPI
import dev.wolly.dsbmaterial.data.DataStoreManager
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val entries: List<SubstitutionEntry>) : UiState()
    data class Error(val message: String) : UiState()
    object NeedsLogin : UiState()
    data class SelectingClass(val classes: List<String>, val u: String, val p: String) : UiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStoreManager = DataStoreManager(application)
    private val gson = Gson()
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    val isRoomFirst: StateFlow<Boolean> = dataStoreManager.swapDataFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicColor: StateFlow<Boolean> = dataStoreManager.dynamicColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val sortByPeriod: StateFlow<Boolean> = dataStoreManager.sortPeriodFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _archive = MutableStateFlow<List<SubstitutionEntry>>(emptyList())
    val archive: StateFlow<List<SubstitutionEntry>> = _archive

    private var lastSuccessEntries: List<SubstitutionEntry> = emptyList()

    init {
        checkCredentialsAndFetch()
        loadArchive()
    }

    private fun loadArchive() {
        viewModelScope.launch {
            dataStoreManager.archiveFlow.collect { json ->
                if (!json.isNullOrEmpty()) {
                    val type = object : TypeToken<List<SubstitutionEntry>>() {}.type
                    val entries: List<SubstitutionEntry> = gson.fromJson(json, type)
                    _archive.value = sortArchive(entries)
                }
            }
        }
    }

    private fun sortArchive(entries: List<SubstitutionEntry>): List<SubstitutionEntry> {
        return entries.sortedWith(
            compareBy<SubstitutionEntry> { it.day }
                .thenBy { it.lesson.filter { c -> c.isDigit() }.toIntOrNull() ?: 999 }
        )
    }

    fun archiveSubstitutions(entries: List<SubstitutionEntry>? = null) {
        val toArchive = entries ?: lastSuccessEntries
        if (toArchive.isNotEmpty()) {
            viewModelScope.launch {
                val newArchive = (toArchive + _archive.value).distinctBy { 
                    it.day + it.lesson + it.subject + it.room + it.art + it.text 
                }
                val sortedArchive = sortArchive(newArchive)
                _archive.value = sortedArchive
                dataStoreManager.saveArchive(gson.toJson(sortedArchive))
            }
        }
    }

    fun removeFromArchive(entry: SubstitutionEntry) {
        viewModelScope.launch {
            val newArchive = _archive.value.filter { it != entry }
            _archive.value = newArchive
            dataStoreManager.saveArchive(gson.toJson(newArchive))
        }
    }

    fun clearArchive() {
        viewModelScope.launch {
            _archive.value = emptyList()
            dataStoreManager.saveArchive("")
        }
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun toggleColumnOrder() {
        viewModelScope.launch {
            dataStoreManager.saveSwapPreference(!isRoomFirst.value)
        }
    }

    fun toggleDynamicColor() {
        viewModelScope.launch {
            dataStoreManager.saveDynamicColorPreference(!dynamicColor.value)
        }
    }

    fun toggleSortByPeriod() {
        viewModelScope.launch {
            dataStoreManager.saveSortPreference(!sortByPeriod.value)
            // Re-sort if we have data
            if (_uiState.value is UiState.Success) {
                _uiState.value = UiState.Success(sortEntries(lastSuccessEntries))
            }
        }
    }

    fun openSettings() {
        _selectedTab.value = 2
    }

    fun closeSettings() {
        _selectedTab.value = 0
        if (lastSuccessEntries.isNotEmpty()) {
            _uiState.value = UiState.Success(sortEntries(lastSuccessEntries))
        } else {
            checkCredentialsAndFetch()
        }
    }

    fun changeClass() {
        viewModelScope.launch {
            val u = dataStoreManager.usernameFlow.first() ?: ""
            val p = dataStoreManager.passwordFlow.first() ?: ""
            if (u.isNotEmpty() && p.isNotEmpty()) {
                fetchClasses(u, p)
            } else {
                _uiState.value = UiState.NeedsLogin
            }
        }
    }

    fun cancelClassSelection() {
        viewModelScope.launch {
            val className = dataStoreManager.classNameFlow.first() ?: ""
            if (className.isEmpty()) {
                _uiState.value = UiState.NeedsLogin
            } else {
                openSettings()
            }
        }
    }

    fun checkCredentialsAndFetch() {
        viewModelScope.launch {
            val username = dataStoreManager.usernameFlow.first()
            val password = dataStoreManager.passwordFlow.first()
            val className = dataStoreManager.classNameFlow.first() ?: ""

            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                _uiState.value = UiState.NeedsLogin
            } else if (className.isEmpty()) {
                fetchClasses(username, password)
            } else {
                fetchData(username, password, className)
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            fetchClasses(username, password)
        }
    }

    private suspend fun fetchClasses(u: String, p: String) {
        _uiState.value = UiState.Loading
        try {
            val api = DSBMobileAPI(u, p)
            val classes = api.getAvailableClasses()
            if (classes.isEmpty()) {
                _uiState.value = UiState.Error("No classes found. Check your credentials.")
            } else {
                _uiState.value = UiState.SelectingClass(classes, u, p)
            }
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Login failed")
        }
    }

    fun selectClass(username: String, password: String, className: String) {
        viewModelScope.launch {
            dataStoreManager.saveCredentials(username, password, className)
            fetchData(username, password, className)
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            dataStoreManager.clearCredentials()
            _uiState.value = UiState.NeedsLogin
            _selectedTab.value = 0
        }
    }

    fun fetchData() {
        checkCredentialsAndFetch()
    }

    private suspend fun fetchData(u: String, p: String, c: String) {
        _uiState.value = UiState.Loading
        try {
            val api = DSBMobileAPI(u, p)
            val entries = api.getSubstitutions(c)
            lastSuccessEntries = entries
            _uiState.value = UiState.Success(sortEntries(entries))
            // Auto-archive
            archiveSubstitutions(entries)
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Unknown error")
        }
    }

    private fun sortEntries(entries: List<SubstitutionEntry>): List<SubstitutionEntry> {
        if (!sortByPeriod.value) return entries
        return entries.sortedBy { it.lesson.filter { c -> c.isDigit() }.toIntOrNull() ?: 999 }
    }
}
