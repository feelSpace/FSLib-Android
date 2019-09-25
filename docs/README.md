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

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the [License](../LICENSE) for the specific language governing permissions and
limitations under the License.

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

:construction: The FSLib will be later available as maven repository.

### Using the AAR package

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

### Link the source code of the module


## Structure of the FSLib module


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
