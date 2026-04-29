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

    @Test fun `default role is student`() {
        val note = UserNote()
        assertEquals("student", note.role)
    }

    @Test fun `default updatedAt is 0L`() {
        val note = UserNote()
        assertEquals(0L, note.updatedAt)
    }

    @Test fun `all default string fields are empty`() {
        val note = UserNote()
        assertEquals("", note.id)
        assertEquals("", note.problemId)
        assertEquals("", note.userId)
        assertEquals("", note.userName)
        assertEquals("", note.noteImageUrl)
        assertEquals("", note.localPath)
    }

    @Test fun `teacher role note`() {
        val note = UserNote(id = "p1_teacher", problemId = "p1", userId = "teacher", role = "teacher")
        assertEquals("teacher", note.role)
        assertEquals("p1_teacher", note.id)
    }
}
