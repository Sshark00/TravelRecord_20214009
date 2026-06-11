package com.example.travelrecord_20214009

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var dbHelper: DBHelper
    private lateinit var tvMapEmpty: TextView
    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())
        tvMapEmpty = view.findViewById(R.id.tv_map_empty)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        loadMarkers()
    }

    override fun onResume() {
        super.onResume()
        if (::dbHelper.isInitialized) {
            loadMarkers()
        }
    }

    private fun loadMarkers() {
        val map = googleMap ?: return
        map.clear()

        val travels = dbHelper.getTravelsWithLocation()
        tvMapEmpty.visibility = if (travels.isEmpty()) View.VISIBLE else View.GONE

        travels.forEach { travel ->
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(travel.latitude, travel.longitude))
                    .title(travel.title)
                    .snippet(travel.date)
            )
        }

        when {
            travels.isEmpty() -> {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(37.5665, 126.9780), 7f)
                )
            }
            travels.size == 1 -> {
                val travel = travels.first()
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(travel.latitude, travel.longitude),
                        12f
                    )
                )
            }
            else -> {
                val boundsBuilder = LatLngBounds.builder()
                travels.forEach { travel ->
                    boundsBuilder.include(LatLng(travel.latitude, travel.longitude))
                }
                try {
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120)
                    )
                } catch (_: Exception) {
                    val center = travels.first()
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(center.latitude, center.longitude),
                            8f
                        )
                    )
                }
            }
        }
    }
}
