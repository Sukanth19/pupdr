package dev.suks.pupdr

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.suks.pupdr.data.PupdrDatabase
import dev.suks.pupdr.data.Repository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoryTest {
    private lateinit var db: PupdrDatabase
    private lateinit var repo: Repository
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ctx, PupdrDatabase::class.java).allowMainThreadQueries().build()
        repo = Repository(db)
    }

    @After fun tearDown() = db.close()

    @Test fun createRoutineFlow() = runTest {
        repo.ensureSeeded(ctx)
        val all = repo.exercises.first()
        assertTrue("library should be big, was ${all.size}", all.size > 800)
        val bench = all.first { it.name == "Barbell Bench Press - Medium Grip" }
        val squat = all.first { it.name == "Barbell Squat" }

        // new routine starts unnamed; picking exercises then naming it
        val id = repo.createRoutine("")
        repo.addToRoutine(id, listOf(bench.id, squat.id))
        repo.renameRoutine(id, "Push", "")
        repo.updateTarget(repo.routineItems(id).first().first().id, 4, "8")
        repo.finishEditing(id, wasNew = true)

        val routine = repo.routines.first().single()
        assertEquals("Push", routine.name)
        assertEquals(2, routine.exerciseCount)
        assertEquals(7, routine.totalSets)
        assertEquals("Barbell Bench Press - Medium Grip · Barbell Squat", routine.preview)

        // start routine on a day and log it
        val day = 20000L
        repo.startRoutine(day, routine.id)
        val sets = repo.setsForDay(day).first()
        assertEquals(7, sets.size)
        assertEquals(8, sets.first().reps)
        repo.updateSet(sets.first().id, 8, 60.0, true)
        assertEquals(60.0, repo.setsBetweenOnce(day, day).first { it.id == sets.first().id }.weight, 0.0)

        // backing out of an untouched new routine discards it; an unnamed one with exercises gets a name
        val empty = repo.createRoutine("")
        repo.finishEditing(empty, wasNew = true)
        val unnamed = repo.createRoutineWith("", listOf(squat.id))
        repo.finishEditing(unnamed, wasNew = true)
        val names = repo.routines.first().map { it.name }
        assertEquals(2, names.size)
        assertTrue(names.none { it.isBlank() })
    }

    @Test fun junkCleanupAndCustomDeletion() = runTest {
        repo.ensureSeeded(ctx)
        val junk = repo.addCustomExercise("**Protein:** about 120–145 g a day (1.6–2 g per kg at 73 kg).", "", "other")
        val real = repo.addCustomExercise("Zercher Carry", "core", "barbell")
        val logged = repo.addCustomExercise("Notes: keep this one, it was logged.", "", "other")
        val rid = repo.createRoutineWith("Mixed", listOf(junk.id, real.id))
        repo.addExercisesToDay(5L, listOf(logged.id))

        assertEquals(1, repo.removeJunkCustomExercises())
        val ids = repo.exercises.first().map { it.id }.toSet()
        assertTrue(real.id in ids && logged.id in ids && junk.id !in ids)
        assertEquals(1, repo.routineItems(rid).first().size)

        repo.deleteCustomExercise(logged.id)
        assertTrue(repo.setsForDay(5L).first().isEmpty())
        // library exercises are protected
        val lib = repo.exercises.first().first { !it.custom }
        repo.deleteCustomExercise(lib.id)
        assertTrue(repo.exercises.first().any { it.id == lib.id })
    }

    @Test fun addSetCopiesPrevious() = runTest {
        repo.ensureSeeded(ctx)
        val id = repo.exercises.first().first().id
        repo.addExercisesToDay(1L, listOf(id))
        val first = repo.setsForDay(1L).first().single()
        repo.updateSet(first.id, 12, 20.0, true)
        repo.addSet(1L, id, repo.setsForDay(1L).first().single())
        val two = repo.setsForDay(1L).first()
        assertEquals(listOf(1, 2), two.map { it.setIndex })
        assertEquals(20.0, two[1].weight, 0.0)

        // next day prefills weight from last time
        repo.addExercisesToDay(2L, listOf(id))
        assertEquals(20.0, repo.setsForDay(2L).first().single().weight, 0.0)
    }
}
