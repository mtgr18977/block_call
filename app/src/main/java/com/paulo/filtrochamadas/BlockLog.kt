package com.paulo.filtrochamadas

import android.content.Context
import java.io.File

/** Histórico local das chamadas bloqueadas (um arquivo de texto no armazenamento privado do app). */
object BlockLog {
    private const val FILE = "blocklog.txt"
    private const val MAX = 200

    data class Entry(val time: Long, val number: String, val reason: String)

    @Synchronized
    fun add(ctx: Context, number: String?, reason: String) {
        val f = File(ctx.filesDir, FILE)
        val line = "${System.currentTimeMillis()}|${number ?: "Oculto"}|$reason"
        val lines = (if (f.exists()) f.readLines() else emptyList()) + line
        f.writeText(lines.takeLast(MAX).joinToString("\n") + "\n")
    }

    /** Mais recentes primeiro. */
    @Synchronized
    fun read(ctx: Context): List<Entry> {
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return emptyList()
        return f.readLines().mapNotNull {
            val p = it.split("|", limit = 3)
            val t = p.getOrNull(0)?.toLongOrNull()
            if (p.size == 3 && t != null) Entry(t, p[1], p[2]) else null
        }.reversed()
    }

    @Synchronized
    fun clear(ctx: Context) {
        File(ctx.filesDir, FILE).delete()
    }
}
