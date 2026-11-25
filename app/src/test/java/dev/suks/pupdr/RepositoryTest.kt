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

    @Test fun seedsLibraryAndImportsRoutine() = runTest {
        repo.ensureSeeded(ctx)
        val all = repo.exercises.first()
        assertTrue("library should be big, was ${all.size}", all.size > 800)

        val drafts = repo.draftImport("# Push\nBench Press 4x8\nMy Weird Move 2x15")
        assertEquals("Barbell Bench Press - Medium Grip", drafts.single().items[0].exerciseName)
        assertTrue(drafts.single().items[1].isNew)
        val result = repo.commitImport(drafts)
        assertEquals(1, result.routines)
        assertEquals(listOf("My Weird Move"), result.created)

        val routine = repo.routines.first().single()
        assertEquals("Push", routine.name)
        assertEquals(2, routine.exerciseCount)

        // start routine on a day, log it, and check export round-trips
        val day = 20000L
        repo.startRoutine(day, routine.id)
        val sets = repo.setsForDay(day).first()
        assertEquals(6, sets.size)
        assertEquals(8, sets.first().reps)
        repo.updateSet(sets.first().id, 8, 60.0, true)
        assertEquals(60.0, repo.setsBetweenOnce(day, day).first { it.id == sets.first().id }.weight, 0.0)

        val json = repo.exportRoutines(listOf(routine.id))
        val again = repo.importRoutines(json)
        assertEquals(0, again.created.size)
        assertEquals(2, repo.routines.first().size)
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
