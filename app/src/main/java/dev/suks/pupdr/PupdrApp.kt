package dev.suks.pupdr

import android.app.Application
import dev.suks.pupdr.data.PupdrDatabase
import dev.suks.pupdr.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PupdrApp : Application() {
    val appScope = CoroutineScope(SupervisorJob())
    lateinit var repo: Repository
        private set
    lateinit var prefs: Prefs
        private set

    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        repo = Repository(PupdrDatabase.build(this))
        prefs = Prefs(this)
        appScope.launch {
            repo.ensureSeeded(this@PupdrApp)
            if (!prefs.junkCleaned) {
                repo.removeJunkCustomExercises()
                prefs.junkCleaned = true
            }
            _ready.value = true
        }
    }
}
