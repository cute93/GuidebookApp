package com.example.guidebook

import com.example.guidebook.models.UserNote
import org.junit.Assert.*
import org.junit.Test

class UserNoteTest {

    @Test fun `note id format is problemId_userId`() {
        val problemId = "prob1"
        val userId = "user1"
        val note = UserNote(
            id = "${problemId}_${userId}",
            problemId = problemId,
            userId = userId
        )
        assertEquals("prob1_user1", note.id)
    }

    @Test fun `teacher role note`() {
        val note = UserNote(id = "p1_teacher", problemId = "p1", userId = "teacher", role = "teacher")
        assertEquals("teacher", note.role)
        assertEquals("p1_teacher", note.id)
    }

    @Test fun `note id uses underscore as separator`() {
        val note = UserNote(id = "problem123_user456")
        assertTrue(note.id.contains('_'))
        assertEquals("problem123", note.id.substringBefore('_'))
        assertEquals("user456", note.id.substringAfter('_'))
    }

    @Test fun `copy preserves problemId and userId`() {
        val original = UserNote(id = "p1_u1", problemId = "p1", userId = "u1", role = "student")
        val updated  = original.copy(noteImageUrl = "https://new.url")
        assertEquals("p1", updated.problemId)
        assertEquals("u1", updated.userId)
        assertEquals("student", updated.role)
    }
}
