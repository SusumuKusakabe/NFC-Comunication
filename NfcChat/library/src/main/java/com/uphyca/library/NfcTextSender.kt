package com.uphyca.library

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.NfcF

const val CustomRequestCommand: Byte = 0x04
const val CustomResponseCommand: Byte = 0x05

private val targetIdm = byteArrayOf(0x02, 0xFE.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
private val targetSystemCode = byteArrayOf(0x40, 0x00)

class NfcTextSender(context: Context) {

    val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    val isEnabled: Boolean
        get() = nfcAdapter != null && nfcAdapter.isEnabled

    fun enableForegroundDispatch(activity: Activity, callback: NfcAdapter.ReaderCallback) {
        val flag = NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        nfcAdapter?.enableReaderMode(activity, callback, flag, null)
    }

    fun disableForegroundDispatch(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }

    fun send(tag: Tag, data: ByteArray): NfcFResult {
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

            if (!idm.contentEquals(targetIdm)) {
                val payload = createSensfReqCommand(targetSystemCode)
                try {
                    val response: ByteArray = it.transceive(payload)

                    println("send : ${payload.dump()} success")
                    println("response : ${response.dump()}")

                    System.arraycopy(response, 2, idm, 0, 8)

                    if (!idm.contentEquals(targetIdm)) {
                        return NfcFResult.CannotFindTargetIdm
                    }

                } catch (e: TagLostException) {
                    println("send : ${payload.dump()} fail")
                    e.printStackTrace()
                    return NfcFResult.TagLostException
                }
            }

            return it.sendAnonymousCommand(idm, data)
        }
    }

    private fun NfcF.sendAnonymousCommand(idm: ByteArray, data: ByteArray): NfcFResult {
        val payload = createAnonymousCommand(idm, CustomRequestCommand, data)

        return try {
            val response: ByteArray = transceive(payload)

            println("send : ${payload.dump()} success")
            println("response : ${response.dump()}")

            NfcFResult.Success(extractData(response))

        } catch (e: TagLostException) {
            println("send : ${payload.dump()} fail")
            e.printStackTrace()
            NfcFResult.TagLostException
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
    object CannotFindTargetIdm : NfcFResult()
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
