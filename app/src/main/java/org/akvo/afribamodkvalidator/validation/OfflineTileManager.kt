package org.akvo.afribamodkvalidator.validation

import android.content.Context
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.modules.SqlTileWriter
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView

/**
 * Manages offline tile downloads for map regions.
 *
 * Uses OSMDroid's CacheManager to download tiles for specified bounding boxes.
 * Tiles are stored in SQLite database for offline use.
 */
class OfflineTileManager(private val mapView: MapView) {

    private val cacheManager = CacheManager(mapView)

    /**
     * Download tiles for a region at specified zoom levels.
     *
     * @param context Android context
     * @param boundingBox Region to download
     * @param zoomMin Minimum zoom level (default 15)
     * @param zoomMax Maximum zoom level (default 18)
     * @param callback Progress and completion callbacks
     */
    fun downloadRegion(
        context: Context,
        boundingBox: BoundingBox,
        zoomMin: Int = DEFAULT_ZOOM_MIN,
        zoomMax: Int = DEFAULT_ZOOM_MAX,
        callback: DownloadCallback? = null
    ) {
        val cacheCallback = object : CacheManager.CacheManagerCallback {
            override fun onTaskComplete() {
                callback?.onComplete()
            }

            override fun onTaskFailed(errors: Int) {
                callback?.onFailed(errors)
            }

            override fun updateProgress(
                progress: Int,
                currentZoomLevel: Int,
                zoomMin: Int,
                zoomMax: Int
            ) {
                callback?.onProgress(progress, currentZoomLevel)
            }

            override fun downloadStarted() {
                callback?.onStarted()
            }

            override fun setPossibleTilesInArea(total: Int) {
                callback?.onTotalTilesCalculated(total)
            }
        }

        cacheManager.downloadAreaAsync(
            context,
            boundingBox,
            zoomMin,
            zoomMax,
            cacheCallback
        )
    }

    /**
     * Estimate the number of tiles that would be downloaded.
     */
    fun estimateTileCount(
        boundingBox: BoundingBox,
        zoomMin: Int = DEFAULT_ZOOM_MIN,
        zoomMax: Int = DEFAULT_ZOOM_MAX
    ): Int {
        return cacheManager.possibleTilesInArea(boundingBox, zoomMin, zoomMax)
    }

    /**
     * Cancel any ongoing download.
     */
    fun cancelDownload() {
        cacheManager.cancelAllJobs()
    }

    /**
     * Get the current cache size in bytes.
     */
    fun getCacheSizeBytes(): Long {
        val tileWriter = SqlTileWriter()
        return try {
            // Note: This is an approximation
            tileWriter.getRowCount("") * AVERAGE_TILE_SIZE_BYTES
        } finally {
            tileWriter.onDetach()
        }
    }

    /**
     * Clear the tile cache.
     */
    fun clearCache(context: Context, boundingBox: BoundingBox, zoomMin: Int, zoomMax: Int) {
        cacheManager.cleanAreaAsync(context, boundingBox, zoomMin, zoomMax)
    }

    interface DownloadCallback {
        fun onStarted()
        fun onTotalTilesCalculated(total: Int)
        fun onProgress(downloaded: Int, currentZoomLevel: Int)
        fun onComplete()
        fun onFailed(errors: Int)
    }

    companion object {
        const val DEFAULT_ZOOM_MIN = 15
        const val DEFAULT_ZOOM_MAX = 18
        private const val AVERAGE_TILE_SIZE_BYTES = 15_000L // ~15KB per tile
    }
}
