package com.example.guidebook

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.guidebook.models.AppUser
import com.example.guidebook.models.Problem
import com.example.guidebook.models.UserNote
import com.example.guidebook.repository.CloudinaryHelper
import com.example.guidebook.repository.GuidebookRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class GuidebookRepositoryTest {

    @get:Rule val rule = InstantTaskExecutorRule()

    private val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    private val mockDb   = mockk<FirebaseFirestore>(relaxed = true)
    private lateinit var repo: GuidebookRepository

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkObject(CloudinaryHelper)
        repo = GuidebookRepository(auth = mockAuth, db = mockDb)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ── getCurrentUid / logout ────────────────────────────────────────────────

    @Test fun `getCurrentUid returns uid of current user`() {
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.uid } returns "uid123"
        every { mockAuth.currentUser } returns mockUser

        assertEquals("uid123", repo.getCurrentUid())
    }

    @Test fun `getCurrentUid returns null when not signed in`() {
        every { mockAuth.currentUser } returns null
        assertNull(repo.getCurrentUid())
    }

    @Test fun `logout delegates to auth signOut`() {
        repo.logout()
        verify { mockAuth.signOut() }
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test fun `login success returns AppUser with correct uid and role`() = runTest {
        val mockUser       = mockk<FirebaseUser>()
        val mockAuthResult = mockk<AuthResult>()
        val mockDocRef     = mockk<DocumentReference>(relaxed = true)
        val mockDocSnap    = mockk<DocumentSnapshot>(relaxed = true)
        val expected       = AppUser(uid = "uid1", name = "김선생", email = "t@t.com", role = "teacher")

        every { mockUser.uid } returns "uid1"
        every { mockAuthResult.user } returns mockUser
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockAuthResult)
        every { mockDb.collection("users").document("uid1") } returns mockDocRef
        every { mockDocRef.get() } returns Tasks.forResult(mockDocSnap)
        every { mockDocSnap.exists() } returns true
        every { mockDocSnap.toObject(AppUser::class.java) } returns expected

        val result = repo.login("t@t.com", "pass")

        assertTrue(result.isSuccess)
        assertEquals("uid1", result.getOrNull()?.uid)
        assertEquals("teacher", result.getOrNull()?.role)
    }

    @Test fun `login failure propagates as Result failure`() = runTest {
        every {
            mockAuth.signInWithEmailAndPassword(any(), any())
        } returns Tasks.forException(Exception("인증 실패"))

        val result = repo.login("x@x.com", "wrong")

        assertTrue(result.isFailure)
    }

    @Test fun `login when firestore profile missing returns failure with message`() = runTest {
        val mockUser       = mockk<FirebaseUser>()
        val mockAuthResult = mockk<AuthResult>()
        val mockDocRef     = mockk<DocumentReference>(relaxed = true)
        val mockDocSnap    = mockk<DocumentSnapshot>(relaxed = true)

        every { mockUser.uid } returns "uid1"
        every { mockAuthResult.user } returns mockUser
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockAuthResult)
        every { mockDb.collection("users").document("uid1") } returns mockDocRef
        every { mockDocRef.get() } returns Tasks.forResult(mockDocSnap)
        every { mockDocSnap.exists() } returns false

        val result = repo.login("t@t.com", "pass")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("프로필") == true)
    }

    // ── getProblems ───────────────────────────────────────────────────────────

    @Test fun `getProblems returns list on success`() = runTest {
        val mockColRef  = mockk<CollectionReference>(relaxed = true)
        val mockQuery   = mockk<Query>(relaxed = true)
        val mockSnap    = mockk<QuerySnapshot>(relaxed = true)
        val mockDocSnap = mockk<DocumentSnapshot>(relaxed = true)
        val problem     = Problem(id = "p1", title = "문제1")

        every { mockDb.collection("problems") } returns mockColRef
        every { mockColRef.orderBy(any<String>()) } returns mockQuery
        every { mockQuery.get() } returns Tasks.forResult(mockSnap)
        every { mockSnap.documents } returns listOf(mockDocSnap)
        every { mockDocSnap.toObject(Problem::class.java) } returns problem

        val result = repo.getProblems()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("p1", result.getOrNull()?.first()?.id)
    }

    @Test fun `getProblems returns failure on firestore exception`() = runTest {
        every { mockDb.collection(any()) } throws RuntimeException("네트워크 오류")

        val result = repo.getProblems()

        assertTrue(result.isFailure)
    }

    // ── getUserNotes ──────────────────────────────────────────────────────────

    @Test fun `getUserNotes returns notes for given problemId`() = runTest {
        val mockColRef  = mockk<CollectionReference>(relaxed = true)
        val mockQuery   = mockk<Query>(relaxed = true)
        val mockSnap    = mockk<QuerySnapshot>(relaxed = true)
        val mockDocSnap = mockk<DocumentSnapshot>(relaxed = true)
        val note        = UserNote(id = "p1_u1", problemId = "p1", userId = "u1")

        every { mockDb.collection("notes") } returns mockColRef
        every { mockColRef.whereEqualTo(any<String>(), any()) } returns mockQuery
        every { mockQuery.get() } returns Tasks.forResult(mockSnap)
        every { mockSnap.documents } returns listOf(mockDocSnap)
        every { mockDocSnap.toObject(UserNote::class.java) } returns note

        val result = repo.getUserNotes("p1")

        assertTrue(result.isSuccess)
        assertEquals("p1_u1", result.getOrNull()?.first()?.id)
    }

    @Test fun `getUserNotes returns failure on exception`() = runTest {
        every { mockDb.collection(any()) } throws RuntimeException("오류")

        val result = repo.getUserNotes("p1")

        assertTrue(result.isFailure)
    }

    // ── uploadNoteDrawing ─────────────────────────────────────────────────────

    @Test fun `uploadNoteDrawing saves note with key problemId_userId`() = runTest {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val mockDocRef = mockk<DocumentReference>(relaxed = true)

        coEvery { CloudinaryHelper.uploadBitmap(any(), any()) } returns "https://res.cloudinary.com/note.png"
        every { mockDb.collection("notes").document(any()) } returns mockDocRef
        every { mockDocRef.set(any()) } returns Tasks.forResult<Void>(null)

        val result = repo.uploadNoteDrawing("prob1", "user1", "홍길동", "student", mockBitmap)

        assertTrue(result.isSuccess)
        assertEquals("prob1_user1", result.getOrNull()?.id)
        assertEquals("prob1", result.getOrNull()?.problemId)
        assertEquals("user1", result.getOrNull()?.userId)
    }

    @Test fun `uploadNoteDrawing returns failure when cloudinary throws`() = runTest {
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        coEvery { CloudinaryHelper.uploadBitmap(any(), any()) } throws RuntimeException("Cloudinary 오류")

        val result = repo.uploadNoteDrawing("p1", "u1", "홍길동", "student", mockBitmap)

        assertTrue(result.isFailure)
    }

    // ── uploadProblem ─────────────────────────────────────────────────────────

    @Test fun `uploadProblem sets teacherId to currentUid`() = runTest {
        val mockUser    = mockk<FirebaseUser>()
        val mockContext = mockk<Context>(relaxed = true)
        val mockCR      = mockk<ContentResolver>(relaxed = true)
        val mockUri     = mockk<Uri>()
        val mockStream  = mockk<java.io.InputStream>(relaxed = true)
        val mockDocRef  = mockk<DocumentReference>(relaxed = true)

        every { mockUser.uid } returns "teacher1"
        every { mockAuth.currentUser } returns mockUser
        every { mockContext.contentResolver } returns mockCR
        every { mockCR.openInputStream(any()) } returns mockStream
        every { mockStream.readBytes() } returns ByteArray(16)
        coEvery { CloudinaryHelper.uploadBytes(any(), any()) } returns "https://res.cloudinary.com/prob.jpg"
        every { mockDb.collection("problems").document(any()) } returns mockDocRef
        every { mockDocRef.set(any()) } returns Tasks.forResult<Void>(null)

        val result = repo.uploadProblem("제목", "수학", mockUri, mockContext)

        assertTrue(result.isSuccess)
        assertEquals("teacher1", result.getOrNull()?.teacherId)
        assertEquals("제목", result.getOrNull()?.title)
    }

    @Test fun `uploadProblem returns failure when not logged in`() = runTest {
        every { mockAuth.currentUser } returns null
        val mockContext = mockk<Context>(relaxed = true)
        val mockUri     = mockk<Uri>()

        val result = repo.uploadProblem("제목", "수학", mockUri, mockContext)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("로그인") == true)
    }
}
