package com.laioffer.eventreporter;

import androidx.test.espresso.action.ViewActions;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.Rule;
import org.junit.Before;
import org.junit.After;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExpressoTestSample {
    private String mStringToBetyped;

    @Rule
    public ActivityTestRule<LoginActivity> mActivityRule = new ActivityTestRule<>(LoginActivity.class);

    @Before
    public void initValidString() {
        mStringToBetyped = "Espresso";
    }

    @Test
    public void checkLoginSuccessful() {
        //Type username
        onView(withId(R.id.editTextLogin)).perform(ViewActions.clearText())
                .perform(ViewActions.typeText("anna"));
        //Type password
        onView(withId(R.id.editTextPassword)).perform(ViewActions.clearText())
                .perform(ViewActions.typeText("666"));
        //Send intent and check intent
        onView(withId(R.id.submit)).perform(ViewActions.click());
        //Check text
        onView(withId(R.id.Events)).check(matches(withText("Events")));
    }

    @Test
    public void checkShowToast() {
        //Type username
        onView(withId(R.id.editTextLogin)).perform(ViewActions.clearText())
                .perform(ViewActions.typeText("anna"));
        //Send intent and check intent
        onView(withId(R.id.submit)).perform(ViewActions.click());
        //Check text
        onView(withId(R.id.Events)).check(matches(withText("Events")));
    }
}
