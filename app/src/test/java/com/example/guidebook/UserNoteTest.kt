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
}
