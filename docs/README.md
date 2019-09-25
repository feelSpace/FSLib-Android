# FSLib-Android Documentation

FSLib-Android is an Android library to control the feelSpace naviBelt from your application.

# Content

* [Copyright and license notice](#copyright-and-license-notice)
* [FSLib for Android](#fslib-for-android)
  * [Structure of the repository](#structure-of-the-repository)
  * [Integration of the FSLib module in an Android project](#integration-of-the-fslib-module-in-an-android-project)
  * [Application permissions](#application-permissions)
  * [Structure of the FSLib module](#structure-of-the-fslib-module)

## Copyright and license notice

Copyright 2017-2019, feelSpace GmbH.

Licensed under the Apache License, Version 2.0:
http://www.apache.org/licenses/LICENSE-2.0

**Note on using feelSpace Trademarks and Copyrights:**

*Attribution:* You must give appropriate credit to feelSpace GmbH when you use feelSpace products in a publicly disclosed derived work. For instance, you must reference feelSpace GmbH in publications, conferences or seminars when a feelSpace product has been used in the presented work.

*Endorsement or Sponsorship:* You may not use feelSpace name, feelSpace products’ name, and logos in a way that suggests an affiliation or endorsement by feelSpace GmbH of your derived work, except if it was explicitly communicated by feelSpace GmbH.

# FSLib for Android

## Structure of the repository

The repository contains three directories:
* **docs**: The documentation of the FSLib for Android.
* **fslib-aar**: The FSLib module as an AAR archive. Two JAR files are also provided with the javadoc and the source-code of the public interfaces. These two JAR files can be used in Android Studio to obtain contextual documentation.
* **fslib-source**: An Android Studio project containing the source code of the FSLib and a demo application.

## Integration of the FSLib module in an Android project

You have two options to integrate the FSLib into your project. You can 1) use the AAR package of the library, or 2) link the source code of the FSLib in your project.

:construction: The FSLib will be soon available as maven repository.

### Using the AAR package

The AAR package of the FSLib is available in the `fslib-aar\` directory of the repository. To import the package:

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

### Link the source code of the module

In ` settings.gradle` adds:
```gradle
include ':fslib
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

## Setup of your project

### Minimum Android SDK version

Note that the minimum Android SDK version for the FSLib is 18 because Bluetooth low-energy is only supported from SDK version version 18. In the `build.gradle` of your application the minSdkVersion must have a value of 18 or higher.

### Application permissions

The FSLib module uses 4 permissions declared in its own “AndroidManifest.xml”:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

These permissions are automatically merged: https://developer.android.com/studio/build/manifest-merge.html

:anger: Note that the Location permissions are necessary for Bluetooth Low-Energy scan. In addition, the Location service must be enabled for scanning. (This is a joke from Google: https://issuetracker.google.com/issues/37065090.) 

In the FSLib, the `BluetoothActivationFragment` is provided to simplify the activation of required services.

## Structure of the FSLib module

The FSLib proposes two approaches for connecting and controlling a belt:

- **The navigation API:** It is the recommended approach. The navigation API provides simple methods to connect and control a belt. The main class to start developing with the navigation API is the `NavigationController`.
- **The general API:** The general API provides a large set of methods to control the belt for complex applications. Your application must manage the mode of the belt and the belt's button events. The main interfaces of the general API are the `BeltConnectionInterface`, the `BeltCommandInterface`, and for advanced belt instructions the `BeltCommunicationInterface`. In any case it is recommended to look at the implementation of the `NavigationController` before you start using the general API.

# Navigation API

## Introduction

The navigation API provides simple methods to connect and control a belt. Although the term "navigation" is used, this API can be used in different scenarios to the control the orientation of a vibration signal. The orientation of the vibration can be a magnetic bearing (i.e. an angle relative to magnetic North) or a position on the belt (e.g. 90 degrees for the right side of the belt).

The main class to connect a belt and control the vibration is the `NavigationController`. You must also implement a `NavigationEventListener` to handle the callback of the navigation controller.

It is recommended to look at the demo application that illustrates how to use the navigation controller. The relevant part of the code is located in the `MainActivity` class of the `app` module.

## Bluetooth activation and permission granting

In order to search for a belt (i.e. scan) your application requires a permission for location data (Not a joke, thanks Google). For more details see [here]( https://developer.android.com/guide/topics/connectivity/bluetooth-le#permissions). You can implement your own procedure for location permission and Bluetooth activation, or use the `BluetoothActivationFragment` provided by the FSLib.

Create and attach a `BluetoothActivationFragment` to the activity that will initiate the connection. The activity must implement the interface `OnBluetoothActivationCallback`.
```java
public class MainActivity extends AppCompatActivity implements OnBluetoothActivationCallback {

    // Fragment to check BT activation and check permissions
    private BluetoothActivationFragment bluetoothActivationFragment;
    protected static final String BLUETOOTH_ACTIVATION_FRAGMENT_TAG =
            "MainActivity.BLUETOOTH_ACTIVATION_FRAGMENT_TAG";

    // Belt navigation controller
    private NavigationController navigationController;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add BT activation and permission-checker fragment
        FragmentManager fm = getSupportFragmentManager();
        bluetoothActivationFragment = (BluetoothActivationFragment) fm.findFragmentByTag(
                BLUETOOTH_ACTIVATION_FRAGMENT_TAG);
        if (bluetoothActivationFragment == null) {
            bluetoothActivationFragment = new BluetoothActivationFragment();
            fm.beginTransaction().add(bluetoothActivationFragment,
                    BLUETOOTH_ACTIVATION_FRAGMENT_TAG).commit();
        }
        // Create the navigation controller
        navigationController = new NavigationController(getApplicationContext(), false);
    }
    
    @Override
    public void onBluetoothActivated() {
        // Bluetooth is active, continue with connection
        navigationController.searchAndConnectBelt();
    }

    @Override
    public void onBluetoothActivationRejected() {
        // Inform user that the connection cannot be performed without Bluetooth
    }

    @Override
    public void onBluetoothActivationFailed() {
        // Inform user that the Bluetooth activation has failed
    }
}
``` 

Before you start the connection procedure you must call the method `activateBluetooth()` of the `BluetoothActivationFragment`, and then start the connection procedure in the callback `onBluetoothActivated()`.

```
    private void connectBelt() {
        bluetoothActivationFragment.activateBluetooth();
    }
    @Override
    public void onBluetoothActivated() {
        // Bluetooth is active, continue with connection
        navigationController.searchAndConnectBelt();
    }
```

If you want to implement your own procedure for location permission and Bluetooth activation, pay attention to the following:
- Scanning for a belt requires a location permission granted (not only declared in the `AndroidManifest.xml` but also asked via `requestPermissions()`). See documentation: [Request app permissions]( https://developer.android.com/training/permissions/requesting).
- Scanning for a belt cannot work without location service enabled. See documentation: [Prompt the user to change location settings](https://developer.android.com/training/location/change-location-settings#prompt)
- You may scan with a filter to avoid the activation of the location service, but scan filters seem broken.
- Bluetooth must be activated. See documentation: [Set up BLE](https://developer.android.com/guide/topics/connectivity/bluetooth-le#setup)
- Other Bluetooth problems are listed [here](https://github.com/iDevicesInc/SweetBlue/wiki/Android-BLE-Issues).

## Connection and disconnect of a belt

To connect and control a belt you must, first, create an instance of the navigation controller for your application. The first argument of the constructor is a `Context` (because accessing the Bluetooth service requires a `Context`), the second argument indicates if the compass accuracy signal is enabled when your application is connected to the belt (see [Compass accuracy signal](#compass-accuracy-signal)).

```java
navigationController = new NavigationController(getApplicationContext(), false);
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

## Control of the belt mode


## Continuous and repeated vibration signals

## Vibration notifications

## Belt orientation

## Belt battery status

## Compass accuracy signal


