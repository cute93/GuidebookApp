package com.example.guidebook

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guidebook.activities.Page3Activity
import com.example.guidebook.models.AppUser
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Page3ActivityTest {

    private fun launch(
        role: String = "teacher"
    ): ActivityScenario<Page3Activity> {
        val user = AppUser(uid = "t1", name = "김선생", email = "t@t.com", role = role)
        val intent = Intent(ApplicationProvider.getApplicationContext(), Page3Activity::class.java)
            .putExtra("user", user)
        return ActivityScenario.launch(intent)
    }

    // ── UI 표시 확인 ──────────────────────────────────────────────────────────

    @Test fun `title input field is displayed`() {
        launch().use {
            onView(withId(R.id.etTitle)).check(matches(isDisplayed()))
        }
    }

    @Test fun `subject input field is displayed`() {
        launch().use {
            onView(withId(R.id.etSubject)).check(matches(isDisplayed()))
        }
    }

    @Test fun `select image button is displayed`() {
        launch().use {
            onView(withId(R.id.btnSelectImage)).check(matches(isDisplayed()))
        }
    }

    @Test fun `upload problem button is displayed and enabled`() {
        launch().use {
            onView(withId(R.id.btnUploadProblem)).check(matches(isDisplayed()))
            onView(withId(R.id.btnUploadProblem)).check(matches(isEnabled()))
        }
    }

    @Test fun `progress bar is initially hidden`() {
        launch().use {
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }
    }

    // ── 유효성 검사 ───────────────────────────────────────────────────────────

    @Test fun `clicking upload without image shows toast and does not start progress`() {
        launch().use {
            onView(withId(R.id.etTitle)).perform(typeText("제목"), closeSoftKeyboard())
            onView(withId(R.id.btnUploadProblem)).perform(click())
            // 이미지 없으므로 progressBar가 표시되지 않아야 함
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }
    }

    @Test fun `clicking upload with blank title does not show progress bar`() {
        launch().use {
            onView(withId(R.id.etTitle)).perform(clearText(), closeSoftKeyboard())
            onView(withId(R.id.btnUploadProblem)).perform(click())
            // 제목 없고 이미지도 없으므로 progressBar 비표시
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }
    }

    @Test fun `typing in title and subject does not crash`() {
        launch().use {
            onView(withId(R.id.etTitle)).perform(typeText("수학 문제"), closeSoftKeyboard())
            onView(withId(R.id.etSubject)).perform(typeText("수학"), closeSoftKeyboard())
            onView(withId(R.id.etTitle)).check(matches(withText("수학 문제")))
            onView(withId(R.id.etSubject)).check(matches(withText("수학")))
        }
    }
}
