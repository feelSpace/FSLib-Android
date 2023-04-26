package de.feelspace.fslibtest;

/**
 * Listener of the simple logger.
 */
public interface SimpleLoggerListener {

    /**
     * Called when a log file has been opened or closed.
     * @param isLogging The log state.
     */
    void onLogStateChanged(boolean isLogging);

}
