package de.feelspace.fslibtest;

public interface AdvancedBeltListener {

    void onRawSensorNotificationsStateChanged(boolean enable);

    void onDebugNotificationsStateChanged(boolean enable);

    void onSensorCalibrationRetrieved(float[] magOffsets, float[] magGains, float magCalibError,
                                      float[] gyroOffsets);

    void onRawSensorRecordNotified(int[][] records);

    void onRawSensorNotificationSequenceError();

    void onErrorNotified(int errorCode);

}
