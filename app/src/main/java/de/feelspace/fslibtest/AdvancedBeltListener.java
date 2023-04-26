package de.feelspace.fslibtest;

public interface AdvancedBeltListener {

    void onRawSensorNotificationsStateChanged(boolean enable);

    void onDebugNotificationsStateChanged(boolean enable);

    void onSensorCalibrationUpdated();

    void onRawSensorRecordNotified(int[][] records);

    void onRawSensorNotificationSequenceError();

    void onErrorNotified(int errorCode);

}
