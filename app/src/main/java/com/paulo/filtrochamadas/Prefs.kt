package com.paulo.filtrochamadas

import android.content.Context

object Prefs {
    private const val FILE = "filtro"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST_SCREEN = "last_screen"
    private const val KEY_LAST_BIND = "last_bind"

    private fun p(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context): Boolean = p(ctx).getBoolean(KEY_ENABLED, true)

    fun setEnabled(ctx: Context, value: Boolean) {
        p(ctx).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    // Diagnóstico: quando o sistema acionou o serviço pela última vez (0 = nunca).
    fun markServiceCreated(ctx: Context) {
        p(ctx).edit().putLong(KEY_LAST_BIND, System.currentTimeMillis()).apply()
    }

    fun markCallScreened(ctx: Context) {
        p(ctx).edit().putLong(KEY_LAST_SCREEN, System.currentTimeMillis()).apply()
    }

    fun lastServiceCreated(ctx: Context): Long = p(ctx).getLong(KEY_LAST_BIND, 0L)
    fun lastCallScreened(ctx: Context): Long = p(ctx).getLong(KEY_LAST_SCREEN, 0L)
}
