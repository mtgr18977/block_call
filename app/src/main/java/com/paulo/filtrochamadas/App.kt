package com.paulo.filtrochamadas

import android.app.Application
import com.google.android.material.color.DynamicColors

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Usa as cores do papel de parede/tema do aparelho (Material You), quando disponível.
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
