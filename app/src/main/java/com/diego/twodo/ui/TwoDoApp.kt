package com.diego.twodo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.diego.twodo.R
import com.diego.twodo.ui.calendar.CalendarScreen
import com.diego.twodo.ui.model.ModelManagerScreen
import com.diego.twodo.ui.onboarding.ModelSetupScreen
import com.diego.twodo.ui.onboarding.WelcomeScreen
import com.diego.twodo.ui.tasks.TasksScreen
import com.diego.twodo.ui.theme.Coral
import com.diego.twodo.ui.theme.SurfaceDark
import com.diego.twodo.ui.theme.TextPrimary
import com.diego.twodo.ui.theme.TextSecondary

enum class AppTab { TASKS, CALENDAR }

@Composable
fun TwoDoApp(
    focusQuickAdd: Boolean = false,
    onQuickAddHandled: () -> Unit = {},
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val welcomeCompleted by mainViewModel.isWelcomeCompleted.collectAsState()
    val showModelSetup by mainViewModel.showModelSetup.collectAsState()
    var currentTab by rememberSaveable { mutableStateOf(AppTab.TASKS) }
    var showModelManager by rememberSaveable { mutableStateOf(false) }
    val showChrome = welcomeCompleted && !showModelSetup && !showModelManager

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (showModelManager) {
                    ModelManagerScreen(onBack = { showModelManager = false })
                } else {
                    AnimatedContent(
                        targetState = Triple(welcomeCompleted, showModelSetup, currentTab),
                        label = "two_do_screens",
                    ) { (completed, setup, tab) ->
                        when {
                            !completed -> WelcomeScreen(onStart = mainViewModel::completeWelcome)
                            setup -> ModelSetupScreen()
                            tab == AppTab.CALENDAR -> CalendarScreen()
                            else -> TasksScreen(
                                focusQuickAdd = focusQuickAdd,
                                onQuickAddHandled = onQuickAddHandled,
                                onOpenModelManager = { showModelManager = true },
                            )
                        }
                    }
                }
            }
            if (showChrome) {
                NavigationBar(containerColor = SurfaceDark) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.TASKS,
                        onClick = { currentTab = AppTab.TASKS },
                        icon = {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null)
                        },
                        label = { Text(stringResource(R.string.tab_tasks)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Coral,
                            selectedTextColor = TextPrimary,
                            indicatorColor = Coral.copy(alpha = 0.25f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                        ),
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.CALENDAR,
                        onClick = { currentTab = AppTab.CALENDAR },
                        icon = {
                            Icon(imageVector = Icons.Outlined.DateRange, contentDescription = null)
                        },
                        label = { Text(stringResource(R.string.tab_calendar)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Coral,
                            selectedTextColor = TextPrimary,
                            indicatorColor = Coral.copy(alpha = 0.25f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                        ),
                    )
                }
            }
        }
    }
}
