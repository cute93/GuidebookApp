package com.example.guidebook

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guidebook.activities.Page1Activity
import com.example.guidebook.models.AppUser
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Page1ActivityRoleTest {

    private fun launchWithUser(role: String): ActivityScenario<Page1Activity> {
        val user = AppUser(uid = "u1", name = "테스트", email = "test@test.com", role = role)
        val intent = Intent(ApplicationProvider.getApplicationContext(), Page1Activity::class.java)
            .putExtra("user", user)
        return ActivityScenario.launch(intent)
    }

    @Test fun `student role hides upload problem button`() {
        launchWithUser("student").use {
            onView(withId(R.id.btnUploadProblem)).check(matches(not(isDisplayed())))
        }
    }

    @Test fun `teacher role shows upload problem button`() {
        launchWithUser("teacher").use {
            onView(withId(R.id.btnUploadProblem)).check(matches(isDisplayed()))
        }
    }

    @Test fun `prev and next buttons are displayed`() {
        launchWithUser("student").use {
            onView(withId(R.id.btnPrev)).check(matches(isDisplayed()))
            onView(withId(R.id.btnNext)).check(matches(isDisplayed()))
        }
    }
}
