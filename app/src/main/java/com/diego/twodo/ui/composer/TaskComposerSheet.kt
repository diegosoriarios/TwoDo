package com.diego.twodo.ui.composer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.diego.twodo.R
import com.diego.twodo.domain.model.TaskBucket
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.SurfaceDark
import com.diego.twodo.ui.theme.TextPrimary
import com.diego.twodo.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class BucketChoice { AUTO, TWO_MIN, THIRTY_MIN, LONG }
private enum class PickerTarget { DUE_DATE, REMINDER }

/**
 * Shared "new task" bottom sheet used by both the in-app flow and the widget's
 * QuickAddActivity. Pure UI — persistence happens through [onSubmit] at the call site.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskComposerSheet(
    onDismiss: () -> Unit,
    onSubmit: (title: String, bucket: TaskBucket?, dueDate: Long?, reminderAt: Long?) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var bucketChoice by rememberSaveable { mutableStateOf(BucketChoice.AUTO) }
    var dueEpochDay by rememberSaveable { mutableLongStateOf(-1L) }
    var reminderAt by rememberSaveable { mutableLongStateOf(-1L) }
    var pickerTarget by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    var pendingReminderEpochDay by rememberSaveable { mutableLongStateOf(-1L) }

    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val ensureNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val inOneHourMillis = remember { TaskComposerDefaults.roundedNextMinuteMillis() + 3_600_000L }
    val tomorrow9Millis = remember { TaskComposerDefaults.tomorrowAt9Millis() }

    val submit: () -> Unit = {
        if (text.isNotBlank()) {
            keyboard?.hide()
            onSubmit(
                text.trim(),
                bucketChoice.toBucket(),
                TaskComposerDefaults.epochDayToMillis(dueEpochDay),
                reminderAt.takeIf { it != -1L },
            )
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.task_add_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                FilledIconButton(
                    onClick = { submit() },
                    enabled = text.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Coral,
                        contentColor = TextPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.task_add_cd),
                    )
                }
            }

            Text(
                text = stringResource(R.string.composer_category),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ComposerChip(
                    label = stringResource(R.string.composer_auto),
                    selected = bucketChoice == BucketChoice.AUTO,
                    onClick = { bucketChoice = BucketChoice.AUTO },
                )
                ComposerChip(
                    label = stringResource(R.string.bucket_two_min),
                    selected = bucketChoice == BucketChoice.TWO_MIN,
                    onClick = { bucketChoice = BucketChoice.TWO_MIN },
                )
                ComposerChip(
                    label = stringResource(R.string.bucket_thirty_min),
                    selected = bucketChoice == BucketChoice.THIRTY_MIN,
                    onClick = { bucketChoice = BucketChoice.THIRTY_MIN },
                )
                ComposerChip(
                    label = stringResource(R.string.bucket_long),
                    selected = bucketChoice == BucketChoice.LONG,
                    onClick = { bucketChoice = BucketChoice.LONG },
                )
            }

            Text(
                text = stringResource(R.string.composer_when),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val today = remember { LocalDate.now() }
                val customDateLabel = if (dueEpochDay != -1L) {
                    TaskComposerDefaults.formatDueDate(dueEpochDay)
                } else {
                    stringResource(R.string.composer_pick_date)
                }
                val customSelected = dueEpochDay != -1L && dueEpochDay !in listOf(
                    today.toEpochDay(),
                    today.plusDays(1).toEpochDay(),
                    today.plusDays(7).toEpochDay(),
                    TaskComposerDefaults.nextWeekendEpochDay(),
                )
                ComposerChip(
                    label = stringResource(R.string.composer_today),
                    selected = dueEpochDay == today.toEpochDay(),
                    onClick = { dueEpochDay = today.toEpochDay() },
                )
                ComposerChip(
                    label = stringResource(R.string.composer_tomorrow),
                    selected = dueEpochDay == today.plusDays(1).toEpochDay(),
                    onClick = { dueEpochDay = today.plusDays(1).toEpochDay() },
                )
                ComposerChip(
                    label = stringResource(R.string.composer_next_week),
                    selected = dueEpochDay == today.plusDays(7).toEpochDay(),
                    onClick = { dueEpochDay = today.plusDays(7).toEpochDay() },
                )
                ComposerChip(
                    label = stringResource(R.string.composer_weekend),
                    selected = dueEpochDay == TaskComposerDefaults.nextWeekendEpochDay(),
                    onClick = { dueEpochDay = TaskComposerDefaults.nextWeekendEpochDay() },
                )
                ComposerChip(
                    label = customDateLabel,
                    selected = customSelected,
                    onClick = { pickerTarget = PickerTarget.DUE_DATE },
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ComposerChip(
                    label = stringResource(R.string.composer_remind_me),
                    selected = reminderAt != -1L,
                    onClick = {
                        if (reminderAt != -1L) {
                            reminderAt = -1L
                        } else {
                            reminderAt = tomorrow9Millis
                            ensureNotificationPermission()
                        }
                    },
                )
                if (reminderAt != -1L) {
                    Text(
                        text = TaskComposerDefaults.formatReminder(reminderAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Coral,
                    )
                }
            }
            if (reminderAt != -1L) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ComposerChip(
                        label = stringResource(R.string.composer_reminder_in_1h),
                        selected = reminderAt == inOneHourMillis,
                        onClick = { reminderAt = inOneHourMillis },
                    )
                    ComposerChip(
                        label = stringResource(R.string.composer_reminder_tomorrow_9),
                        selected = reminderAt == tomorrow9Millis,
                        onClick = { reminderAt = tomorrow9Millis },
                    )
                    ComposerChip(
                        label = stringResource(R.string.composer_reminder_custom),
                        selected = reminderAt != inOneHourMillis && reminderAt != tomorrow9Millis,
                        onClick = { pickerTarget = PickerTarget.REMINDER },
                    )
                }
            }
        }
    }

    when (pickerTarget) {
        PickerTarget.DUE_DATE -> {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = TaskComposerDefaults.epochDayToDatePickerMillis(dueEpochDay),
            )
            DatePickerDialog(
                onDismissRequest = { pickerTarget = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pickerState.selectedDateMillis?.let {
                                dueEpochDay = TaskComposerDefaults.datePickerMillisToEpochDay(it)
                            }
                            pickerTarget = null
                        },
                    ) { Text(stringResource(R.string.composer_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { pickerTarget = null }) {
                        Text(stringResource(R.string.composer_cancel))
                    }
                },
            ) {
                DatePicker(state = pickerState)
            }
        }
        PickerTarget.REMINDER -> {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = TaskComposerDefaults.epochDayToDatePickerMillis(
                    TaskComposerDefaults.millisToEpochDay(reminderAt),
                ),
            )
            DatePickerDialog(
                onDismissRequest = { pickerTarget = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val millis = pickerState.selectedDateMillis
                            if (millis != null) {
                                pendingReminderEpochDay =
                                    TaskComposerDefaults.datePickerMillisToEpochDay(millis)
                                pickerTarget = null
                            }
                        },
                    ) { Text(stringResource(R.string.composer_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { pickerTarget = null }) {
                        Text(stringResource(R.string.composer_cancel))
                    }
                },
            ) {
                DatePicker(state = pickerState)
            }
        }
        null -> Unit
    }

    if (pendingReminderEpochDay != -1L) {
        val timeState = rememberTimePickerState(initialHour = 9, initialMinute = 0)
        DatePickerDialog(
            onDismissRequest = { pendingReminderEpochDay = -1L },
            confirmButton = {
                TextButton(
                    onClick = {
                        reminderAt = TaskComposerDefaults.reminderMillis(
                            pendingReminderEpochDay,
                            timeState.hour,
                            timeState.minute,
                        )
                        pendingReminderEpochDay = -1L
                    },
                ) { Text(stringResource(R.string.composer_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingReminderEpochDay = -1L }) {
                    Text(stringResource(R.string.composer_cancel))
                }
            },
        ) {
            TimePicker(state = timeState, modifier = Modifier.padding(16.dp))
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
}

@Composable
private fun ComposerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SurfaceDark,
            labelColor = TextSecondary,
            selectedContainerColor = Coral,
            selectedLabelColor = TextPrimary,
        ),
    )
}

private fun BucketChoice.toBucket(): TaskBucket? = when (this) {
    BucketChoice.AUTO -> null
    BucketChoice.TWO_MIN -> TaskBucket.TWO_MIN
    BucketChoice.THIRTY_MIN -> TaskBucket.THIRTY_MIN
    BucketChoice.LONG -> TaskBucket.LONG
}

object TaskComposerDefaults {

    private val zone: ZoneId = ZoneId.systemDefault()

    fun epochDayToMillis(epochDay: Long): Long? =
        if (epochDay == -1L) null
        else LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli()

    fun millisToEpochDay(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    /** DatePicker returns UTC-based millis at midnight — convert via UTC to avoid off-by-one days. */
    fun datePickerMillisToEpochDay(datePickerMillis: Long): Long =
        Instant.ofEpochMilli(datePickerMillis).atZone(ZoneId.of("UTC")).toLocalDate().toEpochDay()

    fun epochDayToDatePickerMillis(epochDay: Long): Long? =
        if (epochDay == -1L) null
        else LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

    fun nextWeekendEpochDay(): Long {
        val today = LocalDate.now()
        return when (today.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> today.toEpochDay()
            else -> today.plusDays(((DayOfWeek.SATURDAY.value - today.dayOfWeek.value + 7) % 7).toLong())
                .toEpochDay()
        }
    }

    fun roundedNextMinuteMillis(): Long = (System.currentTimeMillis() / 60_000L + 1L) * 60_000L

    fun tomorrowAt9Millis(): Long =
        LocalDate.now().plusDays(1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

    fun reminderMillis(epochDay: Long, hour: Int, minute: Int): Long =
        LocalDate.ofEpochDay(epochDay).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    fun formatDueDate(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("EEE, MMM d"))

    fun formatReminder(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
}
