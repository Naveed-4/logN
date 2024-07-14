package com.asloki.logn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var serviceSwitch: SwitchMaterial
    private lateinit var filterFab: FloatingActionButton
    private lateinit var sortFab: FloatingActionButton
    private lateinit var customLocationButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    private var allApps: List<AppInfo> = listOf()
    private var currentFilter = Filter.ALL
    private var currentSort = Sort.ASCENDING

//    private val notificationPermissionCode = 1
//    private val manageExternalStorageCode = 2

    private val pickFolder = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                sharedPreferences.edit().putString("LOG_FILE_URI", uri.toString()).apply()
                Toast.makeText(this, "Custom location set successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed with notification listening
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestManageExternalStorage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // Permission granted, proceed with external storage operations
            } else {
                Toast.makeText(this, "External storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        recyclerView = findViewById(R.id.recyclerView)
        serviceSwitch = findViewById(R.id.serviceSwitch)
        filterFab = findViewById(R.id.filterFab)
        sortFab = findViewById(R.id.sortFab)
        customLocationButton = findViewById(R.id.customLocationButton)

        adapter = AppListAdapter(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        requestPermissions()
        setupServiceSwitch()
        setupFilterFab()
        setupSortFab()
        setupCustomLocationButton()
        loadApps()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                requestManageExternalStorage.launch(intent)
            }
        }

        if (!isNotificationListenerEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun setupServiceSwitch() {
        serviceSwitch.isChecked = sharedPreferences.getBoolean("SERVICE_ENABLED", false)
        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            val serviceIntent = Intent(this, BackgroundService::class.java)
            if (isChecked) {
                startService(serviceIntent)
            } else {
                stopService(serviceIntent)
            }
            sharedPreferences.edit().putBoolean("SERVICE_ENABLED", isChecked).apply()
        }
    }

    private fun setupFilterFab() {
        filterFab.setOnClickListener {
            currentFilter = when (currentFilter) {
                Filter.ALL -> Filter.INSTALLED
                Filter.INSTALLED -> Filter.SYSTEM
                Filter.SYSTEM -> Filter.SELECTED
                Filter.SELECTED -> Filter.ALL
            }
            applyFilterAndSort()
            Toast.makeText(this, "Filter: ${currentFilter.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSortFab() {
        sortFab.setOnClickListener {
            currentSort = if (currentSort == Sort.ASCENDING) Sort.DESCENDING else Sort.ASCENDING
            applyFilterAndSort()
            Toast.makeText(this, "Sort: ${currentSort.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCustomLocationButton() {
        customLocationButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            pickFolder.launch(intent)
        }
    }

    @Suppress("QueryPermissionsNeeded")
    private fun loadApps() {
        val packageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }.map { resolveInfo ->
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            AppInfo(
                appInfo.packageName,
                appInfo.loadLabel(packageManager).toString(),
                isSystemApp,
                sharedPreferences.getBoolean(appInfo.packageName, false)
            )
        }

        allApps = installedApps
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        var filteredApps = when (currentFilter) {
            Filter.ALL -> allApps
            Filter.INSTALLED -> allApps.filter { !it.isSystemApp }
            Filter.SYSTEM -> allApps.filter { it.isSystemApp }
            Filter.SELECTED -> allApps.filter { it.isSelected }
        }

        filteredApps = when (currentSort) {
            Sort.ASCENDING -> filteredApps.sortedBy { it.name }
            Sort.DESCENDING -> filteredApps.sortedByDescending { it.name }
        }

        adapter.submitList(filteredApps.map { it.copy() })  // Submit a new list with copied items
        recyclerView.scrollToPosition(0)  // Scroll to top after filtering/sorting
    }

    enum class Filter { ALL, INSTALLED, SYSTEM, SELECTED }
    enum class Sort { ASCENDING, DESCENDING }
}