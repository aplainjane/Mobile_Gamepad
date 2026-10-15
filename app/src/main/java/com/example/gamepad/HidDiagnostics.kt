package com.example.gamepad

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/** A bounded journal that survives transport restarts and is saved when the app stops. */
class HidDiagnostics(context: Context) {
    private val file = File(context.cacheDir, "hid-diagnostics.log")
    private val entries = ArrayDeque<String>()
    private val clock = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)

    init {
        runCatching {
            if (file.exists() && file.length() <= 1_000_000) {
                file.readLines()
                    .filterNot { " Input submitted " in it || " Touch updates=" in it }
                    .takeLast(800).forEach(entries::addLast)
            }
        }
        record("App diagnostic journal opened")
        persist()
    }

    fun record(message: String) {
        val line = "${clock.format(Date())} ${message.replace('\n', ' ').take(700)}"
        if (entries.size >= 800) entries.removeFirst()
        entries.addLast(line)
        Log.i("PocketPadHID", line)
    }

    fun text(): String = entries.joinToString("\n")

    fun persist() {
        runCatching { file.writeText(text()) }
            .onFailure { Log.w("PocketPadHID", "Unable to save diagnostic journal", it) }
    }
}
