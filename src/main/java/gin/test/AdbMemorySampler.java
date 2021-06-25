package gin.test;

import gin.util.AndroidDebugBridge;
import gin.util.JankyFrames;
import gin.util.TestExecutionMemory;

public class AdbMemorySampler implements Runnable{

    private String appName;
    private AndroidDebugBridge adb;
    private TestExecutionMemory executionMemory;
    private boolean running;
    private Thread t;

    public AdbMemorySampler(AndroidDebugBridge adb, String appName, int waitMs){
        this.adb = adb;
        this.appName = appName;
        executionMemory = new TestExecutionMemory();
    }


    private float getMemStats() {
        adb.runShellCommand("ps | grep " + appName, true);
        if(adb.output.split("\\s+").length > 1) {
            String PID = adb.output.split("\\s+")[1];
            adb.runShellCommand("dumpsys meminfo " + PID, true);
            String output = adb.output;
            String memoryReading= "0";
            float memoryFloat = 0;
            for (String line : output.split("\n")) {
                if (line.contains("TOTAL")) {
                    memoryReading = line.split("\\s+")[2];
                    memoryFloat = Float.parseFloat(memoryReading) * 100;

                }
            }
            return  memoryFloat;
        }
        return 0;

    }

    private void UpdateStats(){
        executionMemory.addReading(getMemStats());
    }

    public void resetStats(){
        executionMemory = new TestExecutionMemory();
    }

    public void run(){
        while(running){
            try {
                UpdateStats();
                Thread.sleep(10);
            }catch(InterruptedException e){
                e.printStackTrace();
                System.exit(1);
            }

        }
    }

    public void start(){
        if (t==null){
            t = new Thread(this, "Sampler");
            running = true;
            t.start();
        }
    }

    public void stop(){
        if (running){
            t.stop();
            running = false;
        }
    }


    public TestExecutionMemory getExecutionMemory(){
        return  executionMemory;
    }
}
