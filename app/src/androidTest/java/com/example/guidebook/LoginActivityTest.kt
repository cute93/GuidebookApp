package com.example.guidebook

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guidebook.activities.LoginActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Test fun `login button is displayed`() {
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
    }

    @Test fun `email and password fields are displayed`() {
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
    }

    @Test fun `empty email shows no crash and button remains clickable`() {
        onView(withId(R.id.etEmail)).perform(clearText())
        onView(withId(R.id.etPassword)).perform(clearText())
        onView(withId(R.id.btnLogin)).perform(click())
        // Toast가 표시되고 버튼이 여전히 활성 상태여야 함
        onView(withId(R.id.btnLogin)).check(matches(isEnabled()))
    }

    @Test fun `filling email and password enables login button click`() {
        onView(withId(R.id.etEmail)).perform(typeText("test@test.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("password"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).check(matches(isEnabled()))
    }

    @Test fun `empty password with valid email keeps button enabled after click`() {
        onView(withId(R.id.etEmail)).perform(typeText("test@test.com"), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(clearText())
        onView(withId(R.id.btnLogin)).perform(click())
        onView(withId(R.id.btnLogin)).check(matches(isEnabled()))
    }

    @Test fun `whitespace only email keeps button enabled after click`() {
        onView(withId(R.id.etEmail)).perform(typeText("   "), closeSoftKeyboard())
        onView(withId(R.id.etPassword)).perform(typeText("password"), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())
        onView(withId(R.id.btnLogin)).check(matches(isEnabled()))
    }

    @Test fun `progress bar is initially hidden`() {
        onView(withId(R.id.progressBar)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }
}
