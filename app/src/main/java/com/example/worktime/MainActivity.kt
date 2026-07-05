package com.example.worktime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.worktime.di.AppContainer
import com.example.worktime.ui.MainViewModelFactory
import com.example.worktime.ui.WorktimeApp
import com.example.worktime.ui.theme.WorktimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorktimeTheme {
                val viewModel = viewModel<com.example.worktime.ui.MainViewModel>(
                    factory = MainViewModelFactory(
                        repository = AppContainer.repository(applicationContext),
                        settingsStore = AppContainer.settingsStore(applicationContext),
                        alarmScheduler = AppContainer.alarmScheduler(applicationContext),
                        widgetRefresher = AppContainer.widgetRefresher(applicationContext)
                    )
                )
                WorktimeApp(viewModel = viewModel)
            }
        }
    }
}
