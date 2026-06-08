package com.example.travelrecord_20214009

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

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

        view.findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(requireContext(), TravelAddEditActivity::class.java))
        }

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
        val intent = Intent(requireContext(), TravelDetailActivity::class.java).apply {
            putExtra(TravelDetailActivity.EXTRA_RECORD_ID, record.id)
        }
        startActivity(intent)
    }
}
