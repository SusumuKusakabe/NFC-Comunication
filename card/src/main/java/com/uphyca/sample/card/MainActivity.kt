package com.uphyca.sample.card

import android.content.*
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.cardemulation.NfcFCardEmulation
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.uphyca.sample.card.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private var nfcFCardEmulation: NfcFCardEmulation? = null
    private lateinit var cName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF)) {
            binding.textView.text = "このデバイスは HCE-F をサポートしていません"
            return
        }

        cName = ComponentName(this, MyHostNfcFService::class.java)
    }

    private val logReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent != null && intent.action == ACTION_LOG) {
                val log = intent.getStringExtra("log")
                binding.textView.text = log
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null || !nfcAdapter.isEnabled) {
            binding.textView.text = "NFC を有効にしてください"
            binding.settingButton.visibility = View.VISIBLE
            binding.settingButton.setOnClickListener {
                startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            }
        } else {
            binding.textView.text = ""
            binding.settingButton.visibility = View.GONE

            nfcFCardEmulation = NfcFCardEmulation.getInstance(nfcAdapter)
        }

        nfcFCardEmulation?.enableService(this, cName)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            logReceiver,
            IntentFilter(ACTION_LOG)
        )
    }

    override fun onPause() {
        super.onPause()
        nfcFCardEmulation?.disableService(this)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logReceiver)
    }
}
