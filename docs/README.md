# FSLib-Android Documentation

FSLib-Android is an Android library to control the feelSpace naviBelt from your application.

# Content

* [Copyright and license notice](#copyright-and-license-notice)
* [FSLib for Android](#fslib-for-android)
  * [Structure of the repository](#structure-of-the-repository)
  * [Integration of the FSLib module in an Android project](#integration-of-the-fslib-module-in-an-android-project)
    * [Using the AAR package](#using-the-aar-package)
    * [Linking the source code of the module](#linking-the-source-code-of-the-module)
  * [Setup of your project](#setup-of-your-project)
    * [Minimum Android SDK version](#minimum-android-sdk-version)
    * [Application permissions](#application-permissions)	
  * [Structure of the FSLib module](#structure-of-the-fslib-module)
* [Navigation API](#navigation-api)
  * [Introduction](#introduction)
  * [Bluetooth activation and permission granting](#bluetooth-activation-and-permission-granting)
  * [Connection and disconnect of a belt](#connection-and-disconnection-of-a-belt)
  * [Navigation state and belt mode](#navigation-state-and-belt-mode)
  * [Belt button press](#belt-button-press)
  * [Continuous and repeated vibration signals](#continuous-and-repeated-vibration-signals)
  * [Vibration notifications](#vibration-notifications)
  * [Vibration intensity](#vibration-intensity)
  * [Belt orientation](#belt-orientation)
  * [Belt battery level](#belt-battery-level)
  * [Compass accuracy signal](#compass-accuracy-signal)

## Copyright and license notice

Copyright 2017-2020, feelSpace GmbH.

Licensed under the Apache License, Version 2.0:
http://www.apache.org/licenses/LICENSE-2.0

**Note on using feelSpace Trademarks and Copyrights:**

*Attribution:* You must give appropriate credit to feelSpace GmbH when you use feelSpace products in a publicly disclosed derived work. For instance, you must reference feelSpace GmbH in publications, conferences or seminars when a feelSpace product has been used in the presented work.

*Endorsement or Sponsorship:* You may not use feelSpace name, feelSpace products’ name, and logos in a way that suggests an affiliation or endorsement by feelSpace GmbH of your derived work, except if it was explicitly communicated by feelSpace GmbH.

# FSLib for Android

## Structure of the repository

The repository contains an Android Studio project with additional directories:
* **docs**: The documentation of the FSLib for Android.
* **fslib**: The FSLib module that you can link to your project.
* **app**: A test application module.

## Integration of the FSLib module in an Android project

Since JCenter is not available anymore (versions of FSLib from JCenter are obsolete), the best way to use FSLib it to add it as a module with linked sources.

### Linking the source code of the module

In ` settings.gradle` adds:
```gradle
include ':fslib'
project(':fslib').projectDir = new File('PathToFSLibModule')
```

In ` app/build.gradle`
```gradle
dependencies {
    implementation project(':fslib')
    ...
}
```

:anger: If you find it unintuitive, it is normal, Google didn't find it useful to provide simple way to reuse source code between projects.

### Using the AAR package

An alternative to linked source is to use an AAR package. For some release, an AAR package of the FSLib is attached to the [release](https://github.com/feelSpace/FSLib-Android/releases). To import the package:

* Select in menu “File” > “New Module”,
* Select the option “Import .JAR/.AAR Package” then “Next” ,
* Select the AAR file of the FSLib and click “Finish”.

The name of the library should appear in the `settings.gradle`. You must also add, in the `build.gradle` of your app, a dependency to the FSLib:

```gradle
dependencies {
    // FSLib
    implementation project(':fslib-android-[version number]')
    
    // ...
}
```

For additional information on adding module in Android Studio please refer to: https://developer.android.com/studio/projects/android-library#AddDependency

## Setup of your project

### Minimum Android SDK version

Note that the minimum Android SDK version for the FSLib is 28 because Bluetooth permissions are an nightmare. In the `build.gradle` of your application the minSdkVersion must have a value of 28 or higher.

### Application permissions

The FSLib module uses 6 permissions declared in its own “AndroidManifest.xml”:

```xml
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

These permissions are automatically merged: https://developer.android.com/studio/build/manifest-merge.html

:anger: Note that the Location permissions are necessary for Bluetooth Low-Energy scan. In addition, the Location service must be enabled for scanning. (This is a joke from Google: https://issuetracker.google.com/issues/37065090.) 

You can check the `BluetoothCheckActivity` provided in the app for some hints on how to manage permissions and services.

## Structure of the FSLib module

The FSLib proposes two approaches for connecting and controlling a belt:

- **The navigation API:** It is the recommended approach. The navigation API provides simple methods to connect and control a belt. The main class to start developing with the navigation API is the `NavigationController`.
- **The general API:** The general API provides a large set of methods to control the belt for complex applications. Your application must manage the mode of the belt and the belt's button events. The main interfaces of the general API are the `BeltConnectionInterface`, the `BeltCommandInterface`, and for advanced belt instructions the `BeltCommunicationInterface`. In any case it is recommended to look at the implementation of the `NavigationController` before you start using the general API.

# Navigation API

## Introduction

The navigation API provides simple methods to connect and control a belt. Although the term "navigation" is used, this API can be used in different scenarios to control the orientation of a vibration signal. The orientation of the vibration can be a magnetic bearing (i.e. an angle relative to magnetic North) or a position on the belt (e.g. 90 degrees for the right side of the belt).

The main class to connect a belt and control the vibration is the `NavigationController`. You must also implement a `NavigationEventListener` to handle the callback of the navigation controller.

It is recommended to look at the demo application that illustrates how to use the navigation controller. The relevant part of the code is located in the `MainActivity` class of the `app` module.

## Bluetooth activation and permission granting

In order to search for a belt (i.e. scan) your application requires a permission for location data (Not a joke, thanks Google). For more details see [here]( https://developer.android.com/guide/topics/connectivity/bluetooth-le#permissions). You can implement your own procedure for location permission and Bluetooth activation, or adapt the `BluetoothCheckActivity` available in the [test application within the `app` package](https://github.com/feelSpace/FSLib-Android/tree/master/app/src/main/java/de/feelspace/fslibtest). It is, however, recommended to adapt or implement your own solution to manage Android permissions.

To use the `BluetoothCheckActivity`, your main Activity must extends it and you must implement the `BluetoothCheckCallback`. Before starting the connection with the belt, call `activateBluetooth` and start the connection only in the callback `onBluetoothReady`.
```java
public class MainActivity extends BluetoothCheckActivity implements BluetoothCheckCallback {

    // Belt navigation controller
    private NavigationController navigationController;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the navigation controller
        navigationController = new NavigationController(getApplicationContext());

        // Connect button
        connectButton = findViewById(R.id.activity_main_connect_button);
        connectButton.setOnClickListener(view -> {
            activateBluetooth(this);
        });
    }

    @Override
    public void onBluetoothReady() {
        // Bluetooth is active, continue with connection
        navigationController.searchAndConnectBelt();
    }

    @Override
    public void onBluetoothActivationRejected() {
        // Inform user that the Bluetooth activation has failed
    }

    @Override
    public void onBluetoothActivationFailed() {
        // Inform user that the Bluetooth activation has failed
    }

    @Override
    public void onUnsupportedFeature() {
        // Inform user that the connection cannot be performed without Bluetooth
    }

}
``` 

If you implement your own procedure for location permission and Bluetooth activation, pay attention to the following:
- Scanning for a belt requires a location permission granted (not only declared in the `AndroidManifest.xml` but also asked via `requestPermissions()`). See documentation: [Request app permissions]( https://developer.android.com/training/permissions/requesting).
- Scanning for a belt cannot work without location service enabled. See documentation: [Prompt the user to change location settings](https://developer.android.com/training/location/change-location-settings#prompt)
- You may scan with a filter to avoid the activation of the location service, but scan filters seem broken.
- Bluetooth must be activated. See documentation: [Set up BLE](https://developer.android.com/guide/topics/connectivity/bluetooth-le#setup)
- Other Bluetooth problems are listed [here](https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues). :tada:
- Also, the procedure and required permissions depend on the Android API version (and sometimes the behavior is different from one brand to another).
- Finally, if BLE connection does not work on your smartphone, check the Logcat, clean/remove the application, remove pairing to the belt, and reboot your phone.

## Connection and disconnection of a belt

To connect and control a belt you must, first, create an instance of the navigation controller for your application. The argument of the constructor is a `Context`, because accessing the Bluetooth service requires it.

You can use the same navigation controller instance for succesive connections. For complex applications you may instanciate it in your `Application` or `Service` subclass.

```java
navigationController = new NavigationController(getApplicationContext());
```

You must also implement and register the listener interface for the callbacks of the navigation controller. If the listener is an Activity or Fragment, you should register it in `onResume()` and remove it `onPause()`.
```java
navigationController.addNavigationEventListener(this);
```

To search and connect a belt call `searchAndConnectBelt()`, and for disconnecting `disconnectBelt()`. Note that if you use the `BluetoothActivationFragment`, the connection must be requested in the callback `onBluetoothActivated()`.
```java
    @Override
    public void onBluetoothActivated() {
        // Bluetooth is active, continue with connection
        navigationController.searchAndConnectBelt();
    }
```

The connection state can be retrieved from the navigation controller with `getConnectionState()`. The listeners are also informed of connection events with the callbacks:
- `onBeltConnectionStateChanged(BeltConnectionState state)` when the connection state changed.
- `onBeltConnectionLost()` when the connection with the belt has been lost.
- `onBeltConnectionFailed()` when the connection failed.
- `onNoBeltFound()` when no belt has been found during the connection procedure.

## Navigation state and belt mode

The navigation controller automatically manages the mode of the belt according to the state of the navigation. The navigation state can be changed by the application when calling `startNavigation()`, `stopNavigation()` or `pauseNavigation()`. The state can also change when a belt is connected and a button on the belt is pressed. For instance, pressing the pause button of the belt when the navigation state is `NavigationState.NAVIGATING`, changes the navigation state to `NavigationState.PAUSE` (and the mode of the belt will also be changed to Pause mode). The application controls the vibration of the belt only when in `NAVIGATING` state.

The navigation state can be retrieve with `getNavigationState()`. When the navigation state changes, listeners are informed with the callback `onNavigationStateChanged(NavigationState state)`.

It is possible to change the navigation state even if no belt is connected (e.g. by calling `startNavigation()`). If a belt is connected when the navigation state is `NAVIGATING`, then the belt will start signalizing the navigation signal when connected. In this way, it is possible to use most of the method of the navigation controller without having to consider if a belt is connected or not.

## Belt button press

The navigation controller already manages most of the behavior on button press and mode change. The application must only define a behavior when the home button is pressed, and the navigation is started or stopped. The callback to implement for handling home button press is `onBeltHomeButtonPressed()`.

The detailed behavior of the navigation controller on a button press is the following:
- **Home button**: If the navigation is stopped or started, listeners of navigation events are informed of the button press via `onBeltHomeButtonPressed()`. If the navigation is paused and the belt is not in pause mode, the navigation is resumed automatically. If the navigation is paused and the belt is in pause mode, the vibration intensity is changed.
- **Power button**: On short press, the battery level vibration is started without callback to the application. On long press, the belt is switched off and navigation listeners are informed of the disconnection via `onBeltConnectionStateChanged()` (the callback `onBeltConnectionLost()` is NOT called because the connection is not lost, just stopped).
- **Pause button**: If the navigation is started when the pause button is pressed, the navigation is automatically paused. If the belt was in pause mode from navigation, the navigation is automatically resumed.
- **Compass button**: If the navigation is started when the compass button is pressed, the navigation is paused automatically and the belt goes to compass, crossing or calibration mode according to the type of press. In case the belt is in pause mode, the vibration intensity is changed.

## Continuous and repeated vibration signals

The vibration signal is defined when calling `startNavigation()` and `updateNavigationSignal()`. The `direction` parameter is the orientation of the vibration in degree. This orientation is relative to magnetic North if the parameter `isMagneticBearing` is `true`, otherwise the orientation is relative to the belt itself. The type of vibration is defined by the `signal` parameter. To stop the vibration without stopping the navigation it is possible to specify the value `null` for the parameter `signal`.

## Vibration notifications

In addition to continuous or repeated vibration signals, some temporary vibration signals can be started.
- **notifyDestinationReached()**: Starts a single iteration of the destination reached signal. Using this method, it is possible to stop the navigation when the signal is performed.
- **notifyDirection()**: Starts a temporary vibration in a given direction.
- **notifyWarning()**: Starts a warning vibration signal.
- **notifyBeltBatteryLevel()**: Starts the battery level signal of the belt.

## Vibration intensity

For all vibration signals except the operation warning, the default vibration intensity of the belt is used. When a belt is connected, the default intensity can be retrieved with `getDefaultVibrationIntensity()`. To change the default vibration intensity the method `changeDefaultVibrationIntensity()` must be used. When a belt is connected, listeners of the navigation controller are informed of vibration intensity changes via the callback `onBeltDefaultVibrationIntensityChanged()`. Note that the vibration intensity can be changed using the buttons of the belt.

## Belt orientation

The orientation of the belt (relative to magnetic North) is notified to listeners via the callback `onBeltOrientationUpdated()`. The orientation is updated every 500 milliseconds. The last orientation value can also be retrieved with the method `getBeltHeading()`.

## Belt battery level

The battery level of the belt is notified to listeners via the callback `onBeltBatteryLevelUpdated()`. The last known value of the belt battery level can also be retrieved with the method `getBeltBatteryLevel()`.

## Compass accuracy signal

The belt emits a vibration signal to indicate that the internal compass is inaccurate. This may happen when the belt is used indoor or in a place with magnetic interferences. This compass accuracy signal is performed in compass mode, crossing mode and in application mode (the mode used in navigation). For some applications it is preferable to disable the compass accuracy signal, for instance, because vibration signals are not relative to magnetic North or orientation accuracy is not critical.

You can retrieve the compass accuracy signal state via `isCompassAccuracySignalEnabled()`. However, the value may be unknown for a short period after connection. The state of the compass accuracy signal can be changed when a belt is connected using the method `setCompassAccuracySignal(boolean enable, boolean persistent)`. Any update to the parameter (including the first reading of the parameter after connection) is notified to listeners via the callback `onCompassAccuracySignalStateUpdated(boolean enabled)`.

:warning: The accuracy signal state setting can be temporary, i.e. defined for the current power cycle of the belt and reset when the belt is powered-off, or the setting can be persistent and saved on the belt. In case the setting is saved on the belt, it is important to inform the user of this new configuration as it will also impact the compass and crossing mode when no app is connected to the belt.
