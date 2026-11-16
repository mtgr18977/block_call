package com.paulo.filtrochamadas

import android.content.Context
import java.io.File

/**
 * Histórico local de TODAS as ligações analisadas (bloqueadas e permitidas), com o motivo.
 * Um arquivo de texto no armazenamento privado do app: tempo|número|B ou A|motivo
 */
object BlockLog {
    private const val FILE = "blocklog.txt"
    private const val MAX = 300

    data class Entry(val time: Long, val number: String, val blocked: Boolean, val reason: String)

    @Synchronized
    fun add(ctx: Context, number: String?, blocked: Boolean, reason: String) {
        val f = File(ctx.filesDir, FILE)
        val line = "${System.currentTimeMillis()}|${number ?: "Oculto"}|${if (blocked) "B" else "A"}|$reason"
        val lines = (if (f.exists()) f.readLines() else emptyList()) + line
        f.writeText(lines.takeLast(MAX).joinToString("\n") + "\n")
    }

    /** Mais recentes primeiro. Aceita também o formato antigo (3 campos = sempre bloqueada). */
    @Synchronized
    fun read(ctx: Context): List<Entry> {
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return emptyList()
        return f.readLines().mapNotNull {
            val p = it.split("|", limit = 4)
            val t = p.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            when (p.size) {
                4 -> Entry(t, p[1], p[2] == "B", p[3])
                3 -> Entry(t, p[1], true, p[2])
                else -> null
            }
        }.reversed()
    }

    @Synchronized
    fun clear(ctx: Context) {
        File(ctx.filesDir, FILE).delete()
    }
}
