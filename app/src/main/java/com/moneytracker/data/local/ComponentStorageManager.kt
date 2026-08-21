package com.moneytracker.data.local

import android.content.Context
import com.moneytracker.ui.components.ComboboxSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Autonomous Internal Files Document Storage Manager.
 * Persists and loads hierarchical component data using the coordinate address:
 * {
 *   owner_screen: {
 *     component_id: {
 *       component_label: {
 *         Filter_Key: [ Filter_Values... ]
 *       }
 *     }
 *   }
 * }
 */
class ComponentStorageManager private constructor(private val context: Context) {

    private val mutex = Mutex()
    private val memoryCache = mutableMapOf<String, JSONObject>()

    companion object {
        @Volatile
        private var INSTANCE: ComponentStorageManager? = null

        fun getInstance(context: Context): ComponentStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ComponentStorageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun getStorageDir(): File {
        val dir = File(context.filesDir, "component_stores")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getScreenFile(screenId: String): File {
        return File(getStorageDir(), "${screenId}.json")
    }

    private fun getSettingsFile(): File {
        return File(getStorageDir(), "combobox_settings.json")
    }

    /**
     * Reads the screen JSON root object with in-memory caching and thread-safety
     */
    private suspend fun getScreenRoot(screenId: String): JSONObject = withContext(Dispatchers.IO) {
        mutex.withLock {
            memoryCache[screenId]?.let { return@withContext it }

            val file = getScreenFile(screenId)
            val jsonObject = if (file.exists()) {
                try {
                    val content = file.readText()
                    if (content.isNotBlank()) JSONObject(content) else JSONObject()
                } catch (e: Exception) {
                    JSONObject()
                }
            } else {
                JSONObject()
            }
            memoryCache[screenId] = jsonObject
            jsonObject
        }
    }

    /**
     * Writes the screen JSON root object back to disk atomically
     */
    private suspend fun persistScreenRoot(screenId: String, root: JSONObject) = withContext(Dispatchers.IO) {
        mutex.withLock {
            memoryCache[screenId] = root
            val file = getScreenFile(screenId)
            file.writeText(root.toString(2))
        }
    }

    /**
     * Retrieves values for:
     * owner_screen -> component_id -> component_label -> filter_key
     * Auto-creates default values on disk if not found.
     */
    suspend fun getValues(
        screenId: String,
        componentId: String,
        label: String,
        filterKey: String?,
        defaultValues: List<String> = emptyList()
    ): List<String> = withContext(Dispatchers.IO) {
        val root = getScreenRoot(screenId)
        val normalizedKey = filterKey?.trim()?.ifBlank { "__root__" } ?: "__root__"

        val screenObj = root.optJSONObject(screenId)
        val compObj = screenObj?.optJSONObject(componentId)
        val labelObj = compObj?.optJSONObject(label)
        val array = labelObj?.optJSONArray(normalizedKey)

        if (array != null && array.length() > 0) {
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val str = array.optString(i)
                if (str.isNotBlank()) list.add(str)
            }
            return@withContext list
        }

        // If not found and default values provided, seed them now into the JSON!
        if (defaultValues.isNotEmpty()) {
            saveValues(screenId, componentId, label, filterKey, defaultValues)
            return@withContext defaultValues
        }

        emptyList()
    }

    /**
     * Sets / overwrites the values array for a specific coordinate
     */
    suspend fun saveValues(
        screenId: String,
        componentId: String,
        label: String,
        filterKey: String?,
        values: List<String>
    ) = withContext(Dispatchers.IO) {
        val root = getScreenRoot(screenId)
        val normalizedKey = filterKey?.trim()?.ifBlank { "__root__" } ?: "__root__"

        val screenObj = root.optJSONObject(screenId) ?: JSONObject().also { root.put(screenId, it) }
        val compObj = screenObj.optJSONObject(componentId) ?: JSONObject().also { screenObj.put(componentId, it) }
        val labelObj = compObj.optJSONObject(label) ?: JSONObject().also { compObj.put(label, it) }

        val jsonArray = JSONArray()
        values.distinct().filter { it.isNotBlank() }.forEach { jsonArray.put(it) }
        labelObj.put(normalizedKey, jsonArray)

        persistScreenRoot(screenId, root)
    }

    /**
     * Adds an item to the specific coordinate
     */
    suspend fun addItem(
        screenId: String,
        componentId: String,
        label: String,
        filterKey: String?,
        newItem: String
    ): List<String> = withContext(Dispatchers.IO) {
        val trimmed = newItem.trim()
        if (trimmed.isBlank()) return@withContext getValues(screenId, componentId, label, filterKey)

        val current = getValues(screenId, componentId, label, filterKey).toMutableList()
        if (!current.any { it.equals(trimmed, ignoreCase = true) }) {
            current.add(trimmed)
            saveValues(screenId, componentId, label, filterKey, current)
        }
        current
    }

    /**
     * Edits an item at the specific coordinate
     */
    suspend fun editItem(
        screenId: String,
        componentId: String,
        label: String,
        filterKey: String?,
        oldItem: String,
        newItem: String
    ): List<String> = withContext(Dispatchers.IO) {
        val trimmedNew = newItem.trim()
        if (trimmedNew.isBlank()) return@withContext getValues(screenId, componentId, label, filterKey)

        val current = getValues(screenId, componentId, label, filterKey).toMutableList()
        val index = current.indexOfFirst { it.equals(oldItem, ignoreCase = true) }
        if (index >= 0) {
            current[index] = trimmedNew
            saveValues(screenId, componentId, label, filterKey, current)
        }
        current
    }

    /**
     * Deletes an item from the specific coordinate
     */
    suspend fun deleteItem(
        screenId: String,
        componentId: String,
        label: String,
        filterKey: String?,
        itemToDelete: String
    ): List<String> = withContext(Dispatchers.IO) {
        val current = getValues(screenId, componentId, label, filterKey).toMutableList()
        val removed = current.removeAll { it.equals(itemToDelete, ignoreCase = true) }
        if (removed) {
            saveValues(screenId, componentId, label, filterKey, current)
        }
        current
    }

    // --- GEAR SETTINGS PERSISTENCE ---

    suspend fun getSettings(screenId: String, componentId: String): ComboboxSettings = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = getSettingsFile()
            if (!file.exists()) return@withContext ComboboxSettings()

            try {
                val json = JSONObject(file.readText())
                val key = "${screenId}_${componentId}"
                val obj = json.optJSONObject(key) ?: return@withContext ComboboxSettings()

                ComboboxSettings(
                    pillRows = obj.optInt("pillRows", 1),
                    maxVisibleItems = obj.optInt("maxVisibleItems", 5),
                    scrollStep = obj.optInt("scrollStep", 3),
                    isAlphabeticalSort = obj.optBoolean("isAlphabeticalSort", false)
                )
            } catch (e: Exception) {
                ComboboxSettings()
            }
        }
    }

    suspend fun saveSettings(screenId: String, componentId: String, settings: ComboboxSettings) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = getSettingsFile()
            val json = if (file.exists()) {
                try { JSONObject(file.readText()) } catch (e: Exception) { JSONObject() }
            } else {
                JSONObject()
            }

            val key = "${screenId}_${componentId}"
            val obj = JSONObject().apply {
                put("pillRows", settings.pillRows)
                put("maxVisibleItems", settings.maxVisibleItems)
                put("scrollStep", settings.scrollStep)
                put("isAlphabeticalSort", settings.isAlphabeticalSort)
            }
            json.put(key, obj)
            file.writeText(json.toString(2))
        }
    }
}
