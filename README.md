

# AndroidGin: A Tool for Experimentation with GI in ANdroid

This is a version of the GIN tool (https://github.com/gintool/gin) reporpoused to run on Android Application
### Installing and Buildin

First clone the repo, then build using gradle:

```
gradle assemble
```

the build.gradle file can be used to change the build process

This will build the tool, and output the gin.jar file

### Using the tool

In order to use the compiled tool, you need two things:

Number 1. An application. 
Simply clone the source code of an application into the Apps Folder. You can then find the part of the app you wish to improve and the test suites you wish to utilise

Number 2. A Config File

The current config.properties file shows an example for the wikimedia commons application (https://github.com/commons-app/apps-android-commons)

It is broken down as follows:

appName:name of application package

appPath:Relative path to main directory of the app

filePath=Relative path to the file being improved

apkPath= path to the apps main apk

testApkPath= path to the applications test apk

testRunner= name of the test runner

testAppName= name of the test application package

adbPath= path to the adb executable

deviceName= serial number of the device to run on

tests=name of tests (format: TestClass.TestMethod or TestClass.* for all methods)

perfTests= name of tests on which frame rate will be measured (same format as above)

flavour= flavour of the application being improved (may be left blank)

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





