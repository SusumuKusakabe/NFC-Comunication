package com.uphyca.nfcchat.card

import android.nfc.cardemulation.HostNfcFService
import android.os.Bundle
import com.uphyca.library.CustomRequestCommand
import com.uphyca.library.CustomResponseCommand
import com.uphyca.library.checkIdm
import com.uphyca.library.createAnonymousCommand
import com.uphyca.library.extractData
import com.uphyca.nfcchat.shared.ChatData

// host_nfcf_service.xml と合わせる
private val idm = byteArrayOf(0x02, 0xFE.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

class MyHostNfcFService : HostNfcFService() {

    override fun onCreate() {
        println("MyHostNfcFService, onCreate")
        super.onCreate()
    }

    override fun onDestroy() {
        println("MyHostNfcFService, onDestroy")
        super.onDestroy()
    }

    override fun onDeactivated(reason: Int) {
        println("MyHostNfcFService, reason: $reason")

        if (reason == DEACTIVATION_LINK_LOSS) {
            println("onDeactivated: DEACTIVATION_LINK_LOSS")
        } else {
            println("onDeactivated: Unknown reason")
        }
    }

    override fun processNfcFPacket(commandPacket: ByteArray?, extras: Bundle?): ByteArray {
        println("commandPacket: ${commandPacket?.dump()}")
        println("extras: ${extras?.keySet()}")

        if (commandPacket == null || commandPacket.size < 10) {
            return ByteArray(0)
        }

        if (commandPacket[1] != CustomRequestCommand) {
            return ByteArray(0)
        }

        if (!checkIdm(commandPacket, idm)) {
            return ByteArray(0)
        }

        val responseData = extractData(commandPacket)

        println("text: ${String(responseData)}")

        val chatDataRepository = (application as MyApplication).chatDataRepository

        val text = chatDataRepository.value
        val data = if (text.isEmpty()) ByteArray(0) else text.toByteArray()
        val payload = createAnonymousCommand(idm, CustomResponseCommand, data)

        println("send: ${payload.dump()}")

        chatDataRepository.add(
            ChatData(isMe = false, text = String(responseData)),
            ChatData(isMe = true, text = text)
        )

        return payload
    }

    private fun ByteArray.dump(): String {
        return this.joinToString(prefix = "[", postfix = "]", separator = " ") {
            String.format("%02x", it)
        }
    }
}
