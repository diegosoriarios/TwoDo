package com.diego.twodo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.diego.twodo.ui.onboarding.ModelSetupScreen
import com.diego.twodo.ui.tasks.TasksScreen

@Composable
fun TwoDoApp(mainViewModel: MainViewModel = hiltViewModel()) {
    val showModelSetup by mainViewModel.showModelSetup.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(targetState = showModelSetup, label = "two_do_screens") { showSetup ->
            if (showSetup) {
                ModelSetupScreen()
            } else {
                TasksScreen(onOpenModelSetup = mainViewModel::showModelSetup)
            }
        }
    }
}
