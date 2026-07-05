package com.example.worktime.ui

import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.worktime.domain.model.LiverDoseReminderSource
import com.example.worktime.domain.model.PlanHistoryEvent
import com.example.worktime.domain.model.ShiftEventType
import com.example.worktime.domain.model.ShiftPlan
import com.example.worktime.widget.WidgetPinResultReceiver
import com.example.worktime.widget.WorktimeWidgetProvider
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class MainTab(val title: String) {
    HOME("首页"),
    EDITOR("方案"),
    CALENDAR("日历"),
    SETTINGS("设置")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorktimeApp(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackBarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    val currentTab = MainTab.entries[currentTabIndex]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "日程表 - ${currentTab.title}") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = currentTabIndex == index,
                        onClick = { currentTabIndex = index },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (currentTabIndex == index) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        shape = RoundedCornerShape(99.dp)
                                    )
                            )
                        },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (currentTab) {
                MainTab.HOME -> HomeScreen(
                    uiState = uiState,
                    onCreateAlarms = viewModel::createAlarms,
                    onRebuildAlarms = viewModel::rebuildAlarms,
                    onAddWidget = {
                        val message = requestAddWidget(context)
                        viewModel.showMessage(message)
                    },
                    onGoEditor = { currentTabIndex = MainTab.EDITOR.ordinal }
                )

                MainTab.EDITOR -> EditorScreen(
                    plan = uiState.plan,
                    defaultRemindMinutes = uiState.defaultRemindMinutes,
                    isBusy = uiState.isBusy,
                    onSavePlan = viewModel::savePlan
                )

                MainTab.CALENDAR -> CalendarScreen(
                    uiState = uiState,
                    onPrevMonth = { viewModel.changeMonth(-1) },
                    onNextMonth = { viewModel.changeMonth(1) },
                    onSelectDate = viewModel::selectDate,
                    onEditHistoryEvent = viewModel::updateHistoryEvent,
                    onDeleteHistoryEventsByDate = viewModel::deleteHistoryEventsByDate
                )

                MainTab.SETTINGS -> SettingsScreen(
                    defaultRemindMinutes = uiState.defaultRemindMinutes,
                    onSaveDefaultRemind = viewModel::saveDefaultRemindMinutes
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    uiState: MainUiState,
    onCreateAlarms: () -> Unit,
    onRebuildAlarms: () -> Unit,
    onAddWidget: () -> Unit,
    onGoEditor: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("当前方案", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val plan = uiState.plan
                    if (plan == null) {
                        Text("还没有配置倒班方案")
                    } else {
                        Text("共 ${plan.totalDays} 天")
                        Text("工作2小时，休息8小时")
                        Text("首次上班：${formatDateTime(plan.firstWorkStartAtEpochMillis)}")
                        Text("结束时间：最后一天 12:30")
                        Text("静音闹钟：提前 ${plan.silentRemindBeforeMinutes} 分钟")
                        Text("出发闹钟：提前 ${plan.departRemindBeforeMinutes} 分钟")
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = onGoEditor) { Text("编辑方案") }
                }
}
    }
    item {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("下一次上班", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val next = uiState.nextWorkEvent
                if (next == null) {
                    Text("当前没有未来上班时间")
                } else {
                    val workHour = java.time.Instant.ofEpochMilli(next.startAtEpochMillis)
                        .atZone(java.time.ZoneId.systemDefault()).hour
                    val workMinute = java.time.Instant.ofEpochMilli(next.startAtEpochMillis)
                        .atZone(java.time.ZoneId.systemDefault()).minute
                    val activities = com.example.worktime.domain.model.DailyActivity.getActivitiesForWork(workHour, workMinute)
                    val activitiesText = if (activities.isNotEmpty()) " ${activities.joinToString(" ")}" else ""
                    Text("${formatRelativeDate(next.startAtEpochMillis, uiState.nowEpochMillis)} ${formatTimeRange(next.startAtEpochMillis, next.endAtEpochMillis)}$activitiesText")
                }

                val liverReminder = uiState.nextLiverDoseReminder
                if (liverReminder == null) {
                    Text(
                        "护肝片：当前无提醒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val hint = if (liverReminder.source == LiverDoseReminderSource.MEAL_WINDOW) {
                        "餐后服用"
                    } else {
                        "先吃点东西再服用"
                    }
                    Text(
                        "护肝片建议：${formatRelativeDate(liverReminder.remindAtEpochMillis, uiState.nowEpochMillis)} ${formatClockTime(liverReminder.remindAtEpochMillis)}（$hint）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

            }
        }
    }
    if (uiState.recentSleepSchedule != null || uiState.nextSleepSchedule != null) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("建议睡眠安排", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val recentSchedule = uiState.recentSleepSchedule
                    if (recentSchedule == null) {
                        Text(
                            text = "刚上完班后：暂无建议",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (recentSchedule.noSleepThisRound) {
                        Text(
                            text = "刚上完班后：本轮不睡（工作 ${formatHourMinute(recentSchedule.workStartHour, recentSchedule.workStartMinute)}-${formatHourMinute(recentSchedule.workEndHour, recentSchedule.workEndMinute)}）",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        recentSchedule.sleepSlots.forEach { slot ->
                            val startStr = slot.startTime.toString().substring(0, 5)
                            val endStr = slot.endTime.toString().substring(0, 5)
                            Text(
                                text = "刚上完班后（工作 ${formatHourMinute(recentSchedule.workStartHour, recentSchedule.workStartMinute)}-${formatHourMinute(recentSchedule.workEndHour, recentSchedule.workEndMinute)}）：睡眠 $startStr - $endStr",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    val nextSchedule = uiState.nextSleepSchedule
                    if (nextSchedule == null) {
                        Text(
                            text = "下次上完班后：暂无建议",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (nextSchedule.noSleepThisRound) {
                        Text(
                            text = "下次上完班后：本轮不睡（工作 ${formatHourMinute(nextSchedule.workStartHour, nextSchedule.workStartMinute)}-${formatHourMinute(nextSchedule.workEndHour, nextSchedule.workEndMinute)}）",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        nextSchedule.sleepSlots.forEach { slot ->
                            val startStr = slot.startTime.toString().substring(0, 5)
                            val endStr = slot.endTime.toString().substring(0, 5)
                            Text(
                                text = "下次上完班后（工作 ${formatHourMinute(nextSchedule.workStartHour, nextSchedule.workStartMinute)}-${formatHourMinute(nextSchedule.workEndHour, nextSchedule.workEndMinute)}）：睡眠 $startStr - $endStr",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
    item {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("闹钟状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("已同步系统闹钟：${uiState.alarmMappings.size} 个")
                    Text(
                        text = uiState.nextAlarmMapping?.let { "下一次系统闹钟：${formatDateTime(it.triggerAtEpochMillis)}" }
                            ?: "下一次系统闹钟：暂无"
                    )
                    Text(
                        text = "系统时钟接口限制：一次创建一个闹钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "说明：创建系统闹钟无需弹窗授权，若系统不允许静默创建会自动打开系统时钟页面",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "兼容策略：静默创建失败时自动回退到时钟页面/时钟应用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onCreateAlarms,
                            enabled = !uiState.isBusy
                        ) {
                            Text("一键创建闹钟")
                        }
                        FilledTonalButton(
                            onClick = onRebuildAlarms,
                            enabled = !uiState.isBusy
                        ) {
                            Text("重建闹钟")
                        }
                    }
                    Button(
                        onClick = onAddWidget,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("添加桌面小组件")
                    }
                }
            }
        }
    }

    if (uiState.isBusy) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun EditorScreen(
    plan: ShiftPlan?,
    defaultRemindMinutes: Int,
    isBusy: Boolean,
    onSavePlan: (ShiftPlan, Boolean) -> Unit
) {
    var totalDaysText by rememberSaveable { mutableStateOf("") }
    var firstStartText by rememberSaveable { mutableStateOf("") }
    var silentRemindText by rememberSaveable { mutableStateOf("15") }
    var departRemindText by rememberSaveable { mutableStateOf("30") }
    var createAlarmsAfterSave by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(plan?.updatedAtEpochMillis) {
        val sourcePlan = plan
        if (sourcePlan == null) {
            val defaultStart = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0)
            totalDaysText = "30"
            firstStartText = defaultStart.format(DATE_TIME_PATTERN)
            silentRemindText = "15"
            departRemindText = "30"
        } else {
            totalDaysText = sourcePlan.totalDays.toString()
            firstStartText = formatDateTime(sourcePlan.firstWorkStartAtEpochMillis)
            silentRemindText = sourcePlan.silentRemindBeforeMinutes.toString()
            departRemindText = sourcePlan.departRemindBeforeMinutes.toString()
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "方案编辑",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "工作时间固定2小时，休息8小时，结束时间为最后一天12:30",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            OutlinedTextField(
                value = totalDaysText,
                onValueChange = { totalDaysText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("总共几天") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        item {
            OutlinedTextField(
                value = firstStartText,
                onValueChange = { firstStartText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("首次工作开始时间") }
            )
        }
        item {
            OutlinedTextField(
                value = silentRemindText,
                onValueChange = { silentRemindText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("静音闹钟提前（分钟）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        item {
            OutlinedTextField(
                value = departRemindText,
                onValueChange = { departRemindText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("出发闹钟提前（分钟）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("保存后立即创建闹钟")
                Switch(
                    checked = createAlarmsAfterSave,
                    onCheckedChange = { createAlarmsAfterSave = it }
                )
            }
        }
        localError?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            Button(
                onClick = {
                    val totalDays = totalDaysText.toIntOrNull()
                    val silentRemind = silentRemindText.toIntOrNull()
                    val departRemind = departRemindText.toIntOrNull()
                    val startAtMillis = parseDateTimeMillis(firstStartText)

                    if (totalDays == null || silentRemind == null ||
                        departRemind == null || startAtMillis == null
                    ) {
                        localError = "请输入合法数字，并确认时间格式为 yyyy-MM-dd HH:mm"
                        return@Button
                    }

                    val newPlan = ShiftPlan(
                        id = ShiftPlan.SINGLE_PLAN_ID,
                        totalDays = totalDays,
                        firstWorkStartAtEpochMillis = startAtMillis,
                        silentRemindBeforeMinutes = silentRemind,
                        departRemindBeforeMinutes = departRemind
                    )
                    localError = newPlan.validate()
                    if (localError == null) {
                        onSavePlan(newPlan, createAlarmsAfterSave)
                    }
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存方案")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarScreen(
    uiState: MainUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onEditHistoryEvent: (Long, Long, Long) -> Unit,
    onDeleteHistoryEventsByDate: (LocalDate) -> Unit
) {
    val month = uiState.selectedMonth
    val workEventDates = uiState.workEvents.groupBy { toLocalDate(it.startAtEpochMillis) }
    val historyEventDates = uiState.historyEvents.groupBy { toLocalDate(it.startAtEpochMillis) }
    val cells = buildMonthCells(month)
    val scrollState = rememberScrollState()

    var editingEvent by remember { mutableStateOf<PlanHistoryEvent?>(null) }
    var isListView by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrevMonth) { Text("上个月") }
            Text(
                text = "${month.year}年${month.monthValue}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { isListView = !isListView }) {
                Text(if (isListView) "月历" else "列表", fontSize = 12.sp)
            }
            TextButton(onClick = onNextMonth) { Text("下个月") }
        }

        if (isListView) {
            val sortedWorkEvents = uiState.workEvents
                .sortedBy { it.startAtEpochMillis }
            val eventsByDate = sortedWorkEvents
                .groupBy { toLocalDate(it.startAtEpochMillis) }
                .toSortedMap(compareBy { it })

            if (eventsByDate.isEmpty()) {
                Text(
                    text = "没有工作安排",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                eventsByDate.forEach { (date, events) ->
                    val dayOfWeek = WEEKDAY_LABELS.getOrElse(date.dayOfWeek.value - 1) { "" }
                    Text(
                        text = "${date.monthValue}月${date.dayOfMonth}日 周$dayOfWeek",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    events.forEach { event ->
                        val isPast = event.startAtEpochMillis < uiState.nowEpochMillis
                        val workHour = java.time.Instant.ofEpochMilli(event.startAtEpochMillis)
                            .atZone(java.time.ZoneId.systemDefault()).hour
                        val workMinute = java.time.Instant.ofEpochMilli(event.startAtEpochMillis)
                            .atZone(java.time.ZoneId.systemDefault()).minute
                        val activities = com.example.worktime.domain.model.DailyActivity.getActivitiesForWork(workHour, workMinute)
                        val activitiesText = if (activities.isNotEmpty()) " ${activities.joinToString(" ")}" else ""
                        Text(
                            text = "工作 ${formatTimeRange(event.startAtEpochMillis, event.endAtEpochMillis)}$activitiesText",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (isPast) TextDecoration.LineThrough else null
                            ),
                            color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.Unspecified,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.remainingWorkCount}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "剩余工作",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.remainingRestCount}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "剩余休息",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uiState.remainingNightShiftCount}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "剩余熬夜",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WEEKDAY_LABELS.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { date ->
                        if (date == null) {
                            Box(modifier = Modifier.size(width = 46.dp, height = 52.dp))
                        } else {
                            val isSelected = date == uiState.selectedDate
                            val hasCurrentWork = workEventDates.containsKey(date)
                            val hasHistoryWork = historyEventDates.containsKey(date)
                            val isNightShift = uiState.nightShiftDates.contains(date)
                            Box(
                                modifier = Modifier
                                    .size(width = 46.dp, height = 52.dp)
                                    .padding(2.dp)
                                    .background(
                                        color = when {
                                            isNightShift -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectDate(date) }
                                    .padding(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isNightShift) MaterialTheme.colorScheme.error else Color.Unspecified
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (hasCurrentWork) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        color = if (isNightShift) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(99.dp)
                                                    )
                                            )
                                        }
                                        if (hasHistoryWork) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(99.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()
            Text(
                text = "当日明细：${uiState.selectedDate}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            val dayEvents = uiState.events.filter { toLocalDate(it.startAtEpochMillis) == uiState.selectedDate }
            val dayHistoryEvents = uiState.historyEvents.filter { toLocalDate(it.startAtEpochMillis) == uiState.selectedDate }

            if (dayEvents.isEmpty() && dayHistoryEvents.isEmpty()) {
                Text("当天无事件")
            } else {
                dayEvents.forEach { event ->
                    val typeText = if (event.type == ShiftEventType.WORK) "工作" else "休息"
                    val activitiesText = if (event.type == ShiftEventType.WORK) {
                        val workHour = java.time.Instant.ofEpochMilli(event.startAtEpochMillis)
                            .atZone(java.time.ZoneId.systemDefault()).hour
                        val workMinute = java.time.Instant.ofEpochMilli(event.startAtEpochMillis)
                            .atZone(java.time.ZoneId.systemDefault()).minute
                        val activities = com.example.worktime.domain.model.DailyActivity.getActivitiesForWork(workHour, workMinute)
                        if (activities.isNotEmpty()) " ${activities.joinToString(" ")}" else ""
                    } else ""
                    Text(
                        text = "$typeText ${formatTimeRange(event.startAtEpochMillis, event.endAtEpochMillis)}$activitiesText"
                    )
                }

                if (dayHistoryEvents.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "历史记录",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { onDeleteHistoryEventsByDate(uiState.selectedDate) }
                        ) {
                            Text("删除当天", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    dayHistoryEvents.forEach { event ->
                        val typeText = if (event.type == ShiftEventType.WORK) "工作" else "休息"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "[历史] $typeText ${formatTimeRange(event.startAtEpochMillis, event.endAtEpochMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row {
                                TextButton(
                                    onClick = { editingEvent = event }
                                ) {
                                    Text("编辑", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingEvent?.let { event ->
        EditHistoryEventDialog(
            event = event,
            onDismiss = { editingEvent = null },
            onConfirm = { start, end ->
                onEditHistoryEvent(event.id, start, end)
                editingEvent = null
            }
        )
    }
}

@Composable
private fun EditHistoryEventDialog(
    event: PlanHistoryEvent,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    var startText by remember { mutableStateOf(formatDateTime(event.startAtEpochMillis)) }
    var endText by remember { mutableStateOf(formatDateTime(event.endAtEpochMillis)) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑历史记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("开始时间 (yyyy-MM-dd HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text("结束时间 (yyyy-MM-dd HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )
                localError?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val start = parseDateTimeMillis(startText)
                val end = parseDateTimeMillis(endText)
                if (start == null || end == null) {
                    localError = "时间格式错误，请使用 yyyy-MM-dd HH:mm"
                    return@Button
                }
                if (end <= start) {
                    localError = "结束时间必须晚于开始时间"
                    return@Button
                }
                onConfirm(start, end)
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SettingsScreen(
    defaultRemindMinutes: Int,
    onSaveDefaultRemind: (Int) -> Unit
) {
    var input by rememberSaveable { mutableStateOf(defaultRemindMinutes.toString()) }
    LaunchedEffect(defaultRemindMinutes) {
        input = defaultRemindMinutes.toString()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "默认提前提醒分钟会在新建方案时自动填入。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("默认提前提醒（分钟）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(
            onClick = {
                val minutes = input.toIntOrNull()
                if (minutes != null) {
                    onSaveDefaultRemind(minutes)
                }
            }
        ) {
            Text("保存设置")
        }
    }
}

private val DATE_TIME_PATTERN: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val WEEKDAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

private fun parseDateTimeMillis(text: String): Long? {
    return runCatching {
        val dateTime = LocalDateTime.parse(text, DATE_TIME_PATTERN)
        dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
}

private fun formatDateTime(epochMillis: Long): String {
    return DATE_TIME_PATTERN.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    )
}

private fun formatRelativeDate(epochMillis: Long, nowEpochMillis: Long): String {
    val eventDate = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = Instant.ofEpochMilli(nowEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate).toInt()
    return when (daysDiff) {
        0 -> "今天"
        1 -> "明天"
        2 -> "后天"
        in 3..4 -> "${daysDiff}天后"
        else -> "${eventDate.monthValue}月${eventDate.dayOfMonth}日"
    }
}

private fun formatTimeRange(startEpochMillis: Long, endEpochMillis: Long): String {
    val startTime = Instant.ofEpochMilli(startEpochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    val endTime = Instant.ofEpochMilli(endEpochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    val startStr = String.format("%02d:%02d", startTime.hour, startTime.minute)
    val endStr = String.format("%02d:%02d", endTime.hour, endTime.minute)
    return "$startStr-$endStr"
}

private fun formatClockTime(epochMillis: Long): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    return String.format("%02d:%02d", time.hour, time.minute)
}

private fun formatHourMinute(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
}

private fun toLocalDate(epochMillis: Long): LocalDate {
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return "${hours}小时${mins}分钟"
}

private fun formatCountdown(nowEpochMillis: Long, targetEpochMillis: Long): String {
    if (targetEpochMillis <= nowEpochMillis) return "已开始"
    val duration = Duration.ofMillis(targetEpochMillis - nowEpochMillis)
    val days = duration.toDays()
    val hours = duration.toHours() % 24
    val minutes = duration.toMinutes() % 60
    return "${days}天${hours}小时${minutes}分钟"
}

private fun buildMonthCells(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val leadingBlanks = firstDay.dayOfWeek.value - 1
    val cells = mutableListOf<LocalDate?>()
    repeat(leadingBlanks) { cells += null }
    for (day in 1..month.lengthOfMonth()) {
        cells += month.atDay(day)
    }
    while (cells.size % 7 != 0) {
        cells += null
    }
    return cells
}

private fun requestAddWidget(context: Context): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return "当前系统不支持应用内添加，请长按桌面添加“日程表”小组件"
    }
    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
    val provider = ComponentName(context, WorktimeWidgetProvider::class.java)
    if (!appWidgetManager.isRequestPinAppWidgetSupported) {
        return "当前桌面不支持应用内添加，请长按桌面添加“日程表”小组件"
    }
    val callbackIntent = Intent(context, WidgetPinResultReceiver::class.java)
    val callback = PendingIntent.getBroadcast(
        context,
        20001,
        callbackIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val requested = appWidgetManager.requestPinAppWidget(provider, null, callback)
    return if (requested) {
        "已发起添加小组件请求，请在桌面确认；若无弹窗请长按桌面手动添加"
    } else {
        "添加小组件请求失败，请长按桌面手动添加"
    }
}
