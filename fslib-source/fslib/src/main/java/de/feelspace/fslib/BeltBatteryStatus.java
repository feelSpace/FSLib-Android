/*
 * Copyright (c) 2016-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

/**
 * Battery status information.
 */
public class BeltBatteryStatus implements Parcelable {

    /** Power supply status of the belt. */
    private @NonNull
    PowerStatus powerStatus;

    /** The battery level in percents. */
    private float level;

    /** The time to empty or time to full in seconds. */
    private float tteTtf;

    /** The battery current in milli-ampere. */
    private float current;

    /** The battery voltage in milli-volt. */
    private float voltage;

    /**
     * Constructor.
     */
    protected BeltBatteryStatus(@NonNull PowerStatus status, float level, float tteTtf,
                                float current, float voltage) {
        this.powerStatus = status;
        this.level = level;
        this.tteTtf = tteTtf;
        this.current = current;
        this.voltage = voltage;
    }

    /**
     * Constructor from a binary packet.
     * @param packet the battery status packet.
     * @throws IllegalArgumentException if the packet is malformed.
     */
    protected BeltBatteryStatus(byte[] packet) throws IllegalArgumentException {
        if (packet == null || packet.length < 9) {
            throw new IllegalArgumentException("Malformed battery status packet to create object.");
        }
        powerStatus = PowerStatus.fromValue(packet[0], PowerStatus.UNKNOWN);
        level = ((float)(packet[2] & 0xFF)) + (((float)(packet[1] & 0xFF)) / 256.f);
        tteTtf = ((float)(((packet[4] & 0xFF) << 8) | (packet[3] & 0xFF)))*5.625f;
        current = (packet[6] << 8) | (packet[5] & 0xFF);
        voltage = ((packet[8]  & 0xFF) << 8) | (packet[7] & 0xFF);
    }

    /**
     * Creates a battery status object from a parcel.
     *
     * @param in the parcel to read the battery status.
     */
    public BeltBatteryStatus(Parcel in) {
        powerStatus = PowerStatus.fromValue((byte)in.readInt(), PowerStatus.UNKNOWN);
        level = in.readFloat();
        tteTtf = in.readFloat();
        current = in.readFloat();
        voltage = in.readFloat();
    }

    /**
     * Static creator for parcel.
     */
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public BeltBatteryStatus createFromParcel(Parcel in) {
            return new BeltBatteryStatus(in);
        }
        public BeltBatteryStatus[] newArray(int size) {
            return new BeltBatteryStatus[size];
        }
    };

    /**
     * Returns the power supply status of the belt.
     * @return the power supply status of the belt.
     */
    public PowerStatus getPowerStatus() {
        return powerStatus;
    }

    /**
     * Returns the belt's battery level in percents.
     * @return the belt's battery level in percents.
     */
    public float getLevel() {
        return level;
    }

    /**
     * Returns the time to full (TTF) or time to empty (TTE) of the battery. The TTF is returned
     * when the status is CHARGING, otherwise the TTE is returned.
     * Note that this time is an estimation and may be inaccurate if, the current for charging vary,
     * the belt is used when charging, or when the battery is old.
     *
     * @return the time to full or time to empty of the battery in seconds.
     */
    public float getTteTtf() {
        return tteTtf;
    }

    /**
     * Returns the battery current in milli-ampere.
     * @return the battery current in milli-ampere.
     */
    public float getCurrent() {
        return current;
    }

    /**
     * Returns the battery voltage in milli-volt.
     * @return the battery voltage in milli-volt.
     */
    public float getVoltage() {
        return voltage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(powerStatus.getValue());
        dest.writeFloat(level);
        dest.writeFloat(tteTtf);
        dest.writeFloat(current);
        dest.writeFloat(voltage);
    }
}
