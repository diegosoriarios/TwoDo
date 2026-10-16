package com.diego.twodo.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diego.twodo.R
import com.diego.twodo.domain.model.TaskBucket

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BucketOverrideSheet(
    onDismiss: () -> Unit,
    onSelect: (TaskBucket) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.override_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            BucketOption(
                title = stringResource(R.string.bucket_two_min),
                description = stringResource(R.string.override_two_min_desc),
            ) { onSelect(TaskBucket.TWO_MIN) }
            BucketOption(
                title = stringResource(R.string.bucket_thirty_min),
                description = stringResource(R.string.override_thirty_min_desc),
            ) { onSelect(TaskBucket.THIRTY_MIN) }
            BucketOption(
                title = stringResource(R.string.bucket_long),
                description = stringResource(R.string.override_long_desc),
            ) { onSelect(TaskBucket.LONG) }
        }
    }
}

@Composable
private fun BucketOption(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    )
}
