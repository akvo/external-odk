package com.akvo.externalodk

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Polygon
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon as OsmPolygon

class MapPreviewActivity : Activity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure OSMDroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        
        // Create and configure map view
        mapView = MapView(this)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0)
        
        setContentView(mapView)
        
        // Load and display polygon
        loadAndDisplayPolygon()
    }

    private fun loadAndDisplayPolygon() {
        try {
            val sharedPreferences = getSharedPreferences("region_prefs", Context.MODE_PRIVATE)
            val geoJsonString = sharedPreferences.getString("geojson_polygon", null)
            
            if (geoJsonString == null) {
                Toast.makeText(this, "No polygon configured", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val featureCollection = FeatureCollection.fromJson(geoJsonString)
            val feature = featureCollection.features()?.firstOrNull()
            val polygon = feature?.geometry() as? Polygon
            
            if (polygon == null) {
                Toast.makeText(this, "Invalid polygon data", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Convert Mapbox polygon to OSMDroid polygon
            val coordinates = polygon.coordinates()[0] // Get outer ring
            val geoPoints = coordinates.map { point ->
                GeoPoint(point.latitude(), point.longitude())
            }

            // Create OSMDroid polygon overlay
            val osmPolygon = OsmPolygon(mapView).apply {
                points = geoPoints
                fillColor = Color.argb(50, 0, 150, 255)
                strokeColor = Color.rgb(0, 100, 200)
                strokeWidth = 3f
            }

            mapView.overlays.add(osmPolygon)

            // Calculate center and zoom to polygon
            if (geoPoints.isNotEmpty()) {
                val centerLat = geoPoints.map { it.latitude }.average()
                val centerLon = geoPoints.map { it.longitude }.average()
                mapView.controller.setCenter(GeoPoint(centerLat, centerLon))
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading polygon: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
