package de.feelspace.fslib;

public enum NavigationState {

    /** Navigation stopped */
    STOPPED,

    /** Navigation in pause */
    PAUSED,

    /** Ongoing navigation */
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
