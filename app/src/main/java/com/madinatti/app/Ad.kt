package com.madinatti.app

import com.google.firebase.Timestamp

data class Ad(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val city: String = "",
    val imageUrls: List<String> = emptyList(),
    val emoji: String = "📦",
    val status: String = "active",
    val views: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp? = null,
    val duration: Int = 30
)