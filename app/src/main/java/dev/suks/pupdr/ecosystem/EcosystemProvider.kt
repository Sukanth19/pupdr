package dev.suks.pupdr.ecosystem

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import dev.suks.pupdr.PupdrApp
import kotlinx.coroutines.runBlocking

/** Answers Velto (habits) — see [Ecosystem]. Only [ContentProvider.call] is supported. */
class EcosystemProvider : ContentProvider() {
    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        if (!Ecosystem.isTrusted(ctx, callingPackage)) throw SecurityException("not a pupdr ecosystem app")
        val repo = (ctx.applicationContext as PupdrApp).repo
        val x = extras ?: Bundle()
        return runBlocking {
            when (method) {
                Ecosystem.PING -> Bundle().apply { putInt(Ecosystem.RESULT, Ecosystem.VERSION) }
                Ecosystem.HABIT_SET -> {
                    val key = x.getString(Ecosystem.KEY) ?: return@runBlocking null
                    repo.applyLinkedHabit(key, x.getLong(Ecosystem.DAY), x.getBoolean(Ecosystem.DONE))
                    Bundle().apply { putBoolean(Ecosystem.RESULT, true) }
                }
                Ecosystem.HABIT_GET -> {
                    val key = x.getString(Ecosystem.KEY) ?: return@runBlocking null
                    Bundle().apply { putBoolean(Ecosystem.DONE, repo.habitDone(key, x.getLong(Ecosystem.DAY))) }
                }
                else -> null
            }
        }
    }

    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?) = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
