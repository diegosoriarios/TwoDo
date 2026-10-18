package com.diego.twodo.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.diego.twodo.R
import com.diego.twodo.ui.theme.Charcoal
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.SurfaceDark
import com.diego.twodo.ui.theme.TextPrimary
import com.diego.twodo.ui.theme.TextSecondary
import com.diego.twodo.ui.theme.TwoDoTheme
import kotlinx.coroutines.launch

class FilterPickerActivity : ComponentActivity() {

    private val appWidgetId: Int by lazy {
        intent.getIntExtra(EXTRA_APP_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val current = intent.getStringExtra(EXTRA_CURRENT_FILTER)
            ?.let { name -> WidgetFilter.entries.firstOrNull { it.name == name } }
            ?: WidgetFilter.TODAY
        setContent {
            TwoDoTheme {
                FilterPickerSheet(
                    current = current,
                    onSelect = ::selectFilter,
                    onDismiss = ::finish,
                )
            }
        }
    }

    private fun selectFilter(filter: WidgetFilter) {
        val appContext = applicationContext
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(appContext).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply { this[KEY_WIDGET_FILTER] = filter.name }
            }
            TasksWidget().update(appContext, glanceId)
            finish()
        }
    }

    companion object {
        const val EXTRA_APP_WIDGET_ID = "filter_app_widget_id"
        const val EXTRA_CURRENT_FILTER = "filter_current"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterPickerSheet(
    current: WidgetFilter,
    onSelect: (WidgetFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Charcoal,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextSecondary),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.widget_filter_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
            WidgetFilter.entries.forEach { filter ->
                FilterOption(
                    label = filter.label(),
                    selected = filter == current,
                    onClick = { onSelect(filter) },
                )
            }
        }
    }
}

@Composable
private fun FilterOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Coral else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            CheckIcon(Icons.Filled.Check)
        }
    }
}

@Composable
private fun CheckIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Coral,
    )
}
