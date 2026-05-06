package com.example.guidebook

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guidebook.activities.Page2Activity
import com.example.guidebook.models.AppUser
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Page2ActivityTest {

    private fun launch(
        user: AppUser = AppUser(uid = "u1", name = "학생1", role = "student"),
        problemId: String = "prob1",
        imageUrl: String = "",
        title: String = "테스트 문제"
    ): ActivityScenario<Page2Activity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), Page2Activity::class.java).apply {
            putExtra("user", user)
            putExtra("problemId", problemId)
            putExtra("problemImageUrl", imageUrl)
            putExtra("problemTitle", title)
        }
        return ActivityScenario.launch(intent)
    }

    // ── 툴바 UI ──────────────────────────────────────────────────────────────

    @Test fun `color buttons are displayed`() {
        launch().use {
            onView(withId(R.id.btnColorBlack)).check(matches(isDisplayed()))
            onView(withId(R.id.btnColorBlue)).check(matches(isDisplayed()))
            onView(withId(R.id.btnColorRed)).check(matches(isDisplayed()))
        }
    }

    @Test fun `eraser undo and clear buttons are displayed`() {
        launch().use {
            onView(withId(R.id.btnEraser)).check(matches(isDisplayed()))
            onView(withId(R.id.btnUndo)).check(matches(isDisplayed()))
            onView(withId(R.id.btnClear)).check(matches(isDisplayed()))
        }
    }

    @Test fun `upload button is displayed and enabled`() {
        launch().use {
            onView(withId(R.id.btnUpload)).check(matches(isDisplayed()))
            onView(withId(R.id.btnUpload)).check(matches(isEnabled()))
        }
    }

    @Test fun `drawing canvas is displayed`() {
        launch().use {
            onView(withId(R.id.drawingView)).check(matches(isDisplayed()))
        }
    }

    @Test fun `problem title is shown in header`() {
        launch(title = "수학 문제 1").use {
            onView(withId(R.id.tvProblemTitle2)).check(matches(withText("수학 문제 1")))
        }
    }

    // ── 버튼 동작 ─────────────────────────────────────────────────────────────

    @Test fun `eraser button click does not crash`() {
        launch().use {
            onView(withId(R.id.btnEraser)).perform(click())
        }
    }

    @Test fun `undo button click on empty canvas does not crash`() {
        launch().use {
            onView(withId(R.id.btnUndo)).perform(click())
        }
    }

    @Test fun `clear button click does not crash`() {
        launch().use {
            onView(withId(R.id.btnClear)).perform(click())
        }
    }

    @Test fun `progress bar is initially hidden`() {
        launch().use {
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }
    }

    // ── Intent 예외 처리 ──────────────────────────────────────────────────────

    @Test fun `missing problemId extra finishes activity immediately`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), Page2Activity::class.java)
            .putExtra("user", AppUser(uid = "u1"))
            // problemId 없음

        ActivityScenario.launch<Page2Activity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing)
            }
        }
    }

    @Test fun `missing user extra finishes activity immediately`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), Page2Activity::class.java)
            .putExtra("problemId", "p1")
            // user 없음

        ActivityScenario.launch<Page2Activity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing)
            }
        }
    }
}
