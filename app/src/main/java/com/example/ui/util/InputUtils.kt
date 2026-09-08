package com.example.ui.util

/**
 * Filters out duplicate keypresses that can occur on web browser / emulator streaming
 * interfaces when physical key events and IME text composition are both dispatched.
 */
fun filterDuplicateInput(oldValue: String, newValue: String): String {
    if (newValue.length == oldValue.length + 2) {
        var i = 0
        while (i < oldValue.length && oldValue[i] == newValue[i]) {
            i++
        }
        if (i + 1 < newValue.length && newValue[i] == newValue[i + 1]) {
            val remainingNew = newValue.substring(i + 2)
            val remainingOld = oldValue.substring(i)
            if (remainingNew == remainingOld) {
                return newValue.substring(0, i + 1) + remainingNew
            }
        }
    }
    return newValue
}
