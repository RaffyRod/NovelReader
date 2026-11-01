package com.example.quicknovel

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.quicknovel.CommonActivity
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.ui.AppearanceMode
import com.lagradost.quicknovel.ui.FireThemes

/**
 * Debug-only host so instrumented tests can open plugin hub dialogs without MainActivity.
 */
class PluginHubHostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        FireThemes.migrateIfNeeded(this)
        AppearanceMode.syncNightMode(this)
        CommonActivity.loadThemes(this)
        CommonActivity.init(this)
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this))
    }
}
