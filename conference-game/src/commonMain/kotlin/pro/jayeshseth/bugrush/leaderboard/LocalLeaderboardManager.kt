package pro.jayeshseth.bugrush.leaderboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object LocalLeaderboardManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val storageFile: File by lazy {
        val userHome = System.getProperty("user.home") ?: "."
        val dir = File(userHome, ".devcontrols/leaderboard")
        if (!dir.exists()) dir.mkdirs()
        File(dir, "conference_scores.json")
    }

    private val _entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val entries = _entries.asStateFlow()

    init {
        loadEntries()
    }

    fun loadEntries() {
        try {
            if (storageFile.exists()) {
                val content = storageFile.readText()
                val loaded = json.decodeFromString<List<LeaderboardEntry>>(content)
                _entries.value = loaded.sortedByDescending { it.score }
                return
            }
        } catch (_: Exception) {
            // Fall through to empty list
        }

        _entries.value = emptyList()
    }

    fun clearEntries() {
        _entries.value = emptyList()
        try {
            if (storageFile.exists()) {
                storageFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addEntry(entry: LeaderboardEntry): Int {
        val current = _entries.value.toMutableList()
        current.add(entry)
        val sorted = current.sortedByDescending { it.score }
        _entries.value = sorted
        saveToFile()
        return sorted.indexOf(entry) + 1 // 1-based rank
    }

    private fun saveToFile() {
        try {
            val content = json.encodeToString(_entries.value)
            storageFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
