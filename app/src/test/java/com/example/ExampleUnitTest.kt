package com.example

import com.example.data.location.LocationTracker
import com.example.domain.model.Rarity
import com.example.domain.model.Treasure
import com.example.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun haversine_distance_calculation_is_accurate() {
        val zero = LocationTracker.distanceMeters(37.7793, -122.4193, 37.7793, -122.4193)
        assertEquals(0.0, zero, 0.1)

        val nearby = LocationTracker.distanceMeters(37.7793, -122.4193, 37.7802, -122.4193)
        assertTrue(nearby in 90.0..115.0)
    }

    @Test
    fun treasure_create_map_omits_id_and_filters_nulls() {
        val treasure = Treasure(
            id = "should_not_appear",
            creatorId = "uid_123",
            creatorName = "Tester",
            name = "Shadow Drop",
            description = "Hidden under ancient oak.",
            rarity = Rarity.EPIC.name,
            clue = "Look north.",
            hint = null,
            artifactName = "Lunar Seal",
            artifactDescription = "Glows in moonlight."
        )
        val map = treasure.toCreateMap()
        assertFalse(map.containsKey("id"))
        assertFalse(map.containsKey("hint"))
        assertEquals("PUBLIC", map["visibility"])
        assertEquals("ACTIVE", map["status"])
    }

    @Test
    fun user_level_progression_matches_500_xp_per_level() {
        assertEquals(1L, UserProfile.levelForXp(0L))
        assertEquals(2L, UserProfile.levelForXp(500L))
        assertEquals(3L, UserProfile.levelForXp(1250L))
    }
}
