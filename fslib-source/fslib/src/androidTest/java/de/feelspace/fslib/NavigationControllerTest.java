package de.feelspace.fslib;

import android.content.Context;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class NavigationControllerTest implements NavigationEventListener {

    // Navigation controller
    private NavigationController navigationController;
    private BeltConnectionInterface beltConnection;
    private BeltCommunicationInterface beltController;

    // Constants
    private static final long CONNECTION_TIMEOUT_MS = 5000;

    @Before
    public void connectBelt() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        navigationController = new NavigationController(appContext, false);
        navigationController.addNavigationEventListener(this);
        beltConnection = navigationController.getBeltConnection();
        beltController = beltConnection.getCommunicationInterface();
        connectBeltBlocking();
    }

    @After
    public void disconnectBelt() {
        navigationController.disconnectBelt();
    }

    private void connectBeltBlocking() {
        navigationController.searchAndConnectBelt();
        long timeoutTime = System.currentTimeMillis() + CONNECTION_TIMEOUT_MS;
        while (System.currentTimeMillis() < timeoutTime &&
                navigationController.getConnectionState() != BeltConnectionState.STATE_CONNECTED) {
            SystemClock.sleep(50);
        }
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        // Check connection state
        assertEquals(
                BeltConnectionState.STATE_CONNECTED,
                navigationController.getConnectionState());

        // Check orientation notifications active
        // Wait 1 second for orientation update
        // TODO

        // Check that compass accuracy signal is disable
        assertEquals(
                0,
                beltController.getParameterValue(BeltParameter.ACCURACY_SIGNAL_STATE));

        // Check that the mode change to APP when the navigation is started
        // TODO

        // Check that the mode change to PAUSE when the navigation is paused
        // TODO

        // Check that the mode change to WAIT when the navigation stop
        // TODO

        // Check for home button press when navigation is stopped
        System.out.println("Press Home button on the belt.");
        // TODO

        // Check for home button press when navigating
        // TODO

        // Check for pause navigation when compass button pressed in navigation
        // TODO

        // Check for resume navigation when home button pressed in compass mode and navigation paused
        // TODO

        // Check for destination reached signal combined with stop navigation
        // TODO

        // Check for warning signal (outside APP mode)
        // TODO

        // Check for battery signal (outside APP mode)
        // TODO

    }

    @Override
    public void onNavigationStateChanged(NavigationState state) {

    }

    @Override
    public void onBeltHomeButtonPressed(boolean navigating) {

    }

    @Override
    public void onBeltDefaultVibrationIntensityChanged(int intensity) {

    }

    @Override
    public void onBeltOrientationUpdated(int beltHeading, boolean accurate) {

    }

    @Override
    public void onBeltBatteryLevelUpdated(int batteryLevel, PowerStatus status) {

    }

    @Override
    public void onBeltConnectionStateChanged(BeltConnectionState state) {

    }

    @Override
    public void onBeltConnectionLost() {

    }

    @Override
    public void onBeltConnectionFailed() {

    }

    @Override
    public void onNoBeltFound() {

    }
}
