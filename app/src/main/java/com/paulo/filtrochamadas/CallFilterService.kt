package com.paulo.filtrochamadas

import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Chamado pelo sistema a cada ligação. Regras:
 *  - ligação feita por você (saída): sempre permite
 *  - filtro desligado no app: permite tudo
 *  - sem permissão de contatos: permite (falha "aberta", pra não bloquear todo mundo por engano)
 *  - número oculto/desconhecido: bloqueia
 *  - número que está nos contatos: permite
 *  - qualquer outro: bloqueia
 */
class CallFilterService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, allow())
            return
        }

        if (!Prefs.isEnabled(this) || !hasContactsPermission()) {
            respondToCall(callDetails, allow())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart
        val hidden = callDetails.handlePresentation != android.telecom.TelecomManager.PRESENTATION_ALLOWED ||
            number.isNullOrBlank()

        val allowed = !hidden && isInContacts(number!!)
        respondToCall(callDetails, if (allowed) allow() else block())
    }

    private fun allow() = CallResponse.Builder().build()

    // Bloqueia e recusa, mas mantém no histórico pra você ver o que foi barrado.
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
