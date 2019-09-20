package de.feelspace.fslib;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class NavigationControllerTest {

    NavigationController navigationController;

    @Before
    public void connectBelt() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        navigationController = new NavigationController(appContext, false);
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("de.feelspace.fslib.test", appContext.getPackageName());
    }
}
