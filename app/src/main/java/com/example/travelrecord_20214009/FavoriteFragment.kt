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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_favorite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DBHelper(requireContext())
        recyclerView = view.findViewById(R.id.recycler_favorite)
        emptyState = view.findViewById(R.id.empty_state)
        progressLoading = view.findViewById(R.id.progress_loading)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.favorite_list_title)
        view.findViewById<TextView>(R.id.tv_header_subtitle).text =
            getString(R.string.favorite_list_subtitle)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_favorite, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_sort_date -> {
                        sortOrder = "${DBHelper.COL_DATE} DESC"
                        loadFavoriteList()
                        true
                    }
                    R.id.action_sort_title -> {
                        sortOrder = "${DBHelper.COL_TITLE} ASC"
                        loadFavoriteList()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        adapter = TravelAdapter(
            records = emptyList(),
            coroutineScope = viewLifecycleOwner.lifecycleScope,
            onItemClick = { record -> openDetail(record) },
            onItemLongClick = { anchor, record -> showContextMenu(anchor, record) }
        )
        recyclerView.adapter = adapter

        loadFavoriteList()
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
            loadFavoriteList()
        }
    }

    private fun loadFavoriteList() {
        viewLifecycleOwner.lifecycleScope.launch {
            progressLoading.visibility = View.VISIBLE
            try {
                val records = withContext(Dispatchers.IO) {
                    dbHelper.getFavoriteTravels(sortOrder)
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
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            dbHelper.deleteTravel(record.id)
                        }
                        loadFavoriteList()
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), R.string.error_save_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
