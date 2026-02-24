package com.marfolog.noteswidgetformarkdown.presentation.widget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class TrampolineActivity : ComponentActivity() {
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launched = savedInstanceState?.getBoolean(KEY_LAUNCHED) ?: false
        if (!launched) {
            launched = true
            val targetUri = intent.getStringExtra(EXTRA_URI) ?: run { finish(); return }
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUri)))
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
    }
}
