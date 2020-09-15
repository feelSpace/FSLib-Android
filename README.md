# Smart-FSLib-Android
FSLib-Android is an Android library to control the feelSpace naviBelt from your application. The feelSpace naviBelt is a device that provides tactile feedback using vibration motors located in a belt. The naviBelt can be controlled by an application via Bluetooth Low-Energy. The FSLib can be used in various domains, for instance in navigation applications, VR, simulation, research experiments that require tactile feedback, outdoor and video-games, applications that require tactile feedback to improve focus or attention.

## Documentation

* [FSLib Android documentation](docs/README.md)
* Developer page (coming soon)

## Installation

You can add FSLib to your dependencies in your `build.gradle`:
```
dependencies {
    implementation 'de.feelspace:fslib:2.2.0'
}
```

* For more details and alternatives, see the documentation: [Adding FSLib to your project](docs/README.md#integration-of-the-fslib-module-in-an-android-project)

## Coding convention

### Naming convention

We use the java-style 'convention' for names, see e.g. https://google.github.io/styleguide/javaguide.html. No `m`, `p`, `s`, `mm` prefix to indicate the scope of variables.

### Event notification (implementation of the observer pattern)

For public interfaces of the library see https://www.codeaffine.com/2015/03/11/getting-java-event-notification-right/.

### @NonNull and @Nullable annotations

These annotations can be used by methods that are not accessible outside of the library. However, the @NonNull and @Nullable annotations must not be used in public interfaces of the library. The parameters of public interface methods must always be checked for null value. Please also note that the annotations are not automatically added to the javadoc.


## About feelSpace

* Learn more about feelSpace at [www.feelspace.de](https://www.feelspace.de/?lang=en)

## Copyright and license notice

Copyright 2017-2020, feelSpace GmbH.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0

**Note on using feelSpace Trademarks and Copyrights:**

*Attribution:* You must give appropriate credit to feelSpace GmbH when you use feelSpace products in a publicly disclosed derived work. For instance, you must reference feelSpace GmbH in publications, conferences or seminars when a feelSpace product has been used in the presented work.

*Endorsement or Sponsorship:* You may not use feelSpace name, feelSpace products’ name, and logos in a way that suggests an affiliation or endorsement by feelSpace GmbH of your derived work, except if it was explicitly communicated by feelSpace GmbH.

