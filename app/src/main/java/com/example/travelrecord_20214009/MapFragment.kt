package com.example.travelrecord_20214009

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var dbHelper: DBHelper
    private lateinit var tvMapEmpty: TextView
    private var mapView: MapView? = null
    private var naverMap: NaverMap? = null
    private val markers = mutableListOf<Marker>()

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
        mapView = view.findViewById(R.id.map_view)

        NaverMapSdk.getInstance(requireContext()).onAuthFailedListener =
            NaverMapSdk.OnAuthFailedListener {
                if (!isAdded) return@OnAuthFailedListener
                requireActivity().runOnUiThread {
                    tvMapEmpty.text = getString(R.string.error_map_unavailable)
                    tvMapEmpty.visibility = View.VISIBLE
                }
            }

        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map
        map.uiSettings.isZoomControlEnabled = true
        loadMarkers()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        if (::dbHelper.isInitialized) {
            loadMarkers()
        }
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView?.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    private fun loadMarkers() {
        val map = naverMap ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val travels = withContext(Dispatchers.IO) {
                    dbHelper.getTravelsWithLocation()
                }

                clearMarkers()
                tvMapEmpty.visibility = if (travels.isEmpty()) View.VISIBLE else View.GONE

                travels.forEach { travel ->
                    val marker = Marker().apply {
                        position = LatLng(travel.latitude, travel.longitude)
                        captionText = travel.title
                        subCaptionText = travel.date
                    }
                    marker.map = map
                    markers.add(marker)
                }

                mapView?.post {
                    if (naverMap == null) return@post
                    moveCameraToTravels(map, travels)
                }
            } catch (_: Exception) {
                tvMapEmpty.text = getString(R.string.error_load_failed)
                tvMapEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun moveCameraToTravels(map: NaverMap, travels: List<TravelRecord>) {
        when {
            travels.isEmpty() -> {
                map.moveCamera(
                    CameraUpdate.scrollAndZoomTo(LatLng(37.5665, 126.9780), 7.0)
                )
            }
            travels.size == 1 -> {
                val travel = travels.first()
                map.moveCamera(
                    CameraUpdate.scrollAndZoomTo(
                        LatLng(travel.latitude, travel.longitude),
                        12.0
                    )
                )
            }
            else -> {
                val positions = travels.map { LatLng(it.latitude, it.longitude) }
                try {
                    val bounds = LatLngBounds.from(positions)
                    map.moveCamera(CameraUpdate.fitBounds(bounds, 120))
                } catch (_: Exception) {
                    val center = travels.first()
                    map.moveCamera(
                        CameraUpdate.scrollAndZoomTo(
                            LatLng(center.latitude, center.longitude),
                            8.0
                        )
                    )
                }
            }
        }
    }

    private fun clearMarkers() {
        markers.forEach { it.map = null }
        markers.clear()
    }

    override fun onDestroyView() {
        clearMarkers()
        naverMap = null
        mapView?.onDestroy()
        mapView = null
        super.onDestroyView()
    }
}
