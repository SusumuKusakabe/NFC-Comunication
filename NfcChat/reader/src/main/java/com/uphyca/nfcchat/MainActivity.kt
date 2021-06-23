package com.uphyca.nfcchat

import android.content.Intent
import android.nfc.Tag
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uphyca.library.NfcFResult
import com.uphyca.library.NfcTextSender
import com.uphyca.nfcchat.databinding.ActivityMainBinding
import com.uphyca.nfcchat.shared.ChatAdapter
import com.uphyca.nfcchat.shared.ChatData

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private lateinit var nfcTextSender: NfcTextSender

    private val adapter = ChatAdapter()
    private val chatData = mutableListOf<ChatData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nfcTextSender = NfcTextSender(this)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        if (nfcTextSender.isEnabled) {
            nfcTextSender.enableForegroundDispatch(this) { tag ->
                send(tag)
            }
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
        nfcTextSender.disableForegroundDispatch(this)
        super.onPause()
    }

    private fun send(tag: Tag) {
        val text = binding.editText.text.toString()
        val textBytes = if (text.isEmpty()) ByteArray(0) else text.toByteArray()

        when (val result = nfcTextSender.send(tag, textBytes)) {
            is NfcFResult.Success -> {
                val responseText = String(result.response)
                chatData.add(ChatData(isMe = true, text = text))
                chatData.add(ChatData(isMe = false, text = responseText))
                adapter.submitList(chatData.toList())

                binding.editText.setText("")
            }
            NfcFResult.NotNfcF -> {
                // no op
            }
            NfcFResult.CannotGetTag -> {
                runOnUiThread {
                    MaterialAlertDialogBuilder(this)
                        .setMessage("tag を取得できませんでした")
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
                return
            }
            NfcFResult.CannotGetNfcFTag -> {
                runOnUiThread {
                    MaterialAlertDialogBuilder(this)
                        .setMessage("NfcF tag を取得できませんでした")
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
                return
            }
            NfcFResult.TagLostException -> {
                runOnUiThread {
                    Toast.makeText(this, "TagLostException", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
