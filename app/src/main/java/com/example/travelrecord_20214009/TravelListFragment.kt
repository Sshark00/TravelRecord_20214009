package com.example.travelrecord_20214009

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TravelListFragment : Fragment() {

    private lateinit var dbHelper: DBHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var progressLoading: View
    private lateinit var adapter: TravelAdapter

    private var sortOrder = "${DBHelper.COL_DATE} DESC"

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
        emptyState = view.findViewById(R.id.empty_state)
        progressLoading = view.findViewById(R.id.progress_loading)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.travel_list_title)
        view.findViewById<TextView>(R.id.tv_header_subtitle).text =
            getString(R.string.travel_list_subtitle)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_sort_date -> {
                        sortOrder = "${DBHelper.COL_DATE} DESC"
                        loadTravelList()
                        true
                    }
                    R.id.action_sort_title -> {
                        sortOrder = "${DBHelper.COL_TITLE} ASC"
                        loadTravelList()
                        true
                    }
                    R.id.action_delete_all -> {
                        showDeleteAllDialog()
                        true
                    }
                    R.id.action_app_info -> {
                        showAppInfoDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        adapter = TravelAdapter(
            records = emptyList(),
            coroutineScope = viewLifecycleOwner.lifecycleScope,
            onItemClick = { record -> onTravelItemClick(record) },
            onItemLongClick = { anchor, record -> showContextMenu(anchor, record) }
        )
        recyclerView.adapter = adapter

        view.findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(requireContext(), TravelAddEditActivity::class.java))
        }

        loadTravelList()
    }

    private fun showContextMenu(anchor: View, record: TravelRecord) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.context_travel, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
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
        popup.show()
    }

    override fun onResume() {
        super.onResume()
        if (::dbHelper.isInitialized) {
            loadTravelList()
        }
    }

    private fun loadTravelList() {
        viewLifecycleOwner.lifecycleScope.launch {
            progressLoading.visibility = View.VISIBLE
            try {
                val records = withContext(Dispatchers.IO) {
                    dbHelper.getAllTravels(sortOrder)
                }
                adapter.updateList(records)
                emptyState.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
            } catch (_: Exception) {
                Toast.makeText(requireContext(), R.string.error_load_failed, Toast.LENGTH_SHORT).show()
            } finally {
                progressLoading.visibility = View.GONE
            }
        }
    }

    private fun onTravelItemClick(record: TravelRecord) {
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
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            dbHelper.deleteTravel(record.id)
                        }
                        loadTravelList()
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), R.string.error_save_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showDeleteAllDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_all_title)
            .setMessage(R.string.dialog_delete_all_message)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            dbHelper.deleteAllTravels()
                        }
                        loadTravelList()
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), R.string.error_save_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showAppInfoDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.menu_app_info)
            .setMessage(getString(R.string.app_info_message))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
