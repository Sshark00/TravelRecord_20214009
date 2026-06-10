package com.example.travelrecord_20214009

import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

class FavoriteFragment : Fragment() {

    private lateinit var dbHelper: DBHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: TravelAdapter

    private var contextRecord: TravelRecord? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())
        recyclerView = view.findViewById(R.id.recycler_favorite)
        tvEmpty = view.findViewById(R.id.tv_empty)

        adapter = TravelAdapter(
            records = emptyList(),
            onItemClick = { record -> openDetail(record) },
            onRegisterContextMenu = { itemView, _ ->
                registerForContextMenu(itemView)
            }
        )
        recyclerView.adapter = adapter

        loadFavoriteList()
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        requireActivity().menuInflater.inflate(R.menu.context_travel, menu)
        contextRecord = v.tag as? TravelRecord
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val record = contextRecord ?: return false
        return when (item.itemId) {
            R.id.context_edit -> {
                val intent = Intent(requireContext(), TravelAddEditActivity::class.java).apply {
                    putExtra(TravelAddEditActivity.EXTRA_RECORD_ID, record.id)
                }
                startActivity(intent)
                true
            }
            R.id.context_delete -> {
                showDeleteDialog(record)
                true
            }
            else -> false
        }
    }

    override fun onResume() {
        super.onResume()
        if (::dbHelper.isInitialized) {
            loadFavoriteList()
        }
    }

    private fun loadFavoriteList() {
        val records = dbHelper.getFavoriteTravels()
        adapter.updateList(records)
        tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun openDetail(record: TravelRecord) {
        val intent = Intent(requireContext(), TravelDetailActivity::class.java).apply {
            putExtra(TravelDetailActivity.EXTRA_RECORD_ID, record.id)
        }
        startActivity(intent)
    }

    private fun showDeleteDialog(record: TravelRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, record.title))
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                dbHelper.deleteTravel(record.id)
                loadFavoriteList()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
