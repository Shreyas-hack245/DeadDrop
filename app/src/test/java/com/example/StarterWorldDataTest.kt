package com.example

import com.example.data.location.LocationTracker
import com.example.domain.model.GeoMath
import com.example.domain.model.StarterWorldData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterWorldDataTest {

    private val cities = listOf(
        "Bengaluru" to (12.9716 to 77.5946),
        "San Francisco" to (37.7793 to -122.4193),
        "Equator" to (0.0 to 0.0),
        "Sydney" to (-33.8688 to 151.2093),
        "Tromso" to (69.6492 to 18.9553),
        "Dateline" to (-17.7 to 179.99)
    )

    @Test
    fun offset_lands_at_requested_distance_for_any_bearing_and_latitude() {
        for ((_, base) in cities) {
            for (distance in listOf(80.0, 300.0, 800.0)) {
                for (bearing in 0 until 360 step 45) {
                    val p = GeoMath.offset(base.first, base.second, distance, bearing.toDouble())
                    val measured = LocationTracker.distanceMeters(base.first, base.second, p.latitude, p.longitude)
                    assertEquals(distance, measured, distance * 0.005) // within 0.5 %
                }
            }
        }
    }

    @Test
    fun offset_north_and_east_move_the_expected_axis() {
        val north = GeoMath.offset(12.0, 77.0, 1000.0, 0.0)
        assertTrue(north.latitude > 12.0)
        assertEquals(77.0, north.longitude, 1e-6)

        val east = GeoMath.offset(12.0, 77.0, 1000.0, 90.0)
        assertEquals(12.0, east.latitude, 1e-6)
        assertTrue(east.longitude > 77.0)
    }

    @Test
    fun longitude_is_wrapped_across_the_dateline() {
        val p = GeoMath.offset(0.0, 179.9999, 5000.0, 90.0)
        assertTrue(p.longitude in -180.0..180.0)
        assertTrue(p.longitude < 0.0)
    }

    @Test
    fun starter_drops_are_between_80_and_800_metres_from_the_user() {
        for ((_, base) in cities) {
            for (seed in 1L..20L) {
                val snapshot = StarterWorldData.generateSnapshot(base.first, base.second, seed)
                assertEquals(StarterWorldData.templateIds.size, snapshot.placements.size)
                snapshot.placements.forEach { p ->
                    val d = LocationTracker.distanceMeters(base.first, base.second, p.latitude, p.longitude)
                    assertTrue("distance $d out of range", d in 79.0..801.0)
                }
            }
        }
    }

    @Test
    fun same_seed_gives_same_layout_and_different_seed_gives_different_layout() {
        val a = StarterWorldData.generateSnapshot(12.97, 77.59, seed = 42L, nowMillis = 0L)
        val b = StarterWorldData.generateSnapshot(12.97, 77.59, seed = 42L, nowMillis = 0L)
        val c = StarterWorldData.generateSnapshot(12.97, 77.59, seed = 43L, nowMillis = 0L)
        assertEquals(a, b)
        assertFalse(a.placements == c.placements)
    }

    @Test
    fun built_treasures_keep_static_content_and_only_change_coordinates() {
        val snapshot = StarterWorldData.generateSnapshot(12.97, 77.59, seed = 7L)
        val treasures = StarterWorldData.buildTreasures(snapshot)
        assertEquals(snapshot.placements.size, treasures.size)

        val obsidian = treasures.first { it.id == "starter_obsidian_vault" }
        assertEquals("Obsidian Spire Cache", obsidian.name)
        assertEquals("MYTHIC", obsidian.rarity)
        assertEquals(650L, obsidian.xpReward)
        assertEquals(80L, obsidian.radiusMeters)
        assertTrue(obsidian.isRealmStarterDemo)

        treasures.forEach {
            assertTrue(it.radiusMeters in 10L..1000L) // firestore.rules radius bounds
        }
    }

    @Test
    fun regeneration_rule_uses_5km_threshold() {
        val snapshot = StarterWorldData.generateSnapshot(12.97, 77.59, seed = 1L)
        assertTrue(StarterWorldData.needsRegeneration(null, null))
        assertFalse(StarterWorldData.needsRegeneration(snapshot, 4_900.0))
        assertTrue(StarterWorldData.needsRegeneration(snapshot, 5_100.0))
        assertTrue(StarterWorldData.needsRegeneration(snapshot.copy(version = 0), 10.0))
        assertTrue(
            StarterWorldData.needsRegeneration(
                snapshot.copy(placements = snapshot.placements.drop(1)), 10.0
            )
        )
    }
}

class GeoMathBearingTest {
    @Test
    fun bearing_and_labels() {
        assertEquals(0.0, GeoMath.bearing(10.0, 77.0, 11.0, 77.0), 0.01)
        assertEquals(90.0, GeoMath.bearing(0.0, 77.0, 0.0, 78.0), 0.01)
        assertEquals(180.0, GeoMath.bearing(11.0, 77.0, 10.0, 77.0), 0.01)
        assertEquals("NE", GeoMath.compassLabel(45.0))
        assertEquals("N", GeoMath.compassLabel(359.0))
        assertEquals(-20.0, GeoMath.relativeAngle(350.0, 330.0), 0.001)
        assertEquals(30.0, GeoMath.relativeAngle(350.0, 20.0), 0.001)
    }
}
