package com.paulo.filtrochamadas

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/** Tela simples, montada em código (sem XML): status, 2 botões de permissão e um liga/desliga. */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var btnContacts: Button
    private lateinit var btnRole: Button
    private lateinit var toggle: Switch

    private val reqContacts = 1
    private val reqRole = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad * 2, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "Filtro de Chamadas"
            textSize = 24f
        })

        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, pad, 0, pad)
        }
        root.addView(status)

        btnContacts = Button(this).apply {
            text = "1. Permitir acesso aos contatos"
            setOnClickListener {
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), reqContacts)
            }
        }
        root.addView(btnContacts)

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
        root.addView(btnRole)

        toggle = Switch(this).apply {
            text = "Bloquear quem não é contato"
            isChecked = Prefs.isEnabled(this@MainActivity)
            setPadding(0, pad, 0, 0)
            setOnCheckedChangeListener { _, checked -> Prefs.setEnabled(this@MainActivity, checked) }
        }
        root.addView(toggle)

        setContentView(root)
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
    }
}
