package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PyqQuestion
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class StudyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun profileAndChapterStatus_ownerCrud_succeeds() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val repo = StudyRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureProfile(uid, "Aarav")
            repo.saveProfile(uid, "Aarav S", "12", "NEET", 240L)
            repo.setChapterStatus(uid, "class-12-physics-01", "Revised")
        }

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProfile(uid).first { it != null && it.exam == "NEET" }
        }
        assertEquals("Aarav S", profile?.name)
        assertEquals(240L, profile?.goalMinutes)

        val statuses = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeChapterStatuses(uid).first { it["class-12-physics-01"] == "Revised" }
        }
        assertEquals("Revised", statuses["class-12-physics-01"])
    }

    @Test
    fun tasksNotesAttemptsAndSessions_ownerCrud_succeeds() = runBlocking {
        val uid = signInTestUser("bob_${UUID.randomUUID()}@test.com")
        val repo = StudyRepository(firestore)

        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addTask(uid, "Solve 20 Electrostatics PYQs", "Physics")
        }
        val tasks = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTasks(uid).first { list -> list.any { it.id == taskId } }
        }
        assertTrue(tasks.any { it.id == taskId && !it.done })

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.toggleTask(uid, tasks.first { it.id == taskId })
        }

        val noteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createNote(uid, "Gauss Law", "Physics", "$\\Phi = q/\\varepsilon_0$")
        }
        val notes = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeNotes(uid).first { list -> list.any { it.id == noteId } }
        }
        assertEquals("Gauss Law", notes.first { it.id == noteId }.title)

        val q = PyqQuestion(
            id = "demo_phy_01",
            exam = "JEE",
            subject = "Physics",
            chapterId = "class-11-physics-02",
            difficulty = "Easy",
            q = "Max height?",
            options = listOf("10 m", "20 m", "30 m", "40 m"),
            answer = 1,
            explain = "H = u^2 / 2g"
        )
        val (attemptId, errorId) = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.logAttemptAndSyncErrors(uid, q, selectedOption = 0, secs = 18, confidence = "HIGH", emptyList())
        }
        assertTrue(attemptId.isNotBlank())
        assertTrue(!errorId.isNullOrBlank())

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.setAttemptErrorCategory(uid, attemptId, errorId, "Calculation")
            repo.setFlashcardStatus(uid, "f1", "known")
            repo.logStudySession(uid, 25, "Kinematics")
        }

        val sessions = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeSessions(uid).first { it.isNotEmpty() }
        }
        assertEquals(25L, sessions.first().minutes)
    }

    @Test
    fun crossUserRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID()}@test.com")
        val repo = StudyRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addTask(aliceUid, "Alice private task", "Chemistry")
        }

        signInTestUser("bob_iso_${UUID.randomUUID()}@test.com")
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeTasks(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when reading another user's tasks")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    @Test
    fun unauthenticatedRead_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = StudyRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeTasks("some_user_123").first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
