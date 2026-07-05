package com.example.worktime.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.worktime.alarm.notification.WorkNotificationHelper

class WorkAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val workStartAt = intent.getLongExtra(EXTRA_WORK_START_AT, -1L)
        val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0)
        if (workStartAt <= 0L) return

        WorkNotificationHelper.ensureChannel(context)
        WorkNotificationHelper.showWorkReminder(
            context = context,
            notificationId = requestCode,
            workStartAtEpochMillis = workStartAt
        )
    }

    companion object {
        const val EXTRA_WORK_START_AT = "extra_work_start_at"
        const val EXTRA_TRIGGER_AT = "extra_trigger_at"
        const val EXTRA_REQUEST_CODE = "extra_request_code"
    }
}
