package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.marfolog.noteswidgetformarkdown.util.AppLog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class TrampolineActivity : ComponentActivity() {
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launched = savedInstanceState?.getBoolean(KEY_LAUNCHED) ?: false
        if (!launched) {
            launched = true
            val targetUri = intent.getStringExtra(EXTRA_URI) ?: run {
                AppLog.w(AREA, "Started without a target URI, nothing to open")
                finish()
                return
            }
            // Without a handler for obsidian:// this throws and takes the app down with it,
            // which is exactly what happens on a phone that has no Obsidian installed.
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUri)))
                AppLog.d(AREA, "Opened ${AppLog.redactFileName(targetUri)}")
            } catch (e: ActivityNotFoundException) {
                AppLog.w(AREA, "No app handles this link — is Obsidian installed?", e)
                Toast.makeText(this, "No app can open this note. Is Obsidian installed?", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_LAUNCHED, launched)
    }

    override fun onResume() {
        super.onResume()
        if (launched) {
            lifecycleScope.launch {
                NotesWidget.updateAll(this@TrampolineActivity)
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_URI = "target_uri"
        private const val KEY_LAUNCHED = "launched"
        private const val AREA = "OpenNote"
    }
}
