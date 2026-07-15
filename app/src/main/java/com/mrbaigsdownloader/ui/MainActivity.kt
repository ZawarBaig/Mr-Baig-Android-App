package com.mrbaigsdownloader.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mrbaigsdownloader.R
import com.mrbaigsdownloader.databinding.ActivityMainBinding
import com.mrbaigsdownloader.service.DownloadService
import com.mrbaigsdownloader.ui.downloads.DownloadsFragment
import com.mrbaigsdownloader.ui.home.HomeFragment
import com.mrbaigsdownloader.ui.settings.SettingsFragment
import com.mrbaigsdownloader.ui.tasks.TasksFragment
import com.mrbaigsdownloader.utils.ErrorDialog
import com.mrbaigsdownloader.utils.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Listen for download completed broadcasts to show toast
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                DownloadService.BROADCAST_COMPLETE -> {
                    Toast.makeText(this@MainActivity,
                        "✅ Download complete!", Toast.LENGTH_SHORT).show()
                }
                DownloadService.BROADCAST_FAILED -> {
                    val error = intent.getStringExtra(DownloadService.EXTRA_ERROR) ?: return
                    ErrorDialog.showDownloadError(this@MainActivity, error)
                }
                DownloadService.BROADCAST_YTDLP_UPDATED -> {
                    val success = intent.getBooleanExtra("success", false)
                    val result  = intent.getStringExtra("result") ?: ""
                    if (success) {
                        Toast.makeText(this@MainActivity,
                            "✅ yt-dlp updated: $result", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity,
                            "yt-dlp update failed: $result", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PermissionHelper.requestAll(this)
        setupBottomNav()
        registerReceiver()

        // Handle shared URL from browser
        handleSharedIntent(intent)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val url = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            loadFragment(HomeFragment.newInstance(url))
            binding.bottomNav.selectedItemId = R.id.nav_home
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home      -> loadFragment(HomeFragment())
                R.id.nav_tasks     -> loadFragment(TasksFragment())
                R.id.nav_downloads -> loadFragment(DownloadsFragment())
                R.id.nav_settings  -> loadFragment(SettingsFragment())
            }
            true
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun getBottomNav(): BottomNavigationView = binding.bottomNav

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(DownloadService.BROADCAST_COMPLETE)
            addAction(DownloadService.BROADCAST_FAILED)
            addAction(DownloadService.BROADCAST_YTDLP_UPDATED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, filter)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionHelper.REQUEST_CODE) {
            val denied = grantResults.any { it != PackageManager.PERMISSION_GRANTED }
            if (denied) {
                ErrorDialog.showPermissionDenied(this)
            }
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(downloadReceiver) } catch (e: Exception) { }
        super.onDestroy()
    }
}
