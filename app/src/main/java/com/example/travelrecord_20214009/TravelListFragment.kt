package com.example.travelrecord_20214009

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

class TravelListFragment : Fragment() {

    private lateinit var dbHelper: DBHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: TravelAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_travel_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())
        recyclerView = view.findViewById(R.id.recycler_travel)
        tvEmpty = view.findViewById(R.id.tv_empty)

        adapter = TravelAdapter(emptyList()) { record ->
            onTravelItemClick(record)
        }
        recyclerView.adapter = adapter

        loadTravelList()
    }

    override fun onResume() {
        super.onResume()
        if (::dbHelper.isInitialized) {
            loadTravelList()
        }
    }

    private fun loadTravelList() {
        val records = dbHelper.getAllTravels()
        adapter.updateList(records)
        tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun onTravelItemClick(record: TravelRecord) {
        // Commit 5에서 TravelDetailActivity로 연결 예정
        Toast.makeText(
            requireContext(),
            getString(R.string.travel_item_clicked, record.title),
            Toast.LENGTH_SHORT
        ).show()
    }
}
