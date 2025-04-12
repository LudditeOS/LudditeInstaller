package com.example.ludditeinstaller

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), AppStore.AppStoreCallback {
    private lateinit var logDisplay: LogDisplay
    private lateinit var apiService: ApiService
    private lateinit var appStore: AppStore
    private lateinit var buttonContainer: LinearLayout
    private lateinit var loadingContainer: FrameLayout
    private lateinit var logScrollView: ScrollView
    private var logsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        buttonContainer = findViewById(R.id.button_container)
        loadingContainer = findViewById(R.id.loading_container)
        logScrollView = findViewById(R.id.log_scroll_view)

        // Setup log display
        logDisplay = LogDisplay(this, findViewById(R.id.log_text_view))

        // Setup services
        apiService = ApiService()
        appStore = AppStore(logDisplay)
        appStore.setCallback(this)

        // Setup button listeners
        findViewById<Button>(R.id.btn_refresh).setOnClickListener {
            fetchAndDisplayApps()
        }

        findViewById<Button>(R.id.btn_toggle_logs).setOnClickListener {
            toggleLogVisibility()
        }

        // Initial fetch
        fetchAndDisplayApps()
    }

    private fun toggleLogVisibility() {
        logsVisible = !logsVisible

        logScrollView.visibility = if (logsVisible) View.VISIBLE else View.GONE

        // Update button text
        val toggleButton = findViewById<Button>(R.id.btn_toggle_logs)
        toggleButton.text = if (logsVisible) "Hide Logs" else "Show Logs"
    }

    private fun fetchAndDisplayApps() {
        lifecycleScope.launch {
            try {
                // Show loading indicator
                showLoading(true)

                // Clear previous buttons
                buttonContainer.removeAllViews()

                logDisplay.log("MainActivity", "Fetching apps from API...")
                val apps = apiService.fetchApps()

                // Create buttons for apps
                updateButtons(apps)

                logDisplay.log("MainActivity", "Successfully fetched ${apps.size} apps")
            } catch (e: Exception) {
                logDisplay.log("MainActivity", "Error fetching apps: ${e.message}")
            } finally {
                // Hide loading indicator
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingContainer.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun updateButtons(apps: List<ApkFile>) {
        apps.forEach { app ->
            val button = Button(this).apply {
                text = app.name
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16) // Add some bottom margin
                }
                setOnClickListener {
                    logDisplay.log("MainActivity", "Installing ${app.name}")
                    showLoading(true)
                    appStore.downloadAndInstallApk(context, app.downloadUrl, app.name)
                }
            }
            buttonContainer.addView(button)
        }
    }

    // AppStore.AppStoreCallback implementation
    override fun onDownloadComplete() {
        showLoading(false)
        logDisplay.log("MainActivity", "Download and installation process completed")
    }

    override fun onDownloadFailed(error: String) {
        showLoading(false)
        logDisplay.log("MainActivity", "Download failed: $error")
    }
}