package com.uphyca.nfcchat.card

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.uphyca.nfcchat.shared.ChatData

class ChatDataRepository {

    private val chatDataList = mutableListOf<ChatData>()

    private val _list = MutableLiveData<List<ChatData>>()
    val list: LiveData<List<ChatData>>
        get() = _list

    fun add(vararg chatData: ChatData) {
        chatDataList.addAll(chatData)
        _list.value = chatDataList.toList()
    }

    var value: String = ""
}
