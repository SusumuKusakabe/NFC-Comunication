package com.uphyca.library

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.NfcF

const val CustomRequestCommand: Byte = 0x04
const val CustomResponseCommand: Byte = 0x05

class NfcTextSender(context: Context) {

    val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    val isEnabled: Boolean
        get() = nfcAdapter != null && nfcAdapter.isEnabled

    fun enableForegroundDispatch(activity: Activity) {
        val intent = Intent(activity, activity::class.java)
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

    fun disableForegroundDispatch(activity: Activity) {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    fun send(intent: Intent, data: ByteArray): NfcFResult {
        if (intent.action != NfcAdapter.ACTION_TECH_DISCOVERED) {
            return NfcFResult.NotNfcF
        }

        val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            ?: return NfcFResult.CannotGetTag

        val nfcF: NfcF = NfcF.get(tag)
            ?: return NfcFResult.CannotGetNfcFTag

        return send(tag, nfcF, data)
    }

    private fun send(tag: Tag, nfcF: NfcF, data: ByteArray): NfcFResult {

        val idm = tag.id

        println("id : ${idm.dump()}")
        println("techList : ${tag.techList.joinToString()}")
        println()
        println("manufacturer : ${nfcF.manufacturer.dump()}")
        println("systemCode : ${nfcF.systemCode.dump()}")
        println("maxTransceiveLength : ${nfcF.maxTransceiveLength}")
        println("timeout : ${nfcF.timeout}")
        println("")

        nfcF.use {
            it.connect()

            val payload = createAnonymousCommand(idm, CustomRequestCommand, data)

            return try {
                val response: ByteArray = it.transceive(payload)

                println("send : ${payload.dump()} success")
                println("response : ${response.dump()}")

                NfcFResult.Success(extractData(response))

            } catch (e: TagLostException) {
                println("send : ${payload.dump()} fail")
                e.printStackTrace()
                NfcFResult.TagLostException
            }
        }
    }

    private fun ByteArray.dump(): String {
        return this.joinToString(prefix = "[", postfix = "]", separator = " ") {
            String.format("%02x", it)
        }
    }
}

sealed class NfcFResult {
    class Success(val response: ByteArray) : NfcFResult()
    object NotNfcF : NfcFResult()
    object CannotGetTag : NfcFResult()
    object CannotGetNfcFTag : NfcFResult()
    object TagLostException : NfcFResult()
}

fun extractData(response: ByteArray): ByteArray {
    val dataLength = response.size - 10
    val responseData = ByteArray(dataLength)
    System.arraycopy(response, 10, responseData, 0, dataLength)
    return responseData
}

fun checkIdm(response: ByteArray, idm: ByteArray): Boolean {
    val receivedIdm = ByteArray(8)
    System.arraycopy(response, 2, receivedIdm, 0, 8)
    return receivedIdm.contentEquals(idm)
}
