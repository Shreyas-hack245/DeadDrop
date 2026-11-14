package com.deaddrop.data.repository

import com.deaddrop.data.model.Treasure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface TreasureRepository {
    val treasures: StateFlow<List<Treasure>>
    fun unlock(id: String)
}

/** Offline seeded repository. Replace with a server-validated Firebase implementation for production. */
class DemoTreasureRepository : TreasureRepository {
    private val _treasures = MutableStateFlow(listOf(
        Treasure("moonwell", "The Moonwell Cache", "ANCIENT RELIC", "EPIC", "0.8 km", "Where the old stone drinks the moon, listen for the quietest stream.", "🌙", "Moonwell Sigil", "A silver mark said to guide lost explorers home.", 37.7749, -122.4194),
        Treasure("foxglove", "Foxglove Signal", "HIDDEN SIGNAL", "RARE", "1.4 km", "Three red petals point toward a path no map remembers.", "🦊", "Foxglove Beacon", "A tiny beacon carrying a forgotten forest song.", 37.7791, -122.4158),
        Treasure("tideglass", "Tideglass Archive", "LOST ARCHIVE", "LEGENDARY", "2.1 km", "Find the place where the city folds into the water.", "💎", "Tideglass Archive", "A fragment of a city written in shifting blue light.", 37.7702, -122.4260),
    ))
    override val treasures: StateFlow<List<Treasure>> = _treasures.asStateFlow()
    override fun unlock(id: String) { _treasures.value = _treasures.value.map { if (it.id == id) it.copy(unlocked = true) else it } }
}
