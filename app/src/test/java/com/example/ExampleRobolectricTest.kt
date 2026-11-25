package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.location.LocationTracker
import com.example.domain.model.Rarity
import com.example.domain.model.Treasure
import com.example.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DeadDrop", appName)
    }

    @Test
    fun `haversine distance calculation is accurate`() {
        val zero = LocationTracker.distanceMeters(37.7793, -122.4193, 37.7793, -122.4193)
        assertEquals(0.0, zero, 0.1)

        val nearby = LocationTracker.distanceMeters(37.7793, -122.4193, 37.7802, -122.4193)
        assertTrue(nearby in 90.0..115.0)
    }

    @Test
    fun `treasure create map omits id and filters nulls`() {
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
    fun `user level progression matches 500 xp per level`() {
        assertEquals(1L, UserProfile.levelForXp(0L))
        assertEquals(2L, UserProfile.levelForXp(500L))
        assertEquals(3L, UserProfile.levelForXp(1250L))
    }
}
