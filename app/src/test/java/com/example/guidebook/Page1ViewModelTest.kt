package com.example.guidebook

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.guidebook.models.AppUser
import com.example.guidebook.models.Problem
import com.example.guidebook.repository.GuidebookRepository
import com.example.guidebook.viewmodels.Page1ViewModel
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class Page1ViewModelTest {

    @get:Rule val rule = InstantTaskExecutorRule()

    private val mockRepo = mockk<GuidebookRepository>(relaxed = true)
    private lateinit var vm: Page1ViewModel

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        vm = Page1ViewModel(repo = mockRepo)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun initWithProblems(vararg problems: Problem) {
        every { mockRepo.observeProblems() } returns flowOf(Result.success(problems.toList()))
        coEvery { mockRepo.getUserNotes(any()) } returns Result.success(emptyList())
        vm.init(AppUser(uid = "u1", name = "테스트", role = "student"))
    }

    @Test fun `currentProblem returns null when problems is empty`() {
        every { mockRepo.observeProblems() } returns flowOf(Result.success(emptyList()))
        vm.init(AppUser(uid = "u1", name = "테스트", role = "student"))
        assertNull(vm.currentProblem)
    }

    @Test fun `currentProblem returns first problem initially`() {
        val p = Problem(id = "p1", title = "문제1")
        initWithProblems(p)
        assertEquals(p, vm.currentProblem)
    }

    @Test fun `goToNext increments index within bounds`() {
        val p1 = Problem(id = "p1", title = "문제1")
        val p2 = Problem(id = "p2", title = "문제2")
        initWithProblems(p1, p2)
        vm.goToNext()
        assertEquals(1, vm.currentIndex.value)
        assertEquals(p2, vm.currentProblem)
    }

    @Test fun `goToNext does not exceed last index`() {
        initWithProblems(Problem(id = "p1", title = "문제1"))
        vm.goToNext()
        assertEquals(0, vm.currentIndex.value)
    }

    @Test fun `goToPrev does nothing at index 0`() {
        initWithProblems(Problem(id = "p1", title = "문제1"))
        vm.goToPrev()
        assertEquals(0, vm.currentIndex.value)
    }

    @Test fun `goToPrev decrements index after goToNext`() {
        val p1 = Problem(id = "p1", title = "문제1")
        val p2 = Problem(id = "p2", title = "문제2")
        initWithProblems(p1, p2)
        vm.goToNext()
        vm.goToPrev()
        assertEquals(0, vm.currentIndex.value)
        assertEquals(p1, vm.currentProblem)
    }

    @Test fun `error LiveData is set on observeProblems failure`() {
        every { mockRepo.observeProblems() } returns flowOf(Result.failure(Exception("네트워크 오류")))
        vm.init(AppUser(uid = "u1", name = "테스트", role = "student"))
        assertEquals("네트워크 오류", vm.error.value)
    }
}
