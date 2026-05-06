package com.example.guidebook

import com.example.guidebook.models.Problem
import org.junit.Assert.*
import org.junit.Test

class ProblemTest {

    @Test fun `copy preserves unchanged fields`() {
        val original = Problem(id = "p1", title = "원본", subject = "국어", teacherId = "t1")
        val updated = original.copy(title = "수정됨")
        assertEquals("p1", updated.id)
        assertEquals("수정됨", updated.title)
        assertEquals("국어", updated.subject)
        assertEquals("t1", updated.teacherId)
    }
}
