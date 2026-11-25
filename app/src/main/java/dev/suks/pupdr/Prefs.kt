package dev.suks.pupdr

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("pupdr", Context.MODE_PRIVATE)
    private val _unit = MutableStateFlow(sp.getString("unit", "kg") ?: "kg")
    val unit: StateFlow<String> = _unit

    fun toggleUnit() {
        val next = if (_unit.value == "kg") "lb" else "kg"
        sp.edit().putString("unit", next).apply()
        _unit.value = next
    }
}
