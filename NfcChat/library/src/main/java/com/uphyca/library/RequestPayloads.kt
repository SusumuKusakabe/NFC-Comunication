package com.uphyca.library

// 適当
val serviceList = byteArrayOf(0x0f, 0x09)
val blockList = byteArrayOf(0x00, 0x80.toByte())

fun createRequestResponsePayload(idm: ByteArray): ByteArray {
    val command: Byte = 0x04

    val length = 1 +   // 配列長
        1 +             // コマンドコード
        8               // IDm

    val payload = ByteArray(length)
    payload[0] = length.toByte()
    payload[1] = command

    System.arraycopy(idm, 0, payload, 2, 8)

    return payload
}


fun createReadCommandPayload(idm: ByteArray): ByteArray {
    val command: Byte = 0x06  // read コマンドコード

    check(serviceList.size % 2 == 0)
    check(blockList.size % 2 == 0)

    val length = 1 +   // 配列長
        1 +             // コマンドコード
        8 +             // IDm
        1 +
        serviceList.size +
        1 +
        blockList.size

    val payload = ByteArray(length)
    payload[0] = length.toByte()
    payload[1] = command

    System.arraycopy(idm, 0, payload, 2, 8)

    var i = 10

    payload[i] = (serviceList.size / 2).toByte() // サービスリストの長さ
    i++

    System.arraycopy(serviceList, 0, payload, i, serviceList.size)
    i += serviceList.size

    payload[i] = (blockList.size / 2).toByte() // ブロック数
    i++

    System.arraycopy(blockList, 0, payload, i, blockList.size)

    return payload
}

fun createWriteCommandPayload(idm: ByteArray, data: ByteArray): ByteArray {
    val command: Byte = 0x08  // write コマンドコード

    check(serviceList.size % 2 == 0)
    check(blockList.size % 2 == 0)

    val length = 1 +   // 配列長
        1 +             // コマンドコード
        8 +             // IDm
        1 +
        serviceList.size +
        1 +
        blockList.size +
        data.size

    val payload = ByteArray(length)
    payload[0] = length.toByte()
    payload[1] = command

    System.arraycopy(idm, 0, payload, 2, 8)

    var i = 10

    payload[i] = (serviceList.size / 2).toByte() // サービスリストの長さ
    i++

    System.arraycopy(serviceList, 0, payload, i, serviceList.size)
    i += serviceList.size

    payload[i] = (blockList.size / 2).toByte() // ブロック数
    i++

    System.arraycopy(blockList, 0, payload, i, blockList.size)
    i += blockList.size

    System.arraycopy(data, 0, payload, i, data.size)

    return payload
}

/**
 * [command] 0x03 ~ 0xd3 は OK
 */
fun createAnonymousCommand(idm: ByteArray, command: Byte, data: ByteArray): ByteArray {
    val length = 1 +   // 配列長
        1 +             // コマンドコード
        8 +             // IDm
        data.size

    val payload = ByteArray(length)
    payload[0] = length.toByte()
    payload[1] = command

    System.arraycopy(idm, 0, payload, 2, 8)

    System.arraycopy(data, 0, payload, 10, data.size)

    return payload
}
