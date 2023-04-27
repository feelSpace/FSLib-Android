package de.feelspace.fslibtest;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Yet another logger.
 */
public class SimpleLogger {
    // Debug
    @SuppressWarnings("unused")
    private static final String DEBUG_TAG = "FeelSpace-Debug";
    @SuppressWarnings("unused")
    private static final boolean DEBUG = true;

    // Writer
    private ParcelFileDescriptor fileDescriptor;
    private FileOutputStream logFileOutputStream;
    private OutputStreamWriter logFileStreamWriter;
    private BufferedWriter logFileWriter;

    // Log start time
    private long logStartTimeNano = -1;

    // Log reference
    private @Nullable String logReference;

    // List of listeners
    private @NonNull ArrayList<SimpleLoggerListener> listeners = new ArrayList<>();

    /**
     * Creates a logger.
     */
    public SimpleLogger() {
    }

    /**
     * Adds a listener.
     * @param listener the listener to add.
     */
    public synchronized void addListener(SimpleLoggerListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     * @param listener the listener to remove.
     */
    public synchronized void removeListener(SimpleLoggerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies listeners of a log state change.
     * @param isLogging the log state to notify.
     */
    private void notifyStateChange(boolean isLogging) {
        ArrayList<SimpleLoggerListener> targets;
        synchronized (this) {
            targets = new ArrayList<>(listeners);
        }
        for (SimpleLoggerListener l: targets) {
            l.onLogStateChanged(isLogging);
        }
    }

    /**
     * Returns the log reference.
     * @return the log reference.
     */
    @Nullable public String getLogReference() {
        return logReference;
    }

    /**
     * Returns <code>true</code> if a log file is opened.
     * @return <code>true</code> if a log file is opened.
     */
    public synchronized boolean isLogging() {
        return logFileWriter != null;
    }

    /**
     * Flushes the file writer buffer.
     */
    public synchronized void flushLog() {
        try {
            if (logFileWriter != null) {
                logFileWriter.flush();
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SimpleLogger: Unable to flush to log file.", e);
        }
    }

    public void startLog(
            @NonNull Context context,
            Uri fileUri,
            @Nullable String logReference)
    throws NullPointerException, IOException {
        closeLog();
        fileDescriptor = context.getContentResolver().openFileDescriptor(fileUri, "w");
        if (fileDescriptor == null) {
            throw new NullPointerException("Unable to create descriptor for URI.");
        }
        logFileOutputStream = new FileOutputStream(fileDescriptor.getFileDescriptor());
        logFileStreamWriter = new OutputStreamWriter(logFileOutputStream);
        logFileWriter = new BufferedWriter(logFileStreamWriter);
        // Log start time
        logStartTimeNano = System.nanoTime();
        // Log reference
        this.logReference = logReference;
        notifyStateChange(true);
    }

    /**
     * Starts logging.
     * @param outputStream The log file output stream.
     */
    public void startLog(@NonNull FileOutputStream outputStream, @Nullable String logReference) {
        synchronized (this) {
            // Close previous log
            closeLog();
            // Open log
            logFileOutputStream = outputStream;
            logFileStreamWriter = new OutputStreamWriter(logFileOutputStream);
            logFileWriter = new BufferedWriter(logFileStreamWriter);
            // Log start time
            logStartTimeNano = System.nanoTime();
            // Log reference
            this.logReference = logReference;
        }
        notifyStateChange(true);
    }

    /**
     * Stops logging and closes the log file.
     */
    public void stopLog() {
        boolean notifyStateChanged = false;
        synchronized (this) {
            if (logFileWriter != null) {
                notifyStateChanged = true;
            }
            closeLog();
        }
        if (notifyStateChanged) {
            notifyStateChange(false);
        }
    }

    private void closeLog() {
        try {
            if (logFileWriter != null) {
                logFileWriter.flush();
            }
            if (logFileStreamWriter != null) {
                logFileStreamWriter.flush();
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SimpleLogger: Unable to close log file.", e);
        }
        try {
            if (logFileWriter != null) {
                logFileWriter.close();
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SimpleLogger: Unable to close log file.", e);
        }
        try {
            if (logFileStreamWriter != null) {
                logFileStreamWriter.close();
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SimpleLogger: Unable to close log file.", e);
        }
        try {
            if (logFileOutputStream != null) {
                logFileOutputStream.close();
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SimpleLogger: Unable to close log file.", e);
        }
        try {
            if (fileDescriptor != null) {
                fileDescriptor.close();
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SimpleLogger: Unable to close log file.", e);
        }
        logFileWriter = null;
        logFileStreamWriter = null;
        logFileOutputStream = null;
        fileDescriptor = null;
    }

    /**
     * Writes the log file.
     * @param context The context to get strings.
     * @param separator The separator to add between elements.
     * @param terminator The terminator to add at the end of elements.
     * @param elements The elements to write.
     * @return <code>true</code> if the write operation was successful, <code>false</code>
     * otherwise.
     */
    public synchronized boolean log(Context context, String separator, String terminator,
                                    Object... elements) {
        if (logFileWriter == null) {
            return false;
        }
        try {
            for (int i=0; i<elements.length; i++) {
                Object o = elements[i];
                if (o == null) {
                    logFileWriter.write("null");
                } else if (o instanceof Number) {
                    logFileWriter.write(((Number)o).toString());
                } else if (o instanceof String) {
                    // Replace separator and terminator occurrences
                    String s = (String)o;
                    if (separator.length() > 0) {
                        s = s.replace(separator, "@SEP");
                    }
                    if (terminator.length() > 0) {
                        s = s.replace(terminator, "@TER");
                    }
                    logFileWriter.write(s);
                } else if (o instanceof  Boolean) {
                    logFileWriter.write(((Boolean)o).toString());
                }
                if (i<elements.length-1) {
                    logFileWriter.write(separator);
                }
            }
            logFileWriter.write(terminator);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SimpleLogger: Unable to write line.", e);
            return false;
        }
        return true;
    }

    /**
     * Returns the elapsed time in nanoseconds since the first write operation on the log file.
     * @return the elapsed time in nanoseconds since the first write operation on the log file.
     */
    public synchronized long getElapsedTimeNano() {
        if (logFileWriter == null || logStartTimeNano < 0) {
            return 0;
        }
        return System.nanoTime()-logStartTimeNano;
    }

    /**
     * Returns the elapsed time in milliseconds since the first write operation on the log file.
     * @return the elapsed time in milliseconds since the first write operation on the log file.
     */
    public synchronized long getElapsedTimeMillis() {
        if (logFileWriter == null || logStartTimeNano < 0) {
            return 0;
        }
        return (System.nanoTime()-logStartTimeNano)/1000000;
    }

    /**
     * Returns a time stamp.
     * @param context A context for accessing strings.
     * @return a time stamp.
     */
    public static String getTimeStamp(Context context) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                context.getString(R.string.log_timestamp_pattern), Locale.getDefault());
        return dateFormat.format(new Date());
    }

}
