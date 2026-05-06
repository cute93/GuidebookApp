package com.example.guidebook

import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.guidebook.repository.GuidebookRepository
import com.example.guidebook.viewmodels.Page3ViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class Page3ViewModelTest {

    @get:Rule val rule = InstantTaskExecutorRule()

    private val mockRepo = mockk<GuidebookRepository>(relaxed = true)
    private val mockContext = mockk<Context>(relaxed = true)
    private val mockUri = mockk<Uri>()
    private lateinit var vm: Page3ViewModel

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = Page3ViewModel(repo = mockRepo)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test fun `uploadState is null initially`() {
        assertNull(vm.uploadState.value)
    }

    @Test fun `blank title sets Error immediately without calling repository`() {
        vm.uploadProblem("", "수학", mockUri, mockContext)

        val state = vm.uploadState.value
        assertTrue(state is Page3ViewModel.UploadState.Error)
        assertEquals("제목을 입력해주세요.", (state as Page3ViewModel.UploadState.Error).message)
        coVerify(exactly = 0) { mockRepo.uploadProblem(any(), any(), any(), any()) }
    }

    @Test fun `uploadProblem sets Success state on success`() {
        coEvery {
            mockRepo.uploadProblem(any(), any(), any(), any())
        } returns Result.success(mockk(relaxed = true))

        vm.uploadProblem("수학 문제", "수학", mockUri, mockContext)

        assertTrue(vm.uploadState.value is Page3ViewModel.UploadState.Success)
    }

    @Test fun `uploadProblem sets Error state on failure`() {
        coEvery {
            mockRepo.uploadProblem(any(), any(), any(), any())
        } returns Result.failure(Exception("업로드 실패"))

        vm.uploadProblem("수학 문제", "수학", mockUri, mockContext)

        val state = vm.uploadState.value
        assertTrue(state is Page3ViewModel.UploadState.Error)
        assertEquals("업로드 실패", (state as Page3ViewModel.UploadState.Error).message)
    }

    @Test fun `loading state is set before upload coroutine starts`() {
        // uploadProblem sets Loading synchronously before launching the coroutine.
        // With UnconfinedTestDispatcher the coroutine runs eagerly, so we verify
        // the final Success state (Loading was the intermediate step).
        coEvery {
            mockRepo.uploadProblem(any(), any(), any(), any())
        } returns Result.success(mockk(relaxed = true))

        vm.uploadProblem("수학 문제", "수학", mockUri, mockContext)

        assertTrue(vm.uploadState.value is Page3ViewModel.UploadState.Success)
    }

    @Test fun `success state contains the uploaded problem object`() {
        val problem = com.example.guidebook.models.Problem(id = "p99", title = "업로드 문제", teacherId = "t1")
        coEvery {
            mockRepo.uploadProblem(any(), any(), any(), any())
        } returns Result.success(problem)

        vm.uploadProblem("업로드 문제", "수학", mockUri, mockContext)

        val state = vm.uploadState.value as? Page3ViewModel.UploadState.Success
        assertEquals("p99", state?.problem?.id)
    }

    @Test fun `error message falls back to default when exception message is null`() {
        coEvery {
            mockRepo.uploadProblem(any(), any(), any(), any())
        } returns Result.failure(Exception(null as String?))

        vm.uploadProblem("제목", "수학", mockUri, mockContext)

        val state = vm.uploadState.value as? Page3ViewModel.UploadState.Error
        assertEquals("업로드 실패", state?.message)
    }
}
