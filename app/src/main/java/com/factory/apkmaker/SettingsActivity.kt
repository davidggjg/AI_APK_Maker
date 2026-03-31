package com.factory.apkmaker

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class SettingsActivity : AppCompatActivity() {

    companion object {
        fun getPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences("apk_factory_prefs", Context.MODE_PRIVATE)
        }
    }

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val etGemini = findViewById<EditText>(R.id.etGeminiKey)
        val etGithubToken = findViewById<EditText>(R.id.etGithubToken)
        val etGithubRepo = findViewById<EditText>(R.id.etGithubRepo)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnTest = findViewById<Button>(R.id.btnTest)
        val tvResult = findViewById<TextView>(R.id.tvTestResult)

        val prefs = getPrefs(this)
        etGemini.setText(prefs.getString("gemini_key", ""))
        etGithubToken.setText(prefs.getString("github_token", ""))
        etGithubRepo.setText(prefs.getString("github_repo", ""))

        btnSave.setOnClickListener {
            prefs.edit()
                .putString("gemini_key", etGemini.text.toString().trim())
                .putString("github_token", etGithubToken.text.toString().trim())
                .putString("github_repo", etGithubRepo.text.toString().trim())
                .apply()
            Toast.makeText(this, "✅ נשמר בהצלחה!", Toast.LENGTH_SHORT).show()
        }

        btnTest.setOnClickListener {
            val token = etGithubToken.text.toString().trim()
            val repo = etGithubRepo.text.toString().trim()
            tvResult.text = "בודק..."
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val request = Request.Builder()
                            .url("https://api.github.com/repos/$repo")
                            .header("Authorization", "token $token")
                            .build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) "✅ GitHub מחובר בהצלחה!" 
                        else "❌ שגיאה: ${response.code}"
                    } catch (e: Exception) {
                        "❌ שגיאה: ${e.message}"
                    }
                }
                tvResult.text = result
            }
        }
    }
}
