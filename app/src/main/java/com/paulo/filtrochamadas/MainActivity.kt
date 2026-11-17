package com.paulo.filtrochamadas

import android.Manifest
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var statusTitle: TextView
    private lateinit var statusText: TextView
    private lateinit var todayText: TextView
    private lateinit var toggle: MaterialSwitch
    private lateinit var cardSetup: MaterialCardView
    private lateinit var btnContacts: MaterialButton
    private lateinit var btnRole: MaterialButton
    private lateinit var logTitle: TextView
    private lateinit var btnClear: MaterialButton
    private lateinit var filterGroup: MaterialButtonToggleGroup
    private lateinit var emptyText: TextView
    private lateinit var logList: android.widget.LinearLayout

    private val dateFmt = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))

    private val contactsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    private val roleRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipe = findViewById(R.id.swipe)
        statusTitle = findViewById(R.id.statusTitle)
        statusText = findViewById(R.id.statusText)
        todayText = findViewById(R.id.todayText)
        toggle = findViewById(R.id.toggle)
        cardSetup = findViewById(R.id.cardSetup)
        btnContacts = findViewById(R.id.btnContacts)
        btnRole = findViewById(R.id.btnRole)
        logTitle = findViewById(R.id.logTitle)
        btnClear = findViewById(R.id.btnClear)
        filterGroup = findViewById(R.id.filterGroup)
        emptyText = findViewById(R.id.emptyText)
        logList = findViewById(R.id.logList)

        swipe.setOnRefreshListener {
            refresh()
            swipe.isRefreshing = false
        }

        btnContacts.setOnClickListener {
            contactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }

        btnRole.setOnClickListener {
            val rm = getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                roleRequest.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            }
        }

        toggle.isChecked = Prefs.isEnabled(this)
        toggle.setOnCheckedChangeListener { _, checked ->
            Prefs.setEnabled(this, checked)
            refresh()
        }

        btnClear.setOnClickListener {
            BlockLog.clear(this)
            refresh()
        }

        filterGroup.check(R.id.btnAll)
        filterGroup.addOnButtonCheckedListener { _, _, isChecked -> if (isChecked) refresh() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun refresh() {
        val hasContacts = checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        val hasRole = getSystemService(RoleManager::class.java)
            .isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        val enabled = Prefs.isEnabled(this)
        val ready = hasContacts && hasRole

        // Estado
        when {
            !ready -> {
                statusTitle.text = "Configuração pendente"
                statusText.text = "Conclua os passos abaixo para o filtro funcionar."
            }
            enabled -> {
                statusTitle.text = "Proteção ativa"
                statusText.text = "Só contatos tocam. O resto é bloqueado."
            }
            else -> {
                statusTitle.text = "Bloqueio desligado"
                statusText.text = "Todas as ligações estão tocando normalmente."
            }
        }
        if (toggle.isChecked != enabled) toggle.isChecked = enabled
        toggle.isEnabled = ready

        cardSetup.visibility = if (ready) android.view.View.GONE else android.view.View.VISIBLE
        btnContacts.isEnabled = !hasContacts
        btnRole.isEnabled = hasContacts && !hasRole

        // Diagnóstico: o sistema chegou a chamar o serviço?
        fun fmt(t: Long) = if (t == 0L) "nunca" else dateFmt.format(Date(t))
        findViewById<TextView>(R.id.diagText).text =
            "Diagnóstico · serviço iniciado pelo sistema: ${fmt(Prefs.lastServiceCreated(this))}" +
                " · última ligação analisada: ${fmt(Prefs.lastCallScreened(this))}"

        // Histórico
        val all = BlockLog.read(this)
        val today = all.filter { it.time >= startOfToday() }
        todayText.text = "Hoje: ${today.count { it.blocked }} bloqueadas · ${today.count { !it.blocked }} permitidas"

        val onlyBlocked = filterGroup.checkedButtonId == R.id.btnBlocked
        val shown = if (onlyBlocked) all.filter { it.blocked } else all

        logTitle.text = "Histórico (${shown.size})"
        btnClear.isEnabled = all.isNotEmpty()
        emptyText.visibility = if (shown.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        emptyText.text = if (onlyBlocked) "Nenhuma ligação bloqueada ainda."
        else "Nenhuma ligação registrada ainda."

        logList.removeAllViews()
        val inflater = LayoutInflater.from(this)
        shown.forEach { e ->
            val row = inflater.inflate(R.layout.item_log, logList, false)
            row.findViewById<TextView>(R.id.number).text = e.number
            row.findViewById<TextView>(R.id.detail).text =
                "${dateFmt.format(Date(e.time))}  ·  ${e.reason}"
            row.findViewById<TextView>(R.id.status).apply {
                text = if (e.blocked) "Bloqueada" else "Permitida"
                setTextColor(
                    MaterialColors.getColor(
                        this,
                        if (e.blocked) com.google.android.material.R.attr.colorError
                        else com.google.android.material.R.attr.colorPrimary
                    )
                )
            }
            logList.addView(row)
        }
    }
}
