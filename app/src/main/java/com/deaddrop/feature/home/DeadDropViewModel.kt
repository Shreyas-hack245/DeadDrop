package com.deaddrop.feature.home

import androidx.lifecycle.ViewModel
import com.deaddrop.data.repository.DemoTreasureRepository

/** Presentation state for the seeded hunt flow. A production Firebase-backed repository can replace this source. */
class DeadDropViewModel : ViewModel() {
    private val repository = DemoTreasureRepository()
    val treasures = repository.treasures
    fun unlock(id: String) = repository.unlock(id)
}
