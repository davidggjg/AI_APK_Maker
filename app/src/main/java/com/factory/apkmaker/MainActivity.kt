package com.factory.apkmaker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var etRequest: EditText
    private lateinit var btnCreate: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSettings: ImageButton
    private lateinit var lvHistory: ListView
    private val historyList = mutableListOf<String>()
    private lateinit var historyAdapter: ArrayAdapter<String>
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etRequest = findViewById(R.id.etRequest)
        btnCreate = findViewById(R.id.btnCreate)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        btnSettings = findViewById(R.id.btnSettings)
        lvHistory = findViewById(R.id.lvHistory)

        historyAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historyList)
        lvHistory.adapter = historyAdapter

        lvHistory.setOnItemClickListener { _, _, position, _ ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(historyList[position])))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnCreate.setOnClickListener {
            val userRequest = etRequest.text.toString().trim()
            if (userRequest.isEmpty()) {
                Toast.makeText(this, "נא להזין תיאור של האפליקציה", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            createApp(userRequest)
        }
    }

    private fun createApp(userRequest: String) {
        val prefs = SettingsActivity.getPrefs(this)
        val groqKey = prefs.getString("gemini_key", "") ?: ""
        val githubToken = prefs.getString("github_token", "") ?: ""
        val githubRepo = prefs.getString("github_repo", "") ?: ""

        if (groqKey.isEmpty() || githubToken.isEmpty() || githubRepo.isEmpty()) {
            Toast.makeText(this, "נא להגדיר את המפתחות בהגדרות", Toast.LENGTH_LONG).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                updateStatus("🤖 מייצר קוד עם AI...")
                val code = generateCode(groqKey, userRequest)

                updateStatus("📤 שולח קוד ל-GitHub...")
                pushToGitHub(githubToken, githubRepo, code)

                val artifactUrl = "https://github.com/$githubRepo/actions"
                withContext(Dispatchers.Main) {
                    historyList.add(0, artifactUrl)
                    historyAdapter.notifyDataSetChanged()
                }
                updateStatus("✅ הקוד נשלח! כנס ל-GitHub Actions להוריד את ה-APK")

            } catch (e: Exception) {
                updateStatus("❌ שגיאה: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private suspend fun generateCode(apiKey: String, userRequest: String): String {
        return withContext(Dispatchers.IO) {
            val prompt = """
                Write a complete Android Activity in Kotlin for: $userRequest
IMPORTANT: The class name MUST be exactly "GeneratedApp" not "MainActivity"
IMPORTANT: Do NOT declare any class named MainActivity
                Package: com.factory.apkmaker
                Rules:
                - Return ONLY Kotlin code, no explanations, no markdown
                - Use basic Android SDK only (no Jetpack Compose)
                - Keep it simple and compilable
                - Include all necessary imports
            """.trimIndent()

            val json = JSONObject().apply {
                put("model", "llama-3.3-70b-versatile")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("תגובה ריקה מ-Groq")
            JSONObject(body)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .replace("```kotlin", "").replace("```", "").trim()
        }
    }

    private suspend fun pushToGitHub(token: String, repo: String, code: String) {
        withContext(Dispatchers.IO) {
            val filePath = "app/src/main/java/com/factory/apkmaker/GeneratedApp.kt"
            var sha: String? = null
            try {
                val getSha = Request.Builder()
                    .url("https://api.github.com/repos/$repo/contents/$filePath")
                    .header("Authorization", "token $token")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                val shaResponse = client.newCall(getSha).execute()
                if (shaResponse.isSuccessful) {
                    sha = JSONObject(shaResponse.body?.string() ?: "").optString("sha")
                }
            } catch (_: Exception) {}

            val encoded = Base64.encodeToString(code.toByteArray(), Base64.NO_WRAP)
            val body = JSONObject().apply {
                put("message", "AI generated app update")
                put("content", encoded)
                if (sha != null) put("sha", sha)
            }

            val request = Request.Builder()
                .url("https://api.github.com/repos/$repo/contents/$filePath")
                .header("Authorization", "token $token")
                .header("Accept", "application/vnd.github.v3+json")
                .put(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("GitHub error: ${response.code}")
        }
    }

    private suspend fun updateStatus(msg: String) = withContext(Dispatchers.Main) { tvStatus.text = msg }

    private fun setLoading(loading: Boolean) {
        btnCreate.isEnabled = !loading
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
