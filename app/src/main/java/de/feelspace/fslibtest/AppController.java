package de.feelspace.fslibtest;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import de.feelspace.fslib.BeltCommunicationInterface;
import de.feelspace.fslib.BeltConnectionInterface;
import de.feelspace.fslib.NavigationController;

/**
 * Main application controller.
 */
public class AppController {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // Application context
    private @Nullable Context applicationContext;

    // Belt controller
    private NavigationController navigationController;

    /**
     * Private constructor for singleton.
     */
    private AppController() {
    }

    /**
     * Singleton holder for synchronized instantiation without global synchronization of the
     * <code>getInstance</code> method.
     */
    private static class SingletonHolder {
        /**
         * Singleton instance, only initialized when accessed.
         */
        private static final AppController instance = new AppController();
    }

    /**
     * Returns the unique instance of the controller.
     */
    public static AppController getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * Initializes the controller with a context.
     *
     * @param applicationContext The application context.
     */
    public synchronized void init(@NonNull Context applicationContext) {
        if (this.applicationContext == null) {
            // Keep application context
            this.applicationContext = applicationContext;
            // Initialize the navigation controller
            this.navigationController = new NavigationController(applicationContext);
        }
    }

    public NavigationController getNavigationController() {
        return navigationController;
    }

}
