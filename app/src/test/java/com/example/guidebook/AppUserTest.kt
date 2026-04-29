package com.example.guidebook

import com.example.guidebook.models.AppUser
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class AppUserTest {

    @Test fun `default role is student`() {
        val user = AppUser(uid = "u1", name = "홍길동", email = "test@test.com")
        assertEquals("student", user.role)
    }

    @Test fun `all default fields are empty strings`() {
        val user = AppUser()
        assertEquals("", user.uid)
        assertEquals("", user.name)
        assertEquals("", user.email)
        assertEquals("student", user.role)
    }

    @Test fun `serialization roundtrip preserves all fields`() {
        val original = AppUser(uid = "u1", name = "홍길동", email = "test@test.com", role = "teacher")
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(original) }
        val restored = ObjectInputStream(ByteArrayInputStream(baos.toByteArray())).use {
            it.readObject() as AppUser
        }
        assertEquals(original, restored)
    }

    @Test fun `teacher role is preserved after copy`() {
        val teacher = AppUser(uid = "t1", role = "teacher")
        val copy = teacher.copy(name = "김선생")
        assertEquals("teacher", copy.role)
        assertEquals("t1", copy.uid)
    }
}
