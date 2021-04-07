package com.uphyca.sample.reader

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.NfcF
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uphyca.sample.reader.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter?.isEnabled == true) {
            enableForegroundDispatch(this)
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
        disableForegroundDispatch(this)
        super.onPause()
    }

    private fun enableForegroundDispatch(activity: Activity) {
        val intent = Intent(this, MainActivity::class.java)
        nfcAdapter?.enableForegroundDispatch(
            activity,
            PendingIntent.getActivity(activity, 0, intent, 0),
            arrayOf(
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED, "*/*"),
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            ),
            arrayOf<Array<String>?>(
                null,
                arrayOf(NfcF::class.java.name), // NfcF にのみ反応
                null
            )
        )
    }

    private fun disableForegroundDispatch(activity: Activity) {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent == null) {
            return
        }

        if (intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            startConnection(intent)
        }
    }

    private fun startConnection(intent: Intent) {
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag == null) {
            MaterialAlertDialogBuilder(this)
                .setMessage("tag を取得できませんでした")
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        val nfcF: NfcF? = NfcF.get(tag)
        if (nfcF == null) {
            MaterialAlertDialogBuilder(this)
                .setMessage("NfcF tag を取得できませんでした")
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        binding.textView.text = startConnection(tag, nfcF)
    }

    private fun startConnection(tag: Tag, nfcF: NfcF): String {

        val sb = StringBuilder()

        sb.apply {
            appendLine("id : ${tag.id.dump()}")
            appendLine("techList : ${tag.techList.joinToString()}")
            appendLine()
            appendLine("manufacturer : ${nfcF.manufacturer.dump()}")
            appendLine("systemCode : ${nfcF.systemCode.dump()}")
            appendLine("maxTransceiveLength : ${nfcF.maxTransceiveLength}")
            appendLine("timeout : ${nfcF.timeout}")
            appendLine("")
        }

        val idm = tag.id

        nfcF.use {
            it.connect()

            when (binding.radioGroup.checkedRadioButtonId) {
                R.id.radioRequestResponse -> {
                    val serviceList = byteArrayOf(0x0f, 0x09)


                    // read command
                    val readPayload = ByteArray(16)
                    readPayload[0] = 16    // 配列長
                    readPayload[1] = 0x06  // read コマンドコード

                    // set IDm
                    System.arraycopy(idm, 0, readPayload, 2, 8)

                    readPayload[10] = 0x01 // サービスリストの長さ
                    readPayload[11] = serviceList[0]
                    readPayload[12] = serviceList[1]
                    readPayload[13] = 0x01 // ブロック数
                    readPayload[14] = 0x80.toByte() // ブロックリスト
                    readPayload[15] = 0x00.toByte() // ブロックリスト



                    val writePayload = ByteArray(32)
                    writePayload[0] = 32    // 配列長
                    writePayload[1] = 0x08  // write コマンドコード

                    // set IDm
                    System.arraycopy(idm, 0, writePayload, 2, 8)

                    writePayload[10] = 0x01 // サービスリストの長さ
                    writePayload[11] = serviceList[0]
                    writePayload[12] = serviceList[1]
                    writePayload[13] = 0x01 // ブロック数
                    writePayload[14] = 0x80.toByte() // ブロックリスト
                    writePayload[15] = 0x00.toByte() // ブロックリスト
                    for (i in 0 until 16) {
                        writePayload[16 + i] = i.toByte()
                    }


                    // RequestResponseコマンドを送る
                    val payload: ByteArray = ByteArray(10)
                    payload[0] = 10    // 配列長
                    payload[1] = 0x04  // コマンドコード

                    // set IDm
                    System.arraycopy(idm, 0, payload, 2, 8)





                    sb.apply {
                        appendLine("send : ${writePayload.dump()}")
                        appendLine()
                    }

                    val responseText = try {
                        val response: ByteArray = it.transceive(writePayload)
                        response.dump()
                    } catch (e: TagLostException) {
                        e.printStackTrace()
                        "TagLostException : ${e.message}"
                    }
                    sb.apply {
                        appendLine("response : $responseText")
                        appendLine()
                    }
                }
                else -> {
                    throw IllegalStateException()
                }
            }
        }

        return sb.toString()
    }

    private fun ByteArray.dump(): String {
        return this.joinToString(prefix = "[", postfix = "]", separator = " ") {
            String.format("%02x", it)
        }
    }
}
