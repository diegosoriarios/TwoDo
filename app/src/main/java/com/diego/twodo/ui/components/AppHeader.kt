package com.diego.twodo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diego.twodo.R
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.SurfaceDark
import com.diego.twodo.ui.theme.TextPrimary
import com.diego.twodo.ui.theme.TextSecondary

@Composable
fun AppHeader(
    calendarHighlighted: Boolean,
    onCalendarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (calendarHighlighted) Color.Transparent else SurfaceDark)
                .border(
                    1.dp,
                    if (calendarHighlighted) Coral else TextSecondary.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp),
                )
                .clickable(onClick = onCalendarClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.DateRange,
                contentDescription = stringResource(R.string.calendar_cd),
                tint = if (calendarHighlighted) Coral else TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
