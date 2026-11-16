package com.paulo.filtrochamadas

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tela montada em código (sem XML): status, permissões, liga/desliga e histórico de bloqueios. */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var btnContacts: Button
    private lateinit var btnRole: Button
    private lateinit var toggle: Switch
    private lateinit var logTitle: TextView
    private lateinit var logList: LinearLayout
    private lateinit var btnClear: Button

    private val reqContacts = 1
    private val reqRole = 2
    private val dateFmt = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun params(top: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(40), dp(24), dp(24))
        }

        root.addView(TextView(this).apply {
            text = "Filtro de Chamadas"
            textSize = 24f
        }, params())

        status = TextView(this).apply { textSize = 16f }
        root.addView(status, params(16))

        btnContacts = Button(this).apply {
            text = "1. Permitir acesso aos contatos"
            setOnClickListener {
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), reqContacts)
            }
        }
        root.addView(btnContacts, params(20))

        btnRole = Button(this).apply {
            text = "2. Definir como app de triagem de chamadas"
            setOnClickListener {
                val rm = getSystemService(RoleManager::class.java)
                if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    @Suppress("DEPRECATION")
                    startActivityForResult(
                        rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), reqRole
                    )
                }
            }
        }
        root.addView(btnRole, params(16))

        toggle = Switch(this).apply {
            text = "Bloquear quem não é contato"
            isChecked = Prefs.isEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, checked -> Prefs.setEnabled(this@MainActivity, checked) }
        }
        root.addView(toggle, params(28))

        logTitle = TextView(this).apply { textSize = 18f }
        root.addView(logTitle, params(32))

        logList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(logList, params(8))

        btnClear = Button(this).apply {
            text = "Limpar histórico"
            setOnClickListener {
                BlockLog.clear(this@MainActivity)
                refresh()
            }
        }
        root.addView(btnClear, params(16))

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        refresh()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refresh()
    }

    private fun refresh() {
        val hasContacts = checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        val rm = getSystemService(RoleManager::class.java)
        val hasRole = rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

        btnContacts.isEnabled = !hasContacts
        btnRole.isEnabled = hasContacts && !hasRole

        status.text = when {
            !hasContacts -> "Falta permitir o acesso aos contatos."
            !hasRole -> "Falta definir este app como app de triagem de chamadas."
            Prefs.isEnabled(this) -> "Ativo: só contatos tocam. O resto é bloqueado."
            else -> "Instalado, mas o bloqueio está desligado."
        }

        val entries = BlockLog.read(this)
        logTitle.text = "Chamadas bloqueadas (${entries.size})"
        btnClear.isEnabled = entries.isNotEmpty()
        logList.removeAllViews()
        if (entries.isEmpty()) {
            logList.addView(TextView(this).apply {
                text = "Nenhuma chamada bloqueada ainda."
                textSize = 14f
                alpha = 0.7f
            }, params())
        } else {
            entries.forEach { e ->
                logList.addView(TextView(this).apply {
                    text = "${dateFmt.format(Date(e.time))}  ·  ${e.number}\n${e.reason}"
                    textSize = 15f
                }, params(12))
            }
        }
    }
}
