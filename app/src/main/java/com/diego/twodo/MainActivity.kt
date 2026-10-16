package com.diego.twodo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.diego.twodo.ui.TwoDoApp
import com.diego.twodo.ui.theme.TwoDoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var focusQuickAdd by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        focusQuickAdd = intent.getBooleanExtra(EXTRA_QUICK_ADD, false)
        setContent {
            TwoDoTheme {
                TwoDoApp(
                    focusQuickAdd = focusQuickAdd,
                    onQuickAddHandled = { focusQuickAdd = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        focusQuickAdd = intent.getBooleanExtra(EXTRA_QUICK_ADD, false)
    }

    companion object {
        const val EXTRA_QUICK_ADD = "extra_quick_add"
    }
}
