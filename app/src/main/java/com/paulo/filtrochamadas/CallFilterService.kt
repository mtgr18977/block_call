package com.paulo.filtrochamadas

import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager

/**
 * Chamado pelo sistema a cada ligação. Regras:
 *  - ligação feita por você (saída): sempre permite (e não registra)
 *  - filtro desligado no app: permite tudo
 *  - sem permissão de contatos: permite (falha "aberta", pra não bloquear todo mundo por engano)
 *  - número oculto/desconhecido: bloqueia
 *  - número que está nos contatos: permite
 *  - qualquer outro: bloqueia
 * Toda ligação recebida analisada vai pro histórico, com o motivo da decisão.
 */
class CallFilterService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, allow())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart
        val hidden = callDetails.handlePresentation != TelecomManager.PRESENTATION_ALLOWED ||
            number.isNullOrBlank()
        val shown = if (hidden) null else number

        val blocked: Boolean
        val reason: String
        when {
            !Prefs.isEnabled(this) -> { blocked = false; reason = "Filtro desligado" }
            !hasContactsPermission() -> { blocked = false; reason = "Sem permissão de contatos" }
            hidden -> { blocked = true; reason = "Número oculto" }
            isInContacts(number!!) -> { blocked = false; reason = "É contato" }
            else -> { blocked = true; reason = "Fora dos contatos" }
        }

        // O log nunca pode impedir a resposta ao sistema.
        try {
            BlockLog.add(this, shown, blocked, reason)
        } catch (_: Exception) {
        }

        respondToCall(callDetails, if (blocked) block() else allow())
    }

    private fun allow() = CallResponse.Builder().build()

    // Bloqueia e recusa, mas mantém no histórico de chamadas do sistema.
    private fun block() = CallResponse.Builder()
        .setDisallowCall(true)
        .setRejectCall(true)
        .setSkipCallLog(false)
        .setSkipNotification(true)
        .build()

    private fun hasContactsPermission() =
        checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * PhoneLookup faz a normalização do número pra gente (DDI, DDD, 9º dígito etc.),
     * é a mesma lógica que o app de telefone usa pra mostrar o nome do contato.
     */
    private fun isInContacts(number: String): Boolean {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        return try {
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null, null, null
            )?.use { it.count > 0 } ?: false
        } catch (e: SecurityException) {
            true // sem acesso aos contatos: não bloqueia
        }
    }
}
