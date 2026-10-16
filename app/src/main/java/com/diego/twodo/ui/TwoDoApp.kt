package com.diego.twodo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.diego.twodo.ui.calendar.CalendarScreen
import com.diego.twodo.ui.onboarding.ModelSetupScreen
import com.diego.twodo.ui.onboarding.WelcomeScreen
import com.diego.twodo.ui.tasks.TasksScreen

@Composable
fun TwoDoApp(
    focusQuickAdd: Boolean = false,
    onQuickAddHandled: () -> Unit = {},
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val welcomeCompleted by mainViewModel.isWelcomeCompleted.collectAsState()
    val showModelSetup by mainViewModel.showModelSetup.collectAsState()
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = Triple(welcomeCompleted, showModelSetup, showCalendar),
            label = "two_do_screens",
        ) { (completed, setup, calendar) ->
            when {
                !completed -> WelcomeScreen(onStart = mainViewModel::completeWelcome)
                setup -> ModelSetupScreen()
                calendar -> CalendarScreen(onToggleCalendar = { showCalendar = false })
                else -> TasksScreen(
                    focusQuickAdd = focusQuickAdd,
                    onQuickAddHandled = onQuickAddHandled,
                    onOpenModelSetup = mainViewModel::showModelSetup,
                    onToggleCalendar = { showCalendar = true },
                )
            }
        }
    }
}
