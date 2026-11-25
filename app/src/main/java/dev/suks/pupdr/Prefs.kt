package dev.suks.pupdr

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("pupdr", Context.MODE_PRIVATE)
    private val _unit = MutableStateFlow(sp.getString("unit", "kg") ?: "kg")
    val unit: StateFlow<String> = _unit
    private val _dynamic = MutableStateFlow(sp.getBoolean("dynamic_color", true))
    val dynamicColor: StateFlow<Boolean> = _dynamic

    fun setUnit(u: String) {
        sp.edit().putString("unit", u).apply()
        _unit.value = u
    }

    fun setDynamicColor(on: Boolean) {
        sp.edit().putBoolean("dynamic_color", on).apply()
        _dynamic.value = on
    }
}
