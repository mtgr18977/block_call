package com.paulo.filtrochamadas

import android.content.Context
import android.os.SystemClock
import java.io.File
import kotlin.math.abs

/**
 * Registros de diagnóstico, só no armazenamento privado do app:
 *  - reinícios do aparelho (calculados pela hora de boot, sem precisar de permissão extra)
 *  - falhas/exceções do próprio app
 * Servem pra cruzar horários com o histórico de ligações e saber se o app tem relação
 * com algum reinício.
 */
object Diagnostics {
    private const val BOOTS = "boots.txt"
    private const val CRASHES = "crashes.txt"
    private const val MAX_BOOTS = 30
    private const val MAX_CRASHES = 10
    private const val SEP = "\n=====\n"

    data class Boot(val bootTime: Long, val seenAt: Long)

    /** Hora em que o aparelho ligou = agora - tempo desde o boot. Muda quando o aparelho reinicia. */
    @Synchronized
    fun checkBoot(ctx: Context) {
        val now = System.currentTimeMillis()
        val boot = now - SystemClock.elapsedRealtime()
        val f = File(ctx.filesDir, BOOTS)
        val lines = if (f.exists()) f.readLines() else emptyList()
        val last = lines.lastOrNull()?.split("|")?.getOrNull(0)?.toLongOrNull()
        // Tolerância de 2 min: ajustes automáticos do relógio deslocam um pouco esse cálculo.
        if (last == null || abs(boot - last) > 120_000) {
            f.writeText((lines + "$boot|$now").takeLast(MAX_BOOTS).joinToString("\n") + "\n")
        }
    }

    /** Mais recentes primeiro. O mais antigo é só a primeira leitura (não é um reinício). */
    @Synchronized
    fun boots(ctx: Context): List<Boot> {
        val f = File(ctx.filesDir, BOOTS)
        if (!f.exists()) return emptyList()
        return f.readLines().mapNotNull {
            val p = it.split("|")
            val b = p.getOrNull(0)?.toLongOrNull()
            val s = p.getOrNull(1)?.toLongOrNull()
            if (b != null && s != null) Boot(b, s) else null
        }.reversed()
    }

    @Synchronized
    fun recordCrash(ctx: Context, where: String, t: Throwable) {
        val f = File(ctx.filesDir, CRASHES)
        val old = if (f.exists()) f.readText().split(SEP).filter { it.isNotBlank() } else emptyList()
        val stack = android.util.Log.getStackTraceString(t).take(1800)
        val entry = "${System.currentTimeMillis()}|$where\n$stack"
        f.writeText((old + entry).takeLast(MAX_CRASHES).joinToString(SEP))
    }

    /** Cada item: primeira linha "tempo|onde", depois o stack trace. Mais recentes primeiro. */
    @Synchronized
    fun crashes(ctx: Context): List<String> {
        val f = File(ctx.filesDir, CRASHES)
        if (!f.exists()) return emptyList()
        return f.readText().split(SEP).filter { it.isNotBlank() }.reversed()
    }

    @Synchronized
    fun clear(ctx: Context) {
        File(ctx.filesDir, BOOTS).delete()
        File(ctx.filesDir, CRASHES).delete()
    }
}
