package dev.suks.pupdr

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("pupdr", Context.MODE_PRIVATE)
    private val _unit = MutableStateFlow(sp.getString("unit", "kg") ?: "kg")
    val unit: StateFlow<String> = _unit
    private val _dynamic = MutableStateFlow(sp.getBoolean("dynamic_color_v2", false))
    val dynamicColor: StateFlow<Boolean> = _dynamic

    private fun intFlow(key: String, def: Int) = MutableStateFlow(sp.getInt(key, def))
    private val _waterGoal = intFlow("water_goal", 8)
    private val _proteinGoal = intFlow("protein_goal", 140)
    private val _calorieGoal = intFlow("calorie_goal", 2500)
    private val _carbGoal = intFlow("carb_goal", 280)
    private val _fatGoal = intFlow("fat_goal", 75)
    private val _syncVelto = MutableStateFlow(sp.getBoolean("sync_velto", true))
    private val _syncMidas = MutableStateFlow(sp.getBoolean("sync_midas", true))
    /** Glasses of water a day. */
    val waterGoal: StateFlow<Int> = _waterGoal
    /** Grams of protein a day. */
    val proteinGoal: StateFlow<Int> = _proteinGoal
    val calorieGoal: StateFlow<Int> = _calorieGoal
    val carbGoal: StateFlow<Int> = _carbGoal
    val fatGoal: StateFlow<Int> = _fatGoal
    val syncVelto: StateFlow<Boolean> = _syncVelto
    val syncMidas: StateFlow<Boolean> = _syncMidas

    fun setWaterGoal(v: Int) { val c = v.coerceIn(1, 30); sp.edit().putInt("water_goal", c).apply(); _waterGoal.value = c }
    fun setProteinGoal(v: Int) { val c = v.coerceIn(10, 500); sp.edit().putInt("protein_goal", c).apply(); _proteinGoal.value = c }
    fun setCalorieGoal(v: Int) { val c = v.coerceIn(500, 8000); sp.edit().putInt("calorie_goal", c).apply(); _calorieGoal.value = c }
    fun setCarbGoal(v: Int) { val c = v.coerceIn(0, 1000); sp.edit().putInt("carb_goal", c).apply(); _carbGoal.value = c }
    fun setFatGoal(v: Int) { val c = v.coerceIn(0, 400); sp.edit().putInt("fat_goal", c).apply(); _fatGoal.value = c }
    fun setSyncVelto(on: Boolean) { sp.edit().putBoolean("sync_velto", on).apply(); _syncVelto.value = on }
    fun setSyncMidas(on: Boolean) { sp.edit().putBoolean("sync_midas", on).apply(); _syncMidas.value = on }

    var junkCleaned: Boolean
        get() = sp.getBoolean("junk_cleaned_v1", false)
        set(v) { sp.edit().putBoolean("junk_cleaned_v1", v).apply() }

    fun setUnit(u: String) {
        sp.edit().putString("unit", u).apply()
        _unit.value = u
    }

    fun setDynamicColor(on: Boolean) {
        sp.edit().putBoolean("dynamic_color_v2", on).apply()
        _dynamic.value = on
    }
}
