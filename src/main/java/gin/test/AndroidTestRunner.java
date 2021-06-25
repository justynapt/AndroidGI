package gin.test;

import gin.Patch;
import gin.util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AndroidTestRunner {

    private AndroidDebugBridge adb;
    private  AndroidProject project;
    private List<AndroidTest> tests;
    private String apk;
    private String testapk;
    private String fileName;
    private String testAppId;
    private String testRunner;
    private String appName;
    private AdbJankSampler adbJankSampler;
    private AdbMemorySampler adbMemorySampler;
    private localMemorySampler memorySampler;
    public String UID;


    public AndroidTestRunner(AndroidProject project, AndroidConfig config){

        String adbPath = config.getAdbPath();
        String deviceId = config.getDeviceName();
        testAppId = config.getTestAppName();
        adb = new AndroidDebugBridge(deviceId, testAppId, adbPath);
        AndroidDebugBridge jankAdb = new AndroidDebugBridge(deviceId, testAppId, adbPath);
        appName = config.getAppName();
        adbJankSampler = new AdbJankSampler(jankAdb, appName, 10);
        memorySampler = new localMemorySampler();
        apk = config.getApkPath();
        fileName = config.getFilePath();
        this.project = project;
        testRunner = config.getTestRunner();
        getUID();
        memorySampler.start();
        adbJankSampler.start();
        testapk = config.getTestApkPath();
    }


    //install app, reset all gfxinfo
    public void setUp(){

    }

    private void getUID(){
        adb.runShellCommand("dumpsys package " + appName, true);
        String out = adb.output;
        for(String line : out.split("\r\n")){
            if (line.contains("userId=")){
                UID = line.split("=")[1];
            }
        }
    }

    public void installTestAPK() {
        adb.installApp(testapk);
    }

    public AndroidUnitTestResultSet runTests(Patch patch, int frames, boolean breakOnFail) throws IOException, InterruptedException {
        ArrayList<AndroidTestResult> results = new ArrayList<>();
        if(TestRunner.isPatchedSourceSame(patch.getSourceFile().getSource(), patch.apply())){
            if(!patch.toString().equals("|")) {
                return new AndroidUnitTestResultSet(patch, false, results);
            }
        }
        boolean passed;
        patch.writePatchedSourceToFile(fileName);
        System.out.println("compiling app");
        int buildExit = project.buildApp();
        patch.undoWrite(fileName);
        if (buildExit != 0){
            System.out.println("Failed to build App");
            return new AndroidUnitTestResultSet(patch, false, results);
        }

        if (!project.runLocalTests((ArrayList<AndroidTest>) project.unitTests, memorySampler).isPassed()){
            return new AndroidUnitTestResultSet(patch, false, results);
        }


        System.out.println("Installing App");
        boolean failed = false;
        adb.installApp(apk);
        for (AndroidTest test : project.instrumentedTests) {
            if(! test.isPerformance()) {
                AndroidTestResult result = runInstrumentedTest(test);
                results.add(result);
                passed = result.isPassed();
                if (!passed && breakOnFail) {
                    return new AndroidUnitTestResultSet(patch, false, results);
                }
            }
            else{
                int totalFrames = 0;
                int jankFrames = 0;
                int count = 0;
                passed = true;
                while (totalFrames < frames && count < 10){
                    AndroidTestResult result = runInstrumentedTest(test);
                    if(result.getJankiness().totalFrames > 1) {
                        totalFrames += result.getJankiness().totalFrames;
                        jankFrames += result.getJankiness().jankyFrames;

                        passed = passed && result.isPassed();
                        results.add(result);
                    }
                    count++;
                    System.out.println(totalFrames + "/" + frames + " frames generated so far");
                    System.out.println("Test run " + count + " times");
                    if (count == 5 && 5 == totalFrames){
                       break;
                    }
                    if (!passed && breakOnFail){
                        return new AndroidUnitTestResultSet(patch, false, results);
                    }
                }
            }

        }
        return new AndroidUnitTestResultSet(patch, true, results);
    }

    public AndroidUnitTestResultSet runTests(Patch patch, int frames) throws IOException, InterruptedException {
        return runTests(patch, frames, true);
    }

    public AndroidUnitTestResultSet runTestsLocally(Patch patch, int runs, boolean breakOnFail){
        ArrayList<AndroidTestResult> results = new ArrayList<>();
        if(TestRunner.isPatchedSourceSame(patch.getSourceFile().getSource(), patch.apply())) {
            if (!patch.toString().equals("|")) {
                return new AndroidUnitTestResultSet(patch, false, results);
            }
        }
        patch.writePatchedSourceToFile(fileName);
        if (project.buildUnitTests()!= 0){
            return new AndroidUnitTestResultSet(patch, false, results);
        }
        patch.undoWrite(fileName);
        for(int i = 0; i< runs; i++){

            AndroidTestResult result = project.runLocalTests((ArrayList<AndroidTest>) project.unitTests, memorySampler);
            if (! result.isPassed()){
                System.out.println("test Failed");
                return new AndroidUnitTestResultSet(patch, false, results);
            }
            results.add(result);
        }
        return new AndroidUnitTestResultSet(patch, true, results);
    }



    public static boolean parseResult(String result){
        for(String line : result.split("\n")){
            if (line.startsWith("OK ")){
                return true;
            }
        }
        return false;
    }

    public void stop(){
        memorySampler.stop();
        adbJankSampler.stop();
    }


    public AndroidTestResult runInstrumentedTest(AndroidTest test){
        adbJankSampler.resetStats();
        try{
            Thread.sleep(1000);
        }catch (InterruptedException e){

        }
        //adbMemorySampler.resetStats();
        System.out.println("Running test: " + test);
        String cmd = "am instrument -w  --no_window_animation -e class " + test.getModuleName()
                + "." + test.getFullClassName() + "#" + test.getMethodName() +
                " " + testAppId + "/" + testRunner;
        System.out.println(cmd);
        long startTime = System.currentTimeMillis();
        adb.runShellCommand(cmd, true);
        long endTime = System.currentTimeMillis();
        String out = adb.output;
        boolean passed = parseResult(out);
        System.out.println(passed);
        AndroidTestResult result = new AndroidTestResult(test, 1);
        if(test.isPerformance()){
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e){

                }
            JankyFrames jankyFrames = adbJankSampler.getJankyFrames();
            System.out.println(jankyFrames.per95th);
            result.setJankiness(jankyFrames);
        }

        result.setPassed(passed);
        result.setCPUTime(endTime-startTime);
        return result;
    }



}
