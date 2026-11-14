package com.deaddrop.data.model

data class Treasure(
    val id: String,
    val name: String,
    val category: String,
    val rarity: String,
    val distance: String,
    val clue: String,
    val emoji: String,
    val rewardName: String,
    val rewardDescription: String,
    val latitude: Double,
    val longitude: Double,
    val unlocked: Boolean = false,
)
