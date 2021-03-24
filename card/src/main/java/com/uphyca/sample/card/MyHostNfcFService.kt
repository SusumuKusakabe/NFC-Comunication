package com.uphyca.sample.card

import android.content.Intent
import android.nfc.cardemulation.HostNfcFService
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager

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
        val sb = StringBuilder().apply {
            appendLine("commandPacket: ${commandPacket?.dump()}")
            appendLine("extras: ${extras?.keySet()}")
        }

        if (commandPacket == null || commandPacket.size < 2) {
            return ByteArray(0)
        }

        val idm = byteArrayOf(0x02, 0xFE.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        when (commandPacket[1]) {
            // 簡易的にRequestResponseコマンドにだけ反応する
            0x04.toByte() -> {
                if (commandPacket.size < 10) {
                    return ByteArray(0)
                }

                val receivedIdm = ByteArray(8)
                System.arraycopy(commandPacket, 2, receivedIdm, 0, 8)

                if (!receivedIdm.contentEquals(idm)) {
                    return ByteArray(0)
                }

                val payload = ByteArray(11).also {
                    it[0] = 11    // 配列長
                    it[1] = 0x05  // コマンドコード

                    // set IDm
                    System.arraycopy(idm, 0, it, 2, 8)

                    it[10] = 0x00 // Mode
                }

                sb.appendLine("send: ${payload.dump()}")

                LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(
                        Intent(this, MainActivity::class.java)
                            .setAction(ACTION_LOG)
                            .putExtra("log", sb.toString())
                    )

                println(sb.toString())

                return payload
            }
        }

        return ByteArray(0)
    }

    private fun ByteArray.dump(): String {
        return this.joinToString(prefix = "[", postfix = "]", separator = " ") {
            String.format("%02x", it)
        }
    }
}

val ACTION_LOG = "com.uphyca.sample.card.processNfcFPacketLog"
