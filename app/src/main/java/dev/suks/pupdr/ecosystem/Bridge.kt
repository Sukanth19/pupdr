package dev.suks.pupdr.ecosystem

import android.content.Context
import android.os.Bundle
import dev.suks.pupdr.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Money as Midas reports it. */
data class Spend(val amountMinor: Long, val currency: String, val fractionDigits: Int) {
    fun format(): String {
        val f = NumberFormat.getCurrencyInstance(Locale.getDefault())
        runCatching { f.currency = Currency.getInstance(currency) }
        f.maximumFractionDigits = fractionDigits
        f.minimumFractionDigits = 0
        return f.format(BigDecimal.valueOf(amountMinor, fractionDigits))
    }
}

/** pupdr's side of the link with Velto (habits) and Midas (shopping, spending). */
class Bridge(private val context: Context, private val prefs: Prefs) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun veltoInstalled() = Ecosystem.isInstalled(context, Ecosystem.VELTO)
    fun midasInstalled() = Ecosystem.isInstalled(context, Ecosystem.MIDAS)

    /** Mirrors a habit change made in pupdr into Velto, in the background. */
    fun habitChanged(key: String, day: Long, done: Boolean) {
        if (!prefs.syncVelto.value) return
        scope.launch {
            Ecosystem.call(context, Ecosystem.VELTO, Ecosystem.HABIT_SET, Bundle().apply {
                putString(Ecosystem.KEY, key)
                putLong(Ecosystem.DAY, day)
                putBoolean(Ecosystem.DONE, done)
            })
        }
    }

    /** Adds something (creatine, whey…) to the Midas buy list. True if Midas accepted it. */
    suspend fun addToMidasBuyList(name: String): Boolean = withContext(Dispatchers.IO) {
        if (!prefs.syncMidas.value) return@withContext false
        Ecosystem.call(context, Ecosystem.MIDAS, Ecosystem.BUY_ADD, Bundle().apply {
            putString(Ecosystem.NAME, name)
            putString(Ecosystem.NOTE, "Added from pupdr")
        })?.containsKey(Ecosystem.RESULT) == true
    }

    /** Gym / supplement spending recorded in Midas between two epoch days, or null if unavailable. */
    suspend fun fitnessSpend(from: Long, to: Long): Spend? = withContext(Dispatchers.IO) {
        if (!prefs.syncMidas.value) return@withContext null
        val b = Ecosystem.call(context, Ecosystem.MIDAS, Ecosystem.FITNESS_SPEND, Bundle().apply {
            putLong(Ecosystem.FROM, from)
            putLong(Ecosystem.TO, to)
        }) ?: return@withContext null
        val currency = b.getString(Ecosystem.CURRENCY) ?: return@withContext null
        Spend(b.getLong(Ecosystem.AMOUNT_MINOR), currency, b.getInt(Ecosystem.FRACTION_DIGITS, 2))
    }
}
