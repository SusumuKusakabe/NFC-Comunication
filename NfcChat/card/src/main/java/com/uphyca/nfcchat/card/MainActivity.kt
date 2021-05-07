package com.uphyca.nfcchat.card

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.cardemulation.NfcFCardEmulation
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uphyca.library.NfcTextSender
import com.uphyca.nfcchat.card.databinding.ActivityMainBinding
import com.uphyca.nfcchat.shared.ChatAdapter

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private var nfcFCardEmulation: NfcFCardEmulation? = null

    private lateinit var nfcTextSender: NfcTextSender
    private lateinit var cName: ComponentName

    private val adapter = ChatAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF)) {
            MaterialAlertDialogBuilder(this)
                .setMessage("このデバイスは HCE-F をサポートしていません")
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener {
                    finish()
                }
                .show()
            return
        }

        nfcTextSender = NfcTextSender(this)
        cName = ComponentName(this, MyHostNfcFService::class.java)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        val chatDataRepository = (application as MyApplication).chatDataRepository

        binding.editText.addTextChangedListener(onTextChanged = { text, _, _, _ ->
            chatDataRepository.value = text?.toString() ?: ""
        })

        chatDataRepository.list.observe(this, {
            adapter.submitList(it)
            binding.editText.setText("")
            chatDataRepository.value = ""
        })
    }

    override fun onResume() {
        super.onResume()
        if (nfcTextSender.isEnabled) {
            nfcFCardEmulation = NfcFCardEmulation.getInstance(nfcTextSender.nfcAdapter!!)
            nfcFCardEmulation?.enableService(this, cName)
        } else {
            MaterialAlertDialogBuilder(this)
                .setMessage("NFCを有効にしてください")
                .setPositiveButton("設定を開く") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                }
                .show()
        }
    }

    override fun onPause() {
        nfcFCardEmulation?.disableService(this)
        super.onPause()
    }
}
