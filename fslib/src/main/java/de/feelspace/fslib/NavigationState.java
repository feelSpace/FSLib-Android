package de.feelspace.fslib;

/**
 * Enumeration of navigation states used by the navigation controller.
 *
 *  If the navigation controller is connected to a belt the state of the navigation will be
 *  synchronized to the mode of the belt. If no belt is connected, the navigation controller can
 *  still switch between states including {@link NavigationState#NAVIGATING}.
 */
public enum NavigationState {

    /**
     * The navigation is stopped, no direction or signal is defined.
     */
    STOPPED,

    /**
     * The navigation is paused and can be resumed with the current direction and signal type.
     */
    PAUSED,

    /**
     * The navigation has been started with a direction and signal type.
     */
    NAVIGATING;

    @Override
    public String toString() {
        switch (this) {
            case STOPPED:
                return "Stopped";
            case PAUSED:
                return "Paused";
            case NAVIGATING:
                return "Navigating";
        }
        return "Unknown";
    }
}
