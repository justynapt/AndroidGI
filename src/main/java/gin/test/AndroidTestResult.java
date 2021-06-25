package gin.test;


import gin.util.JankyFrames;
import gin.util.TestExecutionMemory;

public class AndroidTestResult {
    private UnitTest test;
    private int repNumber;
    private JankyFrames jankyFrames;
    private long executionMemory;


    private double cpuTime;


    private boolean passed;

    public AndroidTestResult(UnitTest test, int rep){
        this.test = test;
        this.repNumber = rep;
        this.jankyFrames = new JankyFrames(-1,-1);
        this.cpuTime = Double.MAX_VALUE;
        this.executionMemory = Long.MAX_VALUE;
    }
    public JankyFrames getJankiness() {
        return jankyFrames;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setJankiness(JankyFrames jankiness) {
        jankyFrames = jankiness;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public long getExecutionMemory() {
        return executionMemory;
    }

    public void setExecutionMemory(long executionMemory) {
        this.executionMemory = executionMemory;
    }

    public double getCPUTime() {
        return cpuTime;
    }

    public void setCPUTime(double cpuTime) {
        this.cpuTime = cpuTime;
    }

    public UnitTest getTest(){ return test;}


    @Override
    public String toString(){
        return  "Test result for: " + test + "\n" +
                "Success: " +  passed + "\n" +
                "Janky Frames: " + (double)(jankyFrames.jankyFrames)  + "\n" +
                "Total Frames: " + (double)(jankyFrames.totalFrames) + "\n" +
                "jankiness: " + (double)(jankyFrames.jankyFrames) / (double)(jankyFrames.totalFrames);
        //"Max memory: " + executionMemory.maxReading()+"\n"+
                //"Median Memory: " + executionMemory.medianReading()+"\n"+
                //"Execution Time: " + executionTime;

    }
}
