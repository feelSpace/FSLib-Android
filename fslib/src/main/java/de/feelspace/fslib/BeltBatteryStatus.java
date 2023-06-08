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
    private @NonNull PowerStatus powerStatus;

    /** The battery level in percents. */
    private float level;

    /** The time to empty or time to full in seconds. */
    private float tteTtf;

    /** Optional extra readings (only for debug purpose) */
    private Float[] extraProperties = new Float[]{null, null, null, null};

    /**
     * Constructor.
     */
    protected BeltBatteryStatus(@NonNull PowerStatus status, float level, float tteTtf) {
        this.powerStatus = status;
        this.level = level;
        this.tteTtf = tteTtf;
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
        extraProperties[0] = (float) ((packet[6] << 8) | (packet[5] & 0xFF)); // int16 mA
        extraProperties[1] = (float) (((packet[8]  & 0xFF) << 8) | (packet[7] & 0xFF)); // uint16 mV
        if (packet.length >= 11) {
            extraProperties[2] = (float) ((packet[10] << 8) | (packet[9] & 0xFF)); // int16 dC
            extraProperties[2] /= 256.f;
        }
        if (packet.length >= 13) {
            extraProperties[3] = (float) (((packet[12]  & 0xFF) << 8) | (packet[11] & 0xFF)); // uint16 %C
            extraProperties[3] /= 256.f;
        }
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
        extraProperties[0] = (Float) in.readValue(Float.class.getClassLoader());
        extraProperties[1] = (Float) in.readValue(Float.class.getClassLoader());
        extraProperties[2] = (Float) in.readValue(Float.class.getClassLoader());
        extraProperties[3] = (Float) in.readValue(Float.class.getClassLoader());
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
     * Returns the extra properties (only for debug purposes).
     * @return the extra properties.
     */
    public Float[] getExtra() {
        return extraProperties;
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
        dest.writeValue(extraProperties[0]);
        dest.writeValue(extraProperties[1]);
        dest.writeValue(extraProperties[2]);
        dest.writeValue(extraProperties[3]);
    }
}
