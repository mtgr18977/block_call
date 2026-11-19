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
 *  - não conseguiu consultar os contatos (erro): permite
 *  - qualquer outro: bloqueia
 * Toda ligação recebida analisada vai pro histórico, com o motivo da decisão.
 *
 * Princípio: o serviço NUNCA pode quebrar. Qualquer erro inesperado vira "permitir" + registro
 * no log de falhas, e o sistema sempre recebe uma resposta.
 */
class CallFilterService : CallScreeningService() {

    override fun onCreate() {
        super.onCreate()
        try {
            Prefs.markServiceCreated(this)
            Diagnostics.checkBoot(this)
        } catch (t: Throwable) {
            safeRecordCrash("onCreate", t)
        }
    }

    override fun onScreenCall(callDetails: Call.Details) {
        var response = allow() // resposta padrão segura
        try {
            response = decide(callDetails)
        } catch (t: Throwable) {
            safeRecordCrash("onScreenCall", t)
        }
        respondToCall(callDetails, response)
    }

    private fun decide(callDetails: Call.Details): CallResponse {
        // Diagnóstico: registra que o sistema nos chamou (ligação recebida ou feita).
        Prefs.markCallScreened(this)

        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return allow()

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
            else -> when (lookup(number!!)) {
                Lookup.FOUND -> { blocked = false; reason = "É contato" }
                Lookup.NOT_FOUND -> { blocked = true; reason = "Fora dos contatos" }
                Lookup.ERROR -> { blocked = false; reason = "Erro ao consultar contatos" }
            }
        }

        // O log nunca pode impedir a resposta ao sistema.
        try {
            BlockLog.add(this, shown, blocked, reason)
        } catch (t: Throwable) {
            safeRecordCrash("BlockLog.add", t)
        }

        return if (blocked) block() else allow()
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

    private enum class Lookup { FOUND, NOT_FOUND, ERROR }

    /**
     * PhoneLookup faz a normalização do número pra gente (DDI, DDD, 9º dígito etc.),
     * é a mesma lógica que o app de telefone usa pra mostrar o nome do contato.
     *
     * Cursor nulo (provedor de contatos indisponível, ex.: logo após reiniciar) ou qualquer
     * exceção = ERROR, que deixa a ligação tocar. Antes, isso contava como "fora dos contatos"
     * e bloqueava até ligação de contato.
     */
    private fun lookup(number: String): Lookup {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val cursor = contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null
            ) ?: return Lookup.ERROR
            cursor.use { if (it.count > 0) Lookup.FOUND else Lookup.NOT_FOUND }
        } catch (t: Throwable) {
            safeRecordCrash("lookup", t)
            Lookup.ERROR
        }
    }

    private fun safeRecordCrash(where: String, t: Throwable) {
        try {
            Diagnostics.recordCrash(this, where, t)
        } catch (_: Throwable) {
        }
    }
}
