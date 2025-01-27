package com.example.ludditeinstaller

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var logDisplay: LogDisplay
    private lateinit var apiService: ApiService
    private lateinit var appStore: AppStore
    private lateinit var buttonContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logDisplay = LogDisplay(this, findViewById(R.id.log_text_view))
        apiService = ApiService()
        appStore = AppStore(logDisplay)
        buttonContainer = findViewById(R.id.button_container)

        findViewById<Button>(R.id.btn_clear_logs).setOnClickListener {
            logDisplay.clear()
        }

        findViewById<Button>(R.id.btn_refresh).setOnClickListener {
            fetchAndDisplayApps()
        }

        // Initial fetch
        fetchAndDisplayApps()
    }

    private fun fetchAndDisplayApps() {
        lifecycleScope.launch {
            try {
                logDisplay.log("MainActivity", "Fetching apps from API...")
                val apps = apiService.fetchApps()
                updateButtons(apps)
                logDisplay.log("MainActivity", "Successfully fetched ${apps.size} apps")
            } catch (e: Exception) {
                logDisplay.log("MainActivity", "Error fetching apps: ${e.message}")
            }
        }
    }

    private fun updateButtons(apps: List<ApkFile>) {
        buttonContainer.removeAllViews()

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
                    appStore.downloadAndInstallApk(context, app.downloadUrl, app.objectName)
                }
            }
            buttonContainer.addView(button)
        }
    }
}