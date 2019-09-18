/*
 * Copyright (c) 2016-2019. feelSpace GmbH. All rights reserved.
 *
 * More info: www.feelspace.de
 * Developer contact: dev@feelspace.de
 * General information contact: info@feelspace.de
 */
package de.feelspace.fslib;

import android.support.annotation.NonNull;

/**
 * Button press event.
 */
public class BeltButtonPressEvent {

    /**
     * The pressed button.
     */
    private @NonNull BeltButton button;

    /**
     * The mode before the button press.
     */
    private @NonNull BeltMode previousMode;

    /**
     * The mode after the button press.
     */
    private @NonNull BeltMode subsequentMode;

    /**
     * Creates a button press.
     *
     * @param button the button pressed.
     * @param previousMode the mode before the button press.
     * @param subsequentMode the mode after the button press.
     */
    BeltButtonPressEvent(@NonNull BeltButton button, @NonNull BeltMode previousMode,
                                @NonNull BeltMode subsequentMode) {
        this.button = button;
        this.previousMode = previousMode;
        this.subsequentMode = subsequentMode;
    }

    /**
     * Returns the button for this button press.
     *
     * @return the button pressed.
     */
    public BeltButton getButton() {
        return button;
    }

    /**
     * Returns the mode before the button press.
     *
     * @return the mode before the button press.
     */
    public BeltMode getPreviousMode() {
        return previousMode;
    }

    /**
     * Returns the mode after the button press.
     *
     * @return the mode after the button press.
     */
    public BeltMode getSubsequentMode() {
        return subsequentMode;
    }

    @Override
    public String toString() {
        return "Button pressed: "+button.toString()+" ("+previousMode.toString()+
                " - "+subsequentMode.toString()+")";
    }
}
