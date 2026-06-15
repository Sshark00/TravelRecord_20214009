package com.example.travelrecord_20214009

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        bottomNavigation = findViewById(R.id.bottom_navigation)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            loadFragment(TravelListFragment(), addToBackStack = false)
            bottomNavigation.selectedItemId = R.id.nav_travel
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_travel -> TravelListFragment()
                R.id.nav_favorite -> FavoriteFragment()
                R.id.nav_map -> MapFragment()
                else -> return@setOnItemSelectedListener false
            }

            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment != null && currentFragment.javaClass == fragment.javaClass) {
                return@setOnItemSelectedListener true
            }

            loadFragment(fragment, addToBackStack = true)
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                    syncBottomNavigationWithCurrentFragment()
                } else {
                    finish()
                }
            }
        })
    }

    private fun loadFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }

    private fun syncBottomNavigationWithCurrentFragment() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        val menuItemId = when (currentFragment) {
            is TravelListFragment -> R.id.nav_travel
            is FavoriteFragment -> R.id.nav_favorite
            is MapFragment -> R.id.nav_map
            else -> return
        }
        bottomNavigation.menu.findItem(menuItemId)?.isChecked = true
    }
}
