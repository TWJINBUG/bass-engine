package com.bassengine.control.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(val timestamp: Long, val message: String) {
    fun timeText(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

class LogRepository private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("bass_engine_logs", Context.MODE_PRIVATE)
    private val _logs = MutableStateFlow(load())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun add(message: String) {
        synchronized(this) {
            val next = (_logs.value + LogEntry(System.currentTimeMillis(), message)).takeLast(200)
            _logs.value = next
            persist(next)
        }
    }

    fun clear() {
        synchronized(this) {
            _logs.value = emptyList()
            prefs.edit().remove(KEY).apply()
        }
    }

    fun exportText(): String =
        _logs.value.joinToString("\n") { "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.timestamp))}  ${it.message}" }

    private fun persist(list: List<LogEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(org.json.JSONObject().apply {
                put("t", it.timestamp)
                put("m", it.message)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun load(): List<LogEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(LogEntry(o.optLong("t"), o.optString("m")))
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    companion object {
        private const val KEY = "logs_json"

        @Volatile
        private var instance: LogRepository? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) instance = LogRepository(context.applicationContext)
                }
            }
        }

        fun get(): LogRepository = instance
            ?: throw IllegalStateException("LogRepository is not initialised")
    }
}
