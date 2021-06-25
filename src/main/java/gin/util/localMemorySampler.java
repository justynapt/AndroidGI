package gin.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class localMemorySampler implements Runnable{

    private long procId;
    private long peakVm;
    private ArrayList<Long> samples;
    private boolean running;
    private Thread t;


    public localMemorySampler(){
        procId = -1;
        peakVm = -1;
        samples = new ArrayList();
    }

    public long getPeakVm() {
        return peakVm;
    }

    public void setProcId(long procId) {
        this.procId = procId;
    }

    public void  UpdateStats(){
        String procPath = "/proc/" + procId + "/status";
        Scanner procScan = null;
        try {
            procScan = new Scanner(new File(procPath));
        } catch (FileNotFoundException e) {
            return;
        }
        String memLine = "";
        while (procScan.hasNext()){
            memLine = procScan.nextLine();
            if (memLine.startsWith("VmPeak:")){
                memLine.replace("\t"," ");
                String[] tokens = memLine.split(" ");
                if (tokens.length >= 2) {
                    peakVm = Long.parseLong(tokens[tokens.length - 2]);
                }
            }
            if (memLine.startsWith("VmSize:")){
                memLine.replace("\t"," ");
                String[] tokens = memLine.split(" ");
                if (tokens.length >= 2) {
                    samples.add(Long.parseLong(tokens[tokens.length - 2]));
                }
            }
        }
        procScan.close();

    }

    public void  resetStats(){
        peakVm = -1;
        samples = new ArrayList();
    }


    public void run(){
        while(running){
            try {
                UpdateStats();
                Thread.sleep(1);
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

}



