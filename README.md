

# AndroidGin: A Tool for Experimentation with GI in ANdroid

This is a version of the GIN tool (https://github.com/gintool/gin) reporpoused to run on Android Application. It is aimed to improve responsiveness of Android apps by reducing frame rate.

If using this code for a publication, please cite the associated paper, thank you: <br>
```
@inproceedings{Callan2021:improvingAndroid,
  author    = {James Callan and
               Justyna Petke},
  editor    = {Una{-}May O'Reilly and
               Xavier Devroey},
  title     = {Improving Android App Responsiveness Through Automated Frame Rate
               Reduction},
  booktitle = {Search-Based Software Engineering - 13th International Symposium,
               {SSBSE} 2021, Bari, Italy, October 11-12, 2021, Proceedings},
  series    = {Lecture Notes in Computer Science},
  volume    = {12914},
  pages     = {136--150},
  publisher = {Springer},
  year      = {2021},
  url       = {https://doi.org/10.1007/978-3-030-88106-1\_10},
  doi       = {10.1007/978-3-030-88106-1\_10}
}
```

### Installing and Buildin

First clone the repo, then build using gradle:

```
gradle assemble
```

the build.gradle file can be used to change the build process

This will build the tool, and output the gin.jar file

### Using the tool

In order to use the compiled tool, you need three things:

Number 1. An application. 
Simply make an Apps folder and clone the source code of an application into it. You can then find the part of the app you wish to improve and the test suites you wish to utilise.

Make sure the app can be built and the test will run before attempting to use the tool or it is unlikely to function correctly.

Enter the applications directory and call 
```
./gradlew cAT
```
and
```
./gradlew check 
```

to ensure the application can be tested


Number 2. A Config File

The current config.properties file shows an example for the wikimedia commons application (https://github.com/commons-app/apps-android-commons)

It is broken down as follows:
```
appName=name of application package

appPath=Relative path to main directory of the app

filePath=Relative path to the file being improved

apkPath=Relative path to the apps main apk

testApkPath=Relative path to the applications test apk

testRunner= name of the test runner

testAppName= name of the test application package

adbPath= path to the adb executable

deviceName= serial number of the device to run on (use adb devices command)

tests=name of tests (format: TestClass.TestMethod or TestClass.* for all methods separated by a comma (,))

perfTests= name of tests on which frame rate will be measured (same format as above)

flavour= flavour of the application being improved (may be left blank)
```

3. An android device, either an emulator or a phone must be running/connected to the machine you run the tool on.

Finally run 
```
java -jar gin.jar
```
to run the tool.

### Important Files

AndroidGI.java:

This is the main file to run GI from, it contains code to run GP and Local Search to improve applications frame rate

AndroidCoverageReporter.java:

File to profile the coverage of all tests by ignoring all other methods, must have jacoco plugin enabled in application (WARNING: very slow)

AndroidTestRunner.java:
This File executes all tests on the android device and collects the results and properties of test executions.


AndroidProject.Java:
This File parses the android source code, finding testcases and calling gradle commands

AndroidDebugBridge:
This file communicates with teh device by executing commands on the android debug bridge.

AdbJankSampler.
This File repeatedly samples the frame statistics of a package in a seperate thread, this is the way that framerate is measured.





