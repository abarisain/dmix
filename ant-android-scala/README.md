These scripts are modified from [Exploring Android](http://lamp.epfl.ch/~michelou/android/) to support android sdk tools r14

Usage
------

1. Create an android project either by using eclipse adt plugin or android create command

2. Checkout `ant-android-scala` to your local disk, `/opt/ant-android-scala` for example
3. Copy `local.properties` in ant-android-scala to the project directory you just created in step 1, and change the configurations to fix your environment
4. Edit `build.xml` (use android update command to generate ant build files if the project is created by eclipse), add

    ```xml
    <import file="${ant.android.scala.dir}/build-scala.xml"/>
    <!-- Converts this project's .class files into .dex files -->
    <target name="-dex" depends="-compile, scala-compile, scala-shrink">
        <do-only-if-not-library elseText="Library project: do not dex..." >
            <scala-dex-helper />
        </do-only-if-not-library>
    </target>
    ```

    under

    ```xml
    <import file="${sdk.dir}/tools/ant/build.xml" />
    ```

5. Now you can add some scala source code and use ant command to build the project

Using Android Scala Installer
-----------------------------
See [Android Scala Installer](https://github.com/jbrechtel/Android-Scala-Installer)

Set `scala.library.installed=true` in `local.properties`, the building script will skip proguard when you run `ant debug` or `ant debug install`, don't forget to add

    <uses-library android:name="scala_actors-2.9.1"/>
    <uses-library android:name="scala_collection-2.9.1"/>
    <uses-library android:name="scala_immutable-2.9.1"/>
    <uses-library android:name="scala_library-2.9.1"/>
    <uses-library android:name="scala_mutable-2.9.1"/>

to the application element in you AndroidManifest.xml
