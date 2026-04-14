package com.mohamedabdelazeim.zekr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mohamedabdelazeim.zekr.data.ZekrPrefs
import com.mohamedabdelazeim.zekr.worker.ZekrScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && ZekrPrefs.isEnabled(ctx)) {
            ZekrScheduler.schedule(ctx, ZekrPrefs.getIntervalMinutes(ctx).toLong())
        }
    }
}
