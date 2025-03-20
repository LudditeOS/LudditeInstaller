package com.example.ludditeinstaller

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class ApiService {
    private val apiUrl = "https://download.luddite-os.ch/api/apks"

    suspend fun fetchApps(): List<ApkFile> = withContext(Dispatchers.IO) {
        try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("X-API-Key", BuildConfig.API_KEY)

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(response)
            val apps = mutableListOf<ApkFile>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                apps.add(
                    ApkFile(
                        name = jsonObject.getString("name"),
                        version = jsonObject.getString("version"),
                        objectName = jsonObject.getString("objectName"),
                        downloadUrl = jsonObject.getString("downloadUrl")
                    )
                )
            }
            apps
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}