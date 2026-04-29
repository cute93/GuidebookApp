package com.example.guidebook

import com.example.guidebook.models.Problem
import org.junit.Assert.*
import org.junit.Test

class ProblemTest {

    @Test fun `default createdAt is 0L`() {
        val p = Problem()
        assertEquals(0L, p.createdAt)
    }

    @Test fun `all default fields are empty or zero`() {
        val p = Problem()
        assertEquals("", p.id)
        assertEquals("", p.title)
        assertEquals("", p.imageUrl)
        assertEquals("", p.subject)
        assertEquals("", p.teacherId)
    }

    @Test fun `all fields set correctly`() {
        val p = Problem(
            id = "p1", title = "문제1", imageUrl = "https://img",
            subject = "수학", createdAt = 1000L, teacherId = "t1"
        )
        assertEquals("p1", p.id)
        assertEquals("문제1", p.title)
        assertEquals("수학", p.subject)
        assertEquals(1000L, p.createdAt)
        assertEquals("t1", p.teacherId)
    }

    @Test fun `copy preserves unchanged fields`() {
        val original = Problem(id = "p1", title = "원본", subject = "국어", teacherId = "t1")
        val updated = original.copy(title = "수정됨")
        assertEquals("p1", updated.id)
        assertEquals("수정됨", updated.title)
        assertEquals("국어", updated.subject)
        assertEquals("t1", updated.teacherId)
    }
}
