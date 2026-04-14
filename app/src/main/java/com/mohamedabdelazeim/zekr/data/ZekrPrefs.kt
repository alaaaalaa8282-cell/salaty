package com.mohamedabdelazeim.zekr.data

import android.content.Context

object ZekrPrefs {
    private const val PREFS = "zekr_prefs"

    fun getIntervalMinutes(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("interval", 30)

    fun setIntervalMinutes(ctx: Context, v: Int) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("interval", v).apply()

    fun isEnabled(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("enabled", false)

    fun setEnabled(ctx: Context, v: Boolean) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("enabled", v).apply()

    fun nextZekrIndex(ctx: Context): Int {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt("zekr_index", 0)
        val next = (current + 1) % ZekrData.zekrList.size
        prefs.edit().putInt("zekr_index", next).apply()
        return current
    }
}
