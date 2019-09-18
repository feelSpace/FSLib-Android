# FSLib-Android-BLE
FSLib for Android

# Content

* [Copyright and license notice](#copyright-and-license-notice)
* [FSLib for Android](#fslib-for-android)
  * [Structure of the repository](#structure-of-the-repository)
  * [Integration of the FSLib module in an Android project](#integration-of-the-fslib-module-in-an-android-project)
  * [Application permissions](#application-permissions)
  * [Structure of the FSLib module](#structure-of-the-fslib-module)

## Copyright and license notice

Copyright feelSpace GmbH, 2017-2019.

This agreement applies to the software modules, the source code and the documentation that are hereinafter referred to as “FSLib”, “FSLib API” or “FSLib SDK”. Only individuals, research groups or business entities agreed with in writing by feelSpace GmbH may use the FSLib SDK.

The FSLib SDK is distributed by feelSpace with no warranty, expressed or implied. Do not redistribute, sell, or publish for any purpose, any portion of the FSLib SDK (including the documentation) without the prior explicit written consent of feelSpace GmbH.

All rights reserved by feelSpace GmbH.

**Note on using feelSpace Trademarks and Copyrights:**

*Attribution:* You must give appropriate credit to feelSpace GmbH when you use feelSpace products in a publicly disclosed derived work. For instance, you must reference feelSpace GmbH in publications, conferences or seminars when a feelSpace product has been used in the presented work.

*Endorsement or Sponsorship:* You may not use feelSpace name, feelSpace products’ name, and logos in a way that suggests an affiliation or endorsement by feelSpace GmbH of your derived work, except if it was explicitly communicated by feelSpace GmbH.

# FSLib for Android

## Structure of the repository

The repository contains two directories:
* **fslib-aar**: The FSLib module as an AAR archive. Two JAR files are also provided with the javadoc and the source-code of the public interfaces. These two JAR files can be used in Android Studio to obtain contextual documentation.
* **fslib-demo**: A demo application that illustrates how to use the FSLib. This demo is ready to use and the source code can be easily copied-pasted in your project.

## Integration of the FSLib module in an Android project

To use the FSLib in a project you must import the library “fslib-android-ble-[version number].aar” into your project. In Android Studio, the right way to import the library is:

* Select in menu “File” > “New Module”,
* Select the option “Import .JAR/.AAR Package” then “Next” ,
* Select the AAR file of the FSLib and click “Finish”.

The name of the library should appear in the `settings.gradle`. You must also add, in the `build.gradle` of your app, a dependency to the FSLib:

```gradle
dependencies {
    // FSLib
    implementation project(':fslib-android-ble-[version number]')
    
    // ...
}
```

For additional information on adding module in Android Studio please refer to: https://developer.android.com/studio/projects/android-library#AddDependency

Note that the minimum Android SDK version for the FSLib is 18. In the `build.gradle` of your application the `minSdkVersion` must have a value of 18 or higher.

## Application permissions

The FSLib module uses 4 permissions declared in its own “AndroidManifest.xml”:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

These permissions are automatically merged: https://developer.android.com/studio/build/manifest-merge.html

Note that the Location permissions are necessary for Bluetooth Low-Energy scan. In addition, the Location service must be enabled for scanning. (This is a joke from Google: https://issuetracker.google.com/issues/37065090.) In the FSLib, the `BluetoothActivationFragment` is provided to simplify the activation of required services.

## Structure of the FSLib module

In the FSLib two classes are exposed to connect a belt and send commands:
* **BeltConnectionInterface.java** This is the class that manages the connection with the belt.
* **BeltCommunicationInterface.java** This is the class to control the belt when a connection is established.

### In short

You retrieve and initialize the connection manager, then retrieve the communication manager:

```java
// Retrieve the connection and command manager
BeltConnectionInterface beltConnectionManager = BeltConnectionInterface.getInstance();
beltConnectionManager.init(getApplicationContext(), 0);
BeltCommunicationInterface beltController = beltConnectionManager.getCommunicationInterface();
```
You can use the `BluetoothActivationFragment` to check and activate Bluetooth, and when Bluetooth is ON and Location services are activated you can connect a belt:

```java
beltConnectionManager.searchAndConnect();
```

To control the vibration of the belt you must use the methods of the `BeltCommunicationInterface`. The belt must be in APP mode to start a vibration:

```java
if (beltController.getBeltMode() != BeltMode.BELT_MODE_APP) {
    beltController.switchToMode(BeltMode.BELT_MODE_APP);
}
```

In APP mode you can use the methods `vibrateAtMagneticBearing`, `vibrateAtAngle`, `signal` and `stopVibration` to control the vibration.

```java
// Starts a vibration on the rigth (90°) with the default intensity
beltController.vibrateAtAngle(90, 
                              null, // Default intensity
                              BeltCommunicationInterface.PATTERN_CONTINUOUS, // Vibration pattern
                              1, // Vibration channel
                              true // Stop other channels
                              );
```
