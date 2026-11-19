package com.paulo.filtrochamadas

import android.app.Application
import com.google.android.material.color.DynamicColors

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Usa as cores do papel de parede/tema do aparelho (Material You), quando disponível.
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Se o app travar, guarda o motivo antes de morrer (e repassa ao tratador original).
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler { thread, error ->
            try {
                Diagnostics.recordCrash(this, "travou na thread ${thread.name}", error)
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        })

        try {
            Diagnostics.checkBoot(this)
        } catch (_: Throwable) {
        }
    }
}
