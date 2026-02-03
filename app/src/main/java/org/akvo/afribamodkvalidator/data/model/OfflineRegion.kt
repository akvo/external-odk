package org.akvo.afribamodkvalidator.data.model

import kotlinx.serialization.Serializable
import org.osmdroid.util.BoundingBox

@Serializable
data class OfflineRegionsConfig(
    val regions: List<OfflineRegionJson>
)

@Serializable
data class OfflineRegionJson(
    val name: String,
    val north: Double,
    val east: Double,
    val south: Double,
    val west: Double
)

/**
 * Domain model for offline region with calculated properties.
 */
data class OfflineRegion(
    val name: String,
    val boundingBox: BoundingBox,
    val estimatedTiles: Int = 0,
    val estimatedSizeMb: Int = 0
)

fun OfflineRegionJson.toDomain(): OfflineRegion {
    return OfflineRegion(
        name = name,
        boundingBox = BoundingBox(north, east, south, west)
    )
}
