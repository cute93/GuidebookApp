package com.example.guidebook

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.guidebook.repository.GuidebookRepository
import com.example.guidebook.viewmodels.Page2ViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class Page2ViewModelTest {

    @get:Rule val rule = InstantTaskExecutorRule()

    private val mockRepo = mockk<GuidebookRepository>(relaxed = true)
    private val bitmap = mockk<Bitmap>(relaxed = true)
    private lateinit var vm: Page2ViewModel

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = Page2ViewModel(repo = mockRepo)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test fun `saveState is null before uploadDrawing is called`() {
        assertNull(vm.saveState.value)
    }

    @Test fun `uploadDrawing sets Uploaded state on success`() {
        coEvery {
            mockRepo.uploadNoteDrawing(any(), any(), any(), any(), any())
        } returns Result.success(mockk(relaxed = true))

        vm.uploadDrawing("p1", "u1", "홍길동", "student", bitmap)

        assertTrue(vm.saveState.value is Page2ViewModel.SaveState.Uploaded)
    }

    @Test fun `uploadDrawing sets Error state on failure`() {
        coEvery {
            mockRepo.uploadNoteDrawing(any(), any(), any(), any(), any())
        } returns Result.failure(Exception("네트워크 오류"))

        vm.uploadDrawing("p1", "u1", "홍길동", "student", bitmap)

        val state = vm.saveState.value
        assertTrue(state is Page2ViewModel.SaveState.Error)
        assertEquals("네트워크 오류", (state as Page2ViewModel.SaveState.Error).message)
    }
}
