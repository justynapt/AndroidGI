package gin.test;

import gin.util.AndroidDebugBridge;
import gin.util.JankyFrames;
import gin.util.TestExecutionMemory;

public class AdbJankSampler implements Runnable{

    private String appName;
    private AndroidDebugBridge adb;
    private int waitMs;
    private JankyFrames jankyFrames;
    private boolean running;
    private Thread t;
    int count;

    public AdbJankSampler(AndroidDebugBridge adb, String appName, int waitMs){
        this.adb = adb;
        this.appName = appName;
        this.waitMs = waitMs;
        jankyFrames = new JankyFrames(0,1);
    }

    private JankyFrames getFrameStats(){

        adb.runShellCommand("dumpsys gfxinfo " + appName, true);
        String output = adb.output;
        long totFrames = -1;
        long jankyFrames = 0;
        long per50 = 0;
        long per90 = 0;
        long per95 = 0;
        long per99 = 0;
        for(String line : output.split("\n")){
            if( line.contains("Janky frames:")){
                String jankFramesStr = line.split(" ")[2];
                jankyFrames = Long.parseLong(jankFramesStr);
            }
            if( line.contains("Total frames rendered:")){
                String totFramesStr = line.split(" ")[3];
                totFrames = Long.parseLong(totFramesStr);
            }
            if( line.contains("50th")){
                String perString = line.split(" ")[2].replace("ms","");
                per50  = Long.parseLong(perString);
            }
            if( line.contains("90th")){
                String perString = line.split(" ")[2].replace("ms","");
                per90  = Long.parseLong(perString);
            }
            if( line.contains("95th")){
                String perString = line.split(" ")[2].replace("ms","");
                per95  = Long.parseLong(perString);
            }
            if( line.contains("99th")){
                String perString = line.split(" ")[2].replace("ms","");
                per99  = Long.parseLong(perString);
            }


        }
        JankyFrames out = new  JankyFrames(jankyFrames, totFrames);
        out.per50th = per50;
        out.per90th = per90;
        out.per95th = per95;
        out.per99th = per99;
        return out;
    }


    private void UpdateStats(){
        JankyFrames jankper = getFrameStats();
        if (( jankper.totalFrames > 0)){
            jankyFrames.totalFrames = jankper.totalFrames;
            jankyFrames.jankyFrames = jankper.jankyFrames;
            jankyFrames.per50th = jankper.per50th;
            jankyFrames.per90th = jankper.per90th;
            jankyFrames.per95th = jankper.per95th;
            jankyFrames.per99th = jankper.per99th;
        }

    }

    public void resetStats(){
        jankyFrames = new JankyFrames(0,1);
    }

    public void run(){
        while(running){
            UpdateStats();
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

    public JankyFrames getJankyFrames(){
        return jankyFrames;
    }

    }
