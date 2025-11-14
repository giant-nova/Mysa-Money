package com.giantnovadevs.mysamoney.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String,
    val isFromUser: Boolean,
    val timestamp: Long // For ordering the messages
)