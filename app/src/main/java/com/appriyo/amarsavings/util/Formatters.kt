package com.appriyo.amarsavings.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun Long.formatTaka(): String {
    val nf = NumberFormat.getNumberInstance(Locale.US)
    return "৳${nf.format(this)}"
}

fun Long.formatDate(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.formatDateTime(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.formatTime(): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.formatDateOnly(): String {
    val sdf = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.isToday(): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = this@isToday }
    val cal2 = Calendar.getInstance()
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun Long.isYesterday(): Boolean {
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val txCal = Calendar.getInstance().apply { timeInMillis = this@isYesterday }
    return txCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
            txCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
}

fun Long.smartFormatDate(): String = when {
    isToday() -> "Today · ${formatTime()}"
    isYesterday() -> "Yesterday · ${formatTime()}"
    else -> formatDateTime()
}

fun String.parseTakaAmount(): Long {
    return replace(",", "").replace("৳", "").trim().toLongOrNull() ?: 0L
}

/**
 * Compact number formatting for tight UI labels (e.g. "+৳1.2K").
 * Uses [Locale.US] so output is consistent regardless of device locale.
 */
fun Long.formatCompact(): String = when {
    this >= 10_000_000 -> String.format(Locale.US, "%.1fCr", this / 10_000_000.0)
    this >= 100_000 -> String.format(Locale.US, "%.1fL", this / 100_000.0)
    this >= 1_000 -> String.format(Locale.US, "%.1fK", this / 1_000.0)
    else -> this.toString()
}